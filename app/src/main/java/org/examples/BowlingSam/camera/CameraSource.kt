package org.examples.BowlingSam.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.examples.BowlingSam.*
import org.examples.BowlingSam.data.Person
import org.examples.BowlingSam.ml.PoseDetector
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraSource(
    private val surfaceView: SurfaceView,
    private val listener: CameraSourceListener? = null
) {

    companion object {
        //카메라 미리보기 크기 설정
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480

        /** Threshold for confidence score. */
        private const val MIN_CONFIDENCE = .2f
        private const val TAG = "Camera Source"

        //녹화 리코더 비트율 설정
        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000

        //현재 시각을 파일이름으로 설정하고, 기기의 DCIM폴더에 파일을 생성
        private fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            return File(
                Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "VID_${sdf.format(Date())}.$extension")
//            return  File(context.getExternalFilesDir(null), "VID_${sdf.format(Date())}.$extension")
        }

    }

    //카메라 세팅에 필요한 변수들 선언
    private val lock = Any()
    private var detector: PoseDetector? = null
    private var isTrackerEnabled = false
    private var yuvConverter: YuvToRgbConverter = YuvToRgbConverter(surfaceView.context)
    private lateinit var imageBitmap: Bitmap

    /*
        녹화 관련 코드
     */
    //녹화한 비디오가 저장될 파일 선언
    private var outputFile: File = createFile(surfaceView.rootView.context, "mp4")

    //녹화를 진행할 surface를 생성
    private val recorderSurface: Surface by lazy {
        val surface = MediaCodec.createPersistentInputSurface()

        createRecorder(surface).apply {
            prepare()
            release()
        }

        surface
    }

    //녹화할 리코더 변수 선언
    private val recorder: MediaRecorder by lazy { createRecorder(recorderSurface) }

    //리코더 생성 함수
    private fun createRecorder(surface: Surface) = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        //카메라 미리보기의 크기에 맞춰 녹화 진행
        setVideoSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
        if(framesPerSecond > 0) setVideoFrameRate(framesPerSecond)
        //미리 생성한 파일에 녹화한 파일을 저장
        setOutputFile(outputFile.absolutePath)
        setVideoEncodingBitRate(RECORDER_VIDEO_BITRATE)
        setInputSurface(surface)
    }

    //카메라 특징과 기기의 오리엔테이션을 추적하는 변수 선언
    private lateinit var characteristics: CameraCharacteristics
    private lateinit var relativeOrientation: OrientationLiveData

    // 시간 당 프레임과 시간/ 타이머 변수 선언
    private var fpsTimer: Timer? = null
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0
    private var time = 0

    //카메라를 감지하고 설정하고 연결하는 카메라 메니저 생성
    private val cameraManager: CameraManager by lazy {
        val context = surfaceView.context
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    //이미지를 읽는 버퍼를 생성
    private var imageReader: ImageReader? = null

    //카메라 변수 선언
    private var camera: CameraDevice? = null

    //카메라에 대한 유저의 요청을 처리하는 카메라 세션을 선언
    private var session: CameraCaptureSession? = null

    //이미지를 읽는 과정이 처리될 쓰레드 생섬
    private var imageReaderThread: HandlerThread? = null

    //핸들러 생성
    private var imageReaderHandler: Handler? = null
    private var cameraId: String = ""

    //카메라 초기화 함수
    suspend fun initCamera() {
        camera = openCamera(cameraManager, cameraId)
        imageReader =
            ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 3)

        //카메라로 이미지를 전달받을 때 마다 processImage 함수로 사람을 감지하고 비트맵을 출력함
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                if (!::imageBitmap.isInitialized) {
                    imageBitmap =
                        Bitmap.createBitmap(
                            PREVIEW_WIDTH,
                            PREVIEW_HEIGHT,
                            Bitmap.Config.ARGB_8888
                        )
                }
                yuvConverter.yuvToRgb(image, imageBitmap)
                // Create rotated version for portrait display
                val rotateMatrix = Matrix()
                rotateMatrix.postRotate(90.0f)

                val rotatedBitmap = Bitmap.createBitmap(
                    imageBitmap, 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT,
                    rotateMatrix, false
                )
                processImage(rotatedBitmap)

                image.close()
            }
        }, imageReaderHandler)

        imageReader?.surface?.let { surface ->
            session = createSession(listOf(surface, recorderSurface))

            //카메라 미리보기 처리 요청
            val cameraRequest = camera?.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )?.apply {
                addTarget(surface)
            }
            cameraRequest?.build()?.let {
                session?.setRepeatingRequest(it, null, null)
            }

            //비디오 녹화 처리 요청
            val recordRequest = camera?.createCaptureRequest(
                CameraDevice.TEMPLATE_RECORD
            )?.apply {
                addTarget(surface)
                addTarget(recorderSurface)
            }
            recordRequest?.build()?.let {
                session?.setRepeatingRequest(it, null, null)
            }
        }
    }

    //카메라 세션 생성 함수
    private suspend fun createSession(targets: List<Surface>): CameraCaptureSession =
        suspendCancellableCoroutine { cont ->
            camera?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(captureSession: CameraCaptureSession) =
                    cont.resume(captureSession)

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    cont.resumeWithException(Exception("Session error"))
                }
            }, null)
        }

    //카메라를 시작하는 함수
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(manager: CameraManager, cameraId: String): CameraDevice =
        suspendCancellableCoroutine { cont ->
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) = cont.resume(camera)

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    if (cont.isActive) cont.resumeWithException(Exception("Camera error"))
                }
            }, imageReaderHandler)
        }

    //유저가 세팅한 설정대로 카메라를 세팅하는 함수
    fun prepareCamera(): CameraCharacteristics {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            //카메라는 후면 카메라로 동작하도록 설정
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection != null &&
                cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
            ) {
                continue
            }
            this.cameraId = cameraId
        }

        characteristics = cameraManager.getCameraCharacteristics(cameraId)

        return characteristics

    }

    fun getRecordingSurface(): Surface {
        return recorderSurface
    }

    fun setOrientation(orientation: OrientationLiveData) {
        this.relativeOrientation = orientation
    }

    //사용될 AI모델을 설정하는 함수(본 프로젝트에선 MoveNet의 Lightening 버전으로 고정)
    fun setDetector(detector: PoseDetector) {
        synchronized(lock) {
            if (this.detector != null) {
                this.detector?.close()
                this.detector = null
            }
            this.detector = detector
        }
    }

    fun resume() {
        imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
        imageReaderHandler = Handler(imageReaderThread!!.looper)
        fpsTimer = Timer()
        fpsTimer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    framesPerSecond = frameProcessedInOneSecondInterval
                    frameProcessedInOneSecondInterval = 0
                    time++
                }
            },
            0,
            1000
        )
    }

    fun close() {
        session?.close()
        session = null
        camera?.close()
        camera = null
        imageReader?.close()
        imageReader = null
        stopImageReaderThread()
        detector?.close()
        detector = null
        fpsTimer?.cancel()
        fpsTimer = null
        frameProcessedInOneSecondInterval = 0
        framesPerSecond = 0
    }

    //전달받은 이미지에서 사람을 감지하고, 그 결과에 따라서 사람의 관절을 이은 비트맵을 이미지에 출력함
    private fun processImage(bitmap: Bitmap) {
        val persons = mutableListOf<Person>()
        val classificationResult: List<Pair<String, Float>>? = null

        synchronized(lock) {
            detector?.estimatePoses(bitmap)?.let {
                persons.addAll(it)
            }
        }

        frameProcessedInOneSecondInterval++
        if (frameProcessedInOneSecondInterval == 1) {
            listener?.onTimeListener(time)
        }

        if (persons.isNotEmpty()) {
            listener?.onDetectedInfo(persons[0].score, classificationResult)
        }

        visualize(persons, bitmap)

    }

    //비트맵을 그리는 함수
    private fun visualize(persons: List<Person>, bitmap: Bitmap) {

        val outputBitmap = VisualizationUtils.drawBodyKeypoints(
            bitmap,
            persons.filter { it.score > MIN_CONFIDENCE }, isTrackerEnabled
        )

        val holder = surfaceView.holder
        val surfaceCanvas = holder.lockCanvas()
        surfaceCanvas?.let { canvas ->
            val screenWidth: Int
            val screenHeight: Int
            val left: Int
            val top: Int

            if (canvas.height > canvas.width) {
                val ratio = outputBitmap.height.toFloat() / outputBitmap.width
                screenWidth = canvas.width
                left = 0
                screenHeight = (canvas.width * ratio).toInt()
                top = (canvas.height - screenHeight) / 2
            } else {
                val ratio = outputBitmap.width.toFloat() / outputBitmap.height
                screenHeight = canvas.height
                top = 0
                screenWidth = (canvas.height * ratio).toInt()
                left = (canvas.width - screenWidth) / 2
            }
            val right: Int = left + screenWidth
            val bottom: Int = top + screenHeight

            canvas.drawBitmap(
                outputBitmap, Rect(0, 0, outputBitmap.width, outputBitmap.height),
                Rect(left, top, right, bottom), null
            )
            surfaceView.holder.unlockCanvasAndPost(canvas)
        }

    }

    private fun stopImageReaderThread() {
        imageReaderThread?.quitSafely()
        try {
            imageReaderThread?.join()
            imageReaderThread = null
            imageReaderHandler = null
        } catch (e: InterruptedException) {
            Log.d(TAG, e.message.toString())
        }
    }

    interface CameraSourceListener {

        fun onTimeListener(time: Int)

        fun onDetectedInfo(personScore: Float?, poseLabels: List<Pair<String, Float>>?)
    }
}

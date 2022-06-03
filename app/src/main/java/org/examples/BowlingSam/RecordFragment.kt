package org.examples.BowlingSam

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.examples.BowlingSam.camera.CameraSource
import org.examples.BowlingSam.data.Device
import org.examples.BowlingSam.data.Person
import org.examples.BowlingSam.data.VowlingPose
import org.examples.BowlingSam.ml.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

//자세를 구분하기 위한 enum 클래스
enum class PoseType(val pose: String) {
    ADDRESS("ADDRRESS"),
    PUSHAWAY("PUSHAWAY"),
    DOWNSWING("DOWNSWING"),
    BACKSWING("BACKSWING"),
    FORWARDSWING("FORWARDSWING"),
    FOLLOWTHROUGH("FOLLOWTHROUGH")
}

class RecordFragment : Fragment() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val TAG = "Main"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        fun newInstance() : RecordFragment {
            return RecordFragment()
        }

        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480

        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000

        //자세별 점수
        private var addressScore: Float = 0.0f
        private var pushawayScore: Float = 0.0f
        private var downswingScore: Float = 0.0f
        private var backswingScore: Float = 0.0f
        private var forwardswingScore: Float = 0.0f
        private var followthroughScore: Float = 0.0f
        private var scores = arrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        private var avgScore = 0.0f

        //자세별 각도 차이
        private var addressAngleDifferences = FloatArray(4)
        private var pushawayAngleDifferences = FloatArray(5)
        private var downswingAngleDifferences = FloatArray(5)
        private var backswingAngleDifferences = FloatArray(5)
        private var forwardswingAngleDifferences = FloatArray(5)
        private var followthroughAngleDifferences = FloatArray(5)

        //자세별 최고 점수일 때의 비트맵을 저장할 변수
        private var addressResultBitmap: Bitmap? = null
        private var pushawayResultBitmap: Bitmap? = null
        private var downswingResultBitmap: Bitmap? = null
        private var backswingResultBitmap: Bitmap? = null
        private var forwardswingResultBitmap: Bitmap? = null
        private var followthroughResultBitmap: Bitmap? = null

        //자세별 최고 점수일 때의 사람 객체를 저장할 변수
        private var addressPerson: Person? = null
        private var pushawayPerson: Person? = null
        private var downswingPerson: Person? = null
        private var backswingPerson: Person? = null
        private var forwardswingPerson: Person? = null
        private var followthroughPerson: Person? = null

        //모든 변수를 초기화하는 함수
        fun resetRecordedInfo(){
            addressScore = 0.0f
            pushawayScore = 0.0f
            downswingScore = 0.0f
            backswingScore = 0.0f
            forwardswingScore = 0.0f
            followthroughScore = 0.0f

            resetArray(addressAngleDifferences)
            resetArray(pushawayAngleDifferences)
            resetArray(downswingAngleDifferences)
            resetArray(backswingAngleDifferences)
            resetArray(forwardswingAngleDifferences)
            resetArray(followthroughAngleDifferences)

            addressResultBitmap = null
            pushawayResultBitmap = null
            downswingResultBitmap = null
            backswingResultBitmap = null
            forwardswingResultBitmap = null
            followthroughResultBitmap = null

            addressPerson = null
            pushawayPerson = null
            downswingPerson = null
            backswingPerson = null
            forwardswingPerson = null
            followthroughPerson = null

        }

        fun resetArray(array: FloatArray){
            for(i in 0 until array.size)
                array[i] = 0.0f
        }

        fun getAvgScore(array: Array<Float>):Float{
            var sum = 0.0f
            var count = 1
            for(i in 0 until array.size){
                if(array[i] != 0.0f)
                    sum += array[i]
                count++
            }
            return sum/count
        }

    }

    //앱 실행에 활용되는 하드웨어를 기기의 CPU로 설정
    private var device = Device.CPU

    var timerTask: Timer? = null
    var time = 0

    private lateinit var imgPose: ImageView
    private lateinit var tvTime: TextView
    private lateinit var tvPoseName: TextView
    private var cameraSource: CameraSource? = null
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            }
        }

    /*
        비디오 녹화 관련 코드
     */
    private lateinit var outputFile: File
    private lateinit var recorderSurface: Surface
    private lateinit var recorder: MediaRecorder

    //비디오 리코더를 생성
    private fun createRecorder(surface: Surface) = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setVideoSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
        setVideoFrameRate(30)
        setOutputFile(outputFile.absolutePath)
        setVideoEncodingBitRate(RECORDER_VIDEO_BITRATE)
        setInputSurface(surface)
    }

    private lateinit var fileName: String

    //녹화된 비디오를 저장할 파일을 생성하는 함수
    private fun createFile(context: Context, extension: String): File {

        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        val now = sdf.format(Date())
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "${now}.$extension")
        fileName = now
        return file
//            return  File(context.getExternalFilesDir(null), "VID_${sdf.format(Date())}.$extension")
    }

    //영상 촬영 관련 변수들 선언
    private lateinit var safeContext: Context
    private lateinit var recordButton: ImageView
    private lateinit var closeButton: ImageView
    private lateinit var bottomSheet: LinearLayoutCompat
    private lateinit var relativeOrientation: OrientationLiveData
    private lateinit var surfaceView: SurfaceView
    private var isRecording = false

    //음성 가이드 관련 변수 선언
    private var num1 : MediaPlayer? = null
    private var num2 : MediaPlayer? = null
    private var num3 : MediaPlayer? = null
    private var num4 : MediaPlayer? = null
    private var num5 : MediaPlayer? = null
    private var address : MediaPlayer? = null
    private var push : MediaPlayer? = null
    private var down : MediaPlayer? = null
    private var back : MediaPlayer? = null
    private var forward : MediaPlayer? = null
    private var follow : MediaPlayer? = null
    private var end: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTime =  view.rootView.findViewById(R.id.tvTime)
        tvPoseName =  view.rootView.findViewById(R.id.tvPoseName)
        surfaceView =  view.rootView.findViewById(R.id.surfaceView)
        imgPose =  view.rootView.findViewById(R.id.imgPose)

        //Bottom Sheet 위치 설정(MainActivity의 Bottom Navigation Bar와 위치가 충돌되는 문제 예방)
        bottomSheet = view.rootView.findViewById<LinearLayoutCompat>(R.id.score_sheet)
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.isGestureInsetBottomIgnored = true

        // 권한을 다 얻었다면 카메라를 시작함
        if (allPermissionsGranted()) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        recordButton = view.findViewById(R.id.record_button)
        closeButton = view.findViewById(R.id.close_button)

        //촬영 버튼 클릭 리스너 설정
        recordButton.setOnClickListener {
            if (isRecording) {
                //촬영 종료
                showToast("촬영이 완료되었습니다.")
                recorder.stop()
                imgPose.visibility = View.INVISIBLE
                timerTask?.cancel()
                time = 0
                tvTime.text = "시간: 0"

                //생성된 파일을 시스템에 브로드캐스트함
                MediaScannerConnection.scanFile(
                    surfaceView.context, arrayOf(outputFile.absolutePath), null, null
                )

                //결과 화면에 방금 촬영된 유저의 기록 정보를 보냄
                val intent = Intent(safeContext, ResultPopupActivity::class.java)
                //자세별 점수 정보를 보냄
                intent.putExtra("addressScore", addressScore)
                intent.putExtra("pushawayScore", pushawayScore)
                intent.putExtra("downswingScore", downswingScore)
                intent.putExtra("backswingScore", backswingScore)
                intent.putExtra("forwardswingScore", forwardswingScore)
                intent.putExtra("followthroughScore", followthroughScore)
                intent.putExtra("avgScore", avgScore)

                //자세별 모범 자세와의 각도 차이 정보를 보냄
                intent.putExtra("addressAngleDifferences", addressAngleDifferences)
                intent.putExtra("pushawayAngleDifferences", pushawayAngleDifferences)
                intent.putExtra("downswingAngleDifferences", downswingAngleDifferences)
                intent.putExtra("backswingAngleDifferences", backswingAngleDifferences)
                intent.putExtra("forwardswingAngleDifferences", forwardswingAngleDifferences)
                intent.putExtra("followthroughAngleDifferences", followthroughAngleDifferences)

                //자세별 최고 자세일 때의 프레임(이미지)을 보냄
                val addressResultURI = saveBitmapAsFile(PoseType.ADDRESS, addressResultBitmap)
                val pushawayResultURI = saveBitmapAsFile(PoseType.PUSHAWAY, pushawayResultBitmap)
                val downswingResultURI = saveBitmapAsFile(PoseType.DOWNSWING, downswingResultBitmap)
                val backswingResultURI = saveBitmapAsFile(PoseType.BACKSWING, backswingResultBitmap)
                val forwardswingResultURI = saveBitmapAsFile(PoseType.FORWARDSWING, forwardswingResultBitmap)
                val followthroughResultURI = saveBitmapAsFile(PoseType.FOLLOWTHROUGH, followthroughResultBitmap)
                intent.putExtra("addressuri", addressResultURI.toString())
                intent.putExtra("pushawayuri", pushawayResultURI.toString())
                intent.putExtra("downswinguri", downswingResultURI.toString())
                intent.putExtra("backswinguri", backswingResultURI.toString())
                intent.putExtra("forwardswinguri", forwardswingResultURI.toString())
                intent.putExtra("followthroughuri", followthroughResultURI.toString())

                //자세별 최고 자세일 때의 사람 객체 정보를 보냄
                intent.putExtra("addressperson", addressPerson)
                intent.putExtra("pushawayperson", pushawayPerson)
                intent.putExtra("downswingperson", downswingPerson)
                intent.putExtra("backswingperson", backswingPerson)
                intent.putExtra("forwardswingperson", forwardswingPerson)
                intent.putExtra("followthroughperson", followthroughPerson)

                intent.putExtra("filename", fileName)

                startActivity(intent)

                recordButton.setImageResource(R.drawable.ic_record_btn)
            } else {
                //촬영 시작
                outputFile = createFile(safeContext, "mp4")
                recorder = createRecorder(recorderSurface)

                relativeOrientation.value?.let { recorder.setOrientationHint(it) }
                recorder.prepare()
                recorder.start()

                //타이머로 촬영 시간을 계산
                timerTask = kotlin.concurrent.timer(period = 100) {
                    time++

                    val sec = time / 10
                    MoveNet.setTime(sec)
                    val fiveSec = time % 70
                    if (fiveSec == 14 && sec <= 42) {
                        num4?.start()
                    } else if (fiveSec == 28 && sec <= 42) {
                        num3?.start()
                    } else if (fiveSec == 42 && sec <= 42) {
                        num2?.start()
                    } else if (fiveSec == 56 && sec <= 42) {
                        num1?.start()
                    }

                    activity?.runOnUiThread {
                        tvTime?.text = "시간: ${sec}"

                        /**
                         * 0초: 어드레스 자세 가이드 이미지 및 음성 출력
                         * ~7초: 어드레스 자세 측정
                         * 7초: 푸쉬어웨이 자세 가이드 이미지 및 음성 출력
                         * ~14초: 푸쉬어웨이 자세 측정
                         * 14초: 다운스윙 자세 가이드 이미지 및 음성 출력
                         * ~21초: 다운스윙 자세 측정
                         * 21초: 백스윙 자세 가이드 이미지 및 음성 출력
                         * ~28초: 백스윙 자세 측정
                         * 28초: 포워드스윙 자세 가이드 이미지 및 음성 출력
                         * ~35초: 포워드스윙 자세 측정
                         * 35초: 팔로우스루 자세 가이드 이미지 및 음성 출력
                         * ~42초: 팔로우스루 자세 측정
                         * 42초: 엔딩 멘트 출력
                         * */
                        if (sec == 0) {
                            address?.start()
                            imgPose.setImageResource(R.drawable.pose1)
                            imgPose.visibility = View.VISIBLE
                            tvPoseName.text = getString(R.string.tv_poseName, "어드레스")
                        }

                        if(sec == 2){
                            imgPose.visibility = View.INVISIBLE
                        }

                        if (sec == 7) {
                            /** 정확도가 가장 높았던 각도*/
                            val pose_address = VowlingPose(90.0f, 0.0f, 160.0f, 160.0f)
                            val listSize = MoveNet.getRightElbowAngles()[0].size
                            var maxScore = pose_address.getScore(MoveNet.getRightElbowAngles()[0][0], MoveNet.getRightShoulderAngles()[0][0], MoveNet.getRightHipAngles()[0][0], MoveNet.getRightKneeAngles()[0][0])
                            var maxScoreIndex = 0
                            for(i in 1 until listSize) {
                                if(maxScore < pose_address.getScore(MoveNet.getRightElbowAngles()[0][i], MoveNet.getRightShoulderAngles()[0][i], MoveNet.getRightHipAngles()[0][i], MoveNet.getRightKneeAngles()[0][i])) {
                                    maxScore = pose_address.getScore(MoveNet.getRightElbowAngles()[0][i], MoveNet.getRightShoulderAngles()[0][i], MoveNet.getRightHipAngles()[0][i], MoveNet.getRightKneeAngles()[0][i])
                                    maxScoreIndex = i
                                }
                            }
                            addressScore = maxScore
                            addressResultBitmap = MoveNet.getBitmap()[0][maxScoreIndex]
                            addressPerson = MoveNet.getPersonList()[0][maxScoreIndex]

                            addressAngleDifferences[0] = getAngleDifference(pose_address.getREA(), MoveNet.getRightElbowAngles()[0][maxScoreIndex])
                            addressAngleDifferences[1] = getAngleDifference(pose_address.getRSA(), MoveNet.getRightShoulderAngles()[0][maxScoreIndex])
                            addressAngleDifferences[2] = getAngleDifference(pose_address.getRHA(), MoveNet.getRightHipAngles()[0][maxScoreIndex])
                            addressAngleDifferences[3] = getAngleDifference(pose_address.getRKA(), MoveNet.getRightKneeAngles()[0][maxScoreIndex])

                            push?.start()
                            imgPose.setImageResource(R.drawable.pose2)
                            imgPose.visibility = View.VISIBLE
                            tvPoseName.text = getString(R.string.tv_poseName, "푸시어웨이")
                            scores[0] = addressScore
                            avgScore = getAvgScore(scores)
                        }

                        if(sec == 9){
                            imgPose.visibility = View.INVISIBLE
                        }

                        if (sec == 14) {
                            val pose_pushaway = VowlingPose(105.0f, 15.0f, 150.0f, 150.0f, 150.0f)
                            val listSize = MoveNet.getRightElbowAngles()[1].size
                            var maxScore = pose_pushaway.getScore(MoveNet.getRightElbowAngles()[1][0], MoveNet.getRightShoulderAngles()[1][0], MoveNet.getRightHipAngles()[1][0], MoveNet.getRightKneeAngles()[1][0], MoveNet.getLeftKneeAngles()[0][0])
                            var maxScoreIndex = 0
                            for(i in 1 until listSize) {
                                if(maxScore < pose_pushaway.getScore(MoveNet.getRightElbowAngles()[1][i], MoveNet.getRightShoulderAngles()[1][i], MoveNet.getRightHipAngles()[1][i], MoveNet.getRightKneeAngles()[1][i], MoveNet.getLeftKneeAngles()[0][i])) {
                                    maxScore = pose_pushaway.getScore(MoveNet.getRightElbowAngles()[1][i], MoveNet.getRightShoulderAngles()[1][i], MoveNet.getRightHipAngles()[1][i], MoveNet.getRightKneeAngles()[1][i], MoveNet.getLeftKneeAngles()[0][i])
                                    maxScoreIndex = i
                                }
                            }
                            pushawayScore = maxScore
                            pushawayResultBitmap = MoveNet.getBitmap()[1][maxScoreIndex]
                            pushawayPerson = MoveNet.getPersonList()[1][maxScoreIndex]

                            pushawayAngleDifferences[0] = getAngleDifference(pose_pushaway.getREA(), MoveNet.getRightElbowAngles()[1][maxScoreIndex])
                            pushawayAngleDifferences[1] = getAngleDifference(pose_pushaway.getRSA(), MoveNet.getRightShoulderAngles()[1][maxScoreIndex])
                            pushawayAngleDifferences[2] = getAngleDifference(pose_pushaway.getRHA(), MoveNet.getRightHipAngles()[1][maxScoreIndex])
                            pushawayAngleDifferences[3] = getAngleDifference(pose_pushaway.getRKA(), MoveNet.getRightKneeAngles()[1][maxScoreIndex])
                            pushawayAngleDifferences[4] = getAngleDifference(pose_pushaway.getLKA(), MoveNet.getLeftKneeAngles()[0][maxScoreIndex])

                            down?.start()
                            imgPose.setImageResource(R.drawable.pose3)
                            imgPose.visibility = View.VISIBLE
                            tvPoseName.text = getString(R.string.tv_poseName, "다운스윙")
                            scores[1] = pushawayScore
                            avgScore = getAvgScore(scores)
                        }

                        if(sec == 16){
                            imgPose.visibility = View.INVISIBLE
                        }

                        if (sec == 21) {
                            val pose_downswing = VowlingPose(180.0f, 10.0f, 170.0f, 150.0f, 150.0f)
                            val listSize = MoveNet.getRightElbowAngles()[2].size
                            var maxScore = pose_downswing.getScore(MoveNet.getRightElbowAngles()[2][0], MoveNet.getRightShoulderAngles()[2][0], MoveNet.getRightHipAngles()[2][0], MoveNet.getRightKneeAngles()[2][0], MoveNet.getLeftKneeAngles()[1][0])
                            var maxScoreIndex = 0
                            for(i in 1 until listSize) {
                                if(maxScore < pose_downswing.getScore(MoveNet.getRightElbowAngles()[2][i], MoveNet.getRightShoulderAngles()[2][i], MoveNet.getRightHipAngles()[2][i], MoveNet.getRightKneeAngles()[2][i], MoveNet.getLeftKneeAngles()[1][i])) {
                                    maxScore = pose_downswing.getScore(MoveNet.getRightElbowAngles()[2][i], MoveNet.getRightShoulderAngles()[2][i], MoveNet.getRightHipAngles()[2][i], MoveNet.getRightKneeAngles()[2][i], MoveNet.getLeftKneeAngles()[1][i])
                                    maxScoreIndex = i
                                }
                            }
                            downswingScore = maxScore
                            downswingResultBitmap = MoveNet.getBitmap()[2][maxScoreIndex]
                            downswingPerson = MoveNet.getPersonList()[2][maxScoreIndex]

                            downswingAngleDifferences[0] = getAngleDifference(pose_downswing.getREA(), MoveNet.getRightElbowAngles()[2][maxScoreIndex])
                            downswingAngleDifferences[1] = getAngleDifference(pose_downswing.getRSA(), MoveNet.getRightShoulderAngles()[2][maxScoreIndex])
                            downswingAngleDifferences[2] = getAngleDifference(pose_downswing.getRHA(), MoveNet.getRightHipAngles()[2][maxScoreIndex])
                            downswingAngleDifferences[3] = getAngleDifference(pose_downswing.getRKA(), MoveNet.getRightKneeAngles()[2][maxScoreIndex])
                            downswingAngleDifferences[4] = getAngleDifference(pose_downswing.getLKA(), MoveNet.getLeftKneeAngles()[1][maxScoreIndex])

                            back?.start()
                            imgPose.setImageResource(R.drawable.pose4)
                            imgPose.visibility = View.VISIBLE
                            tvPoseName.text = getString(R.string.tv_poseName, "백스윙")
                            scores[2] = downswingScore
                            avgScore = getAvgScore(scores)
                        }

                        if(sec == 23){
                            imgPose.visibility = View.INVISIBLE
                        }

                        if (sec == 28) {
                            val pose_backswing = VowlingPose(180.0f, 60.0f, 110.0f, 130.0f, 130.0f)
                            val listSize = MoveNet.getRightElbowAngles()[3].size
                            var maxScore = pose_backswing.getScore(MoveNet.getRightElbowAngles()[3][0], MoveNet.getRightShoulderAngles()[3][0], MoveNet.getRightHipAngles()[3][0], MoveNet.getRightKneeAngles()[3][0], MoveNet.getLeftKneeAngles()[2][0])
                            var maxScoreIndex = 0
                            for(i in 1 until listSize) {
                                if(maxScore < pose_backswing.getScore(MoveNet.getRightElbowAngles()[3][i], MoveNet.getRightShoulderAngles()[3][i], MoveNet.getRightHipAngles()[3][i], MoveNet.getRightKneeAngles()[3][i], MoveNet.getLeftKneeAngles()[2][i])) {
                                    maxScore = pose_backswing.getScore(MoveNet.getRightElbowAngles()[3][i], MoveNet.getRightShoulderAngles()[3][i], MoveNet.getRightHipAngles()[3][i], MoveNet.getRightKneeAngles()[3][i], MoveNet.getLeftKneeAngles()[2][i])
                                    maxScoreIndex = i
                                }
                            }
                            backswingScore = maxScore
                            backswingResultBitmap = MoveNet.getBitmap()[3][maxScoreIndex]
                            backswingPerson = MoveNet.getPersonList()[3][maxScoreIndex]

                            backswingAngleDifferences[0] = getAngleDifference(pose_backswing.getREA(), MoveNet.getRightElbowAngles()[3][maxScoreIndex])
                            backswingAngleDifferences[1] = getAngleDifference(pose_backswing.getRSA(), MoveNet.getRightShoulderAngles()[3][maxScoreIndex])
                            backswingAngleDifferences[2] = getAngleDifference(pose_backswing.getRHA(), MoveNet.getRightHipAngles()[3][maxScoreIndex])
                            backswingAngleDifferences[3] = getAngleDifference(pose_backswing.getRKA(), MoveNet.getRightKneeAngles()[3][maxScoreIndex])
                            backswingAngleDifferences[4] = getAngleDifference(pose_backswing.getLKA(), MoveNet.getLeftKneeAngles()[2][maxScoreIndex])

                            forward?.start()
                            imgPose.setImageResource(R.drawable.pose5)
                            imgPose.visibility = View.VISIBLE
                            tvPoseName.text = getString(R.string.tv_poseName, "포워드스윙")
                            scores[3] = backswingScore
                            avgScore = getAvgScore(scores)
                        }

                        if(sec == 30){
                            imgPose.visibility = View.INVISIBLE
                        }

                        if (sec == 35) {
                            val pose_forwardswing = VowlingPose(180.0f, 30.0f, 175.0f, 170.0f, 80.0f)
                            val listSize = MoveNet.getRightElbowAngles()[4].size
                            var maxScore = pose_forwardswing.getScore(MoveNet.getRightElbowAngles()[4][0], MoveNet.getRightShoulderAngles()[4][0], MoveNet.getRightHipAngles()[4][0], MoveNet.getRightKneeAngles()[4][0], MoveNet.getLeftKneeAngles()[3][0])
                            var maxScoreIndex = 0
                            for(i in 1 until listSize) {
                                if(maxScore < pose_forwardswing.getScore(MoveNet.getRightElbowAngles()[4][i], MoveNet.getRightShoulderAngles()[4][i], MoveNet.getRightHipAngles()[4][i], MoveNet.getRightKneeAngles()[4][i], MoveNet.getLeftKneeAngles()[3][i])) {
                                    maxScore = pose_forwardswing.getScore(MoveNet.getRightElbowAngles()[4][i], MoveNet.getRightShoulderAngles()[4][i], MoveNet.getRightHipAngles()[4][i], MoveNet.getRightKneeAngles()[4][i], MoveNet.getLeftKneeAngles()[3][i])
                                    maxScoreIndex = i
                                }
                            }
                            forwardswingScore = maxScore
                            forwardswingResultBitmap = MoveNet.getBitmap()[4][maxScoreIndex]
                            forwardswingPerson = MoveNet.getPersonList()[4][maxScoreIndex]

                            forwardswingAngleDifferences[0] = getAngleDifference(pose_forwardswing.getREA(), MoveNet.getRightElbowAngles()[4][maxScoreIndex])
                            forwardswingAngleDifferences[1] = getAngleDifference(pose_forwardswing.getRSA(), MoveNet.getRightShoulderAngles()[4][maxScoreIndex])
                            forwardswingAngleDifferences[2] = getAngleDifference(pose_forwardswing.getRHA(), MoveNet.getRightHipAngles()[4][maxScoreIndex])
                            forwardswingAngleDifferences[3] = getAngleDifference(pose_forwardswing.getRKA(), MoveNet.getRightKneeAngles()[4][maxScoreIndex])
                            forwardswingAngleDifferences[4] = getAngleDifference(pose_forwardswing.getLKA(), MoveNet.getLeftKneeAngles()[3][maxScoreIndex])

                            follow?.start()
                            imgPose.setImageResource(R.drawable.pose6)
                            imgPose.visibility = View.VISIBLE
                            tvPoseName.text = getString(R.string.tv_poseName, "팔로스루")
                            scores[4] = forwardswingScore
                            avgScore = getAvgScore(scores)
                        }

                        if(sec == 37){
                            imgPose.visibility = View.INVISIBLE
                        }

                        if(sec==42){
                            val pose_followthrough = VowlingPose(160.0f, 160.0f, 175.0f, 180.0f, 100.0f)
                            val listSize = MoveNet.getRightElbowAngles()[5].size
                            var maxScore = pose_followthrough.getScore(MoveNet.getRightElbowAngles()[5][0], MoveNet.getRightShoulderAngles()[5][0], MoveNet.getRightHipAngles()[5][0], MoveNet.getRightKneeAngles()[5][0], MoveNet.getLeftKneeAngles()[4][0])
                            var maxScoreIndex = 0
                            for(i in 1 until listSize) {
                                if(maxScore < pose_followthrough.getScore(MoveNet.getRightElbowAngles()[5][i], MoveNet.getRightShoulderAngles()[5][i], MoveNet.getRightHipAngles()[5][i], MoveNet.getRightKneeAngles()[5][i], MoveNet.getLeftKneeAngles()[4][i])) {
                                    maxScore = pose_followthrough.getScore(MoveNet.getRightElbowAngles()[5][i], MoveNet.getRightShoulderAngles()[5][i], MoveNet.getRightHipAngles()[5][i], MoveNet.getRightKneeAngles()[5][i], MoveNet.getLeftKneeAngles()[4][i])
                                    maxScoreIndex = i
                                }
                            }
                            followthroughScore = maxScore
                            followthroughResultBitmap = MoveNet.getBitmap()[5][maxScoreIndex]
                            followthroughPerson = MoveNet.getPersonList()[5][maxScoreIndex]

                            followthroughAngleDifferences[0] = getAngleDifference(pose_followthrough.getREA(), MoveNet.getRightElbowAngles()[5][maxScoreIndex])
                            followthroughAngleDifferences[1] = getAngleDifference(pose_followthrough.getRSA(), MoveNet.getRightShoulderAngles()[5][maxScoreIndex])
                            followthroughAngleDifferences[2] = getAngleDifference(pose_followthrough.getRHA(), MoveNet.getRightHipAngles()[5][maxScoreIndex])
                            followthroughAngleDifferences[3] = getAngleDifference(pose_followthrough.getRKA(), MoveNet.getRightKneeAngles()[5][maxScoreIndex])
                            followthroughAngleDifferences[4] = getAngleDifference(pose_followthrough.getLKA(), MoveNet.getLeftKneeAngles()[4][maxScoreIndex])

                            end?.start()
                            scores[5] = followthroughScore
                            avgScore = getAvgScore(scores)
                        }
                    }
                }

                recordButton.setImageResource(R.drawable.ic_record_btn_red)

                showToast("촬영이 시작되었습니다.")

            }

            isRecording = !isRecording
        }

        //닫기 버튼 클릭 리스너 설정
        closeButton.setOnClickListener {
            //홈 프레그먼트로 이동
            val navigation: BottomNavigationView = view.rootView.findViewById(R.id.bottom_nav)
            navigation.selectedItemId = R.id.menu_home
        }

    }

    //비트맵을 파일로 저장하는 함수
    private fun saveBitmapAsFile(pose: PoseType, bitmap: Bitmap?): Uri? {

        val wrapper = ContextWrapper(requireActivity().applicationContext)
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)

        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file, "${sdf.format(Date())}${pose.pose}.jpg")
        try{
            val stream: OutputStream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        }catch (e: Exception){
            null
        }

        return Uri.parse(file.absolutePath)
    }

    private fun getAngleDifference(correctAngle: Float, myAngle: Float): Float{
        return myAngle - correctAngle
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    //뷰가 생성되었을 때
    //프레그먼트와 레이아웃을 연결해주는 파트
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_record, container, false)

        return view
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        timerTask?.cancel()
        timerTask = null
        time = 0
        super.onPause()
    }

    override fun onDestroy() {
        cameraSource?.close()
        cameraSource = null
        recorder.reset()
        recorder.release()
        timerTask?.cancel()
        timerTask = null
        time = 0
        super.onDestroy()
    }

    //카메라 권한을 획득했는지 확인
    private fun isCameraPermissionGranted(): Boolean {
        return  activity?.checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    //카메라를 시작하는 함수
    private fun openCamera() {
        num1 = MediaPlayer.create(this.safeContext, R.raw.one)
        num2 = MediaPlayer.create(this.safeContext, R.raw.two)
        num3 = MediaPlayer.create(this.safeContext, R.raw.three)
        num4 = MediaPlayer.create(this.safeContext, R.raw.four)
        num5 = MediaPlayer.create(this.safeContext, R.raw.five)
        address = MediaPlayer.create(this.safeContext, R.raw.address)
        push = MediaPlayer.create(this.safeContext, R.raw.push_away)
        back = MediaPlayer.create(this.safeContext, R.raw.back_swing)
        follow = MediaPlayer.create(this.safeContext, R.raw.follow_throw)
        forward = MediaPlayer.create(this.safeContext, R.raw.forward)
        down = MediaPlayer.create(this.safeContext, R.raw.down_swing)
        end = MediaPlayer.create(this.safeContext, R.raw.end)

        if (isCameraPermissionGranted()) {

            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, object : CameraSource.CameraSourceListener {

                        override fun onTimeListener(time: Int) {
                        }

                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?

                        ) {
                        }

                    }).apply {
                        //기기의 오리엔테이션에 따라 녹화되는 비디오의 방향도 바꿈
                        relativeOrientation = OrientationLiveData(requireContext(), prepareCamera()).apply {
                            observe(viewLifecycleOwner, Observer {
                                    orientation -> Log.d(TAG, "Orientation changed: $orientation")
                            })
                        }
                        recorderSurface = getRecordingSurface()
                        setOrientation(relativeOrientation)

                    }

                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }

    }

    //사람 자세 인식에 사용될 AI모델을 MoveNet의 Lightning 버전으로 설정
    private fun createPoseEstimator() {
        cameraSource?.setDetector(MoveNet.create(this.safeContext, device, ModelType.Lightning))
    }

    private fun showToast(message: String) {
        Toast.makeText(this.safeContext, message, Toast.LENGTH_LONG).show()
    }

    //요구한 모든 권한을 획득 했는지 확인
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            safeContext, it) == PackageManager.PERMISSION_GRANTED
    }

    //카메라 및 저장 권한 획득 여부에 따른 결과 처리 함수
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                openCamera()
            } else {
                Toast.makeText(safeContext,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}

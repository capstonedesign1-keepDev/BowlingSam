/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package org.examples.BowlingSam.ml

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.examples.BowlingSam.data.*
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.collections.ArrayList
import kotlin.math.*


private var rightElbowAngle1 = ArrayList<Float>()
private var rightElbowAngle2 = ArrayList<Float>()
private var rightElbowAngle3 = ArrayList<Float>()
private var rightElbowAngle4 = ArrayList<Float>()
private var rightElbowAngle5 = ArrayList<Float>()
private var rightElbowAngle6 = ArrayList<Float>()

private var rightShoulderAngle1 = ArrayList<Float>()
private var rightShoulderAngle2 = ArrayList<Float>()
private var rightShoulderAngle3 = ArrayList<Float>()
private var rightShoulderAngle4 = ArrayList<Float>()
private var rightShoulderAngle5 = ArrayList<Float>()
private var rightShoulderAngle6 = ArrayList<Float>()

private var rightHipAngle1 = ArrayList<Float>()
private var rightHipAngle2 = ArrayList<Float>()
private var rightHipAngle3 = ArrayList<Float>()
private var rightHipAngle4 = ArrayList<Float>()
private var rightHipAngle5 = ArrayList<Float>()
private var rightHipAngle6 = ArrayList<Float>()

private var rightKneeAngle1 = ArrayList<Float>()
private var rightKneeAngle2 = ArrayList<Float>()
private var rightKneeAngle3 = ArrayList<Float>()
private var rightKneeAngle4 = ArrayList<Float>()
private var rightKneeAngle5 = ArrayList<Float>()
private var rightKneeAngle6 = ArrayList<Float>()

private var leftKneeAngle1 = ArrayList<Float>()
private var leftKneeAngle2 = ArrayList<Float>()
private var leftKneeAngle3 = ArrayList<Float>()
private var leftKneeAngle4 = ArrayList<Float>()
private var leftKneeAngle5 = ArrayList<Float>()

var addressBitmapList = ArrayList<Bitmap>()
var pushawayBitmapList = ArrayList<Bitmap>()
var downswingBitmapList = ArrayList<Bitmap>()
var backswingBitmapList = ArrayList<Bitmap>()
var forwardswingBitmapList = ArrayList<Bitmap>()
var followthroughBitmapList = ArrayList<Bitmap>()

var addressPersonList = ArrayList<Person>()
var pushawayPersonList = ArrayList<Person>()
var downswingPersonList = ArrayList<Person>()
var backswingPersonList = ArrayList<Person>()
var forwardswingPersonList = ArrayList<Person>()
var followthroughPersonList = ArrayList<Person>()

private var rightElbowAngles = arrayOf(rightElbowAngle1, rightElbowAngle2, rightElbowAngle3,
    rightElbowAngle4, rightElbowAngle5, rightElbowAngle6)

private var rightShoulderAngles = arrayOf(rightShoulderAngle1, rightShoulderAngle2, rightShoulderAngle3,
    rightShoulderAngle4, rightShoulderAngle5, rightShoulderAngle6)
private var rightHipAngles = arrayOf(rightHipAngle1, rightHipAngle2, rightHipAngle3,
    rightHipAngle4, rightHipAngle5, rightHipAngle6)
private var rightKneeAngles = arrayOf(rightKneeAngle1, rightKneeAngle2, rightKneeAngle3,
    rightKneeAngle4, rightKneeAngle5, rightKneeAngle6)
private var leftKneeAngles = arrayOf(leftKneeAngle1, leftKneeAngle2, leftKneeAngle3,
    leftKneeAngle4, leftKneeAngle5)

var bitmapArray = arrayOf(addressBitmapList, pushawayBitmapList, downswingBitmapList,
    backswingBitmapList, forwardswingBitmapList, followthroughBitmapList)

var personListArray = arrayOf(addressPersonList, pushawayPersonList, downswingPersonList, backswingPersonList, forwardswingPersonList, followthroughPersonList)

private var biggestScore = FloatArray(6)

enum class ModelType {
    Lightning,
    Thunder
}

class MoveNet(private val interpreter: Interpreter, private var gpuDelegate: GpuDelegate?) :
    PoseDetector {

    companion object {
        private const val MIN_CROP_KEYPOINT_SCORE = .2f
        private const val CPU_NUM_THREADS = 4

        // Parameters that control how large crop region should be expanded from previous frames'
        // body keypoints.
        private const val TORSO_EXPANSION_RATIO = 1.9f
        private const val BODY_EXPANSION_RATIO = 1.2f

        // TFLite file names.
        private const val LIGHTNING_FILENAME = "movenet_lightning.tflite"
        private const val THUNDER_FILENAME = "movenet_thunder.tflite"

        private var time = 0
//        private var timer: Timer? = null

        // allow specifying model type.
        fun create(context: Context, device: Device, modelType: ModelType): MoveNet {
            val options = Interpreter.Options()
            var gpuDelegate: GpuDelegate? = null
            options.setNumThreads(CPU_NUM_THREADS)
            when (device) {
                Device.CPU -> {
                }
                Device.GPU -> {
                    gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                }
                Device.NNAPI -> options.setUseNNAPI(true)
            }

            return MoveNet(
                Interpreter(
                    FileUtil.loadMappedFile(
                        context,
                        if (modelType == ModelType.Lightning) LIGHTNING_FILENAME
                        else THUNDER_FILENAME
                    ), options
                ),
                gpuDelegate
            )
        }

        // default to lightning.
        fun create(context: Context, device: Device): MoveNet =
            create(context, device, ModelType.Lightning)

        fun getBiggestScore(poseNum: Int): Float{
            return biggestScore[poseNum]
        }

        fun resetInfo(){
            resetAngleArray(rightElbowAngles)
            resetAngleArray(rightShoulderAngles)
            resetAngleArray(rightHipAngles)
            resetAngleArray(rightKneeAngles)
            resetAngleArray(leftKneeAngles)
            resetBitmapArray(bitmapArray)
            resetPersonList(personListArray)
        }

        fun resetPersonList(array: Array<ArrayList<Person>>) {
            for(i in 0 until array.size)
                array[i].clear()
        }

        fun resetAngleArray(array:Array<ArrayList<Float>>){
            for(i in 0 until array.size)
                array[i].clear()
        }

        fun resetBitmapArray(array:Array<ArrayList<Bitmap>>){
            for(i in 0 until array.size)
                array[i].clear()
        }

        fun getRightElbowAngles(): Array<ArrayList<Float>>{
            return rightElbowAngles
        }

        fun getRightShoulderAngles(): Array<ArrayList<Float>>{
            return rightShoulderAngles
        }

        fun getRightHipAngles(): Array<ArrayList<Float>>{
            return rightHipAngles
        }

        fun getRightKneeAngles(): Array<ArrayList<Float>>{
            return rightKneeAngles
        }

        fun getLeftKneeAngles(): Array<ArrayList<Float>>{
            return leftKneeAngles
        }

        fun getBitmap(): Array<ArrayList<Bitmap>> {
            return bitmapArray
        }

        fun getPersonList(): Array<ArrayList<Person>> {
            return personListArray
        }

        fun setTime(time: Int) {
            this.time = time
        }

        fun getTime(): Int {
            return this.time
        }
    }

    private var cropRegion: RectF? = null
    private var lastInferenceTimeNanos: Long = -1
    private val inputWidth = interpreter.getInputTensor(0).shape()[1]
    private val inputHeight = interpreter.getInputTensor(0).shape()[2]
    private var outputShape: IntArray = interpreter.getOutputTensor(0).shape()


    //    override fun estimatePoses(bitmap: Bitmap): List<Person> {
    override fun estimatePoses(bitmap: Bitmap): List<Person> {
        val inferenceStartTimeNanos = SystemClock.elapsedRealtimeNanos()
        if (cropRegion == null) {
            cropRegion = initRectF(bitmap.width, bitmap.height)
        }
        var totalScore = 0f

        val numKeyPoints = outputShape[2]
        val keyPoints = mutableListOf<KeyPoint>()

        cropRegion?.run {
            val rect = RectF(
                (left * bitmap.width),
                (top * bitmap.height),
                (right * bitmap.width),
                (bottom * bitmap.height)
            )
            val detectBitmap = Bitmap.createBitmap(
                rect.width().toInt(),
                rect.height().toInt(),
                Bitmap.Config.ARGB_8888
            )
            Canvas(detectBitmap).drawBitmap(
                bitmap,
                -rect.left,
                -rect.top,
                null
            )
            val inputTensor = processInputImage(detectBitmap, inputWidth, inputHeight)
            val outputTensor = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)
            val widthRatio = detectBitmap.width.toFloat() / inputWidth
            val heightRatio = detectBitmap.height.toFloat() / inputHeight

            val positions = mutableListOf<Float>()

            inputTensor?.let { input ->
                interpreter.run(input.buffer, outputTensor.buffer.rewind())
                val output = outputTensor.floatArray
                for (idx in 0 until numKeyPoints) {
                    val x = output[idx * 3 + 1] * inputWidth * widthRatio
                    val y = output[idx * 3 + 0] * inputHeight * heightRatio

                    positions.add(x)
                    positions.add(y)
                    val score = output[idx * 3 + 2]
                    keyPoints.add(
                        KeyPoint(
                            BodyPart.fromInt(idx),
                            PointF(
                                x,
                                y
                            ),
                            score
                        )
                    )
                    totalScore += score
                }
            }
            val matrix = Matrix()
            val points = positions.toFloatArray()

            matrix.postTranslate(rect.left, rect.top)
            matrix.mapPoints(points)
            keyPoints.forEachIndexed { index, keyPoint ->
                keyPoint.coordinate =
                    PointF(
                        points[index * 2],
                        points[index * 2 + 1]
                    )
            }
            // new crop region
            cropRegion = determineRectF(keyPoints, bitmap.width, bitmap.height)
        }
        lastInferenceTimeNanos =
            SystemClock.elapsedRealtimeNanos() - inferenceStartTimeNanos
//        Log.d("TAG", "setTime: $time")

        /** 부위별 각도*/
        val rightElbowAngleArray = arrayOf(keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate,
            keyPoints[BodyPart.RIGHT_ELBOW.position].coordinate,keyPoints[BodyPart.RIGHT_WRIST.position].coordinate)
        val rightShoulderAngleArray = arrayOf(keyPoints[BodyPart.RIGHT_ELBOW.position].coordinate,
            keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate,keyPoints[BodyPart.RIGHT_HIP.position].coordinate)
        val rightHipAngleArray = arrayOf(keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate,
            keyPoints[BodyPart.RIGHT_HIP.position].coordinate,keyPoints[BodyPart.RIGHT_KNEE.position].coordinate)
        val leftKneeAngleArray = arrayOf(keyPoints[BodyPart.LEFT_HIP.position].coordinate,
            keyPoints[BodyPart.LEFT_KNEE.position].coordinate,keyPoints[BodyPart.LEFT_ANKLE.position].coordinate)
        val rightKneeAngleArray = arrayOf(keyPoints[BodyPart.RIGHT_HIP.position].coordinate,
            keyPoints[BodyPart.RIGHT_KNEE.position].coordinate,keyPoints[BodyPart.RIGHT_ANKLE.position].coordinate)

        val rightElbowAngle = getAngle(rightElbowAngleArray)
        val rightShoulderAngle = getAngle(rightShoulderAngleArray)
        val rightHipAngle = getAngle(rightHipAngleArray)
        val leftKneeAngle = getAngle(leftKneeAngleArray)
        val rightKneeAngle = getAngle(rightKneeAngleArray)

        /** 각 자세 인스턴스 생성*/
        val pose_address = VowlingPose(90.0f, 0.0f, 160.0f, 160.0f)
        val pose_pushaway = VowlingPose(105.0f, 15.0f, 150.0f, 150.0f, 150.0f)
        val pose_downswing = VowlingPose(180.0f, 10.0f, 170.0f, 150.0f, 150.0f)
        val pose_backswing = VowlingPose(180.0f, 60.0f, 110.0f, 130.0f, 130.0f)
        val pose_forwardswing = VowlingPose(180.0f, 30.0f, 175.0f, 170.0f, 80.0f)
        val pose_followthrough = VowlingPose(160.0f, 160.0f, 175.0f, 180.0f, 100.0f)

        if(time < 7) {

            Log.d("TAG", "estimatePoses: ${rightElbowAngle}")
            rightElbowAngles[0].add(rightElbowAngle)
            rightShoulderAngles[0].add(rightShoulderAngle)
            rightHipAngles[0].add(rightHipAngle)
            rightKneeAngles[0].add(rightKneeAngle)
            addressBitmapList.add(bitmap)
            addressPersonList.add(Person(keyPoints = keyPoints, score = 100.0f))
        }

        else if(time < 14) {
            rightElbowAngles[1].add(rightElbowAngle)
            rightShoulderAngles[1].add(rightShoulderAngle)
            rightHipAngles[1].add(rightHipAngle)
            rightKneeAngles[1].add(rightKneeAngle)
            leftKneeAngles[0].add(leftKneeAngle)
            pushawayBitmapList.add(bitmap)
            pushawayPersonList.add(Person(keyPoints = keyPoints, score = 100.0f))
        }

        else if(time < 21) {
            rightElbowAngles[2].add(rightElbowAngle)
            rightShoulderAngles[2].add(rightShoulderAngle)
            rightHipAngles[2].add(rightHipAngle)
            rightKneeAngles[2].add(rightKneeAngle)
            leftKneeAngles[1].add(leftKneeAngle)
            downswingBitmapList.add(bitmap)
            downswingPersonList.add(Person(keyPoints = keyPoints, score = 100.0f))
        }

        else if(time < 28) {
            rightElbowAngles[3].add(rightElbowAngle)
            rightShoulderAngles[3].add(rightShoulderAngle)
            rightHipAngles[3].add(rightHipAngle)
            rightKneeAngles[3].add(rightKneeAngle)
            leftKneeAngles[2].add(leftKneeAngle)
            backswingBitmapList.add(bitmap)
            backswingPersonList.add(Person(keyPoints = keyPoints, score = 100.0f))
        }

        else if(time < 35) {
            rightElbowAngles[4].add(rightElbowAngle)
            rightShoulderAngles[4].add(rightShoulderAngle)
            rightHipAngles[4].add(rightHipAngle)
            rightKneeAngles[4].add(rightKneeAngle)
            leftKneeAngles[3].add(leftKneeAngle)
            forwardswingBitmapList.add(bitmap)
            forwardswingPersonList.add(Person(keyPoints = keyPoints, score = 100.0f))
        }

        else if(time < 42){
            rightElbowAngles[5].add(rightElbowAngle)
            rightShoulderAngles[5].add(rightShoulderAngle)
            rightHipAngles[5].add(rightHipAngle)
            rightKneeAngles[5].add(rightKneeAngle)
            leftKneeAngles[4].add(leftKneeAngle)
            followthroughBitmapList.add(bitmap)
            followthroughPersonList.add(Person(keyPoints = keyPoints, score = 100.0f))
        }



        return listOf(Person(keyPoints = keyPoints, score = 100.0f))
    }

    fun getAngle(angleArray: Array<PointF>): Float {
        val p1: Float = hypot(((angleArray[0].x) - (angleArray[1].x)), ((angleArray[0].y) - (angleArray[1].y)))
        val p2: Float = hypot(((angleArray[1].x) - (angleArray[2].x)), ((angleArray[1].y) - (angleArray[2].y)))
        val p3: Float = hypot(((angleArray[2].x) - (angleArray[0].x)), ((angleArray[2].y) - (angleArray[0].y)))
        val radian: Float = acos((p1 * p1 + p2 * p2 - p3 * p3) / (2 * p1 * p2))
        return radian / PI.toFloat() * 180.0f
    }

    override fun lastInferenceTimeNanos(): Long = lastInferenceTimeNanos

    override fun close() {
        gpuDelegate?.close()
        interpreter.close()
        cropRegion = null
//        time = 0
//        timer = null
    }

    /**
     * Prepare input image for detection
     */
    private fun processInputImage(bitmap: Bitmap, inputWidth: Int, inputHeight: Int): TensorImage? {
        val width: Int = bitmap.width
        val height: Int = bitmap.height

        val size = if (height > width) width else height
        val imageProcessor = ImageProcessor.Builder().apply {
            add(ResizeWithCropOrPadOp(size, size))
            add(ResizeOp(inputWidth, inputHeight, ResizeOp.ResizeMethod.BILINEAR))
        }.build()
        val tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    /**
     * Defines the default crop region.
     * The function provides the initial crop region (pads the full image from both
     * sides to make it a square image) when the algorithm cannot reliably determine
     * the crop region from the previous frame.
     */
    private fun initRectF(imageWidth: Int, imageHeight: Int): RectF {
        val xMin: Float
        val yMin: Float
        val width: Float
        val height: Float
        if (imageWidth > imageHeight) {
            width = 1f
            height = imageWidth.toFloat() / imageHeight
            xMin = 0f
            yMin = (imageHeight / 2f - imageWidth / 2f) / imageHeight
        } else {
            height = 1f
            width = imageHeight.toFloat() / imageWidth
            yMin = 0f
            xMin = (imageWidth / 2f - imageHeight / 2) / imageWidth
        }
        return RectF(
            xMin,
            yMin,
            xMin + width,
            yMin + height
        )
    }


    /**
     * Checks whether there are enough torso keypoints.
     * This function checks whether the model is confident at predicting one of the
     * shoulders/hips which is required to determine a good crop region.
     */
    private fun torsoVisible(keyPoints: List<KeyPoint>): Boolean {
        return ((keyPoints[BodyPart.LEFT_HIP.position].score > MIN_CROP_KEYPOINT_SCORE).or(
            keyPoints[BodyPart.RIGHT_HIP.position].score > MIN_CROP_KEYPOINT_SCORE
        )).and(
            (keyPoints[BodyPart.LEFT_SHOULDER.position].score > MIN_CROP_KEYPOINT_SCORE).or(
                keyPoints[BodyPart.RIGHT_SHOULDER.position].score > MIN_CROP_KEYPOINT_SCORE
            )
        )
    }

    /**
     * Determines the region to crop the image for the model to run inference on.
     * The algorithm uses the detected joints from the previous frame to estimate
     * the square region that encloses the full body of the target person and
     * centers at the midpoint of two hip joints. The crop size is determined by
     * the distances between each joints and the center point.
     * When the model is not confident with the four torso joint predictions, the
     * function returns a default crop which is the full image padded to square.
     */
    private fun determineRectF(
        keyPoints: List<KeyPoint>,
        imageWidth: Int,
        imageHeight: Int
    ): RectF {
        val targetKeyPoints = mutableListOf<KeyPoint>()
        keyPoints.forEach {
            targetKeyPoints.add(
                KeyPoint(
                    it.bodyPart,
                    PointF(
                        it.coordinate.x,
                        it.coordinate.y
                    ),
                    it.score
                )
            )
        }
        if (torsoVisible(keyPoints)) {
            val centerX =
                (targetKeyPoints[BodyPart.LEFT_HIP.position].coordinate.x +
                        targetKeyPoints[BodyPart.RIGHT_HIP.position].coordinate.x) / 2f
            val centerY =
                (targetKeyPoints[BodyPart.LEFT_HIP.position].coordinate.y +
                        targetKeyPoints[BodyPart.RIGHT_HIP.position].coordinate.y) / 2f

            val torsoAndBodyDistances =
                determineTorsoAndBodyDistances(keyPoints, targetKeyPoints, centerX, centerY)

            val list = listOf(
                torsoAndBodyDistances.maxTorsoXDistance * TORSO_EXPANSION_RATIO,
                torsoAndBodyDistances.maxTorsoYDistance * TORSO_EXPANSION_RATIO,
                torsoAndBodyDistances.maxBodyXDistance * BODY_EXPANSION_RATIO,
                torsoAndBodyDistances.maxBodyYDistance * BODY_EXPANSION_RATIO
            )

            var cropLengthHalf = list.maxOrNull() ?: 0f
            val tmp = listOf(centerX, imageWidth - centerX, centerY, imageHeight - centerY)
            cropLengthHalf = min(cropLengthHalf, tmp.maxOrNull() ?: 0f)
            val cropCorner = Pair(centerY - cropLengthHalf, centerX - cropLengthHalf)

            return if (cropLengthHalf > max(imageWidth, imageHeight) / 2f) {
                initRectF(imageWidth, imageHeight)
            } else {
                val cropLength = cropLengthHalf * 2
                RectF(
                    cropCorner.second / imageWidth,
                    cropCorner.first / imageHeight,
                    (cropCorner.second + cropLength) / imageWidth,
                    (cropCorner.first + cropLength) / imageHeight,
                )
            }
        } else {
            return initRectF(imageWidth, imageHeight)
        }
    }

    /**
     * Calculates the maximum distance from each keypoints to the center location.
     * The function returns the maximum distances from the two sets of keypoints:
     * full 17 keypoints and 4 torso keypoints. The returned information will be
     * used to determine the crop size. See determineRectF for more detail.
     */
    private fun determineTorsoAndBodyDistances(
        keyPoints: List<KeyPoint>,
        targetKeyPoints: List<KeyPoint>,
        centerX: Float,
        centerY: Float
    ): TorsoAndBodyDistance {
        val torsoJoints = listOf(
            BodyPart.LEFT_SHOULDER.position,
            BodyPart.RIGHT_SHOULDER.position,
            BodyPart.LEFT_HIP.position,
            BodyPart.RIGHT_HIP.position
        )

        var maxTorsoYRange = 0f
        var maxTorsoXRange = 0f
        torsoJoints.forEach { joint ->
            val distY = abs(centerY - targetKeyPoints[joint].coordinate.y)
            val distX = abs(centerX - targetKeyPoints[joint].coordinate.x)
            if (distY > maxTorsoYRange) maxTorsoYRange = distY
            if (distX > maxTorsoXRange) maxTorsoXRange = distX
        }

        var maxBodyYRange = 0f
        var maxBodyXRange = 0f
        for (joint in keyPoints.indices) {
            if (keyPoints[joint].score < MIN_CROP_KEYPOINT_SCORE) continue
            val distY = abs(centerY - keyPoints[joint].coordinate.y)
            val distX = abs(centerX - keyPoints[joint].coordinate.x)

            if (distY > maxBodyYRange) maxBodyYRange = distY
            if (distX > maxBodyXRange) maxBodyXRange = distX
        }
        return TorsoAndBodyDistance(
            maxTorsoYRange,
            maxTorsoXRange,
            maxBodyYRange,
            maxBodyXRange
        )
    }
}

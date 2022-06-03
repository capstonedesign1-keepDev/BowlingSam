package org.examples.BowlingSam

import android.content.Context
import android.content.ContextWrapper
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.examples.BowlingSam.data.Person
import org.examples.BowlingSam.ml.MoveNet
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Math.round
import java.text.SimpleDateFormat
import java.util.*


class ResultPopupActivity: AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var okButton: Button
    private lateinit var comment: TextView
    private lateinit var imageView: ImageView
    private lateinit var commentImageView: ImageView
    private lateinit var wrongAngleDifference1: TextView
    private lateinit var wrongAngleDifference2: TextView
    private lateinit var wrongAngleDifference3: TextView
    private lateinit var wrongAngleDifference4: TextView
    private lateinit var wrongAngleDifference5: TextView
    private lateinit var angleText: TextView
    private lateinit var feedbackText: TextView
    private lateinit var feedback: TextView


    private lateinit var poseAngleDifferences: Array<FloatArray?>

    private var isTrackerEnabled = false


    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //firebase firestore
    private lateinit var firestore: FirebaseFirestore

    var addressOutputBitmap: Bitmap? = null
    var pushawayOutputBitmap: Bitmap? = null
    var downswingOutputBitmap: Bitmap? = null
    var backswingOutputBitmap: Bitmap? = null
    var forwardswingOutputBitmap: Bitmap? = null
    var followthroughOutputBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultpopup)

        spinner = findViewById(R.id.spinner)
        okButton = findViewById(R.id.result_ok)
        comment = findViewById<TextView>(R.id.result_comment)
        commentImageView = findViewById(R.id.result_comment_image)
        wrongAngleDifference1 = findViewById<TextView>(R.id.result_wrongAngle1)
        wrongAngleDifference2 = findViewById<TextView>(R.id.result_wrongAngle2)
        wrongAngleDifference3 = findViewById<TextView>(R.id.result_wrongAngle3)
        wrongAngleDifference4 = findViewById<TextView>(R.id.result_wrongAngle4)
        wrongAngleDifference5 = findViewById<TextView>(R.id.result_wrongAngle5)
        angleText = findViewById<TextView>(R.id.textView2)
        feedbackText = findViewById<TextView>(R.id.textView)
        feedback = findViewById<TextView>(R.id.feedback)
        imageView = findViewById(R.id.result_posture_image)

        //자세별 기록을 확인하기 위한 드롭다운 스피너의 어댑터 설정
        val adapter = ArrayAdapter.createFromResource(this,
            R.array.pose_list, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        var score1 = intent.getFloatExtra("addressScore", 0.0f)
        var score2 = intent.getFloatExtra("pushawayScore", 0.0f)
        var score3 = intent.getFloatExtra("downswingScore", 0.0f)
        var score4 = intent.getFloatExtra("backswingScore", 0.0f)
        var score5 = intent.getFloatExtra("forwardswingScore", 0.0f)
        var score6 = intent.getFloatExtra("followthroughScore", 0.0f)
        var scoreList = listOf(score1, score2, score3, score4, score5, score6)

        var addressResultURI = intent.getStringExtra("addressuri")
        var pushawayResultURI = intent.getStringExtra("pushawayuri")
        var downswingResultURI = intent.getStringExtra("downswinguri")
        var backswingResultURI = intent.getStringExtra("backswinguri")
        var forwardswingResultURI = intent.getStringExtra("forwardswinguri")
        var followthroughResultURI = intent.getStringExtra("followthroughuri")

        //URI를 Bitmap으로 변경
        var addressBitmap = BitmapFactory.decodeFile(addressResultURI)
        var pushawayBitmap = BitmapFactory.decodeFile(pushawayResultURI)
        var downswingBitmap = BitmapFactory.decodeFile(downswingResultURI)
        var backswingBitmap = BitmapFactory.decodeFile(backswingResultURI)
        var forwardswingBitmap = BitmapFactory.decodeFile(forwardswingResultURI)
        var followthroughBitmap = BitmapFactory.decodeFile(followthroughResultURI)
        var bitmapList = listOf(addressResultURI, pushawayResultURI, downswingResultURI, backswingResultURI, forwardswingResultURI, followthroughResultURI)

        var addressAngleDifferences = intent.getFloatArrayExtra("addressAngleDifferences")
        var pushawayAngleDifferences = intent.getFloatArrayExtra("pushawayAngleDifferences")
        var downswingAngleDifferences = intent.getFloatArrayExtra("downswingAngleDifferences")
        var backswingAngleDifferences = intent.getFloatArrayExtra("backswingAngleDifferences")
        var forwardswingAngleDifferences = intent.getFloatArrayExtra("forwardswingAngleDifferences")
        var followthroughAngleDifferences = intent.getFloatArrayExtra("followthroughAngleDifferences")

        var addressPerson = intent.getParcelableExtra<Person>("addressperson")
        var pushawayPerson = intent.getParcelableExtra<Person>("pushawayperson")
        var downswingPerson = intent.getParcelableExtra<Person>("downswingperson")
        var backswingPerson = intent.getParcelableExtra<Person>("backswingperson")
        var forwardswingPerson = intent.getParcelableExtra<Person>("forwardswingperson")
        var followthroughPerson = intent.getParcelableExtra<Person>("followthroughperson")

        var fileName = intent.getStringExtra("filename")

        poseAngleDifferences = arrayOf(addressAngleDifferences, pushawayAngleDifferences,
            downswingAngleDifferences, backswingAngleDifferences, forwardswingAngleDifferences, followthroughAngleDifferences)

        //모범 자세와의 각도 차이에 따라 색깔을 구분하여 비트맵을 생성하고 이미지에 출력
        if(addressBitmap!= null) {
            addressOutputBitmap = visualize(PoseType.ADDRESS, addressPerson!!, addressBitmap, poseAngleDifferences)
        }
        if(pushawayBitmap!= null) {
            pushawayOutputBitmap = visualize(PoseType.PUSHAWAY, pushawayPerson!!, pushawayBitmap, poseAngleDifferences)
        }
        if(downswingBitmap!= null) {
            downswingOutputBitmap = visualize(PoseType.DOWNSWING, downswingPerson!!, downswingBitmap, poseAngleDifferences)
        }
        if(backswingBitmap!=null) {
            backswingOutputBitmap = visualize(PoseType.BACKSWING, backswingPerson!!, backswingBitmap, poseAngleDifferences)
        }
        if(forwardswingBitmap!=null) {
            forwardswingOutputBitmap = visualize(PoseType.FORWARDSWING, forwardswingPerson!!, forwardswingBitmap, poseAngleDifferences)
        }
        if(followthroughBitmap!=null) {
            followthroughOutputBitmap = visualize(PoseType.FOLLOWTHROUGH, followthroughPerson!!, followthroughBitmap, poseAngleDifferences)
        }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    comment.text = null
                    wrongAngleDifference1.text = null
                    wrongAngleDifference2.text = null
                    wrongAngleDifference3.text = null
                    wrongAngleDifference4.text = null
                    wrongAngleDifference5.text = null
                    feedback.text = null
                    if (getSelectedSpinnerItem() == 0) {

                        if(addressBitmap != null) {
                            comment.text = "어드레스 점수: ${round(score1)}"
                            angleText.visibility = View.VISIBLE
                            feedbackText.visibility = View.VISIBLE
                            imageView.setImageBitmap(addressOutputBitmap)
                            resultImage(score1)
                            commentImageView.visibility = View.VISIBLE
                            feedbackAddressAngleDiffernce(addressAngleDifferences!!)
                        }  else {
                            angleText.visibility = View.INVISIBLE
                            feedbackText.visibility = View.INVISIBLE
                            commentImageView.visibility = View.INVISIBLE
                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                            imageView.setImageResource(R.drawable.bowling)
                        }

                    } else if (getSelectedSpinnerItem() == 1) {

                        if(pushawayBitmap != null) {
                            comment.text = "푸쉬어웨이 점수: ${round(score2)}"
                            resultImage(score2)
                            angleText.visibility = View.VISIBLE
                            feedbackText.visibility = View.VISIBLE
                            commentImageView.visibility = View.VISIBLE
                            imageView.setImageBitmap(pushawayOutputBitmap)
                            feedbackAngleDiffernce(pushawayAngleDifferences!!)
                        } else {
                            angleText.visibility = View.INVISIBLE
                            feedbackText.visibility = View.INVISIBLE
                            commentImageView.visibility = View.INVISIBLE
                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                            imageView.setImageResource(R.drawable.bowling)
                        }
                    } else if (getSelectedSpinnerItem() == 2) {

                        if(downswingBitmap != null) {
                            comment.text = "다운스윙 점수: ${round(score3)}"
                            imageView.setImageBitmap(downswingOutputBitmap)
                            resultImage(score3)
                            commentImageView.visibility = View.VISIBLE
                            angleText.visibility = View.VISIBLE
                            feedbackText.visibility = View.VISIBLE
                            feedbackAngleDiffernce(downswingAngleDifferences!!)
                        } else {
                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                            imageView.setImageResource(R.drawable.bowling)
                        }
                    } else if (getSelectedSpinnerItem() == 3) {

                        if(backswingBitmap != null) {
                            comment.text = "백스윙 점수: ${round(score4)}"
                            imageView.setImageBitmap(backswingOutputBitmap)
                            resultImage(score4)
                            commentImageView.visibility = View.VISIBLE
                            angleText.visibility = View.VISIBLE
                            feedbackText.visibility = View.VISIBLE
                            feedbackAngleDiffernce(backswingAngleDifferences!!)
                        } else {
                            angleText.visibility = View.INVISIBLE
                            feedbackText.visibility = View.INVISIBLE
                            commentImageView.visibility = View.INVISIBLE
                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                            imageView.setImageResource(R.drawable.bowling)
                        }
                    } else if (getSelectedSpinnerItem() == 4) {
                        if(forwardswingBitmap != null) {
                            comment.text = "포워드 점수: ${round(score5)}"
                            imageView.setImageBitmap(forwardswingOutputBitmap)
                            resultImage(score5)
                            commentImageView.visibility = View.VISIBLE
                            angleText.visibility = View.VISIBLE
                            feedbackText.visibility = View.VISIBLE
                            feedbackAngleDiffernce(forwardswingAngleDifferences!!)
                        } else {
                            angleText.visibility = View.INVISIBLE
                            feedbackText.visibility = View.INVISIBLE
                            commentImageView.visibility = View.INVISIBLE
                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                            imageView.setImageResource(R.drawable.bowling)
                        }
                    } else {
                        if(followthroughBitmap != null) {
                            comment.text = "팔로우스루 점수: ${round(score6)}"
                            imageView.setImageBitmap(followthroughOutputBitmap)
                            resultImage(score6)
                            commentImageView.visibility = View.VISIBLE
                            angleText.visibility = View.VISIBLE
                            feedbackText.visibility = View.VISIBLE
                            feedbackAngleDiffernce(followthroughAngleDifferences!!)
                        } else {
                            angleText.visibility = View.INVISIBLE
                            feedbackText.visibility = View.INVISIBLE
                            commentImageView.visibility = View.INVISIBLE
                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                            imageView.setImageResource(R.drawable.bowling)
                        }
                    }
                }
            }

            //확인 버튼 클릭 시, 데이터베이스에 기록을 저장
            okButton.setOnClickListener {

                //문서 업데이트
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()

                var videoList = VideoListData()
                videoList.uid = firebaseAuth?.currentUser?.uid

                var addressOutputURI = saveBitmapAsFile(PoseType.ADDRESS, addressOutputBitmap).toString()
                var pushawayOutputURI = saveBitmapAsFile(PoseType.PUSHAWAY, pushawayOutputBitmap).toString()
                var downswingOutputURI = saveBitmapAsFile(PoseType.DOWNSWING, downswingOutputBitmap).toString()
                var backswingOutputURI = saveBitmapAsFile(PoseType.BACKSWING, backswingOutputBitmap).toString()
                var forwardswingOutputURI = saveBitmapAsFile(PoseType.FORWARDSWING, forwardswingOutputBitmap).toString()
                var followthroughOutputURI = saveBitmapAsFile(PoseType.FOLLOWTHROUGH, followthroughOutputBitmap).toString()

                var bitmapOutputList = listOf(addressOutputURI, pushawayOutputURI, downswingOutputURI, backswingOutputURI, forwardswingOutputURI, followthroughOutputURI)

                videoList.videoPath = fileName
                videoList.scoreList = scoreList
                videoList.addressAngleDifference = addressAngleDifferences?.toList()
                videoList.pushawayAngleDifference = pushawayAngleDifferences?.toList()
                videoList.downswingAngleDifference = downswingAngleDifferences?.toList()
                videoList.backswingAngleDifference = backswingAngleDifferences?.toList()
                videoList.forwardswingAngleDifference = forwardswingAngleDifferences?.toList()
                videoList.followthroughAngleDifference = followthroughAngleDifferences?.toList()
                videoList.bitmapOutputList = bitmapOutputList
                videoList.bitmapList = bitmapList

                var scoreArray = arrayOf(scoreList[0], scoreList[1], scoreList[2], scoreList[3], scoreList[4], scoreList[5])
                videoList.score = getAvgScore(scoreArray)
                firestore?.collection("videolist")?.add(videoList)

                RecordFragment.resetRecordedInfo()
                MoveNet.resetInfo()

                finish()
            }
        }

    override fun onDestroy() {
        RecordFragment.resetRecordedInfo()
        MoveNet.resetInfo()
        super.onDestroy()
    }

    //비트맵을 파일로 저장
    private fun saveBitmapAsFile(pose: PoseType, bitmap: Bitmap?): Uri? {

        val wrapper = ContextWrapper(this.applicationContext)
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

    //모범 자세와의 각도 차이에 따라 색깔을 구분하여 비트맵을 생성하고 이미지에 출력하는 함수
    private fun visualize(pose: PoseType, persons: Person, bitmap: Bitmap, array: Array<FloatArray?>): Bitmap {

        val outputBitmap = VisualizationUtils.drawBodyKeypointsByScore(
            pose,
            array,
            bitmap,
            persons, false
        )

        return outputBitmap
    }

    fun getSelectedSpinnerItem(): Int {
        return spinner.selectedItemPosition
    }

    //자세별 점수에 따라 '좋음', '주의', '나쁨' 이미지 출력
    fun resultImage(score: Float){
        if(score >= 40) {
            commentImageView.setImageResource(R.drawable.result_good)
        } else if(score >= 35) {
            commentImageView.setImageResource(R.drawable.result_warning)
        } else {
            commentImageView.setImageResource(R.drawable.result_bad)
        }
    }

    //어드레스 자세에 대한 피드백을 제공하는 함수
    fun feedbackAddressAngleDiffernce(addressAngleDifferences: FloatArray) {
        wrongAngleDifference1.text = "오른쪽 팔꿈치 각도 차이: ${round(addressAngleDifferences[0])}"
        wrongAngleDifference2.text = "오른쪽 어깨 각도 차이: ${round(addressAngleDifferences[1])}"
        wrongAngleDifference3.text = "오른쪽 골반 각도 차이: ${round(addressAngleDifferences[2])}"
        wrongAngleDifference4.text = "오른쪽 무릎 각도 차이: ${round(addressAngleDifferences[3])}"
        if (addressAngleDifferences!![0] >= 10.0) {
            feedback.text = "오른쪽 팔꿈치가 많이 벌어졌네요! 팔을 90도로 만들어서 팔꿈치를 옆구리에 딱 붙여주세요!\n"
        }else if (addressAngleDifferences!![0] <= -10.0) {
            feedback.text = "오른쪽 팔꿈치가 많이 좁네요! 팔을 90도로 만들어서 팔꿈치를 옆구리에 딱 붙여주세요!\n"
        }

        if (addressAngleDifferences!![1] >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 어깨 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(addressAngleDifferences[1] <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 어깨 각도가 많이 좁네요! 어깨를 조금 펴주세요!\n"
        }

        if (addressAngleDifferences!![2] >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 골반 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(addressAngleDifferences[2] <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 골반 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }
        if (addressAngleDifferences!![3] >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 무릎 각도가 많이 벌어졌네요! 무릎을 살짝 구부려주세요!\n"
        }else if(addressAngleDifferences[3] <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 무릎 각도가 많이 좁네요! 무릎을 살짝 펴주세요!\n"
        }
        if(feedback.text == null){
            feedback.text = "짝짝짝! 완벽한 자세에요!"
        }
    }

    //푸쉬어웨이, 다운스윙, 백스윙, 포워드스윙, 팔로우스루 자세에 대한 피드백을 제공하는 함수
    fun feedbackAngleDiffernce(angleDifferences: FloatArray) {
        wrongAngleDifference1.text = "오른쪽 팔꿈치 각도 차이: ${round(angleDifferences!![0])}"
        wrongAngleDifference2.text = "오른쪽 어깨 각도 차이: ${round(angleDifferences!![1])}"
        wrongAngleDifference3.text = "오른쪽 골반 각도 차이: ${round(angleDifferences!![2])}"
        wrongAngleDifference4.text = "오른쪽 무릎 각도 차이: ${round(angleDifferences!![3])}"
        wrongAngleDifference5.text = "왼쪽 무릎 각도 차이: ${round(angleDifferences!![4])}"

        if (angleDifferences!![0] >= 10.0) {
            feedback.text = "오른쪽 팔꿈치가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if (angleDifferences!![0] <= -10.0) {
            feedback.text = "오른쪽 팔꿈치 각도가 많이 좁네요! 각도를 조금 펴주세요!\n"
        }

        if (angleDifferences!![1] >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 어깨 각도가 많이 벌어졌네요! 어깨를 일직선으로 펴주세요!\n"
        }else if(angleDifferences[1] <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 어깨 각도가 많이 좁네요! 어깨를 일직선으로 펴주세요!\n"
        }

        if (angleDifferences!![2] >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 골반 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(angleDifferences[2] <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 골반 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }

        if (angleDifferences!![3] >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 무릎 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(angleDifferences[3] <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 무릎 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }

        if (angleDifferences!![4] >= 10.0) {
            feedback.text = "${feedback.text}왼쪽 무릎 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(angleDifferences[4] <= -10.0) {
            feedback.text = "${feedback.text}왼쪽 무릎 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }

        if(feedback.text == null){
            feedback.text = "짝짝짝! 완벽한 자세에요!"
        }
    }

    //자세의 평균점수를 구하는 함수
    fun getAvgScore(array: Array<Float>):Float{
        var sum = 0.0f
        var count = 0
        for(i in 0 until array.size){
            if(array[i]!=0.0f) {
                sum += array[i]
                count = i + 1
            }

        }
        return sum/count
    }

}
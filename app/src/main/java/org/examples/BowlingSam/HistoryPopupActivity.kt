package org.examples.BowlingSam

import android.annotation.SuppressLint
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.lang.Math.round

class HistoryPopupActivity: AppCompatActivity() {

    //유저의 기록을 표시할 UI Component들을 선언
    private lateinit var spinner: Spinner
    private lateinit var okButton: Button
    private lateinit var comment: TextView
    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var commentImageView: ImageView
    private lateinit var wrongAngleDifference1: TextView
    private lateinit var wrongAngleDifference2: TextView
    private lateinit var wrongAngleDifference3: TextView
    private lateinit var wrongAngleDifference4: TextView
    private lateinit var wrongAngleDifference5: TextView
    private lateinit var feedbackText: TextView
    private lateinit var angleText: TextView
    private lateinit var feedback: TextView

    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //firebase firestore
    private lateinit var firestore: FirebaseFirestore

    //유저의 기록을 불러오기 위한 변수 선언
    private lateinit var poseAngleDifferences: Array<List<Float?>?>

    var item: String? = null
    var videoPath: String? = null
    var scoreList: List<Float>? = null
    var addressAngleDifference: List<Float?>? = null
    var pushawayAngleDifference: List<Float?>? = null
    var downswingAngleDifference: List<Float?>? = null
    var backswingAngleDifference: List<Float?>? = null
    var forwardswingAngleDifference: List<Float?>? = null
    var followthroughAngleDifference: List<Float?>? = null
    var bitmapOutputList: List<String?>? = null
    var bitmapList: List<String?>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historypopup)

        spinner = findViewById(R.id.history_spinner)
        okButton = findViewById(R.id.history_ok)
        comment = findViewById<TextView>(R.id.history_comment)
        commentImageView = findViewById(R.id.history_comment_image)
        wrongAngleDifference1 = findViewById<TextView>(R.id.history_wrongAngle1)
        wrongAngleDifference2 = findViewById<TextView>(R.id.history_wrongAngle2)
        wrongAngleDifference3 = findViewById<TextView>(R.id.history_wrongAngle3)
        wrongAngleDifference4 = findViewById<TextView>(R.id.history_wrongAngle4)
        wrongAngleDifference5 = findViewById<TextView>(R.id.history_wrongAngle5)
        angleText = findViewById<TextView>(R.id.textView)
        feedbackText = findViewById<TextView>(R.id.textView2)
        feedback = findViewById<TextView>(R.id.history_feedback)
        imageView = findViewById(R.id.history_posture_image)
        videoView = findViewById(R.id.history_video)

        //자세별 기록을 확인하기 위한 드롭다운 스피너의 어댑터 설정
        val adapter = ArrayAdapter.createFromResource(this,
            R.array.history_list, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        item = intent.getStringExtra("itemvideopath")

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //유저의 기록 영상을 재생
        val mc = MediaController(this@HistoryPopupActivity)
        mc.setAnchorView(videoView)
        videoView.setMediaController(mc)
        videoView.setVideoURI(Uri.fromFile(File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "${item}.mp4")))

        //데이터베이스에서 유저의 기록 데이터를 불러옴
        firestore.collection("videolist")
            .whereEqualTo("uid", firebaseAuth.uid)
            .get()
            .addOnSuccessListener { result ->
                Log.d("TAG", "myDialog: $result")

                for(document in result) {

                    if (document["videoPath"].toString() == item ) {
                        videoPath = item
                        scoreList = document["scoreList"] as List<Float>?
                        Log.d("TAG", "onCreate: $scoreList")
                        addressAngleDifference = document["addressAngleDifference"] as List<Float?>?
                        pushawayAngleDifference = document["pushawayAngleDifference"] as List<Float?>?
                        downswingAngleDifference = document["downswingAngleDifference"] as List<Float?>?
                        backswingAngleDifference = document["backswingAngleDifference"] as List<Float?>?
                        forwardswingAngleDifference = document["forwardswingAngleDifference"] as List<Float?>?
                        followthroughAngleDifference = document["followthroughAngleDifference"] as List<Float?>?
                        bitmapOutputList = document["bitmapOutputList"] as List<String?>?
                        bitmapList = document["bitmapList"] as List<String?>?

                        var addressOutputBitmap = BitmapFactory.decodeFile(bitmapOutputList!![0])
                        var pushawayOutputBitmap = BitmapFactory.decodeFile(bitmapOutputList!![1])
                        var downswingOutputBitmap = BitmapFactory.decodeFile(bitmapOutputList!![2])
                        var backswingOutputBitmap = BitmapFactory.decodeFile(bitmapOutputList!![3])
                        var forwardswingOutputBitmap = BitmapFactory.decodeFile(bitmapOutputList!![4])
                        var followthroughOutputBitmap = BitmapFactory.decodeFile(bitmapOutputList!![5])

                        var addressAngleDifferences = addressAngleDifference
                        var pushawayAngleDifferences = pushawayAngleDifference
                        var downswingAngleDifferences = downswingAngleDifference
                        var backswingAngleDifferences = backswingAngleDifference
                        var forwardswingAngleDifferences = forwardswingAngleDifference
                        var followthroughAngleDifferences = followthroughAngleDifference

                        poseAngleDifferences = arrayOf(addressAngleDifferences, pushawayAngleDifferences,
            downswingAngleDifferences, backswingAngleDifferences, forwardswingAngleDifferences, followthroughAngleDifferences)

                        var addressScore = scoreList!![0]
                        var pushawayScore = scoreList!![1]
                        var downswingScore = scoreList!![2]
                        var backswingScore = scoreList!![3]
                        var forwardswingScore = scoreList!![4]
                        var followthroughScore = scoreList!![5]

                        var scores = arrayOf(addressScore, pushawayScore, downswingScore, backswingScore, forwardswingScore, followthroughScore)
                        var avgScore = getAvgScore(scores)

                        //스피너의 아이템 선택 리스너 설정
                        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                            override fun onNothingSelected(parent: AdapterView<*>?) {

                            }

                            @SuppressLint("SetTextI18n")
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    wrongAngleDifference1.text = null
                                    wrongAngleDifference2.text = null
                                    wrongAngleDifference3.text = null
                                    wrongAngleDifference4.text = null
                                    wrongAngleDifference5.text = null
                                    angleText.visibility = View.INVISIBLE
                                    feedbackText.visibility = View.INVISIBLE
                                    feedback.text = null
                                    comment.text = null
                                    if (getSelectedSpinnerItem() == 0) {
                                        videoView.visibility = View.VISIBLE
                                        imageView.visibility = View.INVISIBLE
                                        resultImage(avgScore)

                                        videoView.requestFocus()
                                        videoView.start()

                                        comment.text = "평균 점수: $avgScore"
                                    } else if (getSelectedSpinnerItem() == 1) {
                                        videoView.visibility = View.INVISIBLE
                                        imageView.visibility = View.VISIBLE
                                        commentImageView.visibility = View.VISIBLE

                                        if(addressOutputBitmap != null) {
                                            comment.text = "어드레스 점수: ${round(addressScore)}"
                                            imageView.setImageBitmap(addressOutputBitmap)
                                            angleText.visibility = View.VISIBLE
                                            feedbackText.visibility = View.VISIBLE
                                            resultImage(addressScore)
                                            feedbackAddressAngleDiffernce(addressAngleDifferences!!)
                                        }  else {
                                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                                            commentImageView.visibility = View.INVISIBLE
                                            imageView.setImageResource(R.drawable.bowling)
                                        }

                                    } else if (getSelectedSpinnerItem() == 2) {
                                        videoView.visibility = View.INVISIBLE
                                        imageView.visibility = View.VISIBLE
                                        commentImageView.visibility = View.VISIBLE

                                        if(pushawayOutputBitmap != null) {
                                            angleText.visibility = View.VISIBLE
                                            feedbackText.visibility = View.VISIBLE
                                            comment.text = "푸시어웨이 점수: ${round(pushawayScore)}"
                                            imageView.setImageBitmap(pushawayOutputBitmap)
                                            resultImage(pushawayScore)
                                            feedbackAngleDiffernce(pushawayAngleDifferences!!)
                                        } else {
                                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                                            angleText.visibility = View.INVISIBLE
                                            feedbackText.visibility = View.INVISIBLE
                                            commentImageView.visibility = View.INVISIBLE
                                            imageView.setImageResource(R.drawable.bowling)
                                        }

                                    } else if (getSelectedSpinnerItem() == 3) {
                                        videoView.visibility = View.INVISIBLE
                                        imageView.visibility = View.VISIBLE
                                        commentImageView.visibility = View.VISIBLE

                                        if(downswingOutputBitmap != null) {
                                            angleText.visibility = View.VISIBLE
                                            feedbackText.visibility = View.VISIBLE
                                            comment.text = "다운스윙 점수: ${round(downswingScore)}"
                                            imageView.setImageBitmap(downswingOutputBitmap)
                                            resultImage(downswingScore)
                                            feedbackAngleDiffernce(downswingAngleDifferences!!)
                                        } else {
                                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                                            angleText.visibility = View.INVISIBLE
                                            feedbackText.visibility = View.INVISIBLE
                                            commentImageView.visibility = View.INVISIBLE
                                            imageView.setImageResource(R.drawable.bowling)
                                        }
                                    } else if (getSelectedSpinnerItem() == 4) {
                                        videoView.visibility = View.INVISIBLE
                                        imageView.visibility = View.VISIBLE
                                        commentImageView.visibility = View.VISIBLE

                                        if(backswingOutputBitmap != null) {
                                            angleText.visibility = View.VISIBLE
                                            feedbackText.visibility = View.VISIBLE
                                            comment.text = "백스윙 점수: ${round(backswingScore)}"
                                            imageView.setImageBitmap(backswingOutputBitmap)
                                            resultImage(backswingScore)
                                            feedbackAngleDiffernce(backswingAngleDifferences!!)
                                        } else {
                                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                                            angleText.visibility = View.INVISIBLE
                                            feedbackText.visibility = View.INVISIBLE
                                            commentImageView.visibility = View.INVISIBLE
                                            imageView.setImageResource(R.drawable.bowling)
                                        }
                                    } else if(getSelectedSpinnerItem() == 5) {
                                        videoView.visibility = View.INVISIBLE
                                        imageView.visibility = View.VISIBLE
                                        commentImageView.visibility = View.VISIBLE

                                        if(forwardswingOutputBitmap != null) {
                                            angleText.visibility = View.VISIBLE
                                            feedbackText.visibility = View.VISIBLE
                                            comment.text = "백스윙 점수: ${round(forwardswingScore)}"
                                            imageView.setImageBitmap(forwardswingOutputBitmap)
                                            resultImage(forwardswingScore)
                                            feedbackAngleDiffernce(forwardswingAngleDifferences!!)
                                        } else {
                                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                                            angleText.visibility = View.INVISIBLE
                                            feedbackText.visibility = View.INVISIBLE
                                            commentImageView.visibility = View.INVISIBLE
                                            imageView.setImageResource(R.drawable.bowling)
                                        }
                                    } else {
                                        videoView.visibility = View.INVISIBLE
                                        imageView.visibility = View.VISIBLE
                                        commentImageView.visibility = View.VISIBLE

                                        if(followthroughOutputBitmap != null) {
                                            angleText.visibility = View.VISIBLE
                                            feedbackText.visibility = View.VISIBLE
                                            comment.text = "백스윙 점수: ${round(followthroughScore)}"
                                            imageView.setImageBitmap(followthroughOutputBitmap)
                                            resultImage(followthroughScore)
                                            feedbackAngleDiffernce(followthroughAngleDifferences!!)
                                        } else {
                                            comment.text = "영상이 너무 짧아서 해당 자세의 기록이 없습니다"
                                            angleText.visibility = View.INVISIBLE
                                            feedbackText.visibility = View.INVISIBLE
                                            commentImageView.visibility = View.INVISIBLE
                                            imageView.setImageResource(R.drawable.bowling)
                                        }
                                    }

                            }
                        }

                        //확인 버튼 클릭 리스너 설정
                        okButton.setOnClickListener {
                            finish()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->

            }

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    //기록의 자세 평균점수를 구하는 함수
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

    fun getSelectedSpinnerItem(): Int {
        return spinner.selectedItemPosition
    }

    //자세별 점수에 따라 '좋음', '주의', '나쁨' 이미지를 출력
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
    fun feedbackAddressAngleDiffernce(addressAngleDifferences: List<Float?>) {
        wrongAngleDifference1.text = "오른쪽 팔꿈치 각도 차이: ${round(addressAngleDifferences[0]!!)}"
        wrongAngleDifference2.text = "오른쪽 어깨 각도 차이: ${round(addressAngleDifferences[1]!!)}"
        wrongAngleDifference3.text = "오른쪽 골반 각도 차이: ${round(addressAngleDifferences[2]!!)}"
        wrongAngleDifference4.text = "오른쪽 무릎 각도 차이: ${round(addressAngleDifferences[3]!!)}"
        if (addressAngleDifferences[0]!! >= 10.0) {
            feedback.text = "오른쪽 팔꿈치가 많이 벌어졌네요! 팔을 90도로 만들어서 팔꿈치를 옆구리에 딱 붙여주세요!\n"
        }else if (addressAngleDifferences[0]!! <= -10.0) {
            feedback.text = "오른쪽 팔꿈치가 많이 좁네요! 팔을 90도로 만들어서 팔꿈치를 옆구리에 딱 붙여주세요!\n"
        }

        if (addressAngleDifferences[1]!! >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 어깨 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(addressAngleDifferences[1]!! <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 어깨 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }

        if (addressAngleDifferences[2]!! >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 골반 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(addressAngleDifferences[2]!! <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 골반 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }
        if (addressAngleDifferences[3]!! >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 무릎 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(addressAngleDifferences[3]!! <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 무릎 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }
        if(feedback.text == null){
            feedback.text = "짝짝짝! 완벽한 자세에요!"
        }
    }

    //푸쉬어웨이, 다운스윙, 백스윙, 포워드스윙, 팔로우스루 자세에 대한 피드백을 제공하는 함수
    fun feedbackAngleDiffernce(angleDifferences: List<Float?>) {
        wrongAngleDifference1.text = "오른쪽 팔꿈치 각도 차이: ${round(angleDifferences[0]!!)}"
        wrongAngleDifference2.text = "오른쪽 어깨 각도 차이: ${round(angleDifferences[1]!!)}"
        wrongAngleDifference3.text = "오른쪽 골반 각도 차이: ${round(angleDifferences[2]!!)}"
        wrongAngleDifference4.text = "오른쪽 무릎 각도 차이: ${round(angleDifferences[3]!!)}"
        wrongAngleDifference5.text = "왼쪽 무릎 각도 차이: ${round(angleDifferences[4]!!)}"

        if (angleDifferences[0]!! >= 10.0) {
            feedback.text = "오른쪽 팔꿈치가 많이 벌어졌네요! 팔을 90도로 만들어서 팔꿈치를 옆구리에 딱 붙여주세요!\n"
        }else if (angleDifferences[0]!! <= -10.0) {
            feedback.text = "오른쪽 팔꿈치가 많이 좁네요! 팔을 90도로 만들어서 팔꿈치를 옆구리에 딱 붙여주세요!\n"
        }

        if (angleDifferences[1]!! >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 어깨 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(angleDifferences[1]!! <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 어깨 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }

        if (angleDifferences[2]!! >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 골반 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(angleDifferences[2]!! <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 골반 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }

        if (angleDifferences[3]!! >= 10.0) {
            feedback.text = "${feedback.text}오른쪽 무릎 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(angleDifferences[3]!! <= -10.0) {
            feedback.text = "${feedback.text}오른쪽 무릎 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }

        if (angleDifferences[4]!! >= 10.0) {
            feedback.text = "${feedback.text}왼쪽 무릎 각도가 많이 벌어졌네요! 각도를 조금 줄여주세요!\n"
        }else if(angleDifferences[4]!! <= -10.0) {
            feedback.text = "${feedback.text}왼쪽 무릎 각도가 많이 좁네요! 각도를 조금 벌려주세요!\n"
        }

        if(feedback.text == null){
            feedback.text = "짝짝짝! 완벽한 자세에요!"
        }
    }


}
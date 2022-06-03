package org.examples.BowlingSam

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

//유저의 영상 기록 정보를 정의하는 클래스
data class VideoListData(
    @ServerTimestamp
    var createdAt: Date? = null,
    var uid : String? = null,
    var videoPath : String? = "미설정",
    var score : Float? = 0.0f,
    var scoreList: List<Float>? = null,
    var addressAngleDifference: List<Float?>? = null,
    var pushawayAngleDifference: List<Float?>? = null,
    var downswingAngleDifference: List<Float?>? = null,
    var backswingAngleDifference: List<Float?>? = null,
    var forwardswingAngleDifference: List<Float?>? = null,
    var followthroughAngleDifference: List<Float?>? = null,
    var bitmapOutputList: List<String?>? = null,
    var bitmapList: List<String?>? = null,
    val isFavorite : Boolean = false
)
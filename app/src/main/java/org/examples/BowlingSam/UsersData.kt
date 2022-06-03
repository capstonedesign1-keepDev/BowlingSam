package org.examples.BowlingSam

//유저의 정보를 정의하는 클래스
data class UsersData (
    var uid : String? = null,
    var userNickName : String? = "미설정",
    var userID : String? = null,
    var age : Int? = 0,
    var ballsize : Int? = 0,
    var grade : Int? = 9,
    var avg : Int? = 0,
    var recentScore : Int? = 0
){ }
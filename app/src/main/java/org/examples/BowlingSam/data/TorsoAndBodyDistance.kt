package org.examples.BowlingSam.data

//관절과 몸과의 거리를 정의하는 클래스
data class TorsoAndBodyDistance(
    val maxTorsoYDistance: Float,
    val maxTorsoXDistance: Float,
    val maxBodyYDistance: Float,
    val maxBodyXDistance: Float
)

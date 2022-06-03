package org.examples.BowlingSam.data

import kotlin.math.absoluteValue

//볼링의 자세를 정의하는 클래스
class VowlingPose {
    //볼링 모범자세의 각도를 저장하는 변수 선언
    var correctRightElbowAngle: Float = 0.0f
    var correctRightShoulderAngle: Float = 0.0f
    var correctRightHipAngle: Float = 0.0f
    var correctRightKneeAngle: Float = 0.0f
    var correctLeftKneeAngle: Float = 0.0f

    constructor(correctRightElbowAngle: Float, correctRightShoulderAngle: Float,
                correctRightHipAngle: Float, correctRightKneeAngle: Float){
        this.correctRightElbowAngle = correctRightElbowAngle
        this.correctRightShoulderAngle = correctRightShoulderAngle
        this.correctRightHipAngle = correctRightHipAngle
        this.correctRightKneeAngle = correctRightKneeAngle
        this.correctLeftKneeAngle = 0.0f
    }

    constructor(correctRightElbowAngle: Float, correctRightShoulderAngle: Float,
                correctRightHipAngle: Float, correctRightKneeAngle: Float, correctLeftKneeAngle: Float){
        this.correctRightElbowAngle = correctRightElbowAngle
        this.correctRightShoulderAngle = correctRightShoulderAngle
        this.correctRightHipAngle = correctRightHipAngle
        this.correctRightKneeAngle = correctRightKneeAngle
        this.correctLeftKneeAngle = correctLeftKneeAngle
    }

    fun getREA(): Float {
        return this.correctRightElbowAngle
    }

    fun getRSA(): Float {
        return this.correctRightShoulderAngle
    }

    fun getRHA(): Float {
        return this.correctRightHipAngle
    }

    fun getRKA(): Float {
        return this.correctRightKneeAngle
    }

    fun getLKA(): Float {
        return this.correctLeftKneeAngle
    }

    //모범자세의 각도와 유저 자세의 각도를 비교하여 점수를 산출하는 함수
    fun getScore(a1: Float, a2: Float, a3: Float, a4: Float): Float {
        return 100 - ((correctRightElbowAngle - a1).absoluteValue + (correctRightShoulderAngle - a2).absoluteValue +
                (correctRightHipAngle - a3).absoluteValue + (correctRightKneeAngle - a4).absoluteValue)
    }

    fun getScore(a1: Float, a2: Float, a3: Float, a4: Float, a5: Float): Float {
        return 100 - ((correctRightElbowAngle - a1).absoluteValue + (correctRightShoulderAngle - a2).absoluteValue +
                (correctRightHipAngle - a3).absoluteValue + (correctRightKneeAngle - a4).absoluteValue + (correctLeftKneeAngle - a5).absoluteValue)
    }
}
package org.examples.BowlingSam.data

import android.graphics.RectF
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//인식된 사람에 대한 정의를 하는 클래스
@Parcelize
data class Person(
    var id: Int = -1,
    var keyPoints: List<KeyPoint>,
    var boundingBox: RectF? = null,
    var score: Float
) : Parcelable


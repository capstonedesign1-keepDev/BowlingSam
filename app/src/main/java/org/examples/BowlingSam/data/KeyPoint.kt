package org.examples.BowlingSam.data

import android.graphics.PointF
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//인식된 사람의 관절을 정의하는 클래스
@Parcelize
data class KeyPoint(val bodyPart: BodyPart, var coordinate: PointF, val score: Float)  : Parcelable

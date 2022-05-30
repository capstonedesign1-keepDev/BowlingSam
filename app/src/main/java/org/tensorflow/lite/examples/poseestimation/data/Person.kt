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

package org.tensorflow.lite.examples.poseestimation.data

import org.tensorflow.lite.examples.poseestimation.data.KeyPoint
import android.graphics.RectF
import android.graphics.PointF
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import kotlin.math.*

@Parcelize
data class Person(
    var id: Int = -1, // default id is -1
    var keyPoints: List<KeyPoint>,
    var boundingBox: RectF? = null, // Only MoveNet MultiPose return bounding box.
    var score: Float
) : Parcelable


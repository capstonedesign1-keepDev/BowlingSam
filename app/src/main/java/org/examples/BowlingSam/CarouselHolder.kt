package org.examples.BowlingSam

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.carousel_item.view.*

//뷰에 아이템을 바인딩해주는 홀더
class CarouselHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val itemImage = itemView.item_imageview
    private val itemText = itemView.item_text
    private val itemSubText = itemView.item_subtext
    private val itemDuration = itemView.duration

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference

    fun bindWithView(carouselItem: CarouselItem) {

        //이미지뷰에 이미지 로드(Glide API 사용)
        storageRef.child(carouselItem.imageSrc).downloadUrl.addOnCompleteListener { task ->
            if(task.isSuccessful){
                Glide.with(itemView).load(task.result).into(itemImage)
            }
            else {

            }
        }

        itemText.text = carouselItem.text
        itemSubText.text = carouselItem.subtext
        itemDuration.text = carouselItem.videotime

    }
}
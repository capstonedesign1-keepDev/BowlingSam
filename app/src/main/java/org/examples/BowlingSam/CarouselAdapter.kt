package org.examples.BowlingSam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.carousel_item.view.*

//캐러셀에 아이템을 세팅하는 어뎁터
class CarouselAdapter(private var carouselList: ArrayList<CarouselItem>, private val onItemClicked: (CarouselItem) -> Unit) : RecyclerView.Adapter<CarouselHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselHolder {
        return CarouselHolder(LayoutInflater.from(parent.context).inflate(R.layout.carousel_item, parent, false))
    }

    override fun onBindViewHolder(holder: CarouselHolder, position: Int) {
        //데이터와 뷰를 묶음
        holder.bindWithView(carouselList[position])

        //carousel 아이템의 전체 부분에 클릭 이벤트 설정
        /*
        holder.itemView.setOnClickListener{
            onItemClicked(carouselList[position])
        }
        */

        //carousel 아이템의 이미지뷰에 클릭 이벤트 설정
        holder.itemView.item_imageview.setOnClickListener {
            onItemClicked(carouselList[position])
        }

        //carousel 아이템의 텍스트에 클릭 이벤트 설정
        holder.itemView.item_text.setOnClickListener {
            onItemClicked(carouselList[position])
        }
    }

    override fun getItemCount(): Int {
        return carouselList.size
    }

}
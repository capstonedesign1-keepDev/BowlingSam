package org.examples.BowlingSam

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.*
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.history_list_item.view.*
import java.util.*


class HistoryListAdapter(val context: Context, val VideoList: ArrayList<VideoListData>) : BaseAdapter() {

    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //firebase firestore
    private lateinit var firestore: FirebaseFirestore

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view : View
        val holder : ViewHolder
        //파이어베이스 로드
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if(convertView == null){
            view = LayoutInflater.from(context).inflate(R.layout.history_list_item, null)
            holder = ViewHolder()
            holder.view_image1 = view.findViewById(R.id.posturePhotoImg)
            holder.view_text1 = view.findViewById(R.id.posture_number)
            holder.view_text2 = view.findViewById(R.id.correct_score)
            holder.view_favorite = view.findViewById(R.id.checkbox_favorite)
            holder.view_anchor = view.findViewById(R.id.anchor_menu)
            view.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            view = convertView
        }

        val item = VideoList[position]
        var scoreStr : String = "평균 점수 : " + (item.score?.toInt()).toString() + "점"

        holder.view_image1?.setImageResource(R.drawable.bowling)
        holder.view_text1?.text = item.videoPath
        holder.view_text2?.text = scoreStr
        holder.view_favorite?.isChecked = item.isFavorite!!

        // 즐겨찾기 상태가 변할 때마다 이 여부를 해당 position의 아이템에 즐겨찾기 상태를 설정
        holder.view_favorite?.setOnCheckedChangeListener { buttonView, isChecked ->
            holder.view_favorite?.isChecked = isChecked
        }

        //아이템을 길게 터치했을 때, Popup 메뉴를 띄우고 아이템 삭제를 할 수 있음
        val contextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenuOverlapAnchor)
        val popup = PopupMenu(contextThemeWrapper, holder.view_anchor, Gravity.NO_GRAVITY)
        popup.menuInflater.inflate(R.menu.context_menu, popup.menu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true)
        }
        popup.setOnMenuItemClickListener { i ->
            when(i.itemId) {
                R.id.menu_delete -> {
                    VideoList.removeAt(position)
                    notifyDataSetChanged()
                    firestore.collection("videolist")
                        .whereEqualTo("uid", firebaseAuth.uid)
                        .get()
                        .addOnSuccessListener{ result ->
                            for(document in result){
                                if(document["videoPath"].toString() == item.videoPath ){
                                    document.reference.delete()
                                }
                            }
                        }
                }
            }
            false
        }

        view.setOnLongClickListener((object : View.OnLongClickListener {
            override fun onLongClick(p0: View?): Boolean {
                popup.show()
                return true
            }
        }))

        // 하트 즐겨찾기 체크 박스 클릭시
        view.checkbox_favorite.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if(view.checkbox_favorite.isChecked){
                    firestore.collection("videolist")
                        .whereEqualTo("uid", firebaseAuth.uid)
                        .get()
                        .addOnSuccessListener{ result ->
                            //videoID를 통해 구분해서 isFavorite 값 업데이트
                            for(document in result){
                                if(document["videoPath"].toString() == item.videoPath ){
                                    if(document["favorite"] as Boolean){
                                        var docName : String = document.id
                                        firestore.collection("videolist").document(docName).update("favorite",false)
                                    }else {
                                        var docName : String = document.id
                                        firestore.collection("videolist").document(docName).update("favorite",true)
                                    }
                                }
                            }
                        }
                } else {
                    firestore.collection("videolist")
                        .whereEqualTo("uid", firebaseAuth.uid)
                        .get()
                        .addOnSuccessListener{ result ->
                            //videoID를 통해 구분해서 isFavorite 값 업데이트
                            for(document in result){
                                if(document["videoPath"].toString() == item.videoPath ){
                                    if(document["favorite"] as Boolean){
                                        var docName : String = document.id
                                        firestore.collection("videolist").document(docName).update("favorite",false)
                                    }else {
                                        var docName : String = document.id
                                        firestore.collection("videolist").document(docName).update("favorite",true)
                                    }
                                }
                            }
                        }
                }
            }
        })


        // 리스티에 있는 아이템 클릭 시 리스너 설정
        view.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(context, HistoryPopupActivity::class.java)
                intent.putExtra("itemvideopath", item.videoPath)

                context.startActivity(intent)
            }
        })


        return view
    }

    override fun getItem(p0: Int): Any {
        return VideoList.get(p0)
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return VideoList.size
    }

    private class ViewHolder {
        var view_image1 : ImageView? = null
        var view_text1 : TextView? = null
        var view_text2 : TextView? = null
        var view_favorite : CheckBox? = null
        var view_anchor : View? = null
    }
}
package org.examples.BowlingSam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment() {

    private lateinit var practiceListFragment: PracticeListFragment
    private lateinit var postureVideoFragment: PostureVideoFragment

    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //firebase firestore
    private lateinit var firestore: FirebaseFirestore

    companion object {
        fun newInstance() : HomeFragment {
            return HomeFragment()
        }
    }

    private var carouselList = ArrayList<CarouselItem>()
    private lateinit var carouselAdapter: CarouselAdapter

    //뷰가 생성되었을 때
    //프레그먼트와 레이아웃을 연결해주는 파트
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var a : String = ""
        var c : Int
        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        val docRef = firestore.collection("users").document(firebaseAuth?.uid.toString())
        docRef.get()
            .addOnSuccessListener { document ->
                if(document != null){

                    a = document["userNickName"].toString()
                    intro_text.setText(a + "님, \n안녕하세요")
                    a = document["ballsize"].toString()
                    c = a.toInt()
                    ballsize_value.setText(""+ c + " lb")
                    a = document["avg"].toString()
                    avg_value.setText(a)
                    a = document["recentScore"].toString()
                    recent_value.setText(a)
                } else {

                }
            }
            .addOnFailureListener { exception ->

            }

        val view = inflater.inflate(R.layout.fragment_home, container, false)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        //DB에서 불러올 데이터 변수 준비
        var strVideoID : String
        var strImageUri : String
        var strTitle : String
        var strCourse : String
        var strLecturer : String
        var strVideoTime : String

        firestore.collection("lecture").get().addOnSuccessListener {task ->
            for(document in task){

                //DB에서 해당 값 가져오기
                strVideoID = document["videoID"] as String
                strImageUri = document["imageUri"] as String
                strTitle = document["title"] as String
                strCourse = document["course"] as String
                strLecturer = document["lecturer"] as String
                strVideoTime = document["time"] as String

                //캐러셀 데이터 배열을 준비
                carouselList.add(CarouselItem(strVideoID, strImageUri,strTitle, strCourse + " 과정 / " + strLecturer, strVideoTime))

                //캐러셀 어뎁터 인스턴스 설정 및 클릭 리스너 설정
                carouselAdapter = CarouselAdapter(carouselList) {
                    var videoID: String = it.videopath
                    var title: String = it.text

                    postureVideoFragment = PostureVideoFragment.newInstance(videoID, title, "설명 없음.\n")
                    parentFragmentManager.beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left,R.anim.exit_to_right).add(R.id.fragments_frame, postureVideoFragment).commit()
                }

                //캐러셀 아이템 여백 및 크기 설정
                val pageMarginPx = resources.getDimensionPixelOffset(R.dimen.pageMargin)
                val pagerWidth = resources.getDimensionPixelOffset(R.dimen.pageWidth)
                val screenWidth = resources.displayMetrics.widthPixels
                val offsetPx = screenWidth - pageMarginPx - pagerWidth

                viewpager.setPageTransformer { page, position ->
                    page.translationX = position * -offsetPx
                }

                //캐러셀 아이템 하나를 미리 로드
                viewpager.offscreenPageLimit = 1

                viewpager.apply {
                    adapter = carouselAdapter
                    orientation = ViewPager2.ORIENTATION_HORIZONTAL
                }

                //두번째 아이템이 가장 먼저 나오도록 설정
                viewpager.currentItem = 1

                //자세 촬영 버튼 클릭 리스너 설정
                home_start_button.setOnClickListener {

                    onHomeStartButtonClicked(view)
                }

                //자세 버튼 클릭 리스너 설정
                home_posture_button.setOnClickListener {
                    onHomePostureButtonClicked()
                }
            }
        }
    }

    //자세 촬영 버튼 클릭 리스너 정의
    private fun onHomeStartButtonClicked(view: View) {
        val navigation: BottomNavigationView = view.rootView.findViewById(R.id.bottom_nav)
        navigation.selectedItemId = R.id.menu_record
    }

    //자세 버튼 클릭 리스너 정의
    private fun onHomePostureButtonClicked() {
        practiceListFragment = PracticeListFragment.newInstance()
        parentFragmentManager.beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left,R.anim.exit_to_right).add(R.id.fragments_frame, practiceListFragment).addToBackStack(null).commit()
    }

}


package org.examples.BowlingSam

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class OnBoardActivity : AppCompatActivity() {

    var onBoardingViewPagerAdapter : OnBoardViewPagerAdapter? = null
    var tabLayout : DotsIndicator? = null
    var onBoardingViewPager : ViewPager? = null
    var next: TextView? = null
    var position = 0
    var sharedPreferences : SharedPreferences? = null
    var startOnboardBtn : Button? = null
    var loginOnboard : LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //만약 앱을 처음 실행한 것이 아니라면, 로그인 화면으로 바로 이동
        if (restorePrefData()) {
            val i = Intent(this, LoginActivity::class.java)  //login화면으로 이동
            startActivity(i)
            finish()
        }
        setContentView(R.layout.activity_onboarding)

        tabLayout = findViewById(R.id.dots_indicator)
        next = findViewById(R.id.tv_next_onboard)
        startOnboardBtn = findViewById(R.id.btn_startOnboard)
        loginOnboard = findViewById(R.id.onboard_login)

        val onBoardingData : MutableList<OnBoardingData> = ArrayList()
        onBoardingData.add(OnBoardingData("볼링 자세 연습을 집에서!", "볼링 자세를 연습해보고 싶은데 \n 볼링장을 가기는 귀찮은 당신.. \n 이제 볼링쌤을 통해 집에서 볼링 자세를 연습해봐요!", R.drawable.bowling_pose1, "간편하게"))
        onBoardingData.add(OnBoardingData("잘못된 자세에 대한 빠른 피드백!","내 자세의 문제점은 무엇인지 확인하고 \n 문제점에 대한 피드백을 받으세요!  \n 자세별로 피드백을 확인할 수 있어요!", R.drawable.feedback, "빠른 피드백"))
        onBoardingData.add(OnBoardingData("볼링을 아예 모르신다구요?", "그런 당신을 위해! \n 볼링쌤은 자세별 영상을 제공합니다! \n자세를 상세하게 나누어 남녀노소 구분없이 이용할 수 있어요!", R.drawable.family, "초보자들을 위한"))
        onBoardingData.add(OnBoardingData("완성된 볼링 자세를!!", "볼링쌤으로 자세를 연습하고 \n 가족,친구,지인들에게 \n 완성된 볼링 자세를 보여주세요!", R.drawable.confidence, "볼림쌤으로"))

        setOnBoardingViewPagerAdapter(onBoardingData)

        position = onBoardingViewPager!!.currentItem

        //앱이 처음이라면, 회원가입 화면으로 이동
        startOnboardBtn?.setOnClickListener {
            savePrefDate()
            val i = Intent(applicationContext, SignUpActivity::class.java)  //회원가입 화면으로 이동
            startActivity(i)
            finish()
        }

        //이미 계정이 있다면, 로그인 화면으로 이동
        loginOnboard?.setOnClickListener {
            savePrefDate()
            val i = Intent(this, LoginActivity::class.java)
            startActivity(i)
            finish()
        }

    }

    private fun setOnBoardingViewPagerAdapter(onBoardingData: List<OnBoardingData>){

        onBoardingViewPager = findViewById(R.id.screenPager)
        onBoardingViewPagerAdapter = OnBoardViewPagerAdapter(this,onBoardingData)
        onBoardingViewPager!!.adapter = onBoardingViewPagerAdapter
        tabLayout?.attachTo(onBoardingViewPager!!)
    }

    //앱을 처음 실행했을 때만 온보딩 화면을 표시하기 위해, 앱 실행 여부를 저장
    private fun savePrefDate() {
        sharedPreferences = applicationContext.getSharedPreferences("pref", Context.MODE_PRIVATE)
        val editor = sharedPreferences!!.edit()
        editor.putBoolean("isFirstTimeRun", true)
        editor.apply()
    }

    private fun restorePrefData(): Boolean {
        sharedPreferences = applicationContext.getSharedPreferences("pref", Context.MODE_PRIVATE)
        return sharedPreferences!!.getBoolean("isFirstTimeRun", false)
    }
}
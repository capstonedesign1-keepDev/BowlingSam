package org.examples.BowlingSam

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_signup.*

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore : FirebaseFirestore
    var TAG = "SignActivity"
    var isRun = true
    var thread = PassChk()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        auth = FirebaseAuth.getInstance()
        thread.start()

        signUp_btn_cancel.setOnClickListener{
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        signUp_btn_submit.setOnClickListener{
            signUp()

        }
    }
    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
    }

    fun signUp(){
        var email = signup_email.getText().toString()
        var password = signup_password.getText().toString()
        var nickName = signup_nickname.text.toString()

        var age_string = signup_age.text.toString()
        var age : Int = age_string.toInt()

        var ballsize_string = signup_size.text.toString()
        var ballsize : Int = ballsize_string.toInt()

        var intent = Intent(this,LoginActivity::class.java)

        //필수 입력항목 채우지 않았을 경우
        if (signup_email.text.toString().equals("") ||
            signup_nickname.text.toString().equals("") ||
            signup_password.text.toString().equals("") ||
            signup_password_check.text.toString().equals("")){

            Toast.makeText(this, "필수 입력항목이 비어있습니다.", Toast.LENGTH_SHORT).show()

        } else {    //필수 입력항목 입력 완료
            if (!email.contains("@")&&email.length<6){
                var toast = Toast.makeText(this,"이메일 형식이 맞지 않습니다",Toast.LENGTH_SHORT)
                toast.show()
            }else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success")
                            val user = auth.currentUser
                            //updateUI(user)
                            isRun = false
                            var toast = Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT)
                            toast.show()

                            //데이터 저장
                            firestore = FirebaseFirestore.getInstance()

                            var userInfo = UsersData()

                            userInfo.uid = auth?.uid
                            userInfo.userNickName = nickName
                            userInfo.userID = email
                            userInfo.age = age
                            userInfo.ballsize = ballsize

                            firestore?.collection("users")?.document(auth?.uid.toString())?.set(userInfo)


                            startActivity(intent)
                            finish()
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.exception)
                            Toast.makeText(
                                baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }
    inner class PassChk:Thread(){
        override fun run() {
            while (isRun){
                SystemClock.sleep(1000)
                var pass1:String = signup_password.text.toString()
                var pass2:String = signup_password_check.text.toString()
                if (pass1.equals(pass2)){
                    runOnUiThread{
                        set_pw_check_error.setText("")
                        signUp_btn_submit.setEnabled(true)
                    }
                }else{
                    runOnUiThread{
                        set_pw_check_error.setText("비밀번호가 맞지 않습니다")
                        signUp_btn_submit.setEnabled(false)
                    }
                }
                if (pass1.length<6){
                    runOnUiThread {
                        set_pw_error.setText("6글자 이상이여야 합니다")
                        signUp_btn_submit.setEnabled(false)
                    }
                } else {
                    runOnUiThread{
                        set_pw_error.setText("")
                        signUp_btn_submit.setEnabled(true)
                    }
                }
            }
        }
    }
}
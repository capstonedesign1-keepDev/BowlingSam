package org.examples.BowlingSam

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.custom_dialog_age.*
import kotlinx.android.synthetic.main.custom_dialog_ballsize.*
import kotlinx.android.synthetic.main.custom_dialog_ballsize.btnCancle
import kotlinx.android.synthetic.main.custom_dialog_ballsize.btnSave
import kotlinx.android.synthetic.main.custom_dialog_nickname.*
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : Fragment() {

    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //firebase firestore
    private lateinit var firestore: FirebaseFirestore

    //google client
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        fun newInstance() : SettingsFragment {
            return SettingsFragment()
        }
    }

    //메모리에 올라갔을 때
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    //메인 액티비티에 붙었을 때
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    //뷰가 생성되었을 때
    //프레그먼트와 레이아웃을 연결해주는 파트
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var a : String = ""
        var b : Int
        var c : Int
        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        val docRef = firestore.collection("users").document(firebaseAuth?.uid.toString())
        docRef.get()
            .addOnSuccessListener { document ->
                if(document != null){
                    a = document["userNickName"].toString()
                    nickNameText.setText(a)
                    a = document["age"].toString()
                    b = a.toInt()
                    ageText.setText("나이 : " + b)
                    a = document["ballsize"].toString()
                    c = a.toInt()
                    ballsizeText.setText("볼사이즈 : " + c + " lb")
                    a = document["grade"].toString()
                    c = a.toInt()
                    gradeText.setText("등급 : " + c)
                    a = document["avg"].toString()
                    c = a.toInt()
                    avgText.setText("평균 정확도 : "+c+"%")
                } else {

                }
            }
            .addOnFailureListener { exception ->

            }

        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var a : String = ""
        var b : Int
        var c : Int


        // 닉네임 변경 버튼 리스너
        myNickName_btn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val dialog = CustomDialogSetting(view, requireContext())
                dialog.selectDialog(1)
                val docRef = firestore.collection("users").document(firebaseAuth?.uid.toString())
                docRef.get()
                    .addOnSuccessListener { document ->
                        if(document != null){
                            a = document["userNickName"].toString()

                        } else {

                        }
                    }
                    .addOnFailureListener { exception ->

                    }
            }
        })

        // 나이 변경 버튼 리스너
        myAge_btn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val dialog = CustomDialogSetting(view, requireContext())
                dialog.selectDialog(2)
                val docRef = firestore.collection("users").document(firebaseAuth?.uid.toString())
                docRef.get()
                    .addOnSuccessListener { document ->
                        if(document != null){
                            a = document["age"].toString()
                            b = a.toInt()
                            ageText.setText("나이 : " + b)
                        } else {

                        }
                    }
                    .addOnFailureListener { exception ->

                    }

            }
        })

        //볼 사이즈 변경 버튼 리스너
        myBallSize_btn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val dialog = CustomDialogSetting(view, requireContext())
                dialog.selectDialog(3)
                val docRef = firestore.collection("users").document(firebaseAuth?.uid.toString())
                docRef.get()
                    .addOnSuccessListener { document ->
                        if(document != null){
                            a = document["ballsize"].toString()
                            c = a.toInt()
                            ballsizeText.setText("볼사이즈 : " + c + " lb")
                        } else {

                        }
                    }
                    .addOnFailureListener { exception ->

                    }
            }
        })

        // 로그아웃 버튼 리스너
        myLogOut_btn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var builder = AlertDialog.Builder(context)
                builder.setMessage("로그아웃 하시겠습니까?")
                    .setPositiveButton("확인",
                        DialogInterface.OnClickListener{ dialog, which ->
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
                            googleSignInClient = GoogleSignIn.getClient(context!!, gso)
                            googleSignInClient.revokeAccess()

                            signOut()

                            activity?.let {
                                val i = Intent(context, LoginActivity::class.java)
                                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(i)
                            }
                        }
                    )
                    .setNegativeButton("취소",
                        DialogInterface.OnClickListener { dialog, which ->

                        }
                    )
                builder.show()
            }
        })
    }
    // 로그아웃
    private fun signOut() {

        firebaseAuth = FirebaseAuth.getInstance()

        // Firebase sign out
        firebaseAuth.signOut()
        // Google sign out

        Toast.makeText(context, "Logout!", Toast.LENGTH_SHORT).show()

    }

}

class CustomDialogSetting(view: View, context: Context) {
    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //firebase firestore
    private lateinit var firestore: FirebaseFirestore
    private val dialog = Dialog(context)
    private val view = view
    val c = context

    fun selectDialog(select : Int){
        when (select){
            1 -> NicknameDialog()
            2 -> AgeDialog()
            3 -> BallsizeDialog()
        }
    }

    fun NicknameDialog(){
        dialog.setContentView(R.layout.custom_dialog_nickname)
        dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()
        dialog.btnCancle.setOnClickListener{
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.btnSave).setOnClickListener {
            if(dialog.edit_text_nickname.text.toString().length == 0){
                Toast.makeText(c, "입력해!!!", Toast.LENGTH_SHORT).show()
            } else {
                val nickname = dialog.edit_text_nickname.text.toString()
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                var userInfo = UsersData()
                userInfo.userNickName = nickname
                firestore?.collection("users")?.document(firebaseAuth?.uid.toString())?.update("userNickName", userInfo.userNickName.toString())
                view.findViewById<TextView>(R.id.nickNameText).setText(nickname)
                Toast.makeText(c, "잘했어!!!", Toast.LENGTH_SHORT).show()

                dialog.dismiss()

            }
        }
    }

    fun AgeDialog(){
        dialog.setContentView(R.layout.custom_dialog_age)
        dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()
        dialog.btnCancle.setOnClickListener{
            dialog.dismiss()
        }
        dialog.btnSave.setOnClickListener {
            if(dialog.edit_text_age.text.toString().length == 0){
                Toast.makeText(c, "입력해!!!", Toast.LENGTH_SHORT).show()
            } else {
                val age_string = dialog.edit_text_age.text.toString()
                val age : Int = age_string.toInt()
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                var userInfo = UsersData()
                userInfo.age = age
                firestore?.collection("users")?.document(firebaseAuth?.uid.toString())?.update("age", userInfo.age)
                view.findViewById<TextView>(R.id.ageText).setText("나이 : $age")
                Toast.makeText(c, age_string, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }

    fun BallsizeDialog(){
        dialog.setContentView(R.layout.custom_dialog_ballsize)
        dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()
        dialog.btnCancle.setOnClickListener{
            dialog.dismiss()
        }
        dialog.btnSave.setOnClickListener {
            if(dialog.edit_text_ballsize.text.toString().length == 0){
                Toast.makeText(c, "입력해!!!", Toast.LENGTH_SHORT).show()
            } else {
                val ballsize_string = dialog.edit_text_ballsize.text.toString()
                val ballsize : Int = ballsize_string.toInt()
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                var userInfo = UsersData()
                userInfo.ballsize = ballsize
                firestore?.collection("users")?.document(firebaseAuth?.uid.toString())?.update("ballsize", userInfo.ballsize)
                view.findViewById<TextView>(R.id.ballsizeText).setText("볼사이즈 : ${ballsize}lb")
                Toast.makeText(c, ballsize_string, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }
}
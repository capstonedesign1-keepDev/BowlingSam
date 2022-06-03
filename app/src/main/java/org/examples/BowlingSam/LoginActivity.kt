package org.examples.BowlingSam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity(){

    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //firestore
    private lateinit var firestore : FirebaseFirestore
    //google client
    private lateinit var googleSignInClient: GoogleSignInClient

    //private const val TAG = "GoogleActivity"
    private val RC_SIGN_IN = 99


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)


        FirebaseApp.initializeApp(this)

        //Google 로그인 옵션 구성. requestIdToken 및 Email 요청
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("198088682198-cfn48p52kgn7s4h4b7m82pn0k0kj7598.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        //firebase auth 객체
        firebaseAuth = FirebaseAuth.getInstance()

        // 로그인
        login_btn.setOnClickListener {
            signIn()
        }

        // 구글 로그인
        sign_in_button.setOnClickListener{
            signIn_google()
        }

        // 회원가입
        val i = Intent(this, SignUpActivity::class.java)
        signUp_btn.setOnClickListener {
            startActivity(i)
        }
    }

    // onStart. 유저가 앱에 이미 구글 로그인을 했는지 확인
    public override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val currentUser = firebaseAuth.currentUser
        if(account!==null){ // 이미 로그인 되어있을시 바로 메인 액티비티로 이동
            toMainActivity(firebaseAuth.currentUser)
            Toast.makeText(
                baseContext, "자동 로그인 중",
                Toast.LENGTH_LONG
            ).show()
        } else if (currentUser !== null){
            moveMainPage(firebaseAuth?.currentUser)
            Toast.makeText(
                baseContext, "자동 로그인 중",
                Toast.LENGTH_LONG
            ).show()
        }

    } //onStart End


    // firebaseAuthWithGoogle
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d("LoginActivity", "firebaseAuthWithGoogle:" + acct.id!!)

        //Google SignInAccount 객체에서 ID 토큰을 가져와서 Firebase Auth로 교환하고 Firebase에 인증
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    //데이터 저장
                    firebaseAuth = FirebaseAuth.getInstance()
                    firestore = FirebaseFirestore.getInstance()
                    val user = firebaseAuth.currentUser
                    var userInfo =UsersData()

                    userInfo.uid = user?.uid
                    userInfo.userNickName = user?.displayName
                    userInfo.userID = user?.email

                    firestore?.collection("users")?.document(firebaseAuth?.uid.toString())?.set(userInfo)

                    Log.w("LoginActivity", "firebaseAuthWithGoogle 성공", task.exception)
                    toMainActivity(firebaseAuth?.currentUser)
                } else {
                    Log.w("LoginActivity", "firebaseAuthWithGoogle 실패", task.exception)
                    Snackbar.make(login_layout, "로그인에 실패하였습니다.", Snackbar.LENGTH_SHORT).show()
                }
            }
    }// firebaseAuthWithGoogle END


    // toMainActivity
    fun toMainActivity(user: FirebaseUser?) {
        if(user !=null) { // MainActivity 로 이동
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    } // toMainActivity End

    // signIn
    private fun signIn_google() {
        val signInIntent = googleSignInClient!!.signInIntent

        startForResult.launch(signInIntent)
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result: ActivityResult ->

            if(result.resultCode ==  RESULT_OK) {
                val intent: Intent = result.data!!
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(intent)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account!!)

                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("LoginActivity", "Google sign in failed", e)
                }
            }
        }
    // 로그인
    private fun signIn(){
        var signInID:String = editID.text.toString()
        var signInPW:String = editPassword.text.toString()
        if (signInID.isNotEmpty() && signInPW.isNotEmpty()) {
            firebaseAuth?.signInWithEmailAndPassword(signInID, signInPW)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            baseContext, "로그인에 성공 하였습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        moveMainPage(firebaseAuth?.currentUser)
                    } else {
                        signInError.setText("이메일 및 비밀번호 확인")
                        Toast.makeText(
                            baseContext, "로그인에 실패 하였습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }else {
            signInError.setText("이메일 및 비밀번호를 입력하세요.")
        }
    }

    // 유저정보 넘겨주고 메인 액티비티 호출
    fun moveMainPage(user: FirebaseUser?){
        if( user!= null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }

    private fun signOut() { // 로그아웃
        // Firebase sign out
        firebaseAuth.signOut()

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this) {
            //updateUI(null)
        }
    }

    private fun revokeAccess() { //회원탈퇴
        // Firebase sign out
        firebaseAuth.signOut()
        googleSignInClient.revokeAccess().addOnCompleteListener(this) {

        }
    }


}
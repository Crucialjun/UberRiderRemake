package com.example.uberriderremake

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.example.uberriderremake.ui.home.HomeViewModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var providers: ArrayList<AuthUI.IdpConfig>
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dataBase: FirebaseDatabase
    private lateinit var riderInfoRef: DatabaseReference

    private lateinit var btnContinue : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        init()
    }

    private fun init() {
        dataBase = FirebaseDatabase.getInstance()
        riderInfoRef = dataBase.getReference(RIDER_INFO_REFERENCE)

        providers = arrayListOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()

        listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if(user != null) {

                checkUserFromFirebase()
            }else{
                showLoginLayout()
            }
        }

    }

    private fun checkUserFromFirebase() {
        riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        //Toast.makeText(this@MainActivity, "User already registered", Toast.LENGTH_SHORT).show()

                        val model = snapshot.getValue(RiderInfoModel::class.java)
                        goToHomeActivity(model)
                    }else{
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun showRegisterLayout() {
        val dialog = Dialog(this,R.style.DialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val itemView = View.inflate(this,R.layout.layout_register,null)

        val firstName = itemView.findViewById<EditText>(R.id.edt_first_name)
        val lastName = itemView.findViewById<EditText>(R.id.edt_last_name)
        val phone = itemView.findViewById<EditText>(R.id.edt_phone_number)

        btnContinue = itemView.findViewById(R.id.btn_continue)

        if(FirebaseAuth.getInstance().currentUser?.phoneNumber != null &&
            TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser?.phoneNumber)){
            firstName.setText(FirebaseAuth.getInstance().currentUser?.phoneNumber)
        }

        dialog.setContentView(itemView)


        dialog.show()




        btnContinue.setOnClickListener {
            val model = RiderInfoModel()
            model.firstName = firstName.text.toString()
            model.lastName = lastName.text.toString()
            model.phoneNumber = phone.text.toString()

            riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(model)
                .addOnFailureListener { p0 ->
                    Toast.makeText(
                        this@SplashScreenActivity,
                        "${p0.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                }.addOnSuccessListener {
                    Toast.makeText(
                        this@SplashScreenActivity,
                        "Registration succesful}",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    goToHomeActivity(model)
                    findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                }
        }
    }

    private fun goToHomeActivity(model: RiderInfoModel?) {
        Common.currentRider = model
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout =
            AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_signin_phone)
            .setGoogleButtonId(R.id.btn_signin_google)
            .build()

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                //.setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build(),
            LOGIN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == LOGIN_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser

            }
            else{
                Toast.makeText(this, "${response?.error?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    private fun delaySplashScreen() {
        Completable.timer(5,
            TimeUnit.SECONDS,
            AndroidSchedulers.mainThread()).subscribe{
            firebaseAuth.addAuthStateListener(listener)
        }
    }
}
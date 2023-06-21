package com.example.exampleotp

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import com.example.exampleotp.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth

    private var storedVerificationId: String? = null
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var KEY:String? = "6Lcy17YmAAAAAHXsAveMbVNZ2ZeP8ZLNpY3udsKS"


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(false)
        auth.firebaseAuthSettings.forceRecaptchaFlowForTesting(true)

        binding.idBtnGetOtp.setOnClickListener {
            if (TextUtils.isEmpty(binding.idEdtPhoneNumber.text.toString())) {
                Toast.makeText(
                    this,
                    "Please enter a valid phone number.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                onClick(it)
            }
        }

        binding.idBtnVerify.setOnClickListener {
            if (TextUtils.isEmpty(binding.idEdtOtp.text.toString())) {
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show()
            } else {
                verifyPhoneNumberWithCode(storedVerificationId, binding.idEdtOtp.text.toString())
            }
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: $credential")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.d(TAG, "onVerificationFailed: ${e.message}")
                binding.tvError.visibility = View.VISIBLE;
                binding.tvError.text = e.toString();
                val handler = Handler()
                handler.postDelayed({
                    binding.tvError.visibility = View.GONE;
                }, 7000)
                if (e is FirebaseAuthInvalidCredentialsException) {
                    Log.d(TAG, "FirebaseAuthInvalidCredentialsException: ${e.message}")
                } else if (e is FirebaseTooManyRequestsException) {
                    Log.d(TAG, "FirebaseTooManyRequestsException: ${e.message}")
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                Log.d(TAG, "onCodeSent: $verificationId")
                storedVerificationId = verificationId
                resendToken = token
            }
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    fun onClick(view: View) {
        SafetyNet.getClient(this).verifyWithRecaptcha(KEY.toString())
            .addOnSuccessListener(this, OnSuccessListener { response ->
                val userResponseToken = response.tokenResult
                if (response.tokenResult?.isNotEmpty() == true) {
                    Log.d(TAG, "userResponseToken:${userResponseToken}")
                    val phone = "+84" + binding.idEdtPhoneNumber.text.toString()
                    sendVerificationCode(phone)
                }
            })
            .addOnFailureListener(this, OnFailureListener { e ->
                if (e is ApiException) {
                    Log.d(TAG, "Error: ${CommonStatusCodes.getStatusCodeString(e.statusCode)}")
                } else {
                    Log.d(TAG, "Error: ${e.message}")
                }
            })
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = task.result?.user
                } else {
                    Log.d(TAG, "signInWithPhoneAuthCredential:${task.exception!!.message}")
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG)
                        .show()
//                    val phone = "+84" + binding.idEdtPhoneNumber.text.toString()
//                    resendVerificationCode(phone, resendToken)
                }
            }
    }

    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
            signInWithPhoneAuthCredential(credential)
        } catch (e: Exception) {
            binding.tvError.visibility = View.VISIBLE;
            binding.tvError.text = e.message.toString();
            val handler = Handler()
            handler.postDelayed({
                binding.tvError.visibility = View.GONE;
            }, 7000)
        }
    }

    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken?,
    ) {
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
        if (token != null) {
            optionsBuilder.setForceResendingToken(token)
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private fun updateUI(user: FirebaseUser? = auth.currentUser) {
    }

    companion object {
        private const val TAG = "PhoneAuthActivity"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
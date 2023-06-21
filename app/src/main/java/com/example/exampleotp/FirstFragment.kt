package com.example.exampleotp

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.exampleotp.databinding.FragmentFirstBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import java.util.concurrent.TimeUnit

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!


    private lateinit var mAuth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        binding.idBtnGetOtp.setOnClickListener {
            if (TextUtils.isEmpty(binding.idEdtPhoneNumber.text.toString())) {
                Toast.makeText(
                    context,
                    "Please enter a valid phone number.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val phone = "+84" + binding.idEdtPhoneNumber.text.toString()
                sendVerificationCode(phone)
            }
        }

        binding.idBtnVerify.setOnClickListener {
            if (TextUtils.isEmpty(binding.idEdtOtp.text.toString())) {
                Toast.makeText(context, "Please enter OTP", Toast.LENGTH_SHORT).show()
            } else {
                verifyCode(binding.idEdtOtp.text.toString())
            }
        }

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Success!!!", Toast.LENGTH_LONG)
                        .show()
//                    val i = Intent(context, SecondFragment::class.java)
//                    startActivity(i)
//                    finish()
                } else {
                    Toast.makeText(context, task.exception!!.message, Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(mAuth!!)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(mCallBack)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val mCallBack: OnVerificationStateChangedCallbacks =
        object : OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(s: String, forceResendingToken: ForceResendingToken) {
                super.onCodeSent(s, forceResendingToken)
                verificationId = s
            }
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                val code = phoneAuthCredential.smsCode
                if (code != null) {
                    binding.idEdtOtp.setText(code)
                    verifyCode(code)
                }
            }
            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }


    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithCredential(credential)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
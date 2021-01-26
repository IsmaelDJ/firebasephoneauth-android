package com.ismatech.firebasephoneauth

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.ismatech.firebasephoneauth.databinding.FragmentPhoneAuthBinding
import java.util.concurrent.TimeUnit


class PhoneAuthFragment : Fragment() {
    val TAG = "PhoneAuth"

    val viewModel: PhoneAuthViewModel by activityViewModels()


    private lateinit var binding: FragmentPhoneAuthBinding
    private lateinit var progress: ProgressDialog
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private val mAuth = FirebaseAuth.getInstance()


    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(p0: PhoneAuthCredential) {
            Log.d(TAG, "onVerificationCompleted")
            progress.dismiss()

            binding.statusText.text = getString(R.string.veuillez_saisir_le_code_de_validation)
            verificationUI()
            val code = p0.smsCode
            code?.let {
                binding.verifyField.setText(code)
            }

            progress.show()
            signInWithPhoneAuthCredential(p0)
        }

        override fun onVerificationFailed(p0: FirebaseException) {
            progress.dismiss()
            Log.w(TAG, "onVerificationFailed", p0)
            if (p0 is FirebaseAuthInvalidCredentialsException) {
                Log.w(TAG, "FirebaseAuthInvalidCredentialsException")
                binding.statusText.text = getString(R.string.erreur_de_verification_reessayez_svp)
                phoneUI()
            } else if (p0 is FirebaseTooManyRequestsException) {
                Log.w(TAG, "The SMS quota for the project has been exceeded")
            }
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")
            progress.dismiss()

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token
            binding.statusText.text = getString(R.string.veuillez_saisir_le_code_de_validation)
            verificationUI()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_phone_auth, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress = ProgressDialog(requireContext())
        progress.setMessage(getString(R.string.patientez_svp))

        phoneUI()

        binding.apply {
            phoneButton.setOnClickListener { sendCode() }
            verifyButton.setOnClickListener { verifyCode() }
            phoneField.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    sendCode()
                }
                false
            }
            verifyField.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    verifyCode()
                }
                false
            }
        }
    }

    private fun verifyCode() {
        val code = binding.verifyField.text.toString()
        if (code.isEmpty()) {
            binding.verifyField.error = getString(R.string.champ_a_remplir)
            return
        }

        val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
        progress.show()
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    progress.dismiss()
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    viewModel.signInSuccess()

                    findNavController().navigateUp()

                } else {
                    progress.dismiss()
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        binding.statusText.text =
                            getString(R.string.erreur_de_verification_reessayez_svp)
                        phoneUI()
                    }
                }
            }
    }

    private fun sendCode() {
        var phone = binding.phoneField.text.toString()
        if (phone.isEmpty()) {
            binding.phoneField.error = getString(R.string.champ_a_remplir)
            return
        }
        phone = "${binding.ccp.selectedCountryCodeWithPlus}$phone"

        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(callbacks)
            .setActivity(requireActivity())
            .build()
        progress.show()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun phoneUI() {
        binding.phoneLayout.visibility = View.VISIBLE
        binding.verificationLayout.visibility = View.GONE
    }

    fun verificationUI() {
        binding.phoneLayout.visibility = View.GONE
        binding.verificationLayout.visibility = View.VISIBLE
    }

}
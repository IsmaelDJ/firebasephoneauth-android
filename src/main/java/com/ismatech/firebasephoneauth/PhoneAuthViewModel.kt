package com.ismatech.firebasephoneauth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PhoneAuthViewModel : ViewModel() {

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn = _isLoggedIn

    fun signInSuccess() {
        _isLoggedIn.value = true
    }
}
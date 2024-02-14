package com.example.zimarix_1.ui.login

import android.util.Base64
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zimarix_1.R
import com.example.zimarix_1.data.LoginRepository
import kotlinx.coroutines.*
import java.net.Socket
import java.net.SocketException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.example.zimarix_1.RSA_encrpt
import com.example.zimarix_1.aes_decrpt
import com.example.zimarix_1.aes_encrpt
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import com.example.zimarix_1.zimarix_global.Companion.appid
import com.example.zimarix_1.zimarix_global.Companion.appkey
import com.example.zimarix_1.zimarix_global.Companion.dev_mac
import com.example.zimarix_1.zimarix_global.Companion.zimarix_server
import getRandomString
import java.io.InputStream
import java.util.*


class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {
    var ser_resp = ""
    var reg_resp = ""
    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    @RequiresApi(Build.VERSION_CODES.M)
    fun login(username: String, password: String) :String{
        // can be launched in a separate asynchronous job

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder("MyKeyAlias",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()

        if(!Patterns.EMAIL_ADDRESS.matcher(username).matches() || username.isBlank()){
            return "Enter Valid Email.(username)"
        }

        if(password.length < 5){
            return "Password Length Invalid"
        }
        reg_resp = ""
        viewModelScope.launch(Dispatchers.IO) {
            reg_resp = send_login_data_to_server(username, password)
        }
        while(reg_resp == ""){
            Thread.sleep(100)
        }
        if(reg_resp.contains("SUCCESS"))
            _loginResult.value = LoginResult(success = LoggedInUserView(displayName = username))
        return reg_resp
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
    fun send_login_data_to_server(username: String, password: String):String{

        var resp = "LOGIN FAILURE"
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        try {
            val client = Socket(zimarix_server, 11113)
            client!!.outputStream.write("LOGIN".toByteArray())
            //Get Public Key from Server
            var publickey = ""
            val inputStream: InputStream = client.getInputStream()
            // Read data into the buffer
            bytesRead = inputStream.read(buffer)
            // Convert the received bytes to a string (assuming text data)
            publickey = String(buffer, 0, bytesRead)
            if (publickey.contains("END PUBLIC KEY")) {
                val randomKey = getRandomString(16)
                //Encrypt the Random key with public key and send to server
                var encrypted: ByteArray? = RSA_encrpt(publickey, randomKey)
                client!!.outputStream.write(encrypted)

                bytesRead = inputStream.read(buffer)
                if(bytesRead == 16) {
                    appkey = aes_decrpt(randomKey, "abcdefghijklmnop", buffer.copyOf(bytesRead))
                    val reg_data = username + "," + password +','+ dev_mac
                    val ciphertext: ByteArray = aes_encrpt(appkey, "abcdefghijklmnop", reg_data)
                    client!!.outputStream.write(ciphertext)
                    bytesRead = inputStream.read(buffer)
                    if(bytesRead > 0) {
                        val login_resp =
                            aes_decrpt(appkey, "abcdefghijklmnop", buffer.copyOf(bytesRead))
                        val param = login_resp.split(",")
                        if (param.size >= 2) {
                            if (param[0] != "-1") {
                                appid = param[0]
                            }
                            resp = param[1]
                        }
                    }
                }
            }
            client!!.close()
        }catch (t: SocketException){
            resp = "UNABLE TO CONNECT TO SERVER. PLEASE TRY AFTER SOMETIME"
        }
        return resp
    }

    lateinit var client : Socket
    fun send_reg_data_to_server(username: String, password: String, mobile: String):String{
        var resp = "FAILED TO CONNECT TO SERVER"
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        try {
            client = Socket(zimarix_server, 11113)
            client!!.outputStream.write("REG".toByteArray())

            //Get Public Key from Server
            var publickey = ""
            val inputStream: InputStream = client.getInputStream()
            // Read data into the buffer
            bytesRead = inputStream.read(buffer)
            // Convert the received bytes to a string (assuming text data)
            publickey = String(buffer, 0, bytesRead)
            if (publickey.contains("END PUBLIC KEY")) {
                //Generate a random key in device
                val randomKey = getRandomString(16)
                //Encrypt the Random key with public key and send to server
                var encrypted: ByteArray? = RSA_encrpt(publickey, randomKey)
                client!!.outputStream.write(encrypted)

                bytesRead = inputStream.read(buffer)
                if(bytesRead == 16) {
                    appkey = aes_decrpt(randomKey, "abcdefghijklmnop", buffer.copyOf(bytesRead))
                    val reg_data = username + "," + password + "," + mobile +','+dev_mac
                    val ciphertext: ByteArray = aes_encrpt(appkey,"abcdefghijklmnop", reg_data)
                    client!!.outputStream.write(ciphertext)

                    bytesRead = inputStream.read(buffer)
                    Log.d("debug ", " ------------------received $bytesRead bytes\n")
                    resp = aes_decrpt(appkey,"abcdefghijklmnop", buffer.copyOf(bytesRead))
                    Log.d("debug ", " received ======  ff $resp")
                    if(!resp.contains("OTP")){
                        client!!.close()
                    }
                }
            }
        }catch (t: SocketException){
            Log.d("debug ", " ------------------socket exception in registration connect failure\n")
            resp = "Unable to connect to server. Check network"
        }
        return resp
    }

    fun send_pwd_rst_to_server(username: String):String{
        var resp = "FAILED TO CONNECT TO SERVER"
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        try {
            client = Socket(zimarix_server, 11113)
            client!!.outputStream.write("PWDRST".toByteArray())

            //Get Public Key from Server
            var publickey = ""
            val inputStream: InputStream = client.getInputStream()
            // Read data into the buffer
            bytesRead = inputStream.read(buffer)
            // Convert the received bytes to a string (assuming text data)
            publickey = String(buffer, 0, bytesRead)
            if (publickey.contains("END PUBLIC KEY")) {
                //Generate a random key in device
                val randomKey = getRandomString(16)
                //Encrypt the Random key with public key and send to server
                var encrypted: ByteArray? = RSA_encrpt(publickey, randomKey)
                client!!.outputStream.write(encrypted)

                bytesRead = inputStream.read(buffer)
                if(bytesRead == 16) {
                    appkey = aes_decrpt(randomKey, "abcdefghijklmnop", buffer.copyOf(bytesRead))
                    val reg_data = username
                    val ciphertext: ByteArray = aes_encrpt(appkey,"abcdefghijklmnop", reg_data)
                    client!!.outputStream.write(ciphertext)

                    bytesRead = inputStream.read(buffer)
                    resp = aes_decrpt(appkey,"abcdefghijklmnop", buffer.copyOf(bytesRead))
                    Log.d("debug ", " received ======  ff $resp")
                    if(!resp.contains("OTP")){
                        client!!.close()
                    }
                }
            }
        }catch (t: SocketException){
            Log.d("debug ", " ------------------socket exception in registration connect failure\n")
            resp = "Unable to connect to server. Check network"
        }
        return resp
    }

    fun verify_otp(otp_msg: String): String {
        var resp = ""
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        try {
            //Encrypt OTP string
            val ciphertext: ByteArray = aes_encrpt(appkey,"abcdefghijklmnop", otp_msg)
            client.outputStream.write(ciphertext)
            val inputStream: InputStream = client.getInputStream()
            val bytesRead = inputStream.read(buffer)
            val reg_resp = aes_decrpt(appkey,"abcdefghijklmnop", buffer.copyOf(bytesRead))
            Log.d("debug ", " received ======  ff $reg_resp")
            val param = reg_resp.split(",")
            if (param.size >= 2){
                if (param[0] != "-1") {
                    appid = param[0]
                }
                resp = param[1]
            }
        } catch (t: SocketException){
            resp = "Unable to connect to server. Check network"
        }
        return resp
    }

    fun send_update_password(password: String): String {
        var resp = ""
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        try {
            //Encrypt OTP string
            val ciphertext: ByteArray = aes_encrpt(appkey,"abcdefghijklmnop", password)
            client.outputStream.write(ciphertext)
            val inputStream: InputStream = client.getInputStream()
            val bytesRead = inputStream.read(buffer)
            if (bytesRead > 0) {
                resp = aes_decrpt(appkey, "abcdefghijklmnop", buffer.copyOf(bytesRead))
            }else{
                resp = "0,FAILED to get response from server"
            }
        } catch (t: SocketException){
            resp = "0,Unable to connect to server. Check network"
        }
        return resp
    }

    fun close_reg_socket(){
        viewModelScope.launch(Dispatchers.IO){
            client.close()
        }
    }
    fun getKey(): SecretKey {
        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)

        val secretKeyEntry = keystore.getEntry("MyKeyAlias", null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    fun encryptkey(): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val data = appkey+","+ appid
        var temp = data
        while (temp.toByteArray().size % 16 != 0)
            temp += "\u0020"
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val ivBytes = cipher.iv
        val encryptedBytes = cipher.doFinal(temp.toByteArray(Charsets.UTF_8))
        return Pair(ivBytes, encryptedBytes)
    }

    fun otp_validate(mobile_otp: String, email_otp: String): String {
         ser_resp = ""
        if (mobile_otp.length > 6)
            return "INVALID MOBILE OTP"
        if (email_otp.length > 6)
            return "INVALID EMAIL OTP"
         val reg_data = mobile_otp + "," + email_otp+","
        viewModelScope.launch(Dispatchers.IO){
            ser_resp = verify_otp(reg_data)
        }
         while(ser_resp == ""){
             Thread.sleep(100)
         }
         return ser_resp
    }

    fun mobile_otp_validate(mobile_otp: String): String {
        ser_resp = ""
        if (mobile_otp.length > 6)
            return "INVALID MOBILE OTP"
        val reg_data = mobile_otp
        viewModelScope.launch(Dispatchers.IO){
            ser_resp = verify_otp(reg_data)
        }
        while(ser_resp == ""){
            Thread.sleep(100)
        }
        return ser_resp
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun register(username: String, password: String, password1: String, name: String, mobile: String): String {
        // can be launched in a separate asynchronous job
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder("MyKeyAlias",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()

        if(!Patterns.EMAIL_ADDRESS.matcher(username).matches() || username.isBlank()){
            return "Enter Valid Email.(username)"
        }

        if(password.length < 5){
            return "Password Length Invalid"
        }

        if(password != password1) {
            return "Password Do No Match"
        }

        if(name.isBlank()) {
            return "Enter Valid Name"
        }

        if(mobile.isBlank() || mobile.length < 10 || mobile.length > 15) {
            return "Enter Valid Mobile"
        }
        reg_resp = ""
        viewModelScope.launch(Dispatchers.IO) {
            reg_resp = send_reg_data_to_server(username, password, mobile)
        }
        while(reg_resp == ""){
            Thread.sleep(100)
        }
        return reg_resp
    }

    fun forgot_password(username: String): String {

        if(!Patterns.EMAIL_ADDRESS.matcher(username).matches() || username.isBlank()){
            return "Enter Valid Email.(username)"
        }
        reg_resp = ""
        viewModelScope.launch(Dispatchers.IO) {
            reg_resp = send_pwd_rst_to_server(username)
        }
        while(reg_resp == ""){
            Thread.sleep(100)
        }
        return reg_resp
    }
    fun send_password_update(password: String, password1: String): String {

        if(password.length < 5){
            return "-1,Password Length Invalid"
        }

        if(password != password1) {
            return "-1,Password Do No Match"
        }

        reg_resp = ""
        viewModelScope.launch(Dispatchers.IO) {
            reg_resp = send_update_password(password)
        }
        while(reg_resp == ""){
            Thread.sleep(100)
        }
        return reg_resp
    }

}
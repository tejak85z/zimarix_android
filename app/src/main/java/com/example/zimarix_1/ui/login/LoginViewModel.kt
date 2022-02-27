package com.example.zimarix_1.ui.login

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.util.Base64
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zimarix_1.R
import com.example.zimarix_1.data.LoginRepository
import com.example.zimarix_1.data.Result
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.net.wifi.WifiInfo

import androidx.core.content.ContextCompat.getSystemService

import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.example.zimarix_1.AES_decrpt
import com.example.zimarix_1.AES_encrpt
import com.example.zimarix_1.getRandomString
import com.example.zimarix_1.zimarix_global.Companion.appid
import com.example.zimarix_1.zimarix_global.Companion.appkey
import com.example.zimarix_1.zimarix_global.Companion.dev_mac
import com.example.zimarix_1.zimarix_global.Companion.zimarix_server
import java.lang.StringBuilder
import java.net.NetworkInterface
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
        if(reg_resp.contains("OK"))
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

    fun RSA_encrpt(publickey: String, data: String):ByteArray{
        var publicKy = publickey.replace("\\r".toRegex(), "")
            .replace("\\n".toRegex(), "")
            //.replace(System.lineSeparator().toRegex(), "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")

        //Encrypt the data with public key
        var encrypted: ByteArray? = null
        val publicBytes = Base64.decode(publicKy, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val pubKey = keyFactory.generatePublic(keySpec)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING") //or try with "RSA"
        cipher.init(Cipher.ENCRYPT_MODE, pubKey)
        encrypted = cipher.doFinal(data.toByteArray())
        return encrypted
    }



    lateinit var client : Socket
    fun send_login_data_to_server(username: String, password: String):String{
        var resp = "OK"
        try {
            client = Socket(zimarix_server, 11113)
            client!!.outputStream.write("LOGIN".toByteArray())
            Log.i("TAG", "1  =============== ")
            //Get Public Key from Server
            var publickey = ""
            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            while (true) {
                val line = bufferReader.readLine()
                if (line == "-----END PUBLIC KEY-----" || line.length == 0)
                    break
                publickey += line
            }

            //Generate a random key in device
            val randomKey = getRandomString(16)
            //Encrypt the Random key with public key and send to server
            var encrypted: ByteArray? = RSA_encrpt(publickey,randomKey)
            client!!.outputStream.write(encrypted)

            //Receive server AES key which is encrypted with random key
            val enc_ser_key = bufferReader.readLine()
            val decoded_key = Base64.decode(enc_ser_key, Base64.NO_PADDING)

            appkey = AES_decrpt(randomKey,decoded_key)

            val reg_data = username + "," + password +','+ dev_mac
            val ciphertext: ByteArray = AES_encrpt(appkey, reg_data)

            client!!.outputStream.write(ciphertext)

            resp = bufferReader.readLine()
            if(resp.contains("OK")){
                appid = resp.split(",")[1]
                resp = "OK"
            }else if(resp.contains("NU")){
                resp = "Invalid user. Register to create new profile"
            }else if(resp.contains("NP")){
                resp = "Wrong Password"
            }
            client!!.close()
        }catch (t: SocketException){
            resp = "Unable to connect to server. Check network"
        }
        return resp
    }

    fun send_reg_data_to_server(username: String, password: String, mobile: String):String{
        var resp = "OK"
        try {
            client = Socket(zimarix_server, 11113)
            client!!.outputStream.write("REG".toByteArray())

            //Get Public Key from Server
            var publickey = ""
            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            while (true) {
                val line = bufferReader.readLine()
                if (line == "-----END PUBLIC KEY-----" || line.length == 0)
                    break
                publickey += line
            }

            //Generate a random key in device
            val randomKey = getRandomString(16)
            //Encrypt the Random key with public key and send to server
            var encrypted: ByteArray? = RSA_encrpt(publickey,randomKey)
            client!!.outputStream.write(encrypted)
            //Receive server AES key which is encrypted with random key
            val enc_ser_key = bufferReader.readLine()
            val decoded_key = Base64.decode(enc_ser_key, Base64.NO_PADDING)
            appkey = AES_decrpt(randomKey,decoded_key).split(",")[0]

            val reg_data = username + "," + password + "," + mobile +','+dev_mac
            val ciphertext: ByteArray = AES_encrpt(appkey, reg_data)
            client!!.outputStream.write(ciphertext)
            Log.d("debug ", " ======  ff $appkey  ${reg_data.length} $reg_data ${ciphertext.size}")
            resp = bufferReader.readLine()
            if(!resp.contains("OK")){
                client!!.close()
                if (resp.contains("UE")){
                    resp = "Email "+username+" is registered already"
                }else if(resp.contains("ME")){
                    resp = "Mobile "+mobile+" is registered already"
                }
            }
        }catch (t: SocketException){
            resp = "Unable to connect to server. Check network"
        }
        return resp
    }

    fun verify_otp(otp_msg: String): String {
        var resp = ""
        try {
            //Encrypt OTP string
            val ciphertext: ByteArray = AES_encrpt(appkey, otp_msg)
            client!!.outputStream.write(ciphertext)

            val bufferReader = BufferedReader(InputStreamReader(client!!.inputStream))
            resp = bufferReader.readLine()

            if(resp.contains("IV")){
                resp = "Invalid OTP"
            }else if(resp.contains("IM")){
                resp = "Invalid Mobile OTP"
            }else if(resp.contains("IE")){
                resp = "Invalid Email OTP"
            }else if(resp.contains("OK")){
                appid = resp.split(",")[1]
                resp="OK"
            }
        } catch (t: SocketException){
            resp = "Unable to connect to server. Check network"
        }
        return resp
    }

    fun close_reg_socket(){
        viewModelScope.launch(Dispatchers.IO){
            client!!.close()
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
         val reg_data = mobile_otp + "," + email_otp+","
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

}
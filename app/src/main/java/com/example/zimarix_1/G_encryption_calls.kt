package com.example.zimarix_1

import android.util.Base64
import android.util.Log
import getKey
import java.lang.Exception
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

///////////////// AES encryption calls //////////////////////////////
fun aes_encrpt(key: String, iv: String, data: String):ByteArray{
    if (key.length != 16 || iv.length != 16)
        return "".toByteArray()
    val keyBytes = key.toByteArray(charset("UTF8"))
    val skey = SecretKeySpec(keyBytes, "AES")
    val siv = IvParameterSpec(iv.toByteArray())

    val AEScipher = Cipher.getInstance("AES/CBC/NoPadding")
    AEScipher.init(Cipher.ENCRYPT_MODE, skey, siv)

    var padlen = 0
    if(data.length % 16 != 0)
        padlen = 16 - (data.length % 16)
    val padstr = data.padEnd(data.length + padlen, ',')
    val enc_data: ByteArray = AEScipher.doFinal(padstr.toByteArray(Charsets.UTF_8))
    return enc_data
}

fun aes_decrpt(key: String, iv: String, data: ByteArray):String{
    if(data.size % 16 != 0)
        return ""
    if (key.length != 16 || iv.length != 16)
        return ""
    val keyBytes = key.toByteArray(charset("UTF8"))
    val skey = SecretKeySpec(keyBytes, "AES")
    val siv = IvParameterSpec(iv.toByteArray())

    val AEScipher = Cipher.getInstance("AES/CBC/NoPadding")
    AEScipher.init(Cipher.DECRYPT_MODE, skey, siv)
    try {
        val dec_data = AEScipher.doFinal(data).toString(Charsets.UTF_8)
        return dec_data
    }catch (t: Exception){
        return ""
    }
    return ""
}

fun aes_decrpt_byte(key: String, iv: String, data: ByteArray):ByteArray{
    if(data.size % 16 != 0) {
        Log.d("debug ", " -----------------data size is not 16 ${data.size}\n")
        return "".toByteArray()
    }
    if (key.length != 16 || iv.length != 16) {
        Log.d("debug ", " -----------------key size is not 16 $key $iv\n")
        return "".toByteArray()
    }
    val keyBytes = key.toByteArray(charset("UTF8"))
    val skey = SecretKeySpec(keyBytes, "AES")
    val siv = IvParameterSpec(iv.toByteArray())

    val AEScipher = Cipher.getInstance("AES/CBC/NoPadding")
    AEScipher.init(Cipher.DECRYPT_MODE, skey, siv)
    try {
        val dec_data = AEScipher.doFinal(data)
        return dec_data
    }catch (t: Exception){
        return "".toByteArray()
    }
    return "".toByteArray()
}

fun decryptData(ivBytes: ByteArray, data: ByteArray): String{
    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    val spec = IvParameterSpec(ivBytes)

    cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
    return cipher.doFinal(data).toString(Charsets.UTF_8).trim()
}

///////////////// RSA encryption calls //////////////////////////////

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
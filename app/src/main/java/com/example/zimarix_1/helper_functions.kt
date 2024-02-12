import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import com.example.zimarix_1.decryptData
import com.example.zimarix_1.zimarix_global
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.SecretKey

fun showLogoutConfirmationDialog(title:String, msg:String, context: Context, callback: () -> Unit) {
    val alertDialogBuilder = AlertDialog.Builder(context)
    alertDialogBuilder.setTitle(title)
    alertDialogBuilder.setMessage(msg)
    alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
        // Invoke the callback when the user confirms logout
        callback.invoke()
    }
    alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
        dialog.dismiss()
    }
    val alertDialog = alertDialogBuilder.create()
    alertDialog.show()
}

fun clearAppData(context: Context) {
    try {
        val packageName = context.packageName
        val runtime = Runtime.getRuntime()
        runtime.exec("pm clear $packageName")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun load_app_id_and_key(prefs : SharedPreferences):Int{
    val SEncKey = prefs.getString("key", "No data")
    val SEncIv = prefs.getString("iv", "No iv")
    if (SEncKey != "No data" || SEncIv != "No iv") {
        val BEncKey = Base64.decode(SEncKey, Base64.DEFAULT);
        val BEncIv = Base64.decode(SEncIv, Base64.DEFAULT);
        val idkey = decryptData(BEncIv, BEncKey)
        zimarix_global.appid = idkey.split(",")[1]
        zimarix_global.appkey = idkey.split(",")[0]
        Log.d("debug ", " ======  ff ${zimarix_global.appkey}  ${zimarix_global.appid}")
        return 0
    }
    return -1;
}

fun getRandomString(length: Int) : String {
    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { charset.random() }
        .joinToString("")
}

fun getKey(): SecretKey {
    val keystore = KeyStore.getInstance("AndroidKeyStore")
    keystore.load(null)

    val secretKeyEntry = keystore.getEntry("MyKeyAlias", null) as KeyStore.SecretKeyEntry
    return secretKeyEntry.secretKey
}

fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
fun get_device_mac(context: Context): String {
    /*val interfacesList: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
    interfacesList.forEach {
        if (it.displayName == "wlan0") {
            val address = it.hardwareAddress
            val dev_mac = StringBuilder()
            for (b in address) {
                //res1.append(Integer.toHexString(b & 0xFF) + ":");
                dev_mac.append(String.format("%02X", b))
            }
            return dev_mac.toString()
        }
    }
     */
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val serialNumber = Build.SERIAL
    val model = Build.MODEL
    val manufacturer = Build.MANUFACTURER

    val combinedInfo = "$androidId$serialNumber$model$manufacturer"

    return sha256(combinedInfo)
}
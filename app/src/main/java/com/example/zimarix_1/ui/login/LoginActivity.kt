package com.example.zimarix_1.ui.login

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.zimarix_1.MainActivity
import com.example.zimarix_1.databinding.ActivityLoginBinding

import com.example.zimarix_1.R
import com.example.zimarix_1.get_device_mac
import com.example.zimarix_1.load_app_id_and_key
import com.example.zimarix_1.zimarix_global.Companion.dev_mac
import java.util.*


class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading
        val register = binding.register

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        dev_mac = get_device_mac()

        val prefs = getSharedPreferences(getString(R.string.dev_encryption_key), MODE_PRIVATE)
        val ret = load_app_id_and_key(prefs)
        if (ret == 0) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                val ret = loginViewModel.login(username.text.toString(), password.text.toString())
                if(ret.contains("OK")){
                    val editor = getSharedPreferences(getString(R.string.dev_encryption_key), MODE_PRIVATE).edit()
                    val enc_params = loginViewModel.encryptkey()
                    val enciv = Base64.encodeToString(enc_params.first, Base64.DEFAULT);
                    val enckey = Base64.encodeToString(enc_params.second, Base64.DEFAULT);
                    editor.putString("iv", enciv)
                    editor.putString("key", enckey)
                    editor.apply()
                }
            }

            if (register != null) {
                register.setOnClickListener {
                    val layout = LinearLayout(this@LoginActivity)
                    layout.orientation = LinearLayout.VERTICAL

                    val username = EditText(this@LoginActivity)
                    username.setSingleLine()
                    username.hint = "Email Associated with Alexa Account"
                    layout.addView(username)

                    val password = EditText(this@LoginActivity)
                    password.setSingleLine()
                    password.hint = "Setup Account Password"
                    layout.addView(password)

                    val password1 = EditText(this@LoginActivity)
                    password1.setSingleLine()
                    password1.hint = "Re-enter Account Password"
                    layout.addView(password1)

                    val name = EditText(this@LoginActivity)
                    name.setSingleLine()
                    name.hint = "Account  Name"
                    layout.addView(name)

                    val mobile = EditText(this@LoginActivity)
                    mobile.setSingleLine()
                    mobile.hint = "Enter Mobile Number"
                    layout.addView(mobile)

                    layout.setPadding(50, 40, 50, 10)

                    val builder = AlertDialog.Builder(context)
                        .setTitle("User Registration")
                        .setMessage("Enter the following details to register fresh")
                        .setView(layout)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                            // so something, or not - dialog will close
                        }
                    val dialog = builder.create()

                    dialog.setOnShowListener {
                        val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        okButton.setOnClickListener {
                            // dialog won't close by default
                            val ret = loginViewModel.register(
                                username.text.toString(),
                                password.text.toString(),
                                password1.text.toString(),
                                name.text.toString(),
                                mobile.text.toString())
                            if(ret.contains("OK")) {
                                openotpdialog()
                                dialog.dismiss()
                                return@setOnClickListener
                            }else{
                                Toast.makeText(this@LoginActivity, ret, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    dialog.show()
                }
            }
        }
    }

    private fun openotpdialog(){
        val layout1 = LinearLayout(this)
        layout1.orientation = LinearLayout.VERTICAL
        val mobile_otp = EditText(this)
        mobile_otp.setSingleLine()
        mobile_otp.hint = "Enter OTP From moble"
        layout1.addView(mobile_otp)

        val email_otp = EditText(this)
        email_otp.setSingleLine()
        email_otp.hint = "Enter OTP From email"
        layout1.addView(email_otp)

        layout1.setPadding(50, 40, 50, 10)
        val builder = AlertDialog.Builder(this)
            .setTitle("Verify OTP To complete Registration")
            .setMessage("Retry if otp failed to receive within 90 sec")
            .setView(layout1)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                // so something, or not - dialog will close
            }
        val dialog1 = builder.create()
        dialog1.setOnShowListener {
            val okButton = dialog1.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                // dialog won't close by default
                val motp = mobile_otp.text.toString()
                val eotp = email_otp.text.toString()
                val ret = loginViewModel.otp_validate(motp,eotp)
                if(ret == "OK") {
                    val editor = getSharedPreferences(getString(R.string.dev_encryption_key), MODE_PRIVATE).edit()
                    val enc_params = loginViewModel.encryptkey()
                    val enciv = Base64.encodeToString(enc_params.first, Base64.DEFAULT);
                    val enckey = Base64.encodeToString(enc_params.second, Base64.DEFAULT);
                    editor.putString("iv", enciv)
                    editor.putString("key", enckey)
                    editor.apply()
                    dialog1.dismiss()
                    loginViewModel.close_reg_socket()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    return@setOnClickListener
                }
            }
        }
        dialog1.show()
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
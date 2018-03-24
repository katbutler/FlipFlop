package com.katbutler.flipflop

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.katbutler.flipflop.BuildConfig.CLIENT_ID
import com.spotify.sdk.android.authentication.LoginActivity.REQUEST_CODE
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse

class FlipFlopActivity : AppCompatActivity() {

    companion object {
        const val TAG = "FlipFlopActivity"
    }

    private val loginButton by lazy {
        findViewById<Button>(R.id.login_button)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flip_flop)

        loginButton.setOnClickListener {
            spotifyLogin()
        }
    }

    private fun spotifyLogin() {
        val builder = AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                getRedirectUri().toString())
        builder.setScopes(arrayOf("user-read-private", "streaming"))
        val request = builder.build()

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE -> onLoginResult(resultCode, data)
            else -> Log.d(TAG, "Unhandled request code $requestCode")
        }
    }

    private fun onLoginResult(resultCode: Int, data: Intent?) {
        val response = AuthenticationClient.getResponse(resultCode, intent)

        Log.d(TAG, "onLoginResult $resultCode ${data?.data} $response.accessToken")
    }

    private fun getRedirectUri(): Uri {
        return Uri.Builder()
                .scheme(getString(R.string.com_spotify_sdk_redirect_scheme))
                .authority(getString(R.string.com_spotify_sdk_redirect_host))
                .build()
    }
}

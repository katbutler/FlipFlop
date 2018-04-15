package com.katbutler.flipflop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.authentication.LoginActivity
import com.spotify.sdk.android.player.SpotifyPlayer

/**
 * Created by kat on 2018-03-24.
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        const val TAG = "LoginActivity"
        const val EXTRA_IS_SWITCH_ACCOUNT = "isSwitchAccount"

        fun showLoginActivity(activity: Activity, isSwitchAccount: Boolean = false) {
            val loginIntent = Intent(activity, com.katbutler.flipflop.LoginActivity::class.java).apply {
                putExtra(EXTRA_IS_SWITCH_ACCOUNT, isSwitchAccount)
            }

            activity.startActivity(loginIntent)
            activity.finish()
        }
    }

    private val loginButton by lazy {
        findViewById<Button>(R.id.login_button)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d(TAG, "onCreate")

        if (intent.getBooleanExtra(EXTRA_IS_SWITCH_ACCOUNT, false)) {
            spotifyLogin()
        }

        loginButton.setOnClickListener {
            spotifyLogin()
        }
    }

    //region Spotify Authentication
    private fun spotifyLogin() {
        val builder = AuthenticationRequest.Builder(BuildConfig.CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                getRedirectUri().toString())
                .setScopes(arrayOf("user-read-private", "streaming", "playlist-read-private", "user-modify-playback-state"))
                .setShowDialog(true)
        val request = builder.build()

        AuthenticationClient.openLoginActivity(this, LoginActivity.REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            LoginActivity.REQUEST_CODE -> onLoginResult(resultCode, data)
            else -> Log.d(FlipFlopActivity.TAG, "Unhandled request code $requestCode")
        }
    }

    private fun onLoginResult(resultCode: Int, data: Intent?) {
        val response = AuthenticationClient.getResponse(resultCode, data)

        response?.error?.let {
            Toast.makeText(this, "Error: ${response.error}", Toast.LENGTH_LONG).show()
            com.katbutler.flipflop.LoginActivity.showLoginActivity(this, true)
            return@onLoginResult
        }

        response.accessToken?.let {
            SpotifyPrefs.saveAccessToken(this, response.accessToken)

            val flipFlopIntent = Intent(this, FlipFlopActivity::class.java)
            startActivity(flipFlopIntent)

            finish()
        }
    }

    private fun getRedirectUri(): Uri {
        return Uri.Builder()
                .scheme(getString(R.string.com_spotify_sdk_redirect_scheme))
                .authority(getString(R.string.com_spotify_sdk_redirect_host))
                .build()
    }
    //endregion
}
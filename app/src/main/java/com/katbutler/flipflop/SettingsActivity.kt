package com.katbutler.flipflop

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.preference.PreferenceActivity
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.widget.Toast


class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, FlipFlopPreferenceFragment()).commit()
    }

    class FlipFlopPreferenceFragment : PreferenceFragment() {

        private val spotifySettingsActivityName = ComponentName.unflattenFromString("com.spotify.music/.MainActivity")

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference?): Boolean {
            when (preference?.key) {
               "switch_account" -> {
                   if (spotifyIsInstalled()) {
                       val spotfiySettingsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:internal:preferences")).apply {
                           component = spotifySettingsActivityName
                       }
                       startActivity(spotfiySettingsIntent)
                   } else {
                     LoginActivity.showLoginActivity(activity, true)
                   }
               }
                else -> {
                    throw IllegalStateException("Unknown preference selected")
                }
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference)
        }

        private fun spotifyIsInstalled(): Boolean {
            return try {
                val pkgInfo = activity.packageManager.getPackageInfo("com.spotify.music", PackageManager.GET_ACTIVITIES)

                val activity = pkgInfo.activities.find { ComponentName(it.packageName, it.name) == spotifySettingsActivityName }

                activity != null
            } catch (nnf: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

}
package com.katbutler.flipflop

import android.content.Intent
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
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference?): Boolean {
            when (preference?.key) {
               "switch_account" -> {
                   LoginActivity.showLoginActivity(activity, true)
               }
                else -> {
                    throw IllegalStateException("Unknown preference selected")
                }
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference)
        }
    }

}
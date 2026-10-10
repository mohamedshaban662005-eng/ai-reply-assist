package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.example.util.LocaleHelper

class SettingsActivity : ComponentActivity() {

    private lateinit var languageSwitchButton: Button
    private lateinit var currentLanguageTextView: TextView

    override fun attachBaseContext(newBase: Context) {
        val currentLang = LocaleHelper.getAppLanguage(newBase)
        val context = LocaleHelper.setAppLanguage(newBase, currentLang)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        languageSwitchButton = findViewById(R.id.LanguageSwitchButton)
        currentLanguageTextView = findViewById(R.id.tv_current_language)
        val closeButton = findViewById<Button>(R.id.btn_close_settings)

        updateLanguageDisplay()

        languageSwitchButton.setOnClickListener {
            val newLang = LocaleHelper.toggleLanguage(this)
            updateLanguageDisplay()
            // Recreate activity so new locale resources and layout direction are applied
            recreate()
        }

        closeButton?.setOnClickListener {
            finish()
        }
    }

    private fun updateLanguageDisplay() {
        val currentLang = LocaleHelper.getAppLanguage(this)
        val langName = if (currentLang == "ar") {
            getString(R.string.lang_arabic)
        } else {
            getString(R.string.lang_english)
        }
        currentLanguageTextView.text = getString(R.string.language_current_label, langName)
    }
}

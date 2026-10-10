package com.example.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.data.local.SecurePreferences
import java.util.Locale

object LocaleHelper {

    fun getAppLanguage(context: Context): String {
        val prefs = SecurePreferences(context)
        val savedLang = prefs.getAppLanguage()
        if (savedLang.isNotEmpty()) {
            return savedLang
        }
        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return if (currentLocale.language.startsWith("ar")) "ar" else "en"
    }

    fun setAppLanguage(context: Context, languageCode: String): Context {
        val prefs = SecurePreferences(context)
        prefs.setAppLanguage(languageCode)

        val targetLocale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(targetLocale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = LocaleList(targetLocale)
        }

        val config = Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        return context.createConfigurationContext(config)
    }

    fun toggleLanguage(context: Context): String {
        val current = getAppLanguage(context)
        val newLang = if (current == "ar") "en" else "ar"
        setAppLanguage(context, newLang)
        return newLang
    }
}

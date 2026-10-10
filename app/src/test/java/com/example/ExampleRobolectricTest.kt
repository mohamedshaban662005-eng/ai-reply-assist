package com.example

import android.content.Context
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI Reply Assist", appName)
  }

  @Test
  fun `settings activity contains LanguageSwitchButton`() {
    ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val switchButton = activity.findViewById<Button>(R.id.LanguageSwitchButton)
        assertNotNull(switchButton)
        val initialLang = LocaleHelper.getAppLanguage(activity)
        switchButton.performClick()
        val toggledLang = LocaleHelper.getAppLanguage(activity)
        assertEquals(if (initialLang == "ar") "en" else "ar", toggledLang)
      }
    }
  }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AlarmEntity
import com.example.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("QR Alarm", appName)
  }

  @Test
  fun `verify 12h and 24h time formatting`() {
    val formatted12h = TimeFormatter.formatTime(7, 5, false)
    assertEquals("07:05 AM", formatted12h)

    val formatted24h = TimeFormatter.formatTime(19, 45, true)
    assertEquals("19:45", formatted24h)

    val formatted12hPm = TimeFormatter.formatTime(19, 45, false)
    assertEquals("07:45 PM", formatted12hPm)
  }

  @Test
  fun `verify repeat day bitmask logic`() {
    val weekdays = AlarmEntity.DAYS_WEEKDAYS
    assertTrue(AlarmEntity.isDaySelected(weekdays, AlarmEntity.DAY_MON))
    assertTrue(AlarmEntity.isDaySelected(weekdays, AlarmEntity.DAY_FRI))
    assertFalse(AlarmEntity.isDaySelected(weekdays, AlarmEntity.DAY_SAT))
    assertFalse(AlarmEntity.isDaySelected(weekdays, AlarmEntity.DAY_SUN))

    val toggled = AlarmEntity.toggleDay(weekdays, AlarmEntity.DAY_SAT)
    assertTrue(AlarmEntity.isDaySelected(toggled, AlarmEntity.DAY_SAT))
  }

  @Test
  fun `verify repeat day labels`() {
    assertEquals("Weekdays", TimeFormatter.formatRepeatDays(AlarmEntity.DAYS_WEEKDAYS))
    assertEquals("Weekends", TimeFormatter.formatRepeatDays(AlarmEntity.DAYS_WEEKEND))
    assertEquals("Every day", TimeFormatter.formatRepeatDays(AlarmEntity.DAYS_EVERYDAY))
    assertEquals("Once", TimeFormatter.formatRepeatDays(AlarmEntity.DAYS_ONCE))
  }
}

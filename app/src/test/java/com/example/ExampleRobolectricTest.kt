package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Converters
import com.example.data.model.DoseStatus
import com.example.data.model.ProfileRelationship
import com.example.data.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string resource from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DoseNote", appName)
    }

    @Test
    fun `verify type converters`() {
        val converters = Converters()
        val list = listOf("08:00", "20:00")
        val converted = converters.fromStringList(list)
        val reverted = converters.toStringList(converted)
        assertEquals(list, reverted)

        assertEquals(DoseStatus.TAKEN, converters.toDoseStatus("TAKEN"))
        assertEquals(ProfileRelationship.PARENT, converters.toProfileRelationship("PARENT"))
        assertEquals(ScheduleType.FIXED_TIMES, converters.toScheduleType("FIXED_TIMES"))
    }
}

package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


private val ZONE_FILE = "\$ORIGIN public_profile_for_testing.id.blockstack\n\$TTL 3600\n_http._tcp\tIN\tURI\t10\t1\t\"https://gaia.blockstack.org/hub/1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk/profile.json\"\n\n"


@RunWith(AndroidJUnit4::class)
class ZoneFileTest {


    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)


    @Test
    fun testParseZoneFile() {
        val zoneFile = parseZoneFile(ZONE_FILE)
        assertThat(zoneFile.tokenFileUri, `is`("https://gaia.blockstack.org/hub/1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk/profile.json"))
    }

}


package com.laffy.caffeine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class PrivacyTest {

    /** Guards the one value Play reviewers click: a typo here breaks the in-app policy link. */
    @Test
    fun `privacy policy url is https on the documented host and path`() {
        val uri = URI(Privacy.PRIVACY_POLICY_URL)

        assertEquals("https", uri.scheme)
        assertEquals("share.web.huuhoang.tech", uri.host)
        assertEquals("/privacy/caffeine.html", uri.path)
    }
}

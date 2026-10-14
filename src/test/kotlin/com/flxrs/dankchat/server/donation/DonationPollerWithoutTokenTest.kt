package com.flxrs.dankchat.server.donation

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import kotlin.test.assertTrue

@QuarkusTest
@TestProfile(DonationPollerWithoutTokenTest.EmptyToken::class)
class DonationPollerWithoutTokenTest {
    @InjectMock
    @RestClient
    lateinit var streamElements: StreamElementsApi

    @Inject
    lateinit var config: StreamElementsConfig

    @Inject
    lateinit var poller: DonationPoller

    @Test
    fun `empty token disables polling`() {
        poller.poll()

        assertTrue(config.token().isEmpty)
        verifyNoInteractions(streamElements)
    }

    class EmptyToken : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf("dankchat.streamelements.token" to "\${SE_TOKEN_UNSET:}")
    }
}

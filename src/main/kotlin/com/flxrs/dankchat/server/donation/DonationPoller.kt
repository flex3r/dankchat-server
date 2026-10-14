package com.flxrs.dankchat.server.donation

import io.quarkus.runtime.Startup
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.logging.Logger
import kotlin.jvm.optionals.getOrNull

@ApplicationScoped
class DonationPoller(
    @param:RestClient private val streamElements: StreamElementsApi,
    @param:RestClient private val ivr: IvrApi,
    private val config: StreamElementsConfig,
    private val donations: DonationService,
) {
    @Startup
    fun warnIfDisabled() {
        if (config.token().isEmpty) {
            log.warn("SE_TOKEN is not set, donation polling is disabled")
        }
    }

    @Scheduled(
        identity = "donation-poller",
        every = "{dankchat.donation-poller.interval}",
        delayed = "{dankchat.donation-poller.delay}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP,
    )
    fun poll() {
        val token = config.token().getOrNull() ?: return
        val authorization = "Bearer $token"
        val known = donations.knownChannelIds()
        val newDonors =
            fetchTips(authorization)
                .map { it.donation.user }
                .filter { it.channel != null && it.channel !in known }
                .distinctBy { it.channel }
                .mapNotNull { resolveDonor(it, authorization) }

        if (newDonors.isNotEmpty()) {
            log.info("Recording new donors: ${newDonors.map { it.name }}")
            donations.record(newDonors)
        }
    }

    private fun fetchTips(authorization: String): List<TipDocDto> {
        val first = streamElements.tips(config.channelId(), limit = PAGE_SIZE, offset = 0, authorization = authorization)
        val total = first.totalDocs ?: first.total ?: first.docs.size
        val remaining =
            (PAGE_SIZE until total step PAGE_SIZE).flatMap { offset ->
                streamElements.tips(config.channelId(), limit = PAGE_SIZE, offset = offset, authorization = authorization).docs
            }
        return first.docs + remaining
    }

    private fun resolveDonor(
        user: TipUserDto,
        authorization: String,
    ): NewDonor? {
        val channel = user.channel ?: return null
        val twitchId = twitchIdFromStreamElements(channel, authorization) ?: twitchIdFromIvr(user.username)
        if (twitchId == null) {
            log.warn("Could not resolve Twitch id for donor ${user.username} (channel $channel)")
            return null
        }
        return NewDonor(seChannelId = channel, name = user.username, twitchId = twitchId)
    }

    private fun twitchIdFromStreamElements(
        channel: String,
        authorization: String,
    ): String? = lookup("StreamElements channel $channel") {
        streamElements.channel(channel, authorization).takeIf { it.provider == "twitch" }?.providerId
    }

    private fun twitchIdFromIvr(login: String): String? = lookup("IVR user $login") { ivr.users(login).firstOrNull()?.id }

    private inline fun lookup(
        description: String,
        block: () -> String?,
    ): String? = try {
        block()
    } catch (e: WebApplicationException) {
        log.warn("$description lookup failed with ${e.response.status}")
        null
    } catch (e: ProcessingException) {
        log.warn("$description lookup failed", e)
        null
    }

    companion object {
        private const val PAGE_SIZE = 100
        private val log = Logger.getLogger(DonationPoller::class.java)
    }
}

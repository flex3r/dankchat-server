<script lang="ts">
	import { GITHUB_URL } from '#lib/links.js';

	const CONTACT_EMAIL = 'dankchat@flxrs.com';
</script>

<svelte:head>
	<title>Privacy policy - DankChat</title>
</svelte:head>

<article>
	<h1>Privacy policy</h1>
	<p class="muted">Last updated October 15, 2026</p>

	<p>
		This policy covers the <a href={GITHUB_URL} target="_blank" rel="noopener">DankChat app</a>,
		<a href="/">dank.chat</a>
		and the DankChat badge service. For questions or requests, write to
		<a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>.
	</p>

	<h2>Android app</h2>
	<p>
		DankChat has no ads, analytics or crash reporting. Your login, settings and history stay on your device and are
		never sent to us.
	</p>
	<p>
		The app connects directly to <a
			href="https://legal.twitch.com/legal/privacy-notice/"
			target="_blank"
			rel="noopener">Twitch</a
		>, <a href="https://7tv.app/privacy" target="_blank" rel="noopener">7TV</a>,
		<a href="https://betterttv.com/privacy" target="_blank" rel="noopener">BetterTTV</a>,
		<a href="https://www.frankerfacez.com/privacy" target="_blank" rel="noopener">FrankerFaceZ</a>
		and <a href="https://recent-messages.robotty.de/privacy" target="_blank" rel="noopener">recent-messages</a>. If
		you use them, it also connects to <a href="https://supinic.com/" target="_blank" rel="noopener">Supibot</a> and
		your image upload host (<a href="https://kappa.lol/terms" target="_blank" rel="noopener">kappa.lol</a> by default).
		These services have their own privacy policies.
	</p>

	<h2>Data we store</h2>
	<ul>
		<li>
			<strong>Your account</strong>, once you sign in on <a href="/">dank.chat</a>: Twitch user id, display name,
			your badges and your badge choice.
		</li>
		<li>
			<strong>Donors</strong>: StreamElements name, StreamElements channel id and Twitch user id, taken from tips
			to DankChat on
			<a href="https://streamelements.com/privacy" target="_blank" rel="noopener">StreamElements</a>, to give
			supporters their badge. We don't store amounts, messages or payment details.
		</li>
	</ul>
	<p>
		Badges are public: DankChat loads a list of Twitch user ids and their badge to show them in chat. Choosing "No
		badge" removes you from that list. Legal basis: <a
			href="https://gdpr-info.eu/art-6-gdpr/"
			target="_blank"
			rel="noopener">Art. 6(1)(b) GDPR</a
		>
		for accounts and <a href="https://gdpr-info.eu/art-6-gdpr/" target="_blank" rel="noopener">Art. 6(1)(f) GDPR</a> for
		supporter badges.
	</p>

	<h2>Cookies</h2>
	<p>
		One cookie keeps you signed in for up to 30 days, and your theme choice is saved in your browser. There is no
		tracking.
	</p>

	<h2>Hosting</h2>
	<p>
		<a href="/">dank.chat</a> and the badge service run on a server at
		<a href="https://www.netcup.com/en/contact/data-privacy" target="_blank" rel="noopener">netcup</a>
		in Germany. Requests from the site and the app pass through
		<a href="https://www.cloudflare.com/privacypolicy/" target="_blank" rel="noopener">Cloudflare</a> (US, under the
		<a href="https://www.dataprivacyframework.gov/" target="_blank" rel="noopener">EU-US Data Privacy Framework</a
		>), which sees your IP address. We don't log requests.
	</p>

	<h2>Your rights</h2>
	<p>
		You can access, correct, export or delete your data, restrict or object to its use, and complain to a <a
			href="https://www.edpb.europa.eu/about-edpb/about-edpb/members_en"
			target="_blank"
			rel="noopener">data protection authority</a
		>. Your <a href="/account">account page</a> lets you download your data and delete your account; for anything
		else, write to <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>.
	</p>
	<p>
		Deleting your account removes your data right away, and backups expire within 14 days. We keep a one-way hash of
		your StreamElements channel id so past donations don't recreate your account.
	</p>
</article>

<style>
	article {
		max-width: 65ch;
	}

	h2 {
		margin-top: 2rem;
	}
</style>

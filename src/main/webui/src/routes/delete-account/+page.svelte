<script lang="ts">
	const CONTACT_EMAIL = 'dankchat@flxrs.com';
</script>

<svelte:head>
	<title>Delete your account - DankChat</title>
</svelte:head>

<article>
	<h1>Delete your DankChat account</h1>
	<p>You can delete your DankChat account and its data at any time.</p>

	<h2>Steps</h2>
	<ol>
		<li>
			Open your <a href="/account">account page</a> on dank.chat. In the DankChat app, you can also go to Settings →
			DankChat account → Manage account on dank.chat.
		</li>
		<li>Sign in with Twitch.</li>
		<li>Choose <strong>Delete account</strong>, then <strong>Delete my account</strong>.</li>
	</ol>
	<p>
		If you can't sign in, write to <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a> with your Twitch username and
		we'll delete it for you.
	</p>

	<h2>What gets deleted</h2>
	<ul>
		<li>Your Twitch user id, display name, badges and badge choice</li>
		<li>Your donation records: StreamElements name, StreamElements channel id and Twitch user id</li>
	</ul>
	<p>This happens right away. Backups that still contain your data expire within 14 days.</p>

	<h2>What we keep</h2>
	<ul>
		<li>A one-way hash of your StreamElements channel id, so past donations don't recreate your account</li>
	</ul>

	<h2>Not affected</h2>
	<ul>
		<li>Your Twitch account. Deleting that is done on Twitch.</li>
		<li>
			Settings and history in the DankChat app. These stay on your device and are removed when you uninstall the
			app.
		</li>
	</ul>
</article>

<style>
	article {
		max-width: 65ch;
	}

	h2 {
		margin-top: 2rem;
	}
</style>

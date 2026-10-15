<script lang="ts">
	import Popover from './Popover.svelte';
</script>

<Popover>
	<a class="trigger" href="/login" data-sveltekit-reload>Sign in</a>
	{#snippet panel()}
		<p class="info">Sign in with Twitch to change how you appear in chat and manage your data.</p>
	{/snippet}
</Popover>

<style>
	.trigger {
		color: var(--muted);
		text-decoration: none;
	}

	.trigger:hover {
		color: var(--text);
	}

	.info {
		width: 220px;
		margin: 0.35rem 0.5rem;
		font-size: 0.9rem;
		color: var(--muted);
	}
</style>

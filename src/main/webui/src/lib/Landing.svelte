<script lang="ts">
	import { onMount } from 'svelte';
	import ChatLine from './ChatLine.svelte';
	import { FDROID_URL, PLAY_STORE_URL } from './links';
	import { PREVIEW_MESSAGES } from './preview';
	import { session } from './session.svelte';
	import type { Badge } from './types';
	import { TypingPreview } from './typing.svelte';

	const DEV_NAME = 'flex3rs';
	const DEV_COLOR = '#00BCD4';

	let supporterBadge = $state<Badge>();
	const preview = new TypingPreview(PREVIEW_MESSAGES);

	let account = $derived(session.current.status === 'signed-in' ? session.current.account : null);
	let name = $derived(account ? (account.displayName ?? account.twitchId) : DEV_NAME);
	let badge = $derived(account ? account.badges.find((it) => it.id === account.shownBadgeId) : supporterBadge);

	onMount(() => {
		preview.start();
		fetch('/v1/badges')
			.then((response) => (response.ok ? response.json() : null))
			.then((data) => (supporterBadge = data?.badges.find((it: Badge) => it.id === 'supporter')))
			.catch(() => {});
		return () => preview.stop();
	});
</script>

<div class="landing">
	<h1>DankChat</h1>
	<img class="logo" src="/feelsdankman.png" alt="" />
	<div class="preview">
		<ChatLine
			{name}
			{badge}
			color={account ? account.chatColor : DEV_COLOR}
			parts={preview.parts}
			typing={preview.typing}
		/>
	</div>
	<div class="stores">
		<a class="cta" href={PLAY_STORE_URL} target="_blank" rel="noopener">
			<svg viewBox="0 0 24 24" aria-hidden="true"
				><path
					fill="currentColor"
					d="M22.018 13.298l-3.919 2.218-3.515-3.493 3.543-3.521 3.891 2.202a1.49 1.49 0 0 1 0 2.594zM1.337.924a1.486 1.486 0 0 0-.112.568v21.017c0 .217.045.419.124.6l11.155-11.087L1.337.924zm12.207 10.065l3.258-3.238L3.45.195a1.466 1.466 0 0 0-.946-.179l11.04 10.973zm0 2.067l-11 10.933c.298.036.612-.016.906-.183l13.324-7.54-3.23-3.21z"
				/></svg
			>
			Get it on Google Play
		</a>
		<a class="cta secondary" href={FDROID_URL} target="_blank" rel="noopener">
			<svg viewBox="0 0 24 24" aria-hidden="true"
				><path
					fill="currentColor"
					d="M20.472,10.081H3.528c-0.877,0-1.589,0.711-1.589,1.589v10.59c0,0.877,0.711,1.589,1.589,1.589h16.944 c0.877,0,1.589-0.711,1.589-1.589V11.67C22.061,10.792,21.349,10.081,20.472,10.081z M12,22.525c-3.066,0-5.56-2.494-5.56-5.56 s2.494-5.56,5.56-5.56c3.066,0,5.56,2.494,5.56,5.56S15.066,22.525,12,22.525z M12,12.411c-2.511,0-4.554,2.043-4.554,4.554 S9.489,21.519,12,21.519s4.554-2.043,4.554-4.554S14.511,12.411,12,12.411z M12,20.274c-1.563,0-2.881-1.103-3.221-2.568h1.67 c0.275,0.581,0.859,0.979,1.551,0.979c0.96,0,1.721-0.761,1.721-1.721c0-0.96-0.761-1.721-1.721-1.721 c-0.649,0-1.2,0.352-1.493,0.874H8.805c0.378-1.412,1.669-2.462,3.195-2.462c1.818,0,3.309,1.491,3.309,3.309 C15.309,18.783,13.818,20.274,12,20.274z M23.849,0.396c-0.001,0.001-0.002,0.002-0.002,0.003 c-0.002-0.002-0.004-0.003-0.006-0.005c0.001-0.001,0.002-0.003,0.004-0.004c-0.116-0.137-0.279-0.231-0.519-0.238 c-0.202,0.005-0.391,0.097-0.512,0.259l-1.818,2.353c-0.164-0.058-0.339-0.095-0.523-0.095H3.528c-0.184,0-0.358,0.038-0.523,0.095 L1.187,0.41c-0.121-0.162-0.31-0.253-0.512-0.259c-0.24,0.006-0.403,0.1-0.519,0.238c0.001,0.001,0.002,0.003,0.004,0.004 C0.157,0.395,0.155,0.397,0.153,0.399C0.153,0.398,0.152,0.397,0.151,0.396C0.085,0.474-0.146,0.822,0.139,1.22l1.909,2.471 C1.981,3.867,1.94,4.057,1.94,4.257v3.707c0,0.877,0.711,1.589,1.589,1.589h16.944c0.877,0,1.589-0.711,1.589-1.589V4.257 c0-0.2-0.041-0.39-0.109-0.566l1.909-2.471C24.146,0.822,23.915,0.474,23.849,0.396z M6.904,8.228c-0.987,0-1.787-0.8-1.787-1.787 s0.8-1.787,1.787-1.787s1.787,0.8,1.787,1.787S7.891,8.228,6.904,8.228z M17.229,8.228c-0.987,0-1.787-0.8-1.787-1.787 s0.8-1.787,1.787-1.787c0.987,0,1.787,0.8,1.787,1.787S18.216,8.228,17.229,8.228z"
				/></svg
			>
			Get it on F-Droid
		</a>
	</div>
</div>

<style>
	.landing {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 1.5rem;
		padding: 1rem 0 2rem;
	}

	h1 {
		margin: 0;
		font-size: 3rem;
		font-weight: 700;
	}

	.logo {
		width: min(360px, 80vw);
		margin-bottom: 1.5rem;
	}

	.preview {
		width: min(520px, 100%);
	}

	@media (max-width: 560px) {
		.preview :global(p.chat) {
			min-height: calc(var(--emote-unit) * 32 + 1.6em + 2rem + 4px);
		}
	}

	.stores {
		display: flex;
		flex-wrap: wrap;
		justify-content: center;
		gap: 0.75rem;
		margin-top: 1rem;
	}

	.cta {
		display: inline-flex;
		align-items: center;
		gap: 0.6rem;
		padding: 0.8rem 1.4rem;
		border-radius: 999px;
		background: var(--button);
		color: var(--button-text);
		font-weight: 600;
		font-size: 1.1rem;
		text-decoration: none;
		box-shadow: 0 8px 24px rgb(124 77 255 / 0.35);
		transition: transform 0.15s;
	}

	.cta.secondary {
		background: var(--card);
		color: var(--text);
		border: 1px solid var(--border);
		box-shadow: none;
	}

	.cta:hover {
		transform: translateY(-2px);
	}

	.cta svg {
		width: 1.2rem;
		height: 1.2rem;
	}
</style>

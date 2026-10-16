<script lang="ts">
	import { page } from '$app/state';
	import { onMount, type Snippet } from 'svelte';
	import AccountMenu from '#lib/AccountMenu.svelte';
	import { errorMessage } from '#lib/api.js';
	import { FDROID_URL, GITHUB_URL, PLAY_STORE_URL } from '#lib/links.js';
	import { loadSession, session, signOut } from '#lib/session.svelte.js';
	import SignInLink from '#lib/SignInLink.svelte';
	import ThemeToggle from '#lib/ThemeToggle.svelte';

	const PUBLIC_PATHS = ['/', '/privacy', '/delete-account'];

	let { children }: { children: Snippet } = $props();
	let error = $state<string | null>(null);

	let signedOutOnPrivatePage = $derived(
		session.current.status === 'signed-out' && !PUBLIC_PATHS.includes(page.url.pathname)
	);

	onMount(() => {
		loadSession().catch((e) => (error = errorMessage(e)));
	});

	$effect(() => {
		if (signedOutOnPrivatePage) {
			window.location.assign('/login');
		}
	});

	async function handleSignOut() {
		try {
			await signOut();
		} catch (e) {
			error = errorMessage(e);
		}
	}
</script>

{#if page.url.pathname === '/'}
	<nav class="corner">
		{#if session.current.status === 'signed-in'}
			<AccountMenu account={session.current.account} onSignOut={handleSignOut} />
		{:else if session.current.status === 'signed-out'}
			<SignInLink />
		{/if}
		<ThemeToggle />
	</nav>
{:else}
	<header>
		<div class="bar">
			<a class="brand" href="/"><img src="/feelsdankman.png" alt="" />DankChat</a>
			<nav>
				{#if session.current.status === 'signed-in'}
					<AccountMenu account={session.current.account} onSignOut={handleSignOut} />
				{:else if session.current.status === 'signed-out'}
					<SignInLink />
				{/if}
				<ThemeToggle />
			</nav>
		</div>
	</header>
{/if}

<main>
	{#if error}
		<p class="error">{error}</p>
	{/if}
	{#if session.current.status !== 'loading' && !signedOutOnPrivatePage}
		{@render children()}
	{/if}
</main>

<footer>
	<a href={GITHUB_URL} target="_blank" rel="noopener">GitHub</a>
	<!-- The landing page links the stores right above the footer -->
	{#if page.url.pathname !== '/'}
		<a href={PLAY_STORE_URL} target="_blank" rel="noopener">Play Store</a>
		<a href={FDROID_URL} target="_blank" rel="noopener">F-Droid</a>
	{/if}
	<a href="/privacy">Privacy</a>
	<a href="/delete-account">Delete account</a>
</footer>

<style>
	:global(:root) {
		--bg: #f6f5fa;
		--card: #ffffff;
		--text: #1d1b26;
		--muted: #6b6880;
		--border: #e3e0ee;
		--accent: #7c4dff;
		--button: #7c4dff;
		--button-text: #ffffff;
		--danger: #d32f2f;
		color-scheme: light;
	}

	@media (prefers-color-scheme: dark) {
		:global(:root:not([data-theme='light'])) {
			--bg: #13121a;
			--card: #1d1b26;
			--text: #ecebf3;
			--muted: #a19eb5;
			--border: #2e2b3b;
			--accent: #9e7bff;
			--button: #8a5cff;
			--danger: #ff6b6b;
			color-scheme: dark;
		}
	}

	:global(:root[data-theme='dark']) {
		--bg: #13121a;
		--card: #1d1b26;
		--text: #ecebf3;
		--muted: #a19eb5;
		--border: #2e2b3b;
		--accent: #9e7bff;
		--button: #8a5cff;
		--danger: #ff6b6b;
		color-scheme: dark;
	}

	:global(body) {
		display: flex;
		flex-direction: column;
		min-height: 100vh;
		margin: 0;
		background: var(--bg);
		color: var(--text);
		font:
			16px/1.5 system-ui,
			-apple-system,
			'Segoe UI',
			Roboto,
			sans-serif;
	}

	:global(a) {
		color: var(--accent);
	}

	:global(h1, h2) {
		line-height: 1.2;
		margin: 0 0 0.5rem;
	}

	:global(.card h2) {
		font-size: 1.15rem;
		margin-bottom: 1rem;
	}

	:global(.card) {
		background: var(--card);
		border: 1px solid var(--border);
		border-radius: 12px;
		padding: 1.25rem;
		margin-bottom: 1rem;
	}

	:global(.muted) {
		color: var(--muted);
	}

	:global(.error) {
		color: var(--danger);
	}

	:global(.button, button) {
		display: inline-block;
		font: inherit;
		border: 0;
		border-radius: 8px;
		padding: 0.5rem 1rem;
		background: var(--button);
		color: var(--button-text);
		text-decoration: none;
		cursor: pointer;
	}

	:global(.button.secondary, button.secondary) {
		background: none;
		color: var(--text);
		border: 1px solid var(--border);
	}

	:global(button:disabled) {
		opacity: 0.6;
		cursor: default;
	}

	:global(button.link) {
		background: none;
		color: var(--accent);
		padding: 0;
	}

	:global(button.danger) {
		background: var(--danger);
		color: #fff;
	}

	:global(input, select) {
		min-width: 0;
		max-width: 100%;
		box-sizing: border-box;
		font: inherit;
		color: inherit;
		background: var(--bg);
		border: 1px solid var(--border);
		border-radius: 8px;
		padding: 0.45rem 0.6rem;
	}

	:global(img.badge) {
		width: 36px;
		height: 36px;
		object-fit: contain;
	}

	header {
		position: sticky;
		top: 0;
		z-index: 10;
		background: var(--card);
		border-bottom: 1px solid var(--border);
	}

	.bar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		box-sizing: border-box;
		max-width: 720px;
		margin: 0 auto;
		padding: 0.6rem 1rem;
	}

	/* Same box as the header bar, so the controls sit where they do on every other page */
	.corner {
		position: absolute;
		inset: 0 0 auto;
		justify-content: flex-end;
		box-sizing: border-box;
		max-width: 720px;
		height: calc(36px + 1.2rem);
		margin: 0 auto;
		padding: 0.6rem 1rem;
		pointer-events: none;
	}

	.corner > :global(*) {
		pointer-events: auto;
	}

	nav {
		display: flex;
		align-items: center;
		gap: 1rem;
	}

	footer {
		display: flex;
		justify-content: center;
		gap: 1.5rem;
		padding: 1.5rem 1rem;
		border-top: 1px solid var(--border);
		font-size: 0.9rem;
	}

	footer a {
		color: var(--muted);
		text-decoration: none;
	}

	footer a:hover {
		color: var(--text);
	}

	.brand {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		font-weight: 700;
		font-size: 1.2rem;
		color: var(--text);
		text-decoration: none;
	}

	.brand img {
		width: 36px;
		height: 36px;
	}

	main {
		display: flex;
		flex-direction: column;
		flex: 1;
		box-sizing: border-box;
		width: 100%;
		max-width: 720px;
		margin: 0 auto;
		padding: 2.5rem 1rem 3rem;
	}
</style>

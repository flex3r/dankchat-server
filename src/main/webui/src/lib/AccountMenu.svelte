<script lang="ts">
	import Popover from './Popover.svelte';
	import type { Account } from './types';

	let { account, onSignOut }: { account: Account; onSignOut: () => void } = $props();

	let open = $state(false);
	let root = $state<HTMLElement>();

	function closeOnOutsideClick(event: MouseEvent) {
		if (open && !root?.contains(event.target as Node)) {
			open = false;
		}
	}

	function closeOnEscape(event: KeyboardEvent) {
		if (event.key === 'Escape') {
			open = false;
		}
	}

	function signOut() {
		open = false;
		onSignOut();
	}
</script>

<svelte:window onclick={closeOnOutsideClick} onkeydown={closeOnEscape} />

<div bind:this={root}>
	<Popover {open}>
		<button class="trigger" aria-haspopup="menu" aria-expanded={open} onclick={() => (open = !open)}>
			{account.displayName ?? account.twitchId}
			<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
		</button>
		{#snippet panel()}
			<div class="items" role="menu">
				<a class="item" role="menuitem" href="/account" onclick={() => (open = false)}>Account</a>
				{#if account.admin}
					<a class="item" role="menuitem" href="/admin" onclick={() => (open = false)}>Admin</a>
				{/if}
				<button class="item" role="menuitem" onclick={signOut}>Sign out</button>
			</div>
		{/snippet}
	</Popover>
</div>

<style>
	.trigger {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		padding: 0.25rem 0.4rem;
		background: none;
		color: var(--muted);
	}

	.trigger:hover,
	.trigger[aria-expanded='true'] {
		color: var(--text);
	}

	.trigger svg {
		width: 16px;
		height: 16px;
		fill: none;
		stroke: currentColor;
		stroke-width: 2;
		stroke-linecap: round;
		stroke-linejoin: round;
	}

	.items {
		display: flex;
		flex-direction: column;
	}

	.item {
		display: block;
		padding: 0.45rem 0.75rem;
		border-radius: 6px;
		background: none;
		color: var(--text);
		text-align: left;
		text-decoration: none;
	}

	.item:hover,
	.item:focus-visible {
		background: var(--bg);
	}
</style>

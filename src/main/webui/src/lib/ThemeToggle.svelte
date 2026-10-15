<script lang="ts">
	import { theme } from './theme.svelte';

	let label = $derived(theme.current === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
</script>

<button class="toggle" onclick={() => theme.toggle()} aria-label={label} title={label}>
	<svg viewBox="0 0 24 24" aria-hidden="true">
		{#if theme.current === 'dark'}
			<circle cx="12" cy="12" r="4" />
			<path
				d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"
			/>
		{:else}
			<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
		{/if}
	</svg>
</button>

<style>
	.toggle {
		display: grid;
		place-items: center;
		padding: 0.35rem;
		background: none;
		color: var(--muted);
		border-radius: 8px;
	}

	.toggle:hover {
		color: var(--text);
	}

	svg {
		width: 20px;
		height: 20px;
		fill: none;
		stroke: currentColor;
		stroke-width: 2;
		stroke-linecap: round;
		stroke-linejoin: round;
	}
</style>

<script lang="ts">
	import type { Snippet } from 'svelte';

	/** Shows the panel below its trigger on hover, or while open for touch and keyboard users. */
	let { open = false, children, panel }: { open?: boolean; children: Snippet; panel: Snippet } = $props();
</script>

<div class="popover" class:open>
	{@render children()}
	<div class="dropdown">
		<div class="panel">
			{@render panel()}
		</div>
	</div>
</div>

<style>
	.popover {
		position: relative;
	}

	.dropdown {
		display: none;
		position: absolute;
		top: 100%;
		right: 0;
		z-index: 20;
		padding-top: 0.4rem;
	}

	.open .dropdown {
		display: block;
	}

	@media (hover: hover) {
		.popover:hover .dropdown {
			display: block;
		}
	}

	.panel {
		min-width: 160px;
		padding: 0.35rem;
		background: var(--card);
		border: 1px solid var(--border);
		border-radius: 10px;
		box-shadow: 0 8px 24px rgb(0 0 0 / 0.18);
	}
</style>

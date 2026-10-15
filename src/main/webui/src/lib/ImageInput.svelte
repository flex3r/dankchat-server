<script lang="ts">
	let { name, required = false }: { name: string; required?: boolean } = $props();

	let input = $state<HTMLInputElement>();
	let fileName = $state<string | null>(null);

	$effect(() => {
		const form = input?.form;
		const clear = () => (fileName = null);
		form?.addEventListener('reset', clear);
		return () => form?.removeEventListener('reset', clear);
	});

	function pick() {
		fileName = input?.files?.[0]?.name ?? null;
	}
</script>

<span class="picker">
	<label class="button secondary">
		Choose image
		<input
			bind:this={input}
			{name}
			{required}
			type="file"
			accept="image/png,image/gif,image/webp"
			onchange={pick}
		/>
	</label>
	{#if fileName}
		<span class="muted file">{fileName}</span>
	{/if}
</span>

<style>
	.picker {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		min-width: 0;
	}

	label {
		position: relative;
		flex-shrink: 0;
	}

	label:focus-within {
		outline: 2px solid var(--accent);
		outline-offset: 2px;
	}

	/* Hidden visually but still focusable and validated, unlike display: none */
	input {
		position: absolute;
		inset: 0;
		width: 100%;
		opacity: 0;
		cursor: pointer;
	}

	.file {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 0.9rem;
	}
</style>

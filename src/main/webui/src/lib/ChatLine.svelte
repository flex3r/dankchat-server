<script lang="ts">
	import { normalizeColor } from './color';
	import { DEFAULT_MESSAGE, type MessagePart } from './preview';
	import type { Badge } from './types';

	let {
		name,
		badge,
		color,
		parts = DEFAULT_MESSAGE,
		typing = false
	}: { name: string; badge?: Badge; color?: string; parts?: MessagePart[]; typing?: boolean } = $props();

	// The --card token of each theme, chat names are normalized against it like in DankChat.
	const CARD_LIGHT = '#ffffff';
	const CARD_DARK = '#1d1b26';

	let nameColor = $derived(
		color ? `light-dark(${normalizeColor(color, CARD_LIGHT)}, ${normalizeColor(color, CARD_DARK)})` : undefined
	);

	const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
</script>

<p class="chat">
	<span class="time">{time}</span>
	{#if badge}
		{#key badge.id}
			<img class="badge" src={badge.url} alt={badge.title} title={badge.title} />
		{/key}
	{/if}
	<span class="name" style:color={nameColor}>{name}:</span>
	{#each parts as part, i (i)}
		{#if part.type === 'text'}
			<span class="text">{part.text}</span>
		{:else}
			<img
				class="emote"
				src={part.emote.url}
				alt={part.emote.name}
				title={part.emote.name}
				style:height="calc(var(--emote-unit) * {part.emote.height})"
			/>
		{/if}
	{/each}
	{#if typing}
		<span class="caret"></span>
	{/if}
</p>

<style>
	.chat {
		--emote-unit: calc(1.173em * 1.5 / 28);
		box-sizing: border-box;
		min-height: calc(var(--emote-unit) * 32 + 2rem + 2px);
		margin: 0;
		padding: 1rem 1.25rem;
		border: 1px solid var(--border);
		border-radius: 14px;
		background: var(--card);
		font-size: 18px;
		line-height: 1.6;
	}

	.chat > * {
		vertical-align: middle;
	}

	.time {
		font-family: ui-monospace, monospace;
		font-weight: 700;
		font-size: 0.95em;
		letter-spacing: -0.03em;
	}

	.chat .badge {
		width: auto;
		height: 1.173em;
		animation: pop 0.25s ease-out;
	}

	.emote {
		animation: pop 0.2s ease-out;
	}

	@keyframes pop {
		from {
			transform: scale(0.4);
			opacity: 0;
		}
	}

	.name {
		color: var(--accent);
		font-weight: 700;
	}

	.text {
		white-space: pre-wrap;
	}

	.caret {
		display: inline-block;
		width: 2px;
		height: 1.1em;
		margin-left: 1px;
		background: var(--text);
		animation: blink 1s steps(1) infinite;
	}

	@keyframes blink {
		50% {
			opacity: 0;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.chat * {
			animation: none;
		}
	}
</style>

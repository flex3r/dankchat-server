<script lang="ts">
	import { onMount } from 'svelte';
	import { api, errorMessage } from '#lib/api.js';
	import ImageInput from '#lib/ImageInput.svelte';
	import type { AdminBadge } from '#lib/types.js';

	let badges = $state<AdminBadge[] | null>(null);
	let error = $state<string | null>(null);
	let creating = $state(false);
	let createError = $state<string | null>(null);

	onMount(load);

	async function load() {
		try {
			badges = await api.badges();
		} catch (e) {
			error = errorMessage(e);
		}
	}

	async function create(event: SubmitEvent) {
		event.preventDefault();
		const form = event.currentTarget as HTMLFormElement;
		creating = true;
		createError = null;
		try {
			const badge = await api.createBadge(new FormData(form));
			badges = [...(badges ?? []), badge].sort((a, b) => b.priority - a.priority || a.id.localeCompare(b.id));
			form.reset();
		} catch (e) {
			createError = errorMessage(e);
		} finally {
			creating = false;
		}
	}
</script>

<h1>Admin</h1>

{#if error}
	<p class="error">{error}</p>
{:else if badges === null}
	<p class="muted">Loading</p>
{:else}
	<section class="card">
		<h2>Badges</h2>
		<div class="list">
			{#each badges as badge (badge.id)}
				<a class="row" href={`/admin/badge?id=${encodeURIComponent(badge.id)}`}>
					<img class="badge" src={badge.url} alt="" />
					<span class="title">
						{badge.title}
						<small class="muted">{badge.id}, priority {badge.priority}</small>
					</span>
					<span class="muted">{badge.holders} {badge.holders === 1 ? 'user' : 'users'}</span>
				</a>
			{/each}
		</div>
	</section>

	<section class="card">
		<h2>New badge</h2>
		<form class="grid" onsubmit={create}>
			<label>
				Id
				<input name="id" required pattern="[a-z0-9]+(-[a-z0-9]+)*" maxlength="64" placeholder="borgir" />
			</label>
			<label>
				Title
				<input name="title" required maxlength="128" placeholder="Borgir Connoisseur" />
			</label>
			<label>
				Priority
				<input name="priority" type="number" required value="50" />
			</label>
			<div class="field">
				Image
				<ImageInput name="image" required />
			</div>
			<div>
				<button type="submit" disabled={creating}>Create badge</button>
				{#if createError}
					<p class="error">{createError}</p>
				{/if}
			</div>
		</form>
		<p class="muted hint">Higher priority badges win when a user hasn't picked one. Ids can't be changed later.</p>
	</section>
{/if}

<style>
	.list {
		margin: 0 -0.75rem;
	}

	.row {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.5rem 0.75rem;
		border-radius: 8px;
		color: inherit;
		text-decoration: none;
	}

	.row:hover {
		background: var(--bg);
	}

	.title {
		flex: 1;
	}

	.title small {
		display: block;
	}

	.grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
		gap: 0.75rem;
		align-items: end;
	}

	.grid label,
	.grid .field {
		display: grid;
		gap: 0.25rem;
		min-width: 0;
	}

	.hint {
		font-size: 0.9rem;
		margin-bottom: 0;
	}
</style>

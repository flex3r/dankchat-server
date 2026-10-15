<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { api, errorMessage } from '#lib/api.js';
	import ImageInput from '#lib/ImageInput.svelte';
	import type { AdminBadge, BadgeHolder } from '#lib/types.js';

	const id = page.url.searchParams.get('id') ?? '';

	let badge = $state<AdminBadge | null>(null);
	let holders = $state<BadgeHolder[]>([]);
	let error = $state<string | null>(null);
	let saving = $state(false);
	let saveStatus = $state<string | null>(null);
	let grantStatus = $state<string | null>(null);
	let filter = $state('');

	let filtered = $derived(
		holders.filter((holder) => {
			const query = filter.trim().toLowerCase();
			return (
				query === '' ||
				holder.twitchId.includes(query) ||
				(holder.login ?? '').toLowerCase().includes(query) ||
				(holder.displayName ?? '').toLowerCase().includes(query)
			);
		})
	);

	onMount(async () => {
		try {
			const [badges, loadedHolders] = await Promise.all([api.badges(), api.holders(id)]);
			badge = badges.find((it) => it.id === id) ?? null;
			holders = loadedHolders;
		} catch (e) {
			error = errorMessage(e);
		}
	});

	async function save(event: SubmitEvent) {
		event.preventDefault();
		const form = event.currentTarget as HTMLFormElement;
		const data = new FormData(form);
		const image = data.get('image');
		if (image instanceof File && image.size === 0) {
			data.delete('image');
		}
		saving = true;
		saveStatus = null;
		try {
			badge = await api.updateBadge(id, data);
			saveStatus = 'Saved';
		} catch (e) {
			saveStatus = errorMessage(e);
		} finally {
			saving = false;
		}
	}

	async function remove() {
		if (!confirm(`Delete ${badge?.title ?? id}? Everyone who has it loses it.`)) {
			return;
		}
		try {
			await api.deleteBadge(id);
			await goto('/admin');
		} catch (e) {
			saveStatus = errorMessage(e);
		}
	}

	async function grant(event: SubmitEvent) {
		event.preventDefault();
		const form = event.currentTarget as HTMLFormElement;
		const user = String(new FormData(form).get('user') ?? '');
		grantStatus = null;
		try {
			const holder = await api.grant(id, user);
			holders = [holder, ...holders];
			grantStatus = `Granted to ${holder.displayName ?? holder.login}`;
			form.reset();
		} catch (e) {
			grantStatus = errorMessage(e);
		}
	}

	async function revoke(holder: BadgeHolder) {
		if (!confirm(`Remove the badge from ${name(holder)}?`)) {
			return;
		}
		try {
			await api.revoke(id, holder.twitchId);
			holders = holders.filter((it) => it.twitchId !== holder.twitchId);
		} catch (e) {
			grantStatus = errorMessage(e);
		}
	}

	function name(holder: BadgeHolder): string {
		return holder.displayName ?? holder.login ?? holder.twitchId;
	}
</script>

<a class="back" href="/admin">
	<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 18-6-6 6-6" /></svg>
	Admin
</a>

{#if error}
	<p class="error">{error}</p>
{:else if badge === null}
	<p class="muted">Loading</p>
{:else}
	<div class="heading">
		<img class="badge" src={badge.url} alt="" />
		<h1>{badge.title}</h1>
	</div>

	<section class="card">
		<form class="grid" onsubmit={save}>
			<label>
				Title
				<input name="title" required maxlength="128" value={badge.title} />
			</label>
			<label>
				Priority
				<input name="priority" type="number" required value={badge.priority} />
			</label>
			<div class="field">
				New image
				<ImageInput name="image" />
			</div>
			<div class="actions">
				<button type="submit" disabled={saving}>Save</button>
				<button type="button" class="danger" onclick={remove}>Delete badge</button>
			</div>
		</form>
		<p class="muted" aria-live="polite">{saveStatus ?? ''}</p>
	</section>

	<section class="card">
		<h2>Users ({holders.length})</h2>
		<form class="grant" onsubmit={grant}>
			<input name="user" required placeholder="Twitch username or id" aria-label="Twitch username or id" />
			<button type="submit">Grant badge</button>
		</form>
		<p class="muted" aria-live="polite">{grantStatus ?? ''}</p>
		{#if holders.length > 10}
			<input
				class="filter"
				type="search"
				placeholder="Filter users"
				aria-label="Filter users"
				bind:value={filter}
			/>
		{/if}
		<ul>
			{#each filtered as holder (holder.twitchId)}
				<li>
					<span class="name">
						{name(holder)}
						<small class="muted">
							{name(holder) === holder.twitchId ? '' : `${holder.twitchId}, `}{holder.source ===
							'DONATION'
								? 'donation'
								: 'granted'}
							{new Date(holder.grantedAt).toLocaleDateString()}
						</small>
					</span>
					<button class="link" onclick={() => revoke(holder)}>Remove</button>
				</li>
			{/each}
		</ul>
	</section>
{/if}

<style>
	.back {
		display: inline-flex;
		align-items: center;
		gap: 0.2rem;
		margin-bottom: 0.75rem;
		color: var(--muted);
		text-decoration: none;
	}

	.back:hover {
		color: var(--text);
	}

	.back svg {
		width: 18px;
		height: 18px;
		fill: none;
		stroke: currentColor;
		stroke-width: 2;
		stroke-linecap: round;
		stroke-linejoin: round;
	}

	.heading {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		margin-bottom: 1rem;
	}

	.heading h1 {
		margin: 0;
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

	.actions {
		display: flex;
		gap: 0.5rem;
	}

	.grant {
		display: flex;
		gap: 0.5rem;
	}

	.grant input {
		flex: 1;
	}

	.filter {
		width: 100%;
		box-sizing: border-box;
	}

	ul {
		list-style: none;
		padding: 0;
		margin: 0.75rem 0 0;
	}

	li {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		padding: 0.5rem 0;
		border-top: 1px solid var(--border);
	}

	.name small {
		display: block;
	}
</style>

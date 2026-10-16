<script lang="ts">
	import { api, errorMessage } from '#lib/api.js';
	import ChatLine from '#lib/ChatLine.svelte';
	import { deleteAccount, session, updateAccount } from '#lib/session.svelte.js';
	import type { Account, BadgeChoice } from '#lib/types.js';

	let account = $derived(session.current.status === 'signed-in' ? session.current.account : null);
	let saving = $state(false);
	let saved = $state(false);
	let error = $state<string | null>(null);
	let draft = $state<string | null>(null);
	let confirmingDelete = $state(false);
	let deleting = $state(false);
	let deleteError = $state<string | null>(null);

	let savedKey = $derived(account ? choiceKey(account.choice) : '');
	let selected = $derived(draft ?? savedKey);
	let dirty = $derived(selected !== savedKey);
	let automaticBadge = $derived(account?.badges[0]);
	let previewBadge = $derived(previewFor(selected));

	function choiceKey(choice: BadgeChoice): string {
		return choice.type === 'badge' ? `badge:${choice.id}` : choice.type;
	}

	function toChoice(key: string): BadgeChoice {
		if (key.startsWith('badge:')) {
			return { type: 'badge', id: key.slice('badge:'.length) };
		}
		return key === 'hidden' ? { type: 'hidden' } : { type: 'automatic' };
	}

	function previewFor(key: string) {
		if (key === 'automatic') {
			return automaticBadge;
		}
		return account?.badges.find((badge) => `badge:${badge.id}` === key);
	}

	function select(key: string) {
		draft = key;
		saved = false;
	}

	async function confirmDelete() {
		deleting = true;
		deleteError = null;
		try {
			await deleteAccount();
		} catch (e) {
			deleteError = errorMessage(e);
			deleting = false;
		}
	}

	async function save() {
		saving = true;
		error = null;
		try {
			const updated: Account = await api.chooseBadge(toChoice(selected));
			updateAccount(updated);
			draft = null;
			saved = true;
		} catch (e) {
			error = errorMessage(e);
		} finally {
			saving = false;
		}
	}
</script>

{#if account}
	<h1>Account</h1>

	<section class="card">
		<h2>Badge</h2>
		<div class="preview">
			<ChatLine name={account.displayName ?? account.twitchId} badge={previewBadge} color={account.chatColor} />
		</div>
		{#if account.badges.length === 0}
			<p>No DankChat badge yet.</p>
		{:else}
			<fieldset class="tiles" disabled={saving}>
				<legend class="visually-hidden">Badge</legend>
				<label class="tile">
					<input
						class="visually-hidden"
						type="radio"
						name="badge"
						value="automatic"
						checked={selected === 'automatic'}
						onchange={() => select('automatic')}
					/>
					<span class="icon">
						{#if automaticBadge}
							<img src={automaticBadge.url} alt="" />
						{/if}
					</span>
					<span class="label">Automatic</span>
				</label>
				{#each account.badges as badge (badge.id)}
					<label class="tile">
						<input
							class="visually-hidden"
							type="radio"
							name="badge"
							value={`badge:${badge.id}`}
							checked={selected === `badge:${badge.id}`}
							onchange={() => select(`badge:${badge.id}`)}
						/>
						<span class="icon"><img src={badge.url} alt="" /></span>
						<span class="label">{badge.title}</span>
					</label>
				{/each}
				<label class="tile">
					<input
						class="visually-hidden"
						type="radio"
						name="badge"
						value="hidden"
						checked={selected === 'hidden'}
						onchange={() => select('hidden')}
					/>
					<span class="icon none" aria-hidden="true"></span>
					<span class="label">No badge</span>
				</label>
			</fieldset>
			<div class="actions">
				<button onclick={save} disabled={!dirty || saving}>Save</button>
				<span class="status" aria-live="polite">
					{#if error}
						<span class="error">{error}</span>
					{:else if saved}
						Saved
					{/if}
				</span>
			</div>
		{/if}
	</section>

	<section class="card">
		<h2>Your data</h2>
		<div class="buttons">
			<a class="button secondary" href="/api/me/export" download>Download my data</a>
			{#if !confirmingDelete}
				<button class="danger" onclick={() => (confirmingDelete = true)}>Delete account</button>
			{/if}
		</div>
		{#if confirmingDelete}
			<div class="confirm">
				<p>
					This deletes your account, your badges and your donation records. Future donations from the same
					StreamElements account won't add a badge automatically. This can't be undone.
				</p>
				<div class="actions">
					<button class="danger" onclick={confirmDelete} disabled={deleting}>Delete my account</button>
					<button class="link" onclick={() => (confirmingDelete = false)} disabled={deleting}>Cancel</button>
				</div>
				{#if deleteError}
					<p class="error">{deleteError}</p>
				{/if}
			</div>
		{/if}
	</section>
{/if}

<style>
	h1 {
		margin-bottom: 1.25rem;
	}

	.card {
		margin-bottom: 1.25rem;
	}

	.actions {
		display: flex;
		align-items: center;
		gap: 1rem;
		margin-top: 1.25rem;
	}

	.buttons {
		display: flex;
		flex-wrap: wrap;
		gap: 0.75rem;
	}

	.confirm {
		margin-top: 1rem;
		padding: 1rem;
		border: 1px solid var(--danger);
		border-radius: 10px;
	}

	.confirm p {
		margin-top: 0;
	}

	.status {
		color: var(--muted);
	}

	.preview {
		margin-bottom: 1rem;
	}

	.tiles {
		border: 0;
		margin: 0;
		padding: 0;
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
		gap: 0.75rem;
	}

	.tile {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 0.6rem;
		padding: 1.1rem 0.75rem;
		border: 2px solid var(--border);
		border-radius: 14px;
		background: var(--bg);
		cursor: pointer;
		text-align: center;
		transition:
			border-color 0.15s,
			transform 0.15s;
	}

	.tile:hover {
		transform: translateY(-2px);
	}

	.tile:has(input:checked) {
		border-color: var(--accent);
		box-shadow: 0 6px 18px rgb(124 77 255 / 0.25);
	}

	.tile:has(input:focus-visible) {
		outline: 2px solid var(--accent);
		outline-offset: 2px;
	}

	.icon {
		width: 48px;
		height: 48px;
		display: grid;
		place-items: center;
	}

	.icon img {
		width: 48px;
		height: 48px;
	}

	.none {
		border: 3px dashed var(--border);
		border-radius: 50%;
		box-sizing: border-box;
	}

	.label {
		font-weight: 600;
	}

	.visually-hidden {
		position: absolute;
		width: 1px;
		height: 1px;
		overflow: hidden;
		clip-path: inset(50%);
	}
</style>

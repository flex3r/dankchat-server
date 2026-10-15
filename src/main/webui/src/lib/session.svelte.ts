import { goto } from '$app/navigation';
import { api, SignedOutError } from './api';
import type { Account } from './types';

export type Session = { status: 'loading' } | { status: 'signed-out' } | { status: 'signed-in'; account: Account };

export const session = $state<{ current: Session }>({ current: { status: 'loading' } });

export async function loadSession() {
	try {
		session.current = { status: 'signed-in', account: await api.account() };
	} catch (error) {
		if (!(error instanceof SignedOutError)) {
			throw error;
		}
		session.current = { status: 'signed-out' };
	}
}

export function updateAccount(account: Account) {
	session.current = { status: 'signed-in', account };
}

export async function signOut() {
	try {
		await api.logout();
	} catch (error) {
		if (!(error instanceof SignedOutError)) {
			throw error;
		}
	}
	await leave();
}

export async function deleteAccount() {
	await api.deleteAccount();
	await leave();
}

// Navigates home first, so private pages don't send the signed-out user straight back to the login.
async function leave() {
	await goto('/');
	session.current = { status: 'signed-out' };
}

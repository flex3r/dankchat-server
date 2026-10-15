import type { Account, AdminBadge, BadgeChoice, BadgeHolder } from './types';

/** Thrown when the session is missing or expired; the user has to sign in again. */
export class SignedOutError extends Error {}

export class ApiError extends Error {
	constructor(
		readonly status: number,
		message: string
	) {
		super(message);
	}
}

async function request<T>(method: string, path: string, body?: object | FormData): Promise<T> {
	const headers: Record<string, string> = { 'X-Requested-With': 'JavaScript' };
	let payload: BodyInit | undefined;
	if (body instanceof FormData) {
		payload = body;
	} else if (body !== undefined) {
		headers['Content-Type'] = 'application/json';
		payload = JSON.stringify(body);
	}

	const response = await fetch(path, { method, headers, body: payload });
	if (response.status === 401 || response.status === 499) {
		throw new SignedOutError();
	}
	if (!response.ok) {
		const error = await response.json().catch(() => null);
		throw new ApiError(response.status, error?.message ?? `Request failed with status ${response.status}`);
	}
	if (response.status === 204) {
		return undefined as T;
	}
	return response.json();
}

export const api = {
	account: () => request<Account>('GET', '/api/me'),
	chooseBadge: (choice: BadgeChoice) => request<Account>('PUT', '/api/me/badge', choice),
	deleteAccount: () => request<void>('DELETE', '/api/me'),
	logout: () => request<void>('POST', '/logout'),

	badges: () => request<AdminBadge[]>('GET', '/api/admin/badges'),
	createBadge: (form: FormData) => request<AdminBadge>('POST', '/api/admin/badges', form),
	updateBadge: (id: string, form: FormData) =>
		request<AdminBadge>('PUT', `/api/admin/badges/${encodeURIComponent(id)}`, form),
	deleteBadge: (id: string) => request<void>('DELETE', `/api/admin/badges/${encodeURIComponent(id)}`),
	holders: (id: string) => request<BadgeHolder[]>('GET', `/api/admin/badges/${encodeURIComponent(id)}/holders`),
	grant: (id: string, user: string) =>
		request<BadgeHolder>('POST', `/api/admin/badges/${encodeURIComponent(id)}/holders`, { user }),
	revoke: (id: string, twitchId: string) =>
		request<void>('DELETE', `/api/admin/badges/${encodeURIComponent(id)}/holders/${encodeURIComponent(twitchId)}`)
};

export function errorMessage(error: unknown): string {
	if (error instanceof SignedOutError) {
		return 'Your session expired. Sign in again to continue.';
	}
	return error instanceof Error ? error.message : 'Something went wrong';
}

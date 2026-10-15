export interface Badge {
	id: string;
	title: string;
	url: string;
}

export type BadgeChoice = { type: 'automatic' } | { type: 'hidden' } | { type: 'badge'; id: string };

export interface Account {
	twitchId: string;
	displayName: string | null;
	admin: boolean;
	badges: Badge[];
	choice: BadgeChoice;
	shownBadgeId: string | null;
	chatColor: string;
}

export interface AdminBadge extends Badge {
	priority: number;
	holders: number;
}

export interface BadgeHolder {
	twitchId: string;
	login: string | null;
	displayName: string | null;
	source: 'DONATION' | 'GRANT';
	grantedBy: string | null;
	grantedAt: string;
}

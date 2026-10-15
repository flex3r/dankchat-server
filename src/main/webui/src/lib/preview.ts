export interface Emote {
	name: string;
	url: string;
	/** Height at 1x scale, emotes are 28px tall by default. */
	height: number;
}

export type MessagePart = { type: 'text'; text: string } | { type: 'emote'; emote: Emote };

export const FEELS_DANK_MAN: Emote = { name: 'FeelsDankMan', url: '/emotes/feelsdankman.png', height: 32 };
const CLAP: Emote = { name: 'Clap', url: '/emotes/clap.webp', height: 32 };
const MONKA_S: Emote = { name: 'monkaS', url: '/emotes/monkas.webp', height: 28 };
const WAYTOODANK: Emote = { name: 'WAYTOODANK', url: '/emotes/waytoodank.webp', height: 32 };
const PEEPO_HAPPY: Emote = { name: 'peepoHappy', url: '/emotes/peepohappy.webp', height: 32 };
const SEEMS_GOOD: Emote = { name: 'SeemsGood', url: '/emotes/seemsgood.png', height: 28 };

const text = (text: string): MessagePart => ({ type: 'text', text });
const emote = (emote: Emote): MessagePart => ({ type: 'emote', emote });

export const DEFAULT_MESSAGE: MessagePart[] = [emote(FEELS_DANK_MAN)];

export const PREVIEW_MESSAGES: MessagePart[][] = [
	[text('hi chat '), emote(FEELS_DANK_MAN)],
	[text('7TV, BTTV and FFZ emotes '), emote(CLAP)],
	[text('chat is moving so fast '), emote(MONKA_S)],
	[emote(WAYTOODANK)],
	[text('no ads, no tracking '), emote(SEEMS_GOOD)],
	[text('free and open source '), emote(PEEPO_HAPPY)]
];

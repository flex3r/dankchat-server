import type { MessagePart } from './preview';

const CHAR_DELAY = 45;
const EMOTE_DELAY = 250;
const HOLD_DELAY = 2800;

type Unit = { type: 'char'; char: string } | Extract<MessagePart, { type: 'emote' }>;

/** Types out messages one after another, like someone writing in chat. */
export class TypingPreview {
	parts = $state<MessagePart[]>([]);
	typing = $state(false);

	#messages: Unit[][];
	#timer: ReturnType<typeof setTimeout> | undefined;

	constructor(messages: MessagePart[][]) {
		this.#messages = messages.map((message) =>
			message.flatMap((part): Unit[] =>
				part.type === 'text' ? [...part.text].map((char) => ({ type: 'char', char })) : [part]
			)
		);
	}

	start() {
		const instant = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
		this.#play(0, instant ? Infinity : 0);
	}

	stop() {
		clearTimeout(this.#timer);
	}

	#play(index: number, revealed: number) {
		const units = this.#messages[index];
		const count = Math.min(revealed, units.length);
		this.parts = toParts(units.slice(0, count));
		this.typing = count < units.length;

		if (this.typing) {
			const next = units[count];
			this.#timer = setTimeout(
				() => this.#play(index, count + 1),
				next.type === 'char' ? CHAR_DELAY : EMOTE_DELAY
			);
		} else {
			const nextRevealed = revealed === Infinity ? Infinity : 0;
			this.#timer = setTimeout(() => this.#play((index + 1) % this.#messages.length, nextRevealed), HOLD_DELAY);
		}
	}
}

function toParts(units: Unit[]): MessagePart[] {
	const parts: MessagePart[] = [];
	for (const unit of units) {
		const last = parts.at(-1);
		if (unit.type === 'emote') {
			parts.push(unit);
		} else if (last?.type === 'text') {
			last.text += unit.char;
		} else {
			parts.push({ type: 'text', text: unit.char });
		}
	}
	return parts;
}

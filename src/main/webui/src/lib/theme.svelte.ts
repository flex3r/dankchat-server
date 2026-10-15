export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'theme';

function storedTheme(): Theme | null {
	try {
		const value = localStorage.getItem(STORAGE_KEY);
		return value === 'light' || value === 'dark' ? value : null;
	} catch {
		return null;
	}
}

/** Follows the system theme until the user picks the other one. */
class ThemeState {
	#stored = $state(storedTheme());
	#systemDark = $state(false);

	current = $derived<Theme>(this.#stored ?? (this.#systemDark ? 'dark' : 'light'));

	constructor() {
		const query = window.matchMedia('(prefers-color-scheme: dark)');
		this.#systemDark = query.matches;
		query.addEventListener('change', (event) => (this.#systemDark = event.matches));
	}

	toggle() {
		const next: Theme = this.current === 'dark' ? 'light' : 'dark';
		const system: Theme = this.#systemDark ? 'dark' : 'light';
		// Picking the system theme again goes back to following the system.
		this.#stored = next === system ? null : next;
		try {
			if (this.#stored) {
				localStorage.setItem(STORAGE_KEY, this.#stored);
			} else {
				localStorage.removeItem(STORAGE_KEY);
			}
		} catch {
			// Storage can be unavailable (private mode); the choice then lasts for this page only.
		}
		if (this.#stored) {
			document.documentElement.dataset.theme = this.#stored;
		} else {
			delete document.documentElement.dataset.theme;
		}
	}
}

export const theme = new ThemeState();

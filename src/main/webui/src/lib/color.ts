const MIN_CONTRAST_RATIO = 3.5;
const MAX_ITERATIONS = 16;

type Rgb = [number, number, number];
type Hsl = [number, number, number];

/** Adjusts a name color for readable contrast against [background], the same way DankChat does. */
export function normalizeColor(color: string, background: string): string {
	const bg = parseHex(background);
	const corrected = correctColor(parseHex(color), bg);
	const contrast = calculateContrast(corrected, bg);
	if (contrast >= MIN_CONTRAST_RATIO) {
		return toHex(corrected);
	}

	const hsl = rgbToHsl(corrected);
	const shouldLighten = calculateLuminance(bg) < 0.5;
	let low = shouldLighten ? hsl[2] : 0;
	let high = shouldLighten ? 1 : hsl[2];
	let bestL = hsl[2];
	let bestContrast = contrast;

	for (let i = 0; i < MAX_ITERATIONS; i++) {
		const mid = (low + high) / 2;
		const candidateContrast = calculateContrast(hslToRgb([hsl[0], hsl[1], mid]), bg);
		if (candidateContrast >= MIN_CONTRAST_RATIO) {
			bestL = mid;
			bestContrast = candidateContrast;
			if (shouldLighten) {
				high = mid;
			} else {
				low = mid;
			}
		} else {
			if (candidateContrast > bestContrast) {
				bestL = mid;
				bestContrast = candidateContrast;
			}
			if (shouldLighten) {
				low = mid;
			} else {
				high = mid;
			}
		}
	}

	return toHex(hslToRgb([hsl[0], hsl[1], bestL]));
}

/** Clamps lightness to 0.5, darkening greens on light backgrounds and lightening blues on dark ones. */
function correctColor(color: Rgb, background: Rgb): Rgb {
	const [h, s, l] = rgbToHsl(color);
	const hue = h / 360;
	let lightness = l;

	if (calculateLuminance(background) > 0.5) {
		lightness = Math.min(lightness, 0.5);
		if (lightness > 0.4 && hue > 0.1 && hue < 0.33333) {
			lightness -= Math.sin(((hue - 0.1) / (0.33333 - 0.1)) * Math.PI) * s * 0.4;
		}
	} else {
		lightness = Math.max(lightness, 0.5);
		if (lightness < 0.6 && hue > 0.54444 && hue < 0.83333) {
			lightness += Math.sin(((hue - 0.54444) / (0.83333 - 0.54444)) * Math.PI) * s * 0.4;
		}
	}

	return hslToRgb([h, s, lightness]);
}

function parseHex(hex: string): Rgb {
	const value = parseInt(hex.replace('#', ''), 16);
	return [(value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff];
}

function toHex(rgb: Rgb): string {
	return '#' + rgb.map((it) => it.toString(16).padStart(2, '0')).join('');
}

function rgbToHsl([r, g, b]: Rgb): Hsl {
	const rf = r / 255;
	const gf = g / 255;
	const bf = b / 255;
	const max = Math.max(rf, gf, bf);
	const min = Math.min(rf, gf, bf);
	const delta = max - min;
	const l = (max + min) / 2;

	let h = 0;
	let s = 0;
	if (delta !== 0) {
		if (max === rf) {
			h = ((gf - bf) / delta) % 6;
		} else if (max === gf) {
			h = (bf - rf) / delta + 2;
		} else {
			h = (rf - gf) / delta + 4;
		}
		s = delta / (1 - Math.abs(2 * l - 1));
	}

	h = (h * 60) % 360;
	if (h < 0) {
		h += 360;
	}
	return [h, Math.min(Math.max(s, 0), 1), Math.min(Math.max(l, 0), 1)];
}

function hslToRgb([h, s, l]: Hsl): Rgb {
	const c = (1 - Math.abs(2 * l - 1)) * s;
	const m = l - 0.5 * c;
	const x = c * (1 - Math.abs(((h / 60) % 2) - 1));

	const [r, g, b]: Rgb = (() => {
		switch (Math.floor(h / 60)) {
			case 0:
				return [c, x, 0];
			case 1:
				return [x, c, 0];
			case 2:
				return [0, c, x];
			case 3:
				return [0, x, c];
			case 4:
				return [x, 0, c];
			default:
				return [c, 0, x];
		}
	})();

	const channel = (value: number) => Math.min(Math.max(Math.round(255 * (value + m)), 0), 255);
	return [channel(r), channel(g), channel(b)];
}

function calculateLuminance(rgb: Rgb): number {
	const [r, g, b] = rgb.map((it) => {
		const c = it / 255;
		return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	});
	return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function calculateContrast(foreground: Rgb, background: Rgb): number {
	const a = calculateLuminance(foreground) + 0.05;
	const b = calculateLuminance(background) + 0.05;
	return Math.max(a, b) / Math.min(a, b);
}

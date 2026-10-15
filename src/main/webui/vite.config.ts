import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [
		sveltekit({
			adapter: adapter({ fallback: 'index.html' }),
			// Every route is served from the same index page, so assets must not resolve relative to the route
			paths: { relative: false }
		})
	],
	// Quinoa proxies dev requests over IPv4
	server: { host: '127.0.0.1', port: 5173, strictPort: true }
});

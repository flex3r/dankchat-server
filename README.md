# dankchat-server

Backend for [DankChat](https://github.com/flex3r/DankChat): serves DankChat badges, polls StreamElements donations for supporter badges and hosts [dank.chat](https://dank.chat), where users pick their badge and manage their data.

## Development

Requires JDK 21 and a running Docker daemon (Quarkus Dev Services starts PostgreSQL automatically). The website in `src/main/webui` is built with Node, which Quinoa downloads during the build.

To sign in locally, put `TWITCH_CLIENT_ID` and `TWITCH_CLIENT_SECRET` into a `.env` file. The Twitch application needs `http://localhost:8080/login/callback` as a redirect URL.

Dev mode with live reload:

```bash
./gradlew quarkusDev
```

Full build with spotless, detekt and tests:

```bash
./gradlew build
```

Format with `./gradlew spotlessApply`.

## Configuration

| Environment variable | Purpose |
|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD` | PostgreSQL connection in production |
| `SE_TOKEN` | StreamElements JWT; donation polling is skipped without it |
| `TWITCH_CLIENT_ID`, `TWITCH_CLIENT_SECRET` | Twitch application for signing in on the website |

## API

`GET /v1/badges` returns every badge definition and the one badge shown per Twitch user id:

```json
{
  "badges": [{ "id": "supporter", "title": "DankChat Supporter", "url": "https://api.dank.chat/badges/dank.png" }],
  "users": { "73697410": "developer" }
}
```

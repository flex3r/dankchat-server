# dankchat-server

Backend for [DankChat](https://github.com/flex3r/DankChat): serves DankChat badges and polls StreamElements donations for supporter badges.

## Development

Requires JDK 21 and a running Docker daemon (Quarkus Dev Services starts PostgreSQL automatically).

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

## API

`GET /v1/badges` returns every badge definition and the one badge shown per Twitch user id:

```json
{
  "badges": [{ "id": "supporter", "title": "DankChat Supporter", "url": "https://flxrs.com/dankchat/badges/dank.png" }],
  "users": { "73697410": "developer" }
}
```

#!/usr/bin/env bash
# Nightly database dump, keeps the last 14 days.
set -euo pipefail

cd "$(dirname "$0")"
mkdir -p backups

file="backups/dankchat-$(date +%F).dump"
docker compose exec -T db pg_dump --username dankchat --format custom dankchat > "$file.tmp"
mv "$file.tmp" "$file"
find backups -name 'dankchat-*.dump' -mtime +14 -delete

#!/usr/bin/env bash
# Database dump into the folder Nextcloud shares as /backups, keeps the last 14 days. Run by dankchat-backup.timer.
set -euo pipefail

cd "$(dirname "$0")"

dir=/mnt/backups/dankchat-server
name="dankchat-$(date +%F).dump"
docker compose exec -T db pg_dump --username dankchat --format custom dankchat > "$dir/.$name.tmp"
mv "$dir/.$name.tmp" "$dir/$name"
find "$dir" -name 'dankchat-*.dump' -mtime +14 -delete

metrics=/var/lib/node-exporter/textfile/dankchat-backup.prom
cat > "$metrics.tmp" <<EOF
# HELP dankchat_backup_last_success_timestamp_seconds Time of the last successful database backup.
# TYPE dankchat_backup_last_success_timestamp_seconds gauge
dankchat_backup_last_success_timestamp_seconds $(date +%s)
# HELP dankchat_backup_size_bytes Size of the last successful database backup.
# TYPE dankchat_backup_size_bytes gauge
dankchat_backup_size_bytes $(stat -c %s "$dir/$name")
EOF
mv "$metrics.tmp" "$metrics"

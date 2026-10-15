#!/usr/bin/env bash
# SSH forced command of the deploy user: "deploy <commit sha>".
set -euo pipefail

cd "$(dirname "$0")"

read -r command sha extra <<< "${SSH_ORIGINAL_COMMAND:-}"
if [[ $command != deploy || ! $sha =~ ^[0-9a-f]{40}$ || -n $extra ]]; then
  echo "usage: deploy <commit sha>" >&2
  exit 2
fi

set_tag() {
  if grep -q '^TAG=' .env; then
    sed -i "s/^TAG=.*/TAG=$1/" .env
  else
    echo "TAG=$1" >> .env
  fi
}

previous=$(sed -n 's/^TAG=//p' .env)
curl -fsSL "https://raw.githubusercontent.com/flex3r/dankchat-server/$sha/deploy/compose.yaml" -o compose.yaml.new
[[ -f compose.yaml ]] && cp compose.yaml compose.yaml.previous
mv compose.yaml.new compose.yaml
set_tag "$sha"

if ! { docker compose pull --quiet && docker compose up --detach --wait --remove-orphans; }; then
  if [[ -n $previous && -f compose.yaml.previous ]]; then
    echo "Deploy of $sha failed, rolling back to $previous" >&2
    mv compose.yaml.previous compose.yaml
    set_tag "$previous"
    docker compose up --detach --wait --remove-orphans
  fi
  exit 1
fi

docker image prune --all --force --filter "label=org.opencontainers.image.source=https://github.com/flex3r/dankchat-server"
echo "Deployed $sha"

#!/usr/bin/env bash
# 릴리스 워크플로(.github/workflows/release.yml)에 필요한 GitHub Secrets를 로컬 파일에서 등록한다.
# 값은 gh CLI로 바로 넘기고 화면에 찍지 않는다.
#
#   bash scripts/set-github-secrets.sh ~/Downloads/play-service-account.json
set -euo pipefail

cd "$(dirname "$0")/.."
service_account=${1:?"사용법: $0 <서비스 계정 JSON 경로>"}

for f in keystore.properties app/google-services.json "$service_account"; do
  [ -f "$f" ] || { echo "파일이 없습니다: $f" >&2; exit 1; }
done

# 시크릿 끝에 줄바꿈이 붙지 않게 printf로 넘긴다.
prop() { printf '%s' "$(grep -E "^$1=" keystore.properties | head -1 | cut -d= -f2- | tr -d '\r')"; }
store_file=$(prop storeFile)
[ -f "$store_file" ] || { echo "키스토어가 없습니다: $store_file" >&2; exit 1; }

base64 -w0 "$store_file" | gh secret set RELEASE_KEYSTORE_BASE64
prop storePassword | gh secret set KEYSTORE_PASSWORD
prop keyAlias | gh secret set KEY_ALIAS
prop keyPassword | gh secret set KEY_PASSWORD
gh secret set GOOGLE_SERVICES_JSON < app/google-services.json
gh secret set PLAY_SERVICE_ACCOUNT_JSON < "$service_account"

echo "등록된 시크릿:"
gh secret list

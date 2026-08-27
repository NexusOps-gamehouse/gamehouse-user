#!/usr/bin/env bash
#
# 로컬 DB 에 duo_user 계정과 user_svc 스키마를 만든다.
#
#   ./db/init.sh                 # application-secret.yml 의 DB_URL 을 그대로 쓴다
#   ./db/init.sh <host> <port>   # 직접 지정 (예: compose 매핑 포트 15432)
#
# 비밀번호는 application-secret.yml 에서 읽는다 — 이 스크립트에도, init.sql 에도
# 값이 들어 있지 않다. 그 파일은 .gitignore 라 커밋되지 않는다.
#
# 몇 번을 돌려도 안전하다. 계정이 있으면 비밀번호만 맞춘다.
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECRET="$REPO/application-secret.yml"

[ -f "$SECRET" ] || {
  echo "application-secret.yml 이 없습니다: $SECRET" >&2
  echo "backend 레포의 사본을 이 레포 루트에 두세요." >&2
  exit 1
}

# 최상위 키만 읽고 앞뒤 공백·따옴표를 벗긴다.
secret_value() {
  sed -n "s/^$1:[[:space:]]*//p" "$SECRET" | head -1 \
    | sed -e 's/[[:space:]]*$//' -e "s/^['\"]//" -e "s/['\"]$//"
}

# jdbc:postgresql://localhost:5432/duo → host=localhost port=5432 db=duo
DB_URL=$(secret_value DB_URL)
HOSTPORT="${DB_URL#*//}"; HOSTPORT="${HOSTPORT%%/*}"
URL_HOST="${HOSTPORT%%:*}"
URL_PORT="${HOSTPORT##*:}"
[ "$URL_PORT" = "$URL_HOST" ] && URL_PORT=5432
DB_NAME="${DB_URL##*/}"; DB_NAME="${DB_NAME%%\?*}"

DB_HOST="${1:-${URL_HOST:-localhost}}"
DB_PORT="${2:-${URL_PORT:-5432}}"
: "${DB_NAME:=duo}"

ADMIN_USER=$(secret_value DB_USERNAME)
ADMIN_PASS=$(secret_value DB_PASSWORD)
SVC_PASS=$(secret_value USER_DB_PASSWORD)

: "${ADMIN_USER:=duo}"
[ -n "$SVC_PASS" ] || { echo "application-secret.yml 에 USER_DB_PASSWORD 가 없습니다." >&2; exit 1; }

# 실행 경로를 고른다.
#
# 1) postgres 가 Docker 안에 있으면 컨테이너 안에서 돌린다.
#    유닉스 소켓 접속이라 관리 계정 비밀번호가 필요 없다.
#    (application-secret.yml 의 DB_PASSWORD 는 앱이 안 쓰는 값이라
#     예제값 그대로 남아 있는 경우가 많다 — 그때 TCP 로 붙으면 인증 실패한다)
# 2) 컨테이너가 없으면 TCP 로 붙는다. 그때는 DB_PASSWORD 가 맞아야 한다.
CONTAINER="${CONTAINER:-$(docker ps --format '{{.Names}}\t{{.Image}}' 2>/dev/null \
  | grep -i -m1 postgres | cut -f1 || true)}"

# ⚠️ 값을 날것으로 넘긴다. 여기서 따옴표로 감싸면 안 된다.
# init.sql 의 :'user_pw' 가 psql 단계에서 SQL 문자열 리터럴로 감싸주므로,
# 미리 감싸면 따옴표가 두 번 씌워져 비밀번호에 따옴표가 포함돼 저장된다.
PW_ARG="user_pw=$SVC_PASS"

if [ -n "$CONTAINER" ]; then
  echo "대상: ${DB_NAME} (컨테이너 ${CONTAINER}, 관리 계정 ${ADMIN_USER})"
  docker exec -i "$CONTAINER" \
    psql -U "$ADMIN_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -v "$PW_ARG" \
    < "$REPO/db/init.sql"
else
  echo "대상: ${DB_NAME} @ ${DB_HOST}:${DB_PORT} (관리 계정 ${ADMIN_USER})"
  # -h 를 반드시 준다. 빼면 psql 이 /tmp/.s.PGSQL.5432 유닉스 소켓을 찾는데,
  # Postgres 가 Docker 안에 있으면 그 소켓은 호스트에 존재하지 않는다.
  PGPASSWORD="$ADMIN_PASS" psql \
    -h "$DB_HOST" -p "$DB_PORT" -U "$ADMIN_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -v "$PW_ARG" \
    -f "$REPO/db/init.sql"
fi

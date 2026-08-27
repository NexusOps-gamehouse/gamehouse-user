-- gamehouse-user 의 DB 계정 · 스키마 준비.
--
-- ⚠️ 이 파일에는 비밀번호가 들어 있지 않다. 레포에 커밋되는 파일이기 때문이다.
--    값은 실행할 때 -v svc_pw 로 주입한다.
--
--   ./db/init.sh                      ← application-secret.yml 에서 읽어 실행
--   psql ... -v svc_pw="'비번'" -f db/init.sql
--
-- [왜 이 파일이 서비스마다 따로 있나]
-- 모노레포에서는 local-bootstrap.sql 하나가 여섯 서비스의 계정·스키마를 전부 만들었다.
-- 레포가 갈라지면 그 파일이 갈 곳이 없다 — 여기 넣으면 남의 스키마까지 만들게 되고,
-- 여섯 곳에 복사하면 새 서비스가 생길 때마다 여섯 곳을 고쳐야 한다.
--
-- [왜 앱이 못 만드나]
-- 테이블은 Hibernate 의 ddl-auto: update 가 만든다. 스키마도 앱이 만들 수는 있지만
-- (hbm2ddl.create_namespaces), duo_user 같은 일반 계정에는 DB CREATE 권한이 없다.
-- 계정 생성은 superuser 영역이라 애초에 애플리케이션이 할 일이 아니다.
--
-- 몇 번을 돌려도 안전하다. 계정이 이미 있으면 비밀번호만 맞춰준다 —
-- "계정 없음"과 "비밀번호 틀림"이 똑같이 password authentication failed 로 나와서
-- 원인을 구분할 수 없는 문제를 이걸로 없앤다.

\set ON_ERROR_STOP on

-- 계정이 없을 때만 만든다. \gexec 는 SELECT 결과를 SQL 로 실행한다.
-- (DO $$ ... $$ 를 쓰지 않는 이유: psql 변수는 달러 인용 문자열 안에서 치환되지 않는다)
SELECT 'CREATE ROLE duo_user LOGIN'
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'duo_user') \gexec

-- 있든 없든 비밀번호를 지금 값으로 맞춘다.
ALTER ROLE duo_user WITH PASSWORD :'svc_pw';

-- AUTHORIZATION 이 핵심이다. 소유자가 되어야 ddl-auto: update 가 그 안에
-- 테이블을 만들 수 있다. GRANT 로 읽기·쓰기만 주면 부팅 중 테이블 생성에서 막힌다.
CREATE SCHEMA IF NOT EXISTS user_svc AUTHORIZATION duo_user;

-- Hibernate 의 default_schema 는 Hibernate 가 만든 쿼리에만 적용된다.
-- 네이티브 쿼리까지 덮으려면 search_path 도 걸어둔다.
ALTER ROLE duo_user SET search_path TO user_svc;

\echo 'user_svc / duo_user 준비 완료'

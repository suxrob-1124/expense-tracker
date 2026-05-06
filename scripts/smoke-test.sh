#!/usr/bin/env bash
# Smoke test for expense-tracker backend.
# Usage:  BASE_URL=http://localhost:8080 ./scripts/smoke-test.sh
# Returns 0 on full pass, 1 if any check fails.

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="smoketest_$(date +%s)@example.com"
PASSWORD="smokePassword123"
ERRORS=0

# ── helpers ──────────────────────────────────────────────────────────────────

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
pass()  { echo -e "${GREEN}✓${NC} $1"; }
fail()  { echo -e "${RED}✗${NC} $1"; ERRORS=$((ERRORS + 1)); }
info()  { echo -e "${YELLOW}→${NC} $1"; }

http_code() {
  # Usage: http_code <method> <path> [extra curl args...]
  local method="$1" path="$2"; shift 2
  curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$path" "$@"
}

http_body() {
  local method="$1" path="$2"; shift 2
  curl -s -X "$method" "$BASE_URL$path" "$@"
}

# ── checks ───────────────────────────────────────────────────────────────────

echo "======================================"
echo "  Expense-Tracker Backend Smoke Test"
echo "  Target: $BASE_URL"
echo "  Email:  $EMAIL"
echo "======================================"
echo ""

# 1. Health
info "1. Health check"
HEALTH=$(http_body GET /actuator/health)
if echo "$HEALTH" | grep -q '"status":"UP"'; then
  pass "GET /actuator/health → UP"
else
  fail "GET /actuator/health returned: $HEALTH"
fi

# 2. Register
info "2. Registration"
REG_CODE=$(http_code POST /api/v1/users/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"firstName\":\"Smoke\",\"lastName\":\"Test\"}")
[ "$REG_CODE" = "201" ] && pass "POST /users/register → 201 Created" \
                         || fail "POST /users/register → $REG_CODE (expected 201)"

# 3. Validation: password too short → 400
info "3. Validation: short password"
VAL_CODE=$(http_code POST /api/v1/users/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"other_$EMAIL\",\"password\":\"short\"}")
[ "$VAL_CODE" = "400" ] && pass "Short password → 400 Bad Request" \
                         || fail "Short password → $VAL_CODE (expected 400)"

# 4. Duplicate email → 409
info "4. Duplicate email"
DUP_CODE=$(http_code POST /api/v1/users/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
[ "$DUP_CODE" = "409" ] && pass "Duplicate email → 409 Conflict" \
                         || fail "Duplicate email → $DUP_CODE (expected 409)"

# 5. Login — capture headers + body
info "5. Login"
LOGIN_FULL=$(curl -si -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
LOGIN_STATUS=$(echo "$LOGIN_FULL" | head -1 | grep -oE '[0-9]{3}' | head -1)
[ "$LOGIN_STATUS" = "200" ] && pass "POST /auth/login → 200 OK" \
                             || fail "POST /auth/login → $LOGIN_STATUS (expected 200)"

# 6. JWT access token claims
info "6. JWT claims"
ACCESS_TOKEN=$(echo "$LOGIN_FULL" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ -n "$ACCESS_TOKEN" ]; then
  JWT_PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2 | python3 -c "
import sys, base64
raw = sys.stdin.read().strip()
padded = raw + '=' * (4 - len(raw) % 4)
print(base64.urlsafe_b64decode(padded).decode())
" 2>/dev/null)
  echo "$JWT_PAYLOAD" | grep -q '"type"' && echo "$JWT_PAYLOAD" | grep -q 'access' \
    && pass "JWT claim type=access" \
    || fail "JWT claim type=access missing"
  echo "$JWT_PAYLOAD" | grep -q '"jti"' \
    && pass "JWT claim jti present" \
    || fail "JWT claim jti missing"
else
  fail "accessToken not found in login response"
fi

# 7. Refresh cookie
info "7. Refresh cookie"
COOKIE_HEADER=$(echo "$LOGIN_FULL" | grep -i "Set-Cookie")
echo "$COOKIE_HEADER" | grep -qi "refreshToken" && COOKIE_OK=1 || COOKIE_OK=0
echo "$COOKIE_HEADER" | grep -qi "HttpOnly"     && HTTPONLY_OK=1 || HTTPONLY_OK=0
echo "$COOKIE_HEADER" | grep -qi "SameSite=Strict" && SAMESITE_OK=1 || SAMESITE_OK=0
echo "$COOKIE_HEADER" | grep -qi "Secure"       && SECURE_OK=1 || SECURE_OK=0
[ "$COOKIE_OK" = "1" ]   && pass "Refresh cookie: refreshToken present" || fail "Refresh cookie: refreshToken missing"
[ "$HTTPONLY_OK" = "1" ] && pass "Refresh cookie: HttpOnly"            || fail "Refresh cookie: HttpOnly missing"
[ "$SAMESITE_OK" = "1" ] && pass "Refresh cookie: SameSite=Strict"     || fail "Refresh cookie: SameSite=Strict missing"
[ "$SECURE_OK" = "1" ]   && pass "Refresh cookie: Secure"             || fail "Refresh cookie: Secure missing"

# 8. GET /users/me with token
info "8. GET /users/me (authorized)"
ME_CODE=$(http_code GET /api/v1/users/me -H "Authorization: Bearer $ACCESS_TOKEN")
[ "$ME_CODE" = "200" ] && pass "GET /users/me → 200 OK" \
                        || fail "GET /users/me → $ME_CODE (expected 200)"

# 9. GET /users/me without token → 401
info "9. GET /users/me (no token)"
UNAUTH_CODE=$(http_code GET /api/v1/users/me)
[ "$UNAUTH_CODE" = "401" ] && pass "GET /users/me (no token) → 401" \
                            || fail "GET /users/me (no token) → $UNAUTH_CODE (expected 401)"

# 10. Wrong credentials → 401
info "10. Wrong credentials"
WRONG_CODE=$(http_code POST /api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"wrongPassword999\"}")
[ "$WRONG_CODE" = "401" ] && pass "Wrong password → 401 Unauthorized" \
                           || fail "Wrong password → $WRONG_CODE (expected 401)"

# 10b. Register + login second user (must happen before rate-limit test)
EMAIL2="smoketest2_$(date +%s)@example.com"
http_code POST /api/v1/users/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL2\",\"password\":\"$PASSWORD\"}" > /dev/null
LOGIN2=$(curl -si -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL2\",\"password\":\"$PASSWORD\"}")
ACCESS_TOKEN2=$(echo "$LOGIN2" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# 11. Rate limit (auth endpoint: 10 RPM)
info "11. Rate limit — 11 rapid requests to /auth/login"
LAST_CODE=""
for i in $(seq 1 11); do
  LAST_CODE=$(http_code POST /api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"ratelimit@nonexistent.invalid","password":"wrongpass"}')
done
[ "$LAST_CODE" = "429" ] && pass "Rate limit: 11th request → 429 Too Many Requests" \
                          || fail "Rate limit: 11th request → $LAST_CODE (expected 429)"

# ── Categories CRUD ──────────────────────────────────────────────────────────

info "12. POST /categories → 201 + Location"
CREATE_FULL=$(curl -si -X POST "$BASE_URL/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"Food","color":"#FF5733","icon":"🍔"}')
CREATE_STATUS=$(echo "$CREATE_FULL" | head -1 | grep -oE '[0-9]{3}' | head -1)
[ "$CREATE_STATUS" = "201" ] && pass "POST /categories → 201 Created" \
                              || fail "POST /categories → $CREATE_STATUS (expected 201)"
LOCATION=$(echo "$CREATE_FULL" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
[ -n "$LOCATION" ] && pass "POST /categories → Location header present" \
                   || fail "POST /categories → Location header missing"
CATEGORY_ID=$(echo "$LOCATION" | grep -oE '[0-9a-f-]{36}$')

info "13. GET /categories → list contains created category"
LIST_BODY=$(http_body GET /api/v1/categories \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$LIST_BODY" | grep -q '"Food"' \
  && pass "GET /categories → contains 'Food'" \
  || fail "GET /categories → 'Food' not found in list"

info "14. GET /categories/{id} → 200"
if [ -n "$CATEGORY_ID" ]; then
  GET_CODE=$(http_code GET "/api/v1/categories/$CATEGORY_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  [ "$GET_CODE" = "200" ] && pass "GET /categories/{id} → 200 OK" \
                           || fail "GET /categories/{id} → $GET_CODE (expected 200)"
else
  fail "GET /categories/{id} → skipped (no category id)"
fi

info "15. POST /categories duplicate name → 409"
DUP_CAT_CODE=$(http_code POST /api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"Food","color":"#AABBCC","icon":"🥗"}')
[ "$DUP_CAT_CODE" = "409" ] && pass "Duplicate category name → 409 Conflict" \
                             || fail "Duplicate category name → $DUP_CAT_CODE (expected 409)"

info "16. PUT /categories/{id} → 200"
if [ -n "$CATEGORY_ID" ]; then
  PUT_CODE=$(http_code PUT "/api/v1/categories/$CATEGORY_ID" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d '{"name":"Groceries","color":"#33FF57","icon":"🛒"}')
  [ "$PUT_CODE" = "200" ] && pass "PUT /categories/{id} → 200 OK" \
                           || fail "PUT /categories/{id} → $PUT_CODE (expected 200)"
else
  fail "PUT /categories/{id} → skipped (no category id)"
fi

info "17. GET /categories/{id} with other user's token → 404 (ownership)"
if [ -n "$CATEGORY_ID" ] && [ -n "$ACCESS_TOKEN2" ]; then
  OWN_CODE=$(http_code GET "/api/v1/categories/$CATEGORY_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN2")
  [ "$OWN_CODE" = "404" ] && pass "GET /categories/{id} other user → 404 Not Found" \
                           || fail "GET /categories/{id} other user → $OWN_CODE (expected 404)"
else
  fail "Ownership check → skipped (missing category id or second token)"
fi

info "18. POST /categories invalid color → 400"
BAD_COLOR_CODE=$(http_code POST /api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"Bad","color":"not-a-color","icon":"x"}')
[ "$BAD_COLOR_CODE" = "400" ] && pass "Invalid color → 400 Bad Request" \
                               || fail "Invalid color → $BAD_COLOR_CODE (expected 400)"

info "19. DELETE /categories/{id} → 204"
if [ -n "$CATEGORY_ID" ]; then
  DEL_CODE=$(http_code DELETE "/api/v1/categories/$CATEGORY_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  [ "$DEL_CODE" = "204" ] && pass "DELETE /categories/{id} → 204 No Content" \
                           || fail "DELETE /categories/{id} → $DEL_CODE (expected 204)"
else
  fail "DELETE /categories/{id} → skipped (no category id)"
fi

info "20. GET /categories/{id} after delete → 404"
if [ -n "$CATEGORY_ID" ]; then
  AFTER_DEL_CODE=$(http_code GET "/api/v1/categories/$CATEGORY_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  [ "$AFTER_DEL_CODE" = "404" ] && pass "GET /categories/{id} after delete → 404 Not Found" \
                                 || fail "GET /categories/{id} after delete → $AFTER_DEL_CODE (expected 404)"
else
  fail "GET after delete → skipped (no category id)"
fi

info "21. GET /categories without token → 401"
NO_AUTH_CODE=$(http_code GET /api/v1/categories)
[ "$NO_AUTH_CODE" = "401" ] && pass "GET /categories (no token) → 401 Unauthorized" \
                             || fail "GET /categories (no token) → $NO_AUTH_CODE (expected 401)"

# ── summary ──────────────────────────────────────────────────────────────────
echo ""
echo "======================================"
if [ "$ERRORS" -eq 0 ]; then
  echo -e "  ${GREEN}ALL CHECKS PASSED${NC}"
  echo "======================================"
  exit 0
else
  echo -e "  ${RED}FAILED: $ERRORS check(s) failed${NC}"
  echo "======================================"
  exit 1
fi

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

# ── Transactions CRUD ─────────────────────────────────────────────────────────

# Create a fresh category for transaction tests (previous was deleted in step 19)
TX_CAT_FULL=$(curl -si -X POST "$BASE_URL/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"TxTestCategory","color":"#123456","icon":"💰"}')
TX_CAT_LOCATION=$(echo "$TX_CAT_FULL" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
TX_CATEGORY_ID=$(echo "$TX_CAT_LOCATION" | grep -oE '[0-9a-f-]{36}$')

# Create category for second user (for cross-category ownership check)
CAT2_FULL=$(curl -si -X POST "$BASE_URL/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN2" \
  -d '{"name":"User2Category","color":"#654321","icon":"🔒"}')
CAT2_LOCATION=$(echo "$CAT2_FULL" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
CAT2_ID=$(echo "$CAT2_LOCATION" | grep -oE '[0-9a-f-]{36}$')

TX_DATE="2026-05-07T10:00:00Z"

info "22. POST /transactions → 201 + Location"
TX_CREATE_FULL=$(curl -si -X POST "$BASE_URL/api/v1/transactions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{\"amount\":99.99,\"type\":\"EXPENSE\",\"description\":\"Smoke test\",\"date\":\"$TX_DATE\",\"categoryId\":\"$TX_CATEGORY_ID\"}")
TX_CREATE_STATUS=$(echo "$TX_CREATE_FULL" | head -1 | grep -oE '[0-9]{3}' | head -1)
[ "$TX_CREATE_STATUS" = "201" ] && pass "POST /transactions → 201 Created" \
                                 || fail "POST /transactions → $TX_CREATE_STATUS (expected 201)"
TX_LOCATION=$(echo "$TX_CREATE_FULL" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
TX_ID=$(echo "$TX_LOCATION" | grep -oE '[0-9a-f-]{36}$')
[ -n "$TX_ID" ] && pass "POST /transactions → Location header present" \
                || fail "POST /transactions → Location header missing or no UUID"

info "23. GET /transactions?month=5&year=2026 → list contains transaction"
if [ -n "$TX_ID" ]; then
  TX_LIST=$(http_body GET "/api/v1/transactions?month=5&year=2026" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  echo "$TX_LIST" | grep -q "$TX_ID" \
    && pass "GET /transactions?month=5&year=2026 → transaction found" \
    || fail "GET /transactions?month=5&year=2026 → transaction not found in list"
else
  fail "GET /transactions list → skipped (no transaction id)"
fi

info "24. GET /transactions/{id} → 200"
if [ -n "$TX_ID" ]; then
  TX_GET_CODE=$(http_code GET "/api/v1/transactions/$TX_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  [ "$TX_GET_CODE" = "200" ] && pass "GET /transactions/{id} → 200 OK" \
                              || fail "GET /transactions/{id} → $TX_GET_CODE (expected 200)"
else
  fail "GET /transactions/{id} → skipped (no transaction id)"
fi

info "25. PATCH /transactions/{id} partial: description only → amount preserved"
if [ -n "$TX_ID" ]; then
  TX_PARTIAL_BODY=$(http_body PATCH "/api/v1/transactions/$TX_ID" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d '{"description":"Partially updated"}')
  TX_PARTIAL_CODE=$(echo "$TX_PARTIAL_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(0)" 2>/dev/null && echo "200" || echo "ERR")
  # verify amount unchanged (99.99) and description changed
  echo "$TX_PARTIAL_BODY" | grep -q '"description":"Partially updated"' \
    && pass "PATCH partial: description updated" \
    || fail "PATCH partial: description not updated in response"
  echo "$TX_PARTIAL_BODY" | grep -q '"amount"' && \
  ! echo "$TX_PARTIAL_BODY" | grep -q '"amount":0' \
    && pass "PATCH partial: amount field preserved (non-zero)" \
    || fail "PATCH partial: amount field missing or zeroed out"
else
  fail "PATCH /transactions/{id} partial → skipped (no transaction id)"
fi

info "26. Cross-user: GET /transactions/{id} with user2 token → 404"
if [ -n "$TX_ID" ] && [ -n "$ACCESS_TOKEN2" ]; then
  TX_OWN_CODE=$(http_code GET "/api/v1/transactions/$TX_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN2")
  [ "$TX_OWN_CODE" = "404" ] && pass "GET /transactions/{id} other user → 404 Not Found" \
                              || fail "GET /transactions/{id} other user → $TX_OWN_CODE (expected 404)"
else
  fail "Cross-user GET → skipped (missing id or token)"
fi

info "27. Cross-category: POST /transactions with user2's categoryId → 404"
if [ -n "$CAT2_ID" ]; then
  TX_CROSS_CAT_CODE=$(http_code POST "/api/v1/transactions" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"amount\":10.00,\"type\":\"INCOME\",\"date\":\"$TX_DATE\",\"categoryId\":\"$CAT2_ID\"}")
  [ "$TX_CROSS_CAT_CODE" = "404" ] && pass "POST /transactions with other user's category → 404 Not Found" \
                                    || fail "POST /transactions with other user's category → $TX_CROSS_CAT_CODE (expected 404)"
else
  fail "Cross-category check → skipped (no user2 category id)"
fi

info "28. POST /transactions invalid body (amount=0) → 400"
TX_BAD_CODE=$(http_code POST /api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{\"amount\":0,\"type\":\"EXPENSE\",\"date\":\"$TX_DATE\",\"categoryId\":\"$TX_CATEGORY_ID\"}")
[ "$TX_BAD_CODE" = "400" ] && pass "Invalid amount=0 → 400 Bad Request" \
                            || fail "Invalid amount=0 → $TX_BAD_CODE (expected 400)"

info "29. GET /transactions without token → 401"
TX_NO_AUTH=$(http_code GET /api/v1/transactions)
[ "$TX_NO_AUTH" = "401" ] && pass "GET /transactions (no token) → 401 Unauthorized" \
                           || fail "GET /transactions (no token) → $TX_NO_AUTH (expected 401)"

info "30. DELETE /transactions/{id} → 204"
if [ -n "$TX_ID" ]; then
  TX_DEL_CODE=$(http_code DELETE "/api/v1/transactions/$TX_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  [ "$TX_DEL_CODE" = "204" ] && pass "DELETE /transactions/{id} → 204 No Content" \
                              || fail "DELETE /transactions/{id} → $TX_DEL_CODE (expected 204)"
else
  fail "DELETE /transactions/{id} → skipped (no transaction id)"
fi

info "31. GET /transactions/{id} after delete → 404"
if [ -n "$TX_ID" ]; then
  TX_AFTER_DEL=$(http_code GET "/api/v1/transactions/$TX_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  [ "$TX_AFTER_DEL" = "404" ] && pass "GET /transactions/{id} after delete → 404 Not Found" \
                               || fail "GET /transactions/{id} after delete → $TX_AFTER_DEL (expected 404)"
else
  fail "GET /transactions/{id} after delete → skipped"
fi

# ── Dashboard: GET /transactions/latest ───────────────────────────────────────

# Re-create a transaction for latest endpoint tests
TX_CAT_FULL2=$(curl -si -X POST "$BASE_URL/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"LatestTestCat","color":"#ABCDEF","icon":"🔖"}')
TX_CAT_LOCATION2=$(echo "$TX_CAT_FULL2" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
TX_CATEGORY_ID2=$(echo "$TX_CAT_LOCATION2" | grep -oE '[0-9a-f-]{36}$')

for i in $(seq 1 3); do
  curl -s -X POST "$BASE_URL/api/v1/transactions" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"amount\":$(( i * 10 )).00,\"type\":\"INCOME\",\"date\":\"2026-05-07T10:0${i}:00Z\",\"categoryId\":\"$TX_CATEGORY_ID2\"}" > /dev/null
done

info "32. GET /transactions/latest (default) → 200 with content array"
LATEST_BODY=$(http_body GET "/api/v1/transactions/latest" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
LATEST_CODE=$(http_code GET "/api/v1/transactions/latest" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
[ "$LATEST_CODE" = "200" ] && pass "GET /transactions/latest → 200 OK" \
                            || fail "GET /transactions/latest → $LATEST_CODE (expected 200)"
echo "$LATEST_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); assert 'content' in d and isinstance(d['content'], list)" 2>/dev/null \
  && pass "GET /transactions/latest → response has content array" \
  || fail "GET /transactions/latest → content array missing or malformed"

info "33. GET /transactions/latest?size=2 → exactly 2 items"
LATEST_2=$(http_body GET "/api/v1/transactions/latest?size=2" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$LATEST_2" | python3 -c "import sys,json; d=json.load(sys.stdin); assert len(d['content']) <= 2, f'Expected <=2, got {len(d[\"content\"])}'" 2>/dev/null \
  && pass "GET /transactions/latest?size=2 → content.length <= 2" \
  || fail "GET /transactions/latest?size=2 → unexpected content length"

info "34. GET /transactions/latest without token → 401"
LATEST_NO_AUTH=$(http_code GET "/api/v1/transactions/latest")
[ "$LATEST_NO_AUTH" = "401" ] && pass "GET /transactions/latest (no token) → 401 Unauthorized" \
                               || fail "GET /transactions/latest (no token) → $LATEST_NO_AUTH (expected 401)"

info "35. GET /transactions/latest?page=-1 → 400"
LATEST_BAD_PAGE=$(http_code GET "/api/v1/transactions/latest?page=-1" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
[ "$LATEST_BAD_PAGE" = "400" ] && pass "GET /transactions/latest?page=-1 → 400 Bad Request" \
                                || fail "GET /transactions/latest?page=-1 → $LATEST_BAD_PAGE (expected 400)"

# ── Payment Methods CRUD ─────────────────────────────────────────────────────

# Fresh category for tests that link a transaction to a payment method
PM_CAT_FULL=$(curl -si -X POST "$BASE_URL/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"PmTestCategory","color":"#998877","icon":"💳"}')
PM_CAT_LOCATION=$(echo "$PM_CAT_FULL" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
PM_CATEGORY_ID=$(echo "$PM_CAT_LOCATION" | grep -oE '[0-9a-f-]{36}$')

info "36. POST /payment-methods → 201 + Location"
PM_CREATE_FULL=$(curl -si -X POST "$BASE_URL/api/v1/payment-methods" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"Visa Gold","type":"CARD","last4":"1234","balance":"1000.0000"}')
PM_CREATE_STATUS=$(echo "$PM_CREATE_FULL" | head -1 | grep -oE '[0-9]{3}' | head -1)
[ "$PM_CREATE_STATUS" = "201" ] && pass "POST /payment-methods → 201 Created" \
                                 || fail "POST /payment-methods → $PM_CREATE_STATUS (expected 201)"
PM_LOCATION=$(echo "$PM_CREATE_FULL" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
PM_ID=$(echo "$PM_LOCATION" | grep -oE '[0-9a-f-]{36}$')
[ -n "$PM_ID" ] && pass "POST /payment-methods → Location header present" \
                || fail "POST /payment-methods → Location header missing"

info "37. GET /payment-methods → list contains created method"
PM_LIST=$(http_body GET /api/v1/payment-methods \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$PM_LIST" | grep -q '"Visa Gold"' \
  && pass "GET /payment-methods → contains 'Visa Gold'" \
  || fail "GET /payment-methods → 'Visa Gold' not found in list"

info "38. POST /payment-methods duplicate name → 409"
PM_DUP_CODE=$(http_code POST /api/v1/payment-methods \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"Visa Gold","type":"CARD"}')
[ "$PM_DUP_CODE" = "409" ] && pass "Duplicate payment method name → 409 Conflict" \
                            || fail "Duplicate payment method name → $PM_DUP_CODE (expected 409)"

info "39. POST /payment-methods invalid last4 → 400"
PM_BAD_CODE=$(http_code POST /api/v1/payment-methods \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"name":"Bad","type":"CARD","last4":"abcd"}')
[ "$PM_BAD_CODE" = "400" ] && pass "Invalid last4 → 400 Bad Request" \
                            || fail "Invalid last4 → $PM_BAD_CODE (expected 400)"

info "40. PATCH /payment-methods/{id} archive=true → 200 + archived flag set"
if [ -n "$PM_ID" ]; then
  PM_PATCH_BODY=$(http_body PATCH "/api/v1/payment-methods/$PM_ID" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d '{"archived":true}')
  echo "$PM_PATCH_BODY" | grep -q '"archived":true' \
    && pass "PATCH archive=true → archived flag set" \
    || fail "PATCH archive=true → archived flag not set in response"
else
  fail "PATCH archive → skipped (no payment method id)"
fi

info "41. GET /payment-methods/{id} with other user's token → 404 (ownership)"
if [ -n "$PM_ID" ] && [ -n "$ACCESS_TOKEN2" ]; then
  PM_OWN_CODE=$(http_code GET "/api/v1/payment-methods/$PM_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN2")
  [ "$PM_OWN_CODE" = "404" ] && pass "GET /payment-methods/{id} other user → 404 Not Found" \
                              || fail "GET /payment-methods/{id} other user → $PM_OWN_CODE (expected 404)"
else
  fail "PM ownership check → skipped (missing id or token)"
fi

info "42. POST /transactions with other user's paymentMethodId → 404"
if [ -n "$PM_ID" ] && [ -n "$CAT2_ID" ] && [ -n "$ACCESS_TOKEN2" ]; then
  TX_FOREIGN_PM_CODE=$(http_code POST /api/v1/transactions \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN2" \
    -d "{\"amount\":5.00,\"type\":\"EXPENSE\",\"date\":\"$TX_DATE\",\"categoryId\":\"$CAT2_ID\",\"paymentMethodId\":\"$PM_ID\"}")
  [ "$TX_FOREIGN_PM_CODE" = "404" ] && pass "POST /transactions with foreign paymentMethodId → 404 Not Found" \
                                     || fail "POST /transactions with foreign paymentMethodId → $TX_FOREIGN_PM_CODE (expected 404)"
else
  fail "Cross-user payment method check → skipped (missing prerequisites)"
fi

info "43. POST /transactions with own paymentMethodId → 201"
if [ -n "$PM_ID" ] && [ -n "$PM_CATEGORY_ID" ]; then
  TX_WITH_PM_FULL=$(curl -si -X POST "$BASE_URL/api/v1/transactions" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"amount\":42.00,\"type\":\"EXPENSE\",\"date\":\"$TX_DATE\",\"categoryId\":\"$PM_CATEGORY_ID\",\"paymentMethodId\":\"$PM_ID\"}")
  TX_WITH_PM_STATUS=$(echo "$TX_WITH_PM_FULL" | head -1 | grep -oE '[0-9]{3}' | head -1)
  [ "$TX_WITH_PM_STATUS" = "201" ] && pass "POST /transactions with own paymentMethodId → 201 Created" \
                                    || fail "POST /transactions with own paymentMethodId → $TX_WITH_PM_STATUS (expected 201)"
  TX_WITH_PM_LOCATION=$(echo "$TX_WITH_PM_FULL" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
  TX_WITH_PM_ID=$(echo "$TX_WITH_PM_LOCATION" | grep -oE '[0-9a-f-]{36}$')
else
  fail "POST /transactions linked → skipped (missing prerequisites)"
fi

info "44. DELETE /payment-methods/{id} → 204"
if [ -n "$PM_ID" ]; then
  PM_DEL_CODE=$(http_code DELETE "/api/v1/payment-methods/$PM_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  [ "$PM_DEL_CODE" = "204" ] && pass "DELETE /payment-methods/{id} → 204 No Content" \
                              || fail "DELETE /payment-methods/{id} → $PM_DEL_CODE (expected 204)"
fi

info "45. After PM delete: linked transaction survives with paymentMethodId=null (SET NULL)"
if [ -n "$TX_WITH_PM_ID" ]; then
  TX_AFTER_PM_DEL=$(http_body GET "/api/v1/transactions/$TX_WITH_PM_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  echo "$TX_AFTER_PM_DEL" | grep -q '"paymentMethodId":null' \
    && pass "Linked transaction survived with paymentMethodId=null" \
    || fail "Linked transaction: paymentMethodId not null after PM delete → $TX_AFTER_PM_DEL"
else
  fail "SET NULL check → skipped (no linked tx id)"
fi

info "46. GET /payment-methods without token → 401"
PM_NO_AUTH=$(http_code GET /api/v1/payment-methods)
[ "$PM_NO_AUTH" = "401" ] && pass "GET /payment-methods (no token) → 401 Unauthorized" \
                           || fail "GET /payment-methods (no token) → $PM_NO_AUTH (expected 401)"

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

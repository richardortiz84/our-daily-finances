# Our Daily Finances — API Reference

Base URL: `https://<your-railway-domain>`

All endpoints except `/health` require a Firebase ID token:

```
Authorization: Bearer <firebase_id_token>
```

Obtain the token on Android via `FirebaseAuth.getInstance().currentUser.getIdToken(false)`.

---

## Authentication

All protected endpoints return `401 Unauthorized` when:
- The `Authorization` header is missing
- The token is invalid or expired

---

## Health

### `GET /health`

No auth required.

**Response `200`**
```json
{ "status": "ok" }
```

---

## Plaid — Account Linking

### `POST /plaid/link/token`

Creates a Plaid Link token to initialize the Plaid Link SDK on Android.

**Response `200`**
```json
{
  "link_token": "link-sandbox-abc123..."
}
```

Pass `link_token` to the Android Plaid Link SDK to start the account linking flow. After the user completes the flow, the SDK returns a `public_token` — exchange it with the next endpoint.

---

### `POST /plaid/link/exchange`

Exchanges the `public_token` received from the Plaid SDK for a stored access token. Links the bank account to the authenticated user.

**Request body**
```json
{
  "public_token": "public-sandbox-...",
  "institution_name": "Chase"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `public_token` | string | yes | Token from Plaid SDK after user completes Link |
| `institution_name` | string | no | Display name for the institution |

**Response `200`**
```json
{
  "item_id": "plaid-item-id-abc",
  "institution_name": "Chase"
}
```

**Response `409`** — Institution already connected for this user.

---

### `GET /plaid/items`

Lists all linked bank connections (Plaid items) for the authenticated user.

**Response `200`**
```json
{
  "items": [
    {
      "item_id": "plaid-item-id-abc",
      "institution_name": "Chase",
      "created_at": "2024-01-15T10:30:00+00:00"
    }
  ]
}
```

---

### `DELETE /plaid/items/{item_id}`

Unlinks a bank connection. Cascades: removes all associated accounts, transactions, and any dashboard entries for those accounts.

**Path parameter**
| Param | Type | Description |
|---|---|---|
| `item_id` | string | The `item_id` from `/plaid/items` |

**Response `200`**
```json
{ "message": "Item deleted." }
```

**Response `404`** — Item not found or not owned by this user.

---

## Plaid — Balances

### `GET /plaid/balance`

Returns cached balance data for all of the user's linked accounts. Refreshes from Plaid automatically if data is stale (older than 24 hours or never fetched).

**Response `200`**
```json
{
  "accounts": [
    {
      "account_id": "plaid-account-id-xyz",
      "name": "Total Checking",
      "official_name": "Chase Total Checking℠",
      "type": "depository",
      "subtype": "checking",
      "current_balance": 1250.00,
      "available_balance": 1200.00,
      "iso_currency_code": "USD"
    }
  ]
}
```

| Field | Type | Nullable | Description |
|---|---|---|---|
| `account_id` | string | no | Plaid account identifier |
| `name` | string | no | Short account name |
| `official_name` | string | yes | Full official account name |
| `type` | string | no | `depository`, `credit`, `loan`, `investment`, `other` |
| `subtype` | string | yes | e.g. `checking`, `savings`, `credit card` |
| `current_balance` | number | yes | Current balance |
| `available_balance` | number | yes | Available balance (may differ for credit accounts) |
| `iso_currency_code` | string | yes | e.g. `USD` |

---

## Plaid — Transactions

### `GET /plaid/transactions`

Returns cached transactions for all of the user's linked accounts, ordered by date descending. Refreshes from Plaid automatically if data is stale (older than 24 hours or never fetched). Handles added, modified, and removed transactions via Plaid's sync API.

**Response `200`**
```json
{
  "transactions": [
    {
      "transaction_id": "plaid-txn-id-abc",
      "account_id": "plaid-account-id-xyz",
      "amount": 4.55,
      "date": "2024-06-01",
      "name": "Starbucks",
      "merchant_name": "Starbucks",
      "category": ["Food and Drink", "Coffee Shop"],
      "pending": false,
      "iso_currency_code": "USD"
    }
  ]
}
```

| Field | Type | Nullable | Description |
|---|---|---|---|
| `transaction_id` | string | no | Plaid transaction identifier |
| `account_id` | string | no | Plaid account this transaction belongs to |
| `amount` | number | no | Positive = debit (money out), negative = credit (money in) |
| `date` | string | no | Date as `YYYY-MM-DD` |
| `name` | string | no | Transaction name |
| `merchant_name` | string | yes | Cleaned merchant name when available |
| `category` | string[] | no | Plaid category hierarchy, e.g. `["Food and Drink", "Restaurants"]` |
| `pending` | boolean | no | Whether the transaction has settled |
| `iso_currency_code` | string | yes | e.g. `USD` |

---

## Dashboards

Dashboards are shared views that pool accounts from multiple users. The creator is the owner. Any member can add or remove their own linked accounts. Non-owner members can leave; leaving removes their accounts from the dashboard.

### `POST /dashboards`

Creates a new dashboard. The creator is automatically added as a member and becomes the owner. An 8-character invite code is generated for sharing.

**Request body**
```json
{ "name": "Family Budget" }
```

**Response `200`**
```json
{
  "dashboard_id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Family Budget",
  "owner_user_id": "firebase-uid-abc",
  "invite_code": "A3F2B190"
}
```

---

### `GET /dashboards`

Lists all dashboards the authenticated user belongs to (owned or joined). The `invite_code` is only returned for dashboards the user owns.

**Response `200`**
```json
{
  "dashboards": [
    {
      "dashboard_id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Family Budget",
      "owner_user_id": "firebase-uid-abc",
      "invite_code": "A3F2B190"
    },
    {
      "dashboard_id": "661f9511-f30c-52e5-b827-557766551111",
      "name": "Shared Expenses",
      "owner_user_id": "firebase-uid-xyz",
      "invite_code": null
    }
  ]
}
```

---

### `POST /dashboards/join`

Joins a dashboard using an invite code shared by the owner.

**Request body**
```json
{ "invite_code": "A3F2B190" }
```

**Response `200`**
```json
{
  "dashboard_id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Family Budget"
}
```

**Response `404`** — Invalid invite code.  
**Response `409`** — Already a member of this dashboard.

---

### `DELETE /dashboards/{dashboard_id}/leave`

Leaves a dashboard. Removes the user as a member and removes all accounts they added to this dashboard. Owners cannot leave their own dashboard.

**Path parameter**
| Param | Type | Description |
|---|---|---|
| `dashboard_id` | UUID | Dashboard to leave |

**Response `200`**
```json
{ "message": "Left dashboard." }
```

**Response `403`** — Owner cannot leave, or user is not a member.  
**Response `404`** — Dashboard not found.

---

### `POST /dashboards/{dashboard_id}/accounts`

Adds one of the user's linked accounts to a dashboard. The user must be a member of the dashboard and must own the account.

**Path parameter**
| Param | Type | Description |
|---|---|---|
| `dashboard_id` | UUID | Target dashboard |

**Request body**
```json
{ "account_id": "plaid-account-id-xyz" }
```

Use an `account_id` from `GET /plaid/balance`.

**Response `200`**
```json
{ "message": "Account added." }
```

**Response `403`** — Not a member of this dashboard, or account not owned by user.  
**Response `409`** — Account already on this dashboard.

---

### `DELETE /dashboards/{dashboard_id}/accounts/{account_id}`

Removes an account from a dashboard. Only the user who added the account can remove it.

**Path parameters**
| Param | Type | Description |
|---|---|---|
| `dashboard_id` | UUID | Dashboard containing the account |
| `account_id` | string | Plaid account ID to remove |

**Response `200`**
```json
{ "message": "Account removed." }
```

**Response `403`** — Account was added by a different user.  
**Response `404`** — Account not on this dashboard.

---

### `GET /dashboards/{dashboard_id}`

Returns full dashboard detail: member list, all linked accounts with balances, and all transactions for those accounts. Must be a member to view. `invite_code` is only returned to the owner.

**Path parameter**
| Param | Type | Description |
|---|---|---|
| `dashboard_id` | UUID | Dashboard to fetch |

**Response `200`**
```json
{
  "dashboard_id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Family Budget",
  "owner_user_id": "firebase-uid-abc",
  "invite_code": "A3F2B190",
  "members": [
    {
      "user_id": "firebase-uid-abc",
      "joined_at": "2024-01-15T10:30:00+00:00"
    },
    {
      "user_id": "firebase-uid-xyz",
      "joined_at": "2024-01-16T08:00:00+00:00"
    }
  ],
  "accounts": [
    {
      "account_id": "plaid-account-id-xyz",
      "name": "Total Checking",
      "official_name": "Chase Total Checking℠",
      "type": "depository",
      "subtype": "checking",
      "current_balance": 1250.00,
      "available_balance": 1200.00,
      "iso_currency_code": "USD",
      "added_by_user_id": "firebase-uid-abc"
    }
  ],
  "transactions": [
    {
      "transaction_id": "plaid-txn-id-abc",
      "account_id": "plaid-account-id-xyz",
      "amount": 4.55,
      "date": "2024-06-01",
      "name": "Starbucks",
      "merchant_name": "Starbucks",
      "category": ["Food and Drink", "Coffee Shop"],
      "pending": false,
      "iso_currency_code": "USD"
    }
  ]
}
```

Transactions are ordered by date descending and span all accounts on the dashboard (across all members).

**Response `403`** — Not a member of this dashboard.  
**Response `404`** — Dashboard not found.

---

## Common Error Shape

All error responses use FastAPI's default shape:

```json
{ "detail": "Human-readable error message." }
```

---

## Typical Android Flow

```
1. User opens app → Firebase sign-in → get ID token

2. Link a bank account:
   POST /plaid/link/token          → link_token
   (pass link_token to Plaid SDK)
   (user completes Link UI → public_token)
   POST /plaid/link/exchange       → item_id

3. View balances and transactions:
   GET /plaid/balance              → accounts[]
   GET /plaid/transactions         → transactions[]

4. Create a shared dashboard:
   POST /dashboards                → dashboard_id, invite_code
   POST /dashboards/{id}/accounts  → add an account to it
   (share invite_code out-of-band)

5. Other user joins and contributes:
   POST /dashboards/join           → join by invite_code
   POST /dashboards/{id}/accounts  → add their own account

6. View combined dashboard:
   GET /dashboards/{id}            → all accounts + transactions
```

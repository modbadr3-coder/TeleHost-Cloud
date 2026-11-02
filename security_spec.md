# Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. Every `TelegramBot` document under `/users/{userId}/bots/{botId}` must belong strictly to the authenticated user (`request.auth.uid == userId` and `data.userId == userId`).
2. Every `WebSiteProject` document under `/users/{userId}/sites/{siteId}` must belong strictly to the authenticated user (`request.auth.uid == userId` and `data.userId == userId`).
3. Every `DeploymentLog` document under `/users/{userId}/logs/{logId}` must belong strictly to the authenticated user (`request.auth.uid == userId` and `data.userId == userId`) and is append-only (updates disallowed, owner can delete).
4. Document IDs (`userId`, `botId`, `siteId`, `logId`) on single-document operations must match `^[a-zA-Z0-9_\-]+$` and be `<= 128` characters.
5. `createdAt` and `userId` are immutable on all updates (`incoming().createdAt == existing().createdAt` and `incoming().userId == existing().userId`).
6. All strings have explicit `.size()` bounds and all timestamps enforce `is timestamp && <= request.time`.

## 2. The "Dirty Dozen" Payloads
1. Unauthenticated read/list on `/users/alice_123/bots`.
2. Cross-user read (`bob_456` reading `/users/alice_123/bots/bot_1`).
3. Identity spoofing on create (`alice_123` creating `/users/alice_123/bots/bot_1` with `userId: "bob_456"`).
4. Shadow field injection (`isVerified: true` extra field on bot creation or update).
5. Missing required field (`botToken` omitted on bot creation).
6. Invalid enum state (`status: "hacked"` on bot creation/update).
7. Oversized string DoS (`htmlContent` > 250000 chars or `name` > 100 chars).
8. Immutable field tampering (`createdAt` or `userId` modified during bot or site update).
9. Type poisoning (`processedUpdatesCount: "not_an_int"` on update).
10. Future timestamp injection (`createdAt` in the future `> request.time`).
11. Malformed path ID (`botId` containing spaces or special characters on `get`/`create`).
12. Unauthorized update on immutable `logs` collection.

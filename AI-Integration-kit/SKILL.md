---
name: freshchat-to-freshdesk-android-migration
description: >-
  Migrates Android apps from Freshchat Android SDK (com.github.freshworks:freshchat-android)
  to Freshdesk Android SDK (com.freshworks.sdk:freshdesk). Audits Freshchat usage, applies
  API replacements from the migration reference, flags gaps with no Freshdesk equivalent,
  and verifies against the migration checklist. Use when the user mentions Freshchat migration,
  freshchat-android, Freshdesk SDK replacement, identifyUser to JWT, or moving off Freshchat on Android.
---

# Freshchat → Freshdesk Android SDK migration

You are migrating an **existing** Android app from the **Freshchat Android SDK** to the **Freshdesk Android SDK**. This is a **re-integration**, not a dependency swap.

## Required reading

1. **This file** — workflow, rules, and deliverables.
2. **[MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md](./MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md)** — authoritative API mapping, before/after code, gaps, and §19 checklist.

Do not invent Freshdesk APIs. If the migration doc marks a capability as `NA` or documents a gap, report it; do not stub equivalents.

## Non-negotiable facts (§1)

- Freshdesk uses an **embedded web widget**, not Freshchat’s native UI. Resource theming (`Theme.Freshchat.*`, `freshchat_*` strings) does **not** carry over; configure appearance in the Freshdesk portal.
- **`initialize(context, SDKConfig, callback)` is async.** Most SDK calls before init or while offline are **dropped**, not queued indefinitely. Gate UI and preflight-sensitive calls on the init callback (see migration doc §5.2).
- **Identity is JWT-first.** There is no `identifyUser(externalId, restoreId)`. With JWT enforcement, `setUserProperties` / `setTicketProperties` may be **silently ignored**.
- **No programmatic messaging** — `Freshchat.sendMessage` has no equivalent.
- **Custom fields** must exist as Contact/Ticket fields on the portal-linked widget before `setUserProperties` / `setTicketProperties`.
- **`minSdk` 26** (was 23 on Freshchat).

## Workflow

### Step 1 — Discover Freshchat usage (read-only)

Search the consumer project for:

| Signal | Examples |
|--------|----------|
| Gradle | `freshchat-android`, JitPack `jitpack.io` |
| Packages | `com.freshchat.consumer.sdk` |
| Types / calls | `Freshchat`, `FreshchatConfig`, `FreshchatUser`, `FreshchatMessage`, `FaqOptions`, `ConversationOptions` |
| Broadcasts | `FRESHCHAT_*`, `Event.EventName` |
| Resources | `freshchat_*` strings, `Theme.Freshchat` |

Build an **inventory** (markdown table):

| File | Symbol / API | Migration doc section | Status |
|------|----------------|----------------------|--------|
| … | … | §5, §12, … | Direct / Changed / Gap / New |

Use migration doc **§3** (API mapping index) to assign status. Cross-check **Appendix A** for confirmed gaps.

### Step 2 — Portal and planning gate

Before editing app code, confirm **§2** and **§19 Phase 1** with the user (or list blockers):

- Freshdesk credentials: `token`, `host`, `sdkID` (Admin Settings → Mobile Chat SDK)
- JWT enforcement decision and backend signing
- Contact/Ticket fields whitelisted for every custom property key
- FCM configured in portal
- Product/design sign-off on **gaps** (tag filtering, deeplinks, pull unread count, notification interception, native theming, etc.)

If credentials or portal setup are missing, **stop at planning** and output blockers instead of placeholder secrets.

### Step 3 — Project setup (§4, §19 Phase 2)

Apply in order:

1. Remove Freshchat dependency and JitPack if unused elsewhere.
2. Add Maven Central + `com.freshworks.sdk:freshdesk` (use latest stable version from Maven Central unless the user pins a version).
3. Raise `minSdk` to 26.
4. Resolve manifest merge issues (FileProvider `${applicationId}.fileProvider`, backup rules, `enableOnBackInvokedCallback`) per §4.4.
5. Remove Freshchat-only resources, themes, `FreshchatImageLoader`, and obsolete ProGuard rules if any.

Match the project’s existing style (Kotlin vs Java, Groovy vs KTS).

### Step 4 — Code migration (§5–§17, §19 Phase 3)

For each inventory row with status **Changed** or **Direct**:

1. Open the matching section in the migration document.
2. Apply the documented before/after pattern with **minimal diffs**.
3. Prefer **`Application.onCreate()`** for `FreshdeskSDK.initialize` (not launcher Activity only).

**High-impact replacements** (verify every occurrence):

| Freshchat | Freshdesk |
|-----------|-----------|
| `Freshchat.getInstance(ctx)` | `FreshdeskSDK` |
| `init(FreshchatConfig)` | `initialize(context, SDKConfig, callback)` |
| `showConversations` | `openSupport` |
| `showFAQs` | `openKnowledgeBase` |
| `setUser` / `setUserProperty` | `setUserProperties(Map)` |
| `getUser()` sync | `getUser(onFailure, callback)` |
| `identifyUser` / restore flow | JWT via `SDKConfig.jwt` + `authenticateAndUpdate` |
| `resetUser(context)` | `resetUser(onSuccess, onFailure)`; block support on failure |
| `setUser(jwt)` / `restoreUser(jwt)` | init JWT + `authenticateAndUpdate` |
| `getUnreadCountAsync` | `UNREAD_COUNT` broadcast only |
| `getUserIdTokenStatus()` | `USER_STATE_CHANGE` + `UserState` |
| Push: `RemoteMessage` helpers | `remoteMessage.data` map variants |
| `FRESHCHAT_EVENTS` | Per-action `SDKEventID.*` receivers |
| `setCustomLinkHandler` | `setLinkHandler` (default: open unknown URLs) |
| String overrides in `res/values` | `ContentConfiguration` |

**Gap rows (Appendix A):** do not implement fake APIs. Add a **Gap report** entry: old behavior, Freshdesk status, suggested workaround (e.g. ticket properties instead of bot variables), owner for product decision.

### Step 5 — Verification (§19 Phase 4)

Guide or implement tests where the project already has them; otherwise list **manual verification** steps:

- Init callback gates UI; offline cold start does not crash
- Custom properties visible in Freshdesk (whitelisting)
- Push: token registration, foreground/background, tap behavior
- Unread badge from broadcast
- JWT lifecycle: authenticated → expired → refresh
- Logout: `dismissFreshdeskViews` → `resetUser` → different user; offline logout retry
- Link handler covers unknown URLs
- Crash reporter still works (install app exception handler **before** `initialize` — §18)
- API 26 and latest API; RTL/locale if applicable

### Step 6 — Deliverables

Provide:

1. **Migration summary** — files changed, dependency diff, init pattern used.
2. **Inventory** — updated with resolution per row.
3. **Gap report** — anything from Appendix A or §6/§10/§11 still unresolved.
4. **Checklist** — copy §19 phases with `[x]` / `[ ]` for the user.

## Agent rules

- **Minimal scope:** only migration-related edits unless the user asks for more.
- **No dual SDK** in production unless the user explicitly requests a temporary feature flag; default is remove Freshchat when Freshdesk compiles and critical paths work.
- **Call order:** register `setNotificationConfig`, `setPushRegistrationToken`, and `setLinkHandler` after init readiness when preflight applies (§5.2, §15.1).
- **JWT enforcement:** distinguish property updates (may be blocked) from `authenticateAndUpdate` (see migration doc §9).
- **Do not** keep handling for `MethodNotAllowedException`, `JwtException`, or `FreshchatCallbackStatus` unless the app still ships Freshchat.
- After migration, point the user to the public [Freshdesk Android SDK README](https://github.com/freshworks/freshdesk-android-sdk/blob/main/README.md) for features not covered by the migration guide.

## Quick reference — migration doc map

| Topic | Section |
|-------|---------|
| API index | §3 |
| Gradle / manifest | §4 |
| Initialization | §5 |
| Opening UI | §6 |
| User properties | §7 |
| Logout / reset | §8 |
| JWT | §9 |
| Unread | §10 |
| sendMessage / bots | §11 |
| Push | §12 |
| Analytics | §13 |
| Events | §14 |
| Listeners | §15 |
| Localization | §16 |
| Theming | §17 |
| Diagnostics | §18 |
| Checklist | §19 |
| No equivalent APIs | Appendix A |
| Versions / coordinates | Appendix C |

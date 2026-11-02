# Migration — Android Freshchat SDK to Freshdesk SDK APIs

> **Audience:** Android app developers currently integrating the **Freshchat Android SDK** (`com.github.freshworks:freshchat-android`) who are moving to the **Freshdesk Android SDK** (`com.freshworks.sdk:freshdesk`).
>
> **Source of truth for this document:** Freshchat Android SDK `6.5.10` and Freshdesk Android SDK `2.2.4`. Code samples are **Kotlin**.

---

## 1. Read this first — what actually changes

The Freshdesk Android SDK is **not** a drop-in replacement for Freshchat. It is a different product with a different architecture, so plan this as a re-integration rather than a dependency bump.

| # | Change | Why it matters to you |
|---|--------|-----------------------|
| 1 | **Native UI → embedded web widget.** Freshchat shipped native Activities. Freshdesk shows a single screen that hosts a web widget. | Resource-based theming (`Theme.Freshchat.SelectedTheme`, `freshchat*` theme attributes, `res/values` string overrides) **does not carry over**. Appearance is now set in the Freshdesk portal. |
| 2 | **Chat channels → tickets and topics.** Freshchat's model was channels/conversations filtered by tags. Freshdesk's model is topics + tickets + knowledge base. | Anything built on tags, channel IDs, parallel conversations or conversation reference IDs has no direct equivalent. |
| 3 | **Init is asynchronous and call order matters.** Freshchat's `init()` returned a `boolean` synchronously. Freshdesk's `initialize()` takes a readiness callback, and most SDK calls made before `initialize` runs — or while the device is offline — are ignored rather than retried. | Call `initialize` first and make your other SDK calls from its callback (or after it). A few methods, such as the version getters and listener setters, can be called at any time — see [§5.2](#52-after--freshdesk). |
| 4 | **Identity is JWT-first.** Freshchat had `identifyUser(externalId, restoreId)` plus a JWT mode. Freshdesk has no `externalId`/`restoreId` API — identity lives in a server-signed JWT. | If JWT enforcement is on, `setUserProperties` / `setTicketProperties` are silently ignored. |
| 5 | **No programmatic message sending.** | `Freshchat.sendMessage(...)` has no equivalent. The end user must type. |
| 6 | **Custom fields must be pre-whitelisted in the portal.** | Keys passed to `setUserProperties` / `setTicketProperties` that are not configured as Contact Fields / Ticket Fields on the linked widget are dropped. |
| 7 | **minSdk rises from 23 to 26.** | If you still support API 23–25, you cannot migrate without dropping those devices. |

---

## 2. Prerequisites — portal and server setup

Complete these **before** touching app code.

| Step | Freshchat | Freshdesk |
|------|-----------|-----------|
| Credentials location | Freshchat portal → app ID + app key + domain | Freshdesk portal → **Admin Settings → Mobile Chat SDK** |
| Credentials needed | `appId`, `appKey`, `domain` | `token` (account token), `host` (portal URL), `sdkID` |
| Custom user fields | Created implicitly by `setUserProperties` | Must be pre-created as **Contact Fields** on the SDK-linked widget |
| Custom ticket fields | N/A (no ticketing) | Must be pre-created as **Ticket Fields** on the SDK-linked widget |
| Push credential | FCM server key configured in Freshchat portal | FCM credential configured under **Admin Settings → Mobile Chat SDK** |
| JWT signing key | Freshchat JWT key | **JWT encryption key** on the same Mobile Chat SDK page — backend only, never in the app |

**JWT enforcement is a decision you must make up front.** It changes which APIs are usable. See [§9](#9-jwt-authentication-and-user-state).

---

## 3. API mapping index

One row per capability. `NA` means no Freshdesk equivalent exists in `2.2.4`. Detailed before/after code follows in [§5](#5-initialization) onward.

| # | Capability | Freshchat Android | Freshdesk Android | Status |
|---|-----------|-------------------|-------------------|--------|
| 1 | Dependency | `com.github.freshworks:freshchat-android` (JitPack) | `com.freshworks.sdk:freshdesk` (Maven Central) | Changed — [§4](#4-dependency-and-project-setup) |
| 2 | minSdk | 23 | 26 | Changed |
| 3 | Entry point | `Freshchat.getInstance(context)` (singleton + statics) | `FreshdeskSDK` (Kotlin `object`) | Changed |
| 4 | Initialize | `init(FreshchatConfig): Boolean` | `initialize(context, SDKConfig, callback)` | Changed — [§5](#5-initialization) |
| 5 | Config object | `FreshchatConfig(appId, appKey)` + setters | `SDKConfig(token, host, sdkID, …)` data class | Changed |
| 6 | SDK version | `getSDKVersionName()` / `getSDKVersionCode()` | `getSDKVersionName()` / `getSDKVersionCode()` / `getSDKBuildID()` | Direct |
| 7 | Feature toggles (camera, gallery, file, team-member info, response expectation) | `FreshchatConfig.setCameraCaptureEnabled(…)` etc. | NA — portal-controlled | Gap — [§5.3](#53-config-flags-with-no-code-equivalent) |
| 8 | Get user | `getUser(): FreshchatUser` | `getUser(onFailure, userCallback)` | Changed — [§7](#7-user-identity-and-properties) |
| 9 | Set standard user fields | `setUser(FreshchatUser)` | `setUserProperties(Map<String, Any>)` | Changed |
| 10 | Set custom user properties | `setUserProperty(k, v)` / `setUserProperties(Map<String, String>)` | `setUserProperties(Map<String, Any>)` — merged with standard keys | Changed |
| 11 | Identify / restore user | `identifyUser(externalId, restoreId)` | NA — use JWT | Gap — [§9](#9-jwt-authentication-and-user-state) |
| 12 | Get user ID / alias | `getFreshchatUserId(): String` | `getUser { it.alias }` | Changed |
| 13 | Restore ID | `getUser().getRestoreId()` + `FRESHCHAT_USER_RESTORE_ID_GENERATED` broadcast | `User.restoreId` via `getUser` / `USER_CREATED` event | Changed |
| 14 | Reset user | `Freshchat.resetUser(context)` | `resetUser(onSuccess, onFailure)` | Changed — [§8](#8-session-management-and-logout) |
| 15 | Ticket properties | NA (no ticketing) | `setTicketProperties(Map<String, Any>)` | New |
| 16 | Set user via JWT | `setUser(jwtIdToken)` | `SDKConfig.jwt` at init, or `authenticateAndUpdate(jwt)` | Changed — [§9](#9-jwt-authentication-and-user-state) |
| 17 | Restore user via JWT | `restoreUser(jwtIdToken)` | Not required — `initialize` / `authenticateAndUpdate` handle it | Changed |
| 18 | Read JWT status on demand | `getUserIdTokenStatus(): JwtTokenStatus` | NA — push-only via `USER_STATE_CHANGE` broadcast | Gap — [§9.3](#93-there-is-no-pull-based-jwt-status-getter) |
| 19 | Token refresh prompt | `FRESHCHAT_SET_TOKEN_TO_REFRESH_DEVICE_PROPERTIES` broadcast | `UserState.AUTH_EXPIRED` via `USER_STATE_CHANGE` | Changed |
| 20 | Show conversations | `Freshchat.showConversations(context)` | `openSupport(context)` | Changed — [§6](#6-opening-the-sdk-ui) |
| 21 | Show FAQs | `Freshchat.showFAQs(context)` | `openKnowledgeBase(context)` | Direct (renamed) |
| 22 | FAQ customization | `FaqOptions` (grid/list, contact-us placement, tag filters) | NA | Gap — [§6.3](#63-faqoptions-has-no-equivalent) |
| 23 | Filter conversations by tag | `ConversationOptions.filterByTags(tags, title)` | NA | Gap — [§6.4](#64-tag-filtering-and-parallel-conversations-have-no-equivalent) |
| 24 | Parallel conversations | `showConversations(ctx, conversationReferenceId, topicName)` | `openTopic(context, topicName, topicId)` — partial | Gap — [§6.4](#64-tag-filtering-and-parallel-conversations-have-no-equivalent) |
| 25 | Deeplink into channel/FAQ | `openFreshchatDeeplink(context, url)` | NA | Gap — [§6.5](#65-deeplinks-have-no-equivalent) |
| 26 | Dismiss SDK screens | NA | `dismissFreshdeskViews()` | New |
| 27 | Conversation banner message | `setConversationBannerMessage(context, msg)` (deprecated) | NA | Gap |
| 28 | Send message programmatically | `Freshchat.sendMessage(context, FreshchatMessage)` | NA | Gap — [§11.1](#111-sendmessage-has-no-equivalent) |
| 29 | Bot variables | `setBotVariables(vars, botSpecificVars)` | NA | Gap — [§11.2](#112-bot-variables-have-no-equivalent) |
| 30 | Unread count (pull) | `getUnreadCountAsync(callback)` | NA — event only | Gap — [§10](#10-unread-count) |
| 31 | Unread count (push) | `FRESHCHAT_UNREAD_MESSAGE_COUNT_CHANGED` broadcast | `SDKEventID.UNREAD_COUNT` broadcast, `Int` extra | Changed |
| 32 | Unread count by tag | `getUnreadCountAsync(callback, tagsList)` | NA | Gap |
| 33 | Unread count by reference ID | `getUnreadCountAsyncWithReferenceId(cb, topic, refId)` | NA | Gap |
| 34 | Register FCM token | `setPushRegistrationToken(token)` | `setPushRegistrationToken(token)` | Direct — [§12](#12-push-notifications) |
| 35 | Is SDK notification? | `Freshchat.isFreshchatNotification(remoteMessage)` | `isFreshdeskSDKNotification(remoteMessage.data)` | Changed (takes `Map`, not `RemoteMessage`) |
| 36 | Handle push | `Freshchat.handleFcmMessage(context, remoteMessage)` | `handleFCMNotification(remoteMessage.data)` | Changed |
| 37 | Notification appearance | `setNotificationConfig(FreshchatNotificationConfig)` | `setNotificationConfig(NotificationConfig)` | Changed — [§12.3](#123-notification-appearance) |
| 38 | Notification interception | `setNotificationInterceptionEnabled(true)` + `FRESHCHAT_ACTION_NOTIFICATION_INTERCEPTED` | NA | Gap — [§12.4](#124-notification-interception-has-no-equivalent) |
| 39 | Launch activity on finish | `launchActivityOnFinish(className)` | NA | Gap |
| 40 | Track custom event | `Freshchat.trackEvent(context, name, props)` | `trackEvent(eventName, eventData)` | Direct — [§13](#13-analytics-and-event-tracking) |
| 41 | SDK UI/state events | `FRESHCHAT_EVENTS` broadcast + `Event`/`EventName`/`Property` (44 event names, 29 properties) | `SDKEventID.*` broadcasts (7 usable) | Reduced — [§14](#14-sdk-events) |
| 42 | User created event | NA (closest: restore-ID-generated) | `SDKEventID.USER_CREATED`, `User` Parcelable extra | New |
| 43 | Custom link handling | `setCustomLinkHandler(LinkHandler)` — returns `Boolean` | `setLinkHandler(FreshDeskSDKLinkHandler)` — returns `Unit` | Changed — [§15.1](#151-custom-link-handler) |
| 44 | Auto-linkify by regex | `linkifyWithPattern(regex, scheme)` | NA | Gap |
| 45 | User interaction listener | `setFreshchatUserInteractionListener(…)` | `setFreshdeskUserInteractionListener(…)` | Direct (renamed) |
| 46 | WebView locale listener | `setWebviewListener(FreshchatWebViewListener)` | `setWebViewListener(FreshdeskWebviewListener)` | Direct (renamed) |
| 47 | Notify app locale change | `Freshchat.notifyAppLocaleChange(context)` | NA — set `SDKConfig.locale`, re-`initialize` to change | Gap — [§16](#16-localization) |
| 48 | Custom image loader | `Freshchat.setImageLoader(FreshchatImageLoader)` | NA | Gap |
| 49 | Theming | Override `Theme.Freshchat.SelectedTheme` + `freshchat*` attrs | NA in code — set in the Freshdesk portal | Gap — [§17](#17-theming-and-appearance) |
| 50 | String localization | Override `freshchat_*` string resources | `ContentConfiguration` (code-first string overrides) | Changed — [§16](#16-localization) |
| 51 | Agent profile click redirect | `setAgentProfileClickRedirectionUri(uri)` | NA | Gap |
| 52 | Content/text overrides | NA | `ContentConfiguration` at init or `setContentConfiguration(…)` | New — [§16.2](#162-contentconfiguration-new-capability) |

---

## 4. Dependency and project setup

### 4.1 Remove Freshchat

```groovy
// settings.gradle / build.gradle — REMOVE
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation 'com.github.freshworks:freshchat-android:<version>'
}
```

> **Note:** confirm your exact existing coordinate — the Freshchat artifact has historically been published under `com.github.freshworks` on JitPack.

Also remove:

- Any `freshchat_*` string resource overrides used for localization.
- Your `Theme.Freshchat.SelectedTheme` override and any `freshchat*` theme attributes.
- The `freshchat_file_provider_authority` string override and `@xml/freshchat_file_provider_paths` provider entry, **if** they were added only for Freshchat.
- Any `FreshchatImageLoader` implementation.

### 4.2 Add Freshdesk

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.freshworks.sdk:freshdesk:<latest-version>")
}
```

### 4.3 Requirements comparison

| Item | Freshchat `6.5.10` | Freshdesk `2.2.4` |
|------|--------------------|-------------------|
| Distribution | JitPack | Maven Central |
| `minSdk` | 23 | **26** |
| `compileSdk` / `targetSdk` | 36 | 36 |
| Java/Kotlin target | 1.8 | 1.8 |
| Permissions merged by SDK | `INTERNET`, `ACCESS_NETWORK_STATE` | `INTERNET`, `ACCESS_NETWORK_STATE`, `CHANGE_NETWORK_STATE` |
| SDK Activities merged | 13 native Activities | 1 (`SDKWebViewActivity`) |
| FileProvider | App must supply authority via `freshchat_file_provider_authority` string | SDK declares `${applicationId}.fileProvider` itself |
| App-side manifest work | FileProvider + `FirebaseMessagingService` | `POST_NOTIFICATIONS` (API 33+) + `FirebaseMessagingService` |

### 4.4 Manifest merge conflicts to watch for

The Freshdesk SDK manifest sets attributes on `<application>` that your app may already define:

- `android:dataExtractionRules="@xml/data_extraction_rules"`
- `android:fullBackupContent="@xml/backup_rules"`
- `android:enableOnBackInvokedCallback="true"`

It also declares a `FileProvider` with authority `${applicationId}.fileProvider`.

If your app declares any of these, resolve with `tools:replace` on `<application>`, and rename your own FileProvider authority if it collides.

### 4.5 Import changes

```kotlin
// Remove
import com.freshchat.consumer.sdk.Freshchat
import com.freshchat.consumer.sdk.FreshchatConfig
import com.freshchat.consumer.sdk.FreshchatUser
import com.freshchat.consumer.sdk.FreshchatMessage
import com.freshchat.consumer.sdk.FreshchatNotificationConfig
import com.freshchat.consumer.sdk.ConversationOptions
import com.freshchat.consumer.sdk.FaqOptions

// Add
import com.freshworks.sdk.freshdesk.FreshdeskSDK
import com.freshworks.sdk.freshdesk.data.SDKConfig
import com.freshworks.sdk.freshdesk.notification.NotificationConfig
import com.freshworks.sdk.freshdesk.backend.model.User
import com.freshworks.sdk.freshdesk.events.SDKEventID
import com.freshworks.sdk.freshdesk.events.UserState
import com.freshworks.sdk.freshdesk.handlers.FreshDeskSDKLinkHandler
import com.freshworks.sdk.freshdesk.handlers.FreshdeskUserInteractionListener
import com.freshworks.sdk.freshdesk.core.FreshdeskWebviewListener
// Optional — only if you use ContentConfiguration (§16.2)
import com.freshworks.sdk.freshdesk.data.content.ContentConfiguration
import com.freshworks.sdk.freshdesk.data.content.HeaderContent
import com.freshworks.sdk.freshdesk.data.content.PlaceholderContent
import com.freshworks.sdk.freshdesk.data.content.PrivacyPolicyContent
import com.freshworks.sdk.freshdesk.data.content.TicketFormContent
import com.freshworks.sdk.freshdesk.data.content.ChannelResponseContent
import com.freshworks.sdk.freshdesk.data.content.ChannelResponseOnlineContent
import com.freshworks.sdk.freshdesk.data.content.TimeUnitTemplates
```

---

## 5. Initialization

### 5.1 Before — Freshchat

```kotlin
// Called from the launcher Activity's onCreate()
val config = FreshchatConfig("<APP_ID>", "<APP_KEY>").apply {
    setDomain("msdk.freshchat.com")
    setCameraCaptureEnabled(true)
    setGallerySelectionEnabled(true)
    setFileSelectionEnabled(true)
    setTeamMemberInfoVisible(true)
    setResponseExpectationEnabled(true)
    setUserEventsTrackingEnabled(true)
}

val didInit: Boolean = Freshchat.getInstance(this).init(config)
if (!didInit) {
    // Unsupported OS, inactive account, invalid domain, or missing permission
}
```

### 5.2 After — Freshdesk

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        FreshdeskSDK.initialize(
            context = applicationContext,
            sdkConfig = SDKConfig(
                token = "<ACCOUNT_TOKEN>",
                host = "https://yourcompany.freshdesk.com",
                sdkID = "<SDK_ID>",
                locale = "en",                       // "" = device locale
                jwt = null,                          // required if JWT enforcement is on
                headerProps = emptyMap(),
                contentConfiguration = null,
                debugMode = BuildConfig.DEBUG
            ),
            initializedCallback = {
                // The SDK is ready. Safe to open support screens from here on.
                sdkReady = true
                FreshdeskSDK.setNotificationConfig(
                    NotificationConfig(
                        soundEnabled = true,
                        smallIconResId = R.drawable.ic_notification,
                        largeIconResId = R.drawable.ic_notification
                    )
                )
            }
        )
    }
}
```

**Behavioural differences you must design around:**

| | Freshchat | Freshdesk |
|---|---|---|
| Recommended call site | Launcher Activity `onCreate()` | `Application.onCreate()` with **application context** |
| Return value | `Boolean` (synchronous success/failure) | `Unit` — readiness arrives via `initializedCallback` |
| Readiness signal | None needed; `showConversations` worked right after `init` | **Must** wait for the callback before `openSupport` / `openTopic` / `openKnowledgeBase` |
| Re-initialization | Effectively once per process | Supported — reloads the SDK with the new config (for example, a new locale or user) |
| Offline behaviour | Queued and retried by the SDK | Calls are **ignored** when offline; methods that open a screen also show a toast |
| `initializedCallback` | N/A | **No default value** — you must pass a lambda or explicit `null` |

> **Call order matters.** Most SDK methods only take effect if `initialize` has already been called **and** the device is online. Calls made before `initialize`, or while offline, are **ignored and not retried** — the only sign is a log message (or `onFailure`, for methods that accept one). Calls made after `initialize` but before the support screen has finished loading are fine: the SDK holds on to them and applies them once it is ready.
>
> **Safe to call at any time**, including before `initialize`: `getSDKVersionName` / `getSDKVersionCode` / `getSDKBuildID`, `setWebViewListener` / `getWebViewListener`, and `setFreshdeskUserInteractionListener` / `getFreshdeskUserInteractionListener`. `isFreshdeskSDKNotification` needs `initialize` to have been called (it returns `false` otherwise) but works offline.
>
> **Call only after `initialize`** — everything else: `openSupport`, `openTopic`, `openKnowledgeBase`, `getUser`, `resetUser`, `setUserProperties`, `authenticateAndUpdate`, `setTicketProperties`, `setContentConfiguration`, `setPushRegistrationToken`, `handleFCMNotification`, `setNotificationConfig`, `setLinkHandler`, `trackEvent`, `dismissFreshdeskViews`.
>
> Practical rule: call `initialize` first, and either make the calls in the second list from within `initializedCallback`, or only make them once a flag set inside that callback is true. Persist anything that can arrive out of order (notably the FCM token, see [§12.1](#121-token-registration)) so you can re-submit it once the SDK is ready.

### 5.3 Config flags with no code equivalent

These `FreshchatConfig` toggles have **no** `SDKConfig` counterpart. Their behaviour is now controlled by the widget configuration in the Freshdesk portal:

| Freshchat setter | Default | Freshdesk equivalent |
|------------------|---------|----------------------|
| `setCameraCaptureEnabled(Boolean)` | `true` | Portal widget config |
| `setGallerySelectionEnabled(Boolean)` | `true` | Portal widget config |
| `setFileSelectionEnabled(Boolean)` | `true` | Portal widget config |
| `setTeamMemberInfoVisible(Boolean)` | `true` | Portal widget config |
| `setResponseExpectationEnabled(Boolean)` | `true` | Portal widget config; labels via `ContentConfiguration` |
| `setUserEventsTrackingEnabled(Boolean)` | `true` | No toggle — omit `trackEvent` calls instead |
| `setDomain(String)` | — | Replaced by `SDKConfig.host` (full portal URL) |

**Action:** audit which of these you set to a non-default value and raise the equivalent portal configuration with your Freshdesk admin before cutover.

---

## 6. Opening the SDK UI

Use an **Activity** context for all Freshdesk UI entry points so back navigation behaves correctly.

### 6.1 Conversations → support home

```kotlin
// Before
Freshchat.showConversations(this)

// After
FreshdeskSDK.openSupport(this)
```

> **Not a like-for-like screen.** `showConversations` opened the channel list (and skipped straight into the channel if only one was active). `openSupport` opens Freshdesk's **Support Home** — chat, topics and FAQs together. Review your in-app entry points, button labels and analytics screen names, because the first screen the user sees changes.

### 6.2 FAQs → knowledge base

```kotlin
// Before
Freshchat.showFAQs(this)

// After
FreshdeskSDK.openKnowledgeBase(this)
```

Terminology changes from "FAQ" to "Knowledge Base". Update in-app copy if you want to match Freshdesk product language.

### 6.3 `FaqOptions` has no equivalent

```kotlin
// Before — none of this survives migration
val faqOptions = FaqOptions()
    .showFaqCategoriesAsGrid(true)
    .showContactUsOnFaqScreens(true)
    .showContactUsOnAppBar(false)
    .showContactUsOnFaqNotHelpful(true)
    .filterByTags(listOf("android"), "Android Help", FaqOptions.FilterType.CATEGORY)
    .filterContactUsByTags(listOf("billing"), "Billing")

Freshchat.showFAQs(this, faqOptions)
```

`openKnowledgeBase(context)` takes **no options parameter**. Lost capabilities:

| Lost | Workaround |
|------|-----------|
| Grid vs list category layout | None in code. Raise with the Freshworks product team if this is a blocker. |
| Contact-us button visibility and placement | None in code — portal widget config only. |
| Tag-based article filtering | None. Restructure content in the portal (separate solution categories / widgets) so the default view is already scoped. |
| Tag-based category filtering | Same as above. |
| Tag-scoped contact-us | None. |

### 6.4 Tag filtering and parallel conversations have no equivalent

```kotlin
// Before — tag-filtered conversation list
val options = ConversationOptions().filterByTags(listOf("premium"), "Premium Support")
Freshchat.showConversations(this, options)

// Before — parallel conversation by reference ID
Freshchat.showConversations(this, "ORDER-123", "orders")
```

Freshdesk's closest concept is `openTopic`, which **pre-selects the topic a new ticket is created under**. It does not filter an existing list, and there is no per-reference thread or per-reference unread count.

```kotlin
// After — closest available behaviour
FreshdeskSDK.openTopic(
    context = this,
    topicName = "Order Support",
    topicId = "12345"
)
```

Notes:

- Both `topicName` and `topicId` are **nullable but have no default values** — pass both explicitly (one may be `null`).
- To carry the order/reference context an agent needs, use `setTicketProperties` before `openTopic`:

```kotlin
FreshdeskSDK.setTicketProperties(mapOf("order_id" to "ORD-12345"))
FreshdeskSDK.openTopic(this, topicName = "Order Support", topicId = null)
```

This is the recommended migration path for reference-ID-based parallel conversations: the context becomes ticket metadata rather than a separate persisted thread. **You lose** independent threads and per-reference unread counts.

### 6.5 Deeplinks have no equivalent

```kotlin
// Before
Freshchat.openFreshchatDeeplink(this, "freshchat://channels?id=192")
```

`NA`. There is no Freshdesk API to deep-link to a specific channel, conversation or article.

**Workaround:** route your own deeplinks to `openSupport(context)` or `openTopic(context, topicName, topicId)` and accept the coarser landing point. If you were deriving the deeplink from an intercepted notification (see [§12.4](#124-notification-interception-has-no-equivalent)), that entire flow needs redesigning.

### 6.6 Dismissing SDK screens — new

```kotlin
FreshdeskSDK.dismissFreshdeskViews()
```

No Freshchat equivalent. Useful before logout. It only works once the SDK is ready (after `initializedCallback` has run); before that, it does nothing.

---

## 7. User identity and properties

### 7.1 Setting standard user fields

```kotlin
// Before
// Note: FreshchatUser setters are builder-style (they return FreshchatUser),
// so Kotlin exposes them as read-only synthetic properties — call the setters.
val user = Freshchat.getInstance(this).user
    .setFirstName("Jane")
    .setLastName("Doe")
    .setEmail("jane@example.com")
    .setPhone("+1", "9876543210")

try {
    Freshchat.getInstance(this).setUser(user)
} catch (e: MethodNotAllowedException) {
    // Thrown when the account is in JWT mode
}
```

```kotlin
// After
FreshdeskSDK.setUserProperties(
    mapOf(
        "firstName" to "Jane",
        "lastName" to "Doe",
        "email" to "jane@example.com",
        "phone" to "9876543210",
        "phoneCountry" to "+1",
        "plan" to "premium"        // custom property, same call
    )
)
```

| | Freshchat | Freshdesk |
|---|---|---|
| Shape | Fixed schema on `FreshchatUser` | Free-form `Map<String, Any>` |
| Standard keys | `firstName`, `lastName`, `email`, `phone`, `phoneCountryCode` | `firstName`, `lastName`, `email`, `phone`, `phoneCountry` |
| Standard vs custom | Two different calls (`setUser` vs `setUserProperties`) | **One** call — merged into a single map |
| Value type | `String` only | `Any` (primitives) |
| Custom key registration | Implicit | **Must be pre-whitelisted as Contact Fields in the portal, or silently dropped** |
| Under JWT enforcement | Throws `MethodNotAllowedException` | **Silently ignored** (error logged only) |

> **Watch out:** Freshchat threw a checked `MethodNotAllowedException` you could catch. Freshdesk fails silently under JWT enforcement. If you rely on properties reaching the backend, verify via `getUser` rather than assuming the call succeeded.

Freshchat's documented validation limits (key ≤ 32 chars, value ≤ 256, email ≤ 384, phone ≤ 16, key pattern `[\w -]+`) are enforced client-side. Freshdesk performs **no client-side validation** — limits are server-side.

### 7.2 Reading the current user

```kotlin
// Before — synchronous, cached, standard fields only
val user: FreshchatUser = Freshchat.getInstance(this).user
val alias: String = Freshchat.getInstance(this).freshchatUserId
val restoreId: String? = user.restoreId
// No getter existed for custom properties
```

```kotlin
// After — asynchronous, full profile including custom properties
FreshdeskSDK.getUser(
    onFailure = { error -> Log.e("Freshdesk", "getUser failed", error) },
    userCallback = { user: User ->
        val alias = user.alias
        val restoreId = user.restoreId
        // user.customProps is Map<String, JsonElement> — read the keys you need;
        // avoid writing user data or custom property values to Logcat
        val plan = user.customProps["plan"]?.toDisplayString()
    }
)
```

`getUser` is strictly more capable — it returns custom properties, which Freshchat never exposed. Changes to note:

- **Asynchronous.** Any synchronous code path that read `Freshchat.getInstance(ctx).user` must be restructured around a callback.
- `getFreshchatUserId()` → `user.alias`.
- `user.customProps` values are `JsonElement`, not `String`. Use the SDK's `JsonElement.toDisplayString()` extension (in `com.freshworks.sdk.freshdesk.backend.model`) to render a value without JSON quoting.
- `onFailure` is optional (defaults to `null`); `userCallback` is required.

### 7.3 `identifyUser` has no equivalent

```kotlin
// Before
Freshchat.getInstance(this).identifyUser("user-123", restoreId)
```

`NA`. There is no `externalId` / `restoreId` API in the Freshdesk SDK. Identity is established through a JWT — see [§9](#9-jwt-authentication-and-user-state).

`User.restoreId` is still *readable* (via `getUser` or the `USER_CREATED` event), but you cannot pass a restore ID back in to restore a session. Cross-device restore is a JWT-payload concern handled by your backend.

### 7.4 Ticket properties — new capability

Freshchat had no ticketing concept.

```kotlin
FreshdeskSDK.setTicketProperties(
    mapOf(
        "order_id" to "ORD-12345",
        "product_name" to "Premium Widget"
    )
)
FreshdeskSDK.openSupport(this)
```

- Attaches metadata to the **next** conversation, and is only transmitted once the user **sends a message**.
- Call it again before each new support flow — it is not sticky across sessions.
- Every custom key must be pre-whitelisted as a **Ticket Field** on the linked widget.
- Silently ignored under JWT enforcement — put ticket fields in the JWT payload instead.

---

## 8. Session management and logout

```kotlin
// Before — static, fire-and-forget
Freshchat.resetUser(this)
```

**Freshdesk logout can fail, and your app logout must not hide that.** If `resetUser` fails (for example, the device is offline or the SDK isn't ready yet), the previous user stays signed in to Freshdesk. If your app then goes straight to the login screen, the **next person to open support can see the previous user's conversations**. Clearing your own app session is not enough.

Treat the Freshdesk reset as pending until `onSuccess` fires. Save that pending state so it survives an app restart, retry until the reset succeeds, and keep support closed in the meantime:

```kotlin
// After — keep support blocked until the Freshdesk reset succeeds
object FreshdeskLogout {
    private const val KEY_RESET_PENDING = "freshdesk_reset_pending"
    private lateinit var prefs: SharedPreferences

    // Call once from Application.onCreate(), before FreshdeskSDK.initialize
    fun init(context: Context) {
        prefs = context.getSharedPreferences("freshdesk_logout", Context.MODE_PRIVATE)
    }

    val isResetPending: Boolean
        get() = prefs.getBoolean(KEY_RESET_PENDING, false)

    // Call when the user logs out of your app
    fun start() {
        prefs.edit().putBoolean(KEY_RESET_PENDING, true).apply()
        FreshdeskSDK.dismissFreshdeskViews()
        retry()
    }

    // Call from initializedCallback, when the device comes back online,
    // and whenever someone tries to open support
    fun retry() {
        if (!isResetPending) return
        FreshdeskSDK.resetUser(
            onSuccess = { prefs.edit().putBoolean(KEY_RESET_PENDING, false).apply() },
            onFailure = { error -> Log.w("Freshdesk", "Reset failed, will retry: $error") }
        )
    }
}

fun logout() {
    FreshdeskLogout.start()
    clearAppSession()
    navigateToLogin()
}

// Route every support entry point through a check like this
fun openSupportIfAllowed(activity: Activity) {
    if (FreshdeskLogout.isResetPending) {
        FreshdeskLogout.retry()
        showSupportUnavailable()   // e.g. "Support is unavailable right now. Please try again."
        return
    }
    FreshdeskSDK.openSupport(activity)
}
```

While a reset is pending, also hold back any calls that set up the **next** user (`authenticateAndUpdate`, `setUserProperties`, `setTicketProperties`, or re-`initialize` with a new JWT). Make them only after `onSuccess`.

| | Freshchat | Freshdesk |
|---|---|---|
| Signature | `static void resetUser(Context)` | `resetUser(onSuccess: (String) -> Unit, onFailure: (String) -> Unit)` |
| Callbacks | None | **Both required** — no defaults |
| Preconditions | None | The SDK must be ready (after `initializedCallback` has run) **and** the device must be online, else `onFailure` fires |
| Side effects | Clears local user data | Unregisters push, clears the stored JWT, signs the user out of the support widget, clears all SDK notifications, emits `USER_CLEARED` |
| Thread | Caller's | Callbacks dispatched to the **main thread** |

**Switching users:** call `resetUser()` first and **wait for `onSuccess`**, then either `authenticateAndUpdate(newJwt)` or re-`initialize()` with the new user's config. Do **not** call `authenticateAndUpdate` alone to switch users.

---

## 9. JWT authentication and user state

### 9.1 Setting and refreshing the token

```kotlin
// Before
try {
    Freshchat.getInstance(this).setUser(jwt)        // authenticate / update
    Freshchat.getInstance(this).restoreUser(jwt)    // restore by reference_id
} catch (e: MethodNotAllowedException) {
    // JWT not enabled for this account
} catch (e: JwtException) {
    // Null/empty token, or reference_id missing
}
```

```kotlin
// After — token at init
FreshdeskSDK.initialize(
    context = applicationContext,
    sdkConfig = SDKConfig(
        token = "<ACCOUNT_TOKEN>",
        host = "https://yourcompany.freshdesk.com",
        sdkID = "<SDK_ID>",
        jwt = "<JWT from your backend>"
    )
) { /* SDK ready */ }

// After — token refresh / post-login authentication
FreshdeskSDK.authenticateAndUpdate(jwt)
```

| Freshchat | Freshdesk |
|-----------|-----------|
| `setUser(jwt)` — authenticate + update properties | `authenticateAndUpdate(jwt)` — does both |
| `restoreUser(jwt)` — restore via `reference_id` | Not required; `initialize` / `authenticateAndUpdate` handle restore |
| Two distinct calls for the two cases | **One** call for both cases |
| Throws checked `MethodNotAllowedException` / `JwtException` | Never throws; empty token is logged and ignored |

If JWT enforcement is **on** in the portal, pass a valid `SDKConfig.jwt` at init. Omitting it does not throw — the widget reports `UserState.JWT_ABSENT` and fails to load its content, so the user sees an unusable support screen. `authenticateAndUpdate(jwt)` is a valid recovery/upgrade path (including visitor → identified user) and keeps working when JWT enforcement is on, unlike `setUserProperties` / `setTicketProperties`. Supplying the token at init is still the recommended flow for enforced widgets.

### 9.2 Status values mapping

```kotlin
// Before
val status: JwtTokenStatus = Freshchat.getInstance(this).userIdTokenStatus
```

| Freshchat `JwtTokenStatus` | Freshdesk `UserState` constant | Value |
|---------------------------|-------------------------------|-------|
| `TOKEN_NOT_SET` | `UserState.JWT_ABSENT` | `jwt_not_present` |
| `TOKEN_NOT_PROCESSED` | — (no equivalent) | — |
| `TOKEN_VALID` | `UserState.AUTHENTICATED` | `authenticated` |
| `TOKEN_INVALID` | `UserState.NOT_AUTHENTICATED` | `not_authenticated` |
| `TOKEN_EXPIRED` | `UserState.AUTH_EXPIRED` | `auth_expired` |
| — | `UserState.IDENTIFIER_UPDATED` | `identifier_updated` |
| — | `UserState.UNDEFINED` | `undefined` |

### 9.3 There is no pull-based JWT status getter

Freshchat gave you a synchronous getter you could call at any time. **Freshdesk has no equivalent.** State changes are pushed only, via the `SDKEventID.USER_STATE_CHANGE` local broadcast.

**Workaround: cache the last reported state yourself.**

```kotlin
object FreshdeskAuthState {
    @Volatile var current: String = UserState.UNDEFINED
}

private val userStateReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != SDKEventID.USER_STATE_CHANGE) return
        val state = intent.getStringExtra(SDKEventID.USER_STATE_CHANGE) ?: return
        FreshdeskAuthState.current = state

        when (state) {
            UserState.AUTH_EXPIRED -> FreshdeskSDK.authenticateAndUpdate(fetchNewJwt())
            UserState.AUTHENTICATED -> { /* safe to open support */ }
            UserState.NOT_AUTHENTICATED -> { /* surface a retry / re-login */ }
            UserState.JWT_ABSENT -> { /* enforced widget, no token supplied */ }
        }
    }
}

// Register app-wide so no state change is missed
LocalBroadcastManager.getInstance(context).registerReceiver(
    userStateReceiver,
    IntentFilter(SDKEventID.USER_STATE_CHANGE)
)
```

Register this in `Application.onCreate()` rather than an Activity, since auth state changes can occur while no Activity is foregrounded.

### 9.4 Token refresh trigger

Freshchat broadcast `FRESHCHAT_SET_TOKEN_TO_REFRESH_DEVICE_PROPERTIES` when it needed a fresh token. Freshdesk's equivalent trigger is `UserState.AUTH_EXPIRED` on `USER_STATE_CHANGE`, handled in the receiver above.

Add an `exp` claim (Unix seconds) to your JWT if you want a bounded lifetime.

---

## 10. Unread count

```kotlin
// Before — pull, with three variants
Freshchat.getInstance(this).getUnreadCountAsync { status, count ->
    if (status == FreshchatCallbackStatus.STATUS_SUCCESS) updateBadge(count)
}
Freshchat.getInstance(this).getUnreadCountAsync(callback, listOf("vip"))
Freshchat.getInstance(this).getUnreadCountAsyncWithReferenceId(callback, "orders", "ORDER-123")

// Before — push
// FRESHCHAT_UNREAD_MESSAGE_COUNT_CHANGED local broadcast (no extras; then call the getter)
```

```kotlin
// After — push only
private val unreadReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == SDKEventID.UNREAD_COUNT) {
            val count = intent.getIntExtra(SDKEventID.UNREAD_COUNT, 0)
            updateBadge(count)
        }
    }
}

LocalBroadcastManager.getInstance(this).registerReceiver(
    unreadReceiver,
    IntentFilter(SDKEventID.UNREAD_COUNT)
)
```

| | Freshchat | Freshdesk |
|---|---|---|
| Pull API | `getUnreadCountAsync(callback)` | **NA** |
| Push event | `FRESHCHAT_UNREAD_MESSAGE_COUNT_CHANGED` (no extras — you then called the getter) | `SDKEventID.UNREAD_COUNT`, `Int` extra under the same key |
| Filter by tag | `getUnreadCountAsync(cb, tagsList)` | **NA** |
| Filter by reference ID | `getUnreadCountAsyncWithReferenceId(cb, topic, refId)` | **NA** |

**Workaround for the missing pull API:** register an app-wide receiver in `Application.onCreate()`, persist the latest count (e.g. `SharedPreferences` or a `StateFlow` in a singleton), and read your own cached value wherever you previously called `getUnreadCountAsync`. This is a required change if any screen reads the badge count on demand.

Tag-scoped and reference-scoped counts have **no workaround** — they depend on Freshchat's channel/tag model.

---

## 11. Messaging and bot APIs with no equivalent

### 11.1 `sendMessage` has no equivalent

```kotlin
// Before
val message = FreshchatMessage()
    .setMessage("Where is my package?")
    .setTag("orders")
Freshchat.sendMessage(this, message)
```

`NA`. There is no Freshdesk API to programmatically pre-fill or send a message. The end user must type after the UI opens.

**Workaround:** move the information you were injecting into a message into `setTicketProperties(...)` so the agent still receives the context, then open the relevant topic:

```kotlin
FreshdeskSDK.setTicketProperties(
    mapOf("order_id" to "ORD-123", "issue" to "package_not_delivered")
)
FreshdeskSDK.openTopic(this, topicName = "Order Support", topicId = null)
```

Note the behavioural difference: ticket properties reach Freshdesk only after the user sends their first message, whereas `sendMessage` created the conversation immediately with no user action.

### 11.2 Bot variables have no equivalent

```kotlin
// Before
Freshchat.getInstance(this).setBotVariables(
    mapOf("userTier" to "gold"),
    mapOf("bot-alias" to mapOf("sku" to "ABC"))
)
```

`NA`. There is no API to pass common or bot-specific variables.

**Workaround:** pass the same values as user properties (`setUserProperties`) or ticket properties (`setTicketProperties`), whitelisted in the portal, and reference them from bot/automation rules server-side. Confirm with your Freshdesk admin that the bot flows you depend on can read those fields.

### 11.3 Other removed customization APIs

| Freshchat API | Status | Workaround |
|---------------|--------|-----------|
| `setConversationBannerMessage(context, msg)` (already deprecated) | NA | Use `ContentConfiguration` header/content strings for static copy; no dynamic per-session banner. |
| `linkifyWithPattern(regex, defaultScheme)` | NA | None. Only whole-link handling via `setLinkHandler`. |
| `setImageLoader(FreshchatImageLoader)` | NA | None — the web widget loads its own images. Remove your `FreshchatImageLoader`, `FreshchatImageLoaderRequest` and `FreshchatCallback` implementations. |
| `setAgentProfileClickRedirectionUri(uri)` | NA | None. Agent-profile taps are not interceptable. |
| `notifyAppLocaleChange(context)` | NA | Re-`initialize()` with a new `SDKConfig.locale`. See [§16](#16-localization). |
| `setEdgeDebugApiLoggingEnabled(context, enabled)` | NA | Use `SDKConfig.debugMode = true` for verbose SDK logging. |

---

## 12. Push notifications

### 12.1 Token registration

```kotlin
// Before
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Freshchat.getInstance(this).setPushRegistrationToken(token)
    }
}
```

```kotlin
// After
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FreshdeskSDK.setPushRegistrationToken(token)
    }
}
```

Direct equivalent, but note two differences:

- Freshdesk's `setPushRegistrationToken` only **stores** the token locally; upload happens later once the SDK has an active user session.
- Like most SDK methods, it is ignored if called **before `initialize`** or while the device is offline, and it is not retried. Persist the latest token yourself and re-submit it from the `initializedCallback` if you cannot guarantee ordering:

```kotlin
initializedCallback = {
    prefs.getString("fcm_token", null)?.let { FreshdeskSDK.setPushRegistrationToken(it) }
}
```

### 12.2 Receiving and handling a push

```kotlin
// Before
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    if (Freshchat.isFreshchatNotification(remoteMessage)) {
        Freshchat.handleFcmMessage(this, remoteMessage)
    }
}
```

```kotlin
// After — note: pass remoteMessage.data (a Map), not the RemoteMessage
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    if (FreshdeskSDK.isFreshdeskSDKNotification(remoteMessage.data)) {
        FreshdeskSDK.handleFCMNotification(remoteMessage.data)
    } else {
        // your app's own notifications
    }
}
```

| | Freshchat | Freshdesk |
|---|---|---|
| Detection param | `RemoteMessage` / `Intent` / `Bundle` overloads | `Map<String, String>` only |
| Handle param | `(Context, Object /* RemoteMessage */)` | `(Map<String, String>)` — no context needed |
| Payload marker | `source == "freshchat_user"` | `source == "freshdesk_user"` |
| Preconditions | SDK initialized | `isFreshdeskSDKNotification` requires init but **not** network |
| Suppression | — | `handleFCMNotification` shows nothing while the Freshdesk support screen is already open |

Also add, for API 33+:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

and request it at runtime. (Freshchat did not require this because its minSdk predated the permission, but if you already handle it, nothing changes.)

### 12.3 Notification appearance

```kotlin
// Before
Freshchat.getInstance(this).setNotificationConfig(
    FreshchatNotificationConfig()
        .setNotificationSoundEnabled(true)
        .setSmallIcon(R.drawable.ic_notification)
        .setLargeIcon(R.drawable.ic_notification_large)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setImportance(NotificationManagerCompat.IMPORTANCE_HIGH)
        .setNotificationSound(customSoundUri)
        .launchActivityOnFinish(MainActivity::class.java.name)
        .setNotificationInterceptionEnabled(false)
)
```

```kotlin
// After — call from the initializedCallback
FreshdeskSDK.setNotificationConfig(
    NotificationConfig(
        soundEnabled = true,
        smallIconResId = R.drawable.ic_notification,
        largeIconResId = R.drawable.ic_notification_large,
        priority = Notification.PRIORITY_HIGH,
        importance = NotificationManager.IMPORTANCE_HIGH
    )
)
```

Field-level mapping:

| Freshchat | Freshdesk | Notes |
|-----------|-----------|-------|
| `setNotificationSoundEnabled(Boolean)` | `soundEnabled: Boolean = true` | Direct |
| `setSmallIcon(@DrawableRes Int)` | `smallIconResId: Int` | Direct. Freshdesk defaults to its own `ic_launcher_foreground` — **always set your own** |
| `setLargeIcon(@DrawableRes Int)` | `largeIconResId: Int` | Same as above |
| `setPriority(Int)` | `priority: Int` | Direct (pre-Oreo) |
| `setImportance(Int)` | `importance: Int` | Direct (Oreo+ channel) |
| `setNotificationSound(Uri)` | **NA** | No custom sound URI. Only on/off. |
| `launchActivityOnFinish(String)` | **NA** | No control over back/up navigation target from a notification-launched screen. |
| `setNotificationInterceptionEnabled(Boolean)` | **NA** | See [§12.4](#124-notification-interception-has-no-equivalent) |

Freshdesk creates its own notification channel: id `fd_conv_notif_ch`, name `Conversations`. `importance` only applies at channel creation (fresh install), matching Freshchat's behaviour.

### 12.4 Notification interception has no equivalent

```kotlin
// Before
Freshchat.getInstance(this).setNotificationConfig(
    FreshchatNotificationConfig().setNotificationInterceptionEnabled(true)
)

// then, on FRESHCHAT_ACTION_NOTIFICATION_INTERCEPTED:
val isFreshchat = intent.getBooleanExtra("FRESHCHAT", false)
val deeplink = intent.getStringExtra("FRESHCHAT_DEEPLINK")
// decide whether to navigate, and where
```

`NA`. Freshdesk always handles the notification tap itself and opens the relevant screen. There is no callback to intercept, block, or redirect that navigation, and no deeplink is surfaced.

**Impact:** if you used interception to enforce your own navigation stack, gate on login state, or route through an in-app router, that flow has no replacement. Raise it with the Freshworks product team if it is a blocker for your app.

---

## 13. Analytics and event tracking

```kotlin
// Before
Freshchat.trackEvent(
    this,
    "checkout_completed",
    mapOf("order_id" to "123", "amount" to 99)
)
```

```kotlin
// After
FreshdeskSDK.trackEvent(
    eventName = "checkout_completed",
    eventData = mapOf("order_id" to "123", "amount" to 99.99)
)

FreshdeskSDK.trackEvent("profile_updated")   // eventData defaults to empty
```

Effectively a direct equivalent. Differences:

| | Freshchat | Freshdesk |
|---|---|---|
| Signature | `static trackEvent(Context, String, Map<String, Object>?)` | `trackEvent(eventName: String, eventData: Map<String, Any> = mutableMapOf())` |
| Context | Required | Not required |
| Properties | Nullable | Defaults to empty map |
| Global disable | `FreshchatConfig.setUserEventsTrackingEnabled(false)` | No toggle — remove the calls instead |
| Client-side validation | Throws `InvalidEventException` on bad names | None — limits are enforced server-side |

Use concise `snake_case` names. Keep values primitive (`String`, `Int`, `Long`, `Double`, `Float`, `Boolean`) — nested objects are not supported.

Server-side limits to design within: 121 unique event names per account, 32-char event name, 32-char property key, 256-char property value, 20 properties per event. Confirm current values with your Freshdesk admin if events are being rejected.

---

## 14. SDK events

Both SDKs use `LocalBroadcastManager`. The event **model** is what changes.

### 14.1 Before — one broadcast, rich typed payload

```kotlin
LocalBroadcastManager.getInstance(this).registerReceiver(
    object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val event = Freshchat.getEventFromBundle(intent?.extras ?: return) ?: return
            when (event.eventName) {
                Event.EventName.FCEventMessageSent -> {
                    val channel = event.properties[Event.Property.FCPropertyChannelName]
                    val convId = event.properties[Event.Property.FCPropertyConversationID]
                }
                Event.EventName.FCEventIdTokenStatusChange -> { /* … */ }
                Event.EventName.FCEventCsatSubmit -> { /* … */ }
                else -> {}
            }
        }
    },
    IntentFilter(Freshchat.FRESHCHAT_EVENTS)
)
```

Freshchat exposed **44 event names** (`Event.EventName`) and **29 event properties** (`Event.Property`) through a single `FRESHCHAT_EVENTS` broadcast.

### 14.2 After — several purpose-built broadcasts

```kotlin
private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            SDKEventID.UNREAD_COUNT ->
                updateBadge(intent.getIntExtra(SDKEventID.UNREAD_COUNT, 0))

            SDKEventID.USER_STATE_CHANGE ->
                handleAuthState(intent.getStringExtra(SDKEventID.USER_STATE_CHANGE))

            SDKEventID.USER_CREATED -> {
                val user: User? = intent.getParcelableExtra(SDKEventID.USER_CREATED)
                // Log the event name only; do not log user fields such as alias.
                Log.d("Freshdesk", "Event received: ${SDKEventID.USER_CREATED}")
            }

            SDKEventID.USER_CLEARED -> onSessionCleared()

            SDKEventID.MESSAGE_SENT ->
                // Log the event name only. The extra is raw JSON that may contain message content.
                Log.d("Freshdesk", "Event received: ${SDKEventID.MESSAGE_SENT}")

            SDKEventID.MESSAGE_RECEIVED ->
                // Log the event name only. The extra is raw JSON that may contain message content.
                Log.d("Freshdesk", "Event received: ${SDKEventID.MESSAGE_RECEIVED}")

            SDKEventID.DOWNLOAD_FILE ->
                handleDownload(intent.getStringExtra(SDKEventID.DOWNLOAD_FILE))
        }
    }
}

LocalBroadcastManager.getInstance(this).registerReceiver(
    receiver,
    IntentFilter().apply {
        addAction(SDKEventID.UNREAD_COUNT)
        addAction(SDKEventID.USER_STATE_CHANGE)
        addAction(SDKEventID.USER_CREATED)
        addAction(SDKEventID.USER_CLEARED)
        addAction(SDKEventID.MESSAGE_SENT)
        addAction(SDKEventID.MESSAGE_RECEIVED)
        addAction(SDKEventID.DOWNLOAD_FILE)
    }
)
```

### 14.3 Event reference

| Freshdesk action | Constant value | Extra key | Payload type |
|------------------|----------------|-----------|--------------|
| `SDKEventID.UNREAD_COUNT` | `com.freshdesk.sdk.FDUnreadCount` | same as action | `Int` |
| `SDKEventID.USER_STATE_CHANGE` | `com.freshdesk.sdk.FDUserStateChange` | same as action | `String` (`UserState` value) |
| `SDKEventID.USER_CREATED` | `com.freshdesk.sdk.FDUserCreated` | same as action | `User` (Parcelable) |
| `SDKEventID.USER_CLEARED` | `com.freshdesk.sdk.FDUserCleared` | — | none |
| `SDKEventID.MESSAGE_SENT` | `com.freshdesk.sdk.FDMessageSent` | same as action | `String` (JSON) |
| `SDKEventID.MESSAGE_RECEIVED` | `com.freshdesk.sdk.FDMessageReceived` | same as action | `String` (JSON) |
| `SDKEventID.DOWNLOAD_FILE` | `com.freshdesk.sdk.FDDownloadFile` | same as action | `String` (URL) |

### 14.4 Freshchat events with no Freshdesk equivalent

Only **3** of Freshchat's 44 event names have a Freshdesk analogue — `FCEventMessageSent` → `MESSAGE_SENT`, `FCEventMessageReceive` → `MESSAGE_RECEIVED`, and `FCEventIdTokenStatusChange` → `USER_STATE_CHANGE`. The remaining **41** are listed below. (Conversely, Freshdesk's `UNREAD_COUNT`, `USER_CREATED`, `USER_CLEARED` and `DOWNLOAD_FILE` have no `Event.EventName` predecessor.)

If your analytics or product logic depends on any of these, there is **no migration path** today. Plan to drop or re-source them.

| Category | Freshchat events lost |
|----------|----------------------|
| FAQ / Knowledge base | `FCEventFAQCategoryListOpen`, `FCEventFAQListOpen`, `FCEventFAQOpen`, `FCEventFAQSearch`, `FCEventFAQVote`, `FCEventBotFAQOpen`, `FCEventBotFAQVote` |
| Navigation | `FCEventChannelListOpen`, `FCEventConversationOpen`, `FCEventScreenView` |
| CSAT | `FCEventCsatOpen`, `FCEventCsatSubmit`, `FCEventCsatExpiry` |
| Rich messaging | `FCEventCarouselShow`, `FCEventCarouselOptionSelect`, `FCEventCarouselOptionView`, `FCEventDropDownShow`, `FCEventDropDownSent`, `FCEventButtonReceive`, `FCEventButtonSent`, `FCEventDatePickerShow/Select`, `FCEventTimePickerShow/Select`, `FCEventQuickActionSelect` |
| Calendar / booking | `FCEventCalendarFindTimeSlotClick`, `FCEventCalendarInviteCancel`, `FCEventCalendarNoTimeSlotFound`, `FCEventCalendarBookingSuccess/Retry/Failure` |
| Attachments | `FCEventFileAttachmentUploadSuccess/Error`, `FCEventFileAttachmentOpen/OpenError`, `FCEventBotFileAttachmentUpload` |
| Other | `FCEventLinkTap`, `FCEventNotificationReceive`, `FCEventShowOriginalClick`, `FCEventHideOriginalClick`, `FCEventAgentProfileClick` |

Note also that Freshdesk's `MESSAGE_SENT` / `MESSAGE_RECEIVED` extras are raw JSON strings — there is no typed `Event.Property` accessor equivalent, so you must parse them yourself and treat the schema as unstable.

**New in Freshdesk:** `USER_CREATED` (fires when Freshdesk creates a new user for this app install) and `USER_CLEARED` (after `resetUser`) have no Freshchat predecessors. Freshchat's closest analogue to `USER_CREATED` was `FRESHCHAT_USER_RESTORE_ID_GENERATED`, which only fired once after an anonymous user's first message.

---

## 15. Listeners and handlers

### 15.1 Custom link handler

```kotlin
// Before — returns Boolean.
// NOTE: the SDK stores this in a WeakReference, so the handler must be an object
// your app keeps alive (e.g. the Activity, or an Application-scoped singleton).
// A bare lambda would be garbage-collected and silently stop working.
class SupportActivity : AppCompatActivity(), LinkHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Freshchat.getInstance(this).setCustomLinkHandler(this)
    }

    override fun handleLink(url: String, bundle: Bundle?): Boolean {
        return if (url.startsWith("myapp://")) {
            router.navigate(url)
            true      // handled by the app
        } else {
            false     // let the SDK open it
        }
    }
}
```

```kotlin
// After — returns Unit; you always handle it.
// Register only once the SDK is ready: setLinkHandler is ignored before initialize completes.
class SupportActivity : AppCompatActivity(), FreshDeskSDKLinkHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // sdkReady is the flag set in initializedCallback (see [§5.2](#52-after--freshdesk)).
        // If it is still false, initializedCallback registers the handler instead (below).
        if (sdkReady) FreshdeskSDK.setLinkHandler(this)
    }

    override fun handleLink(url: String?) {
        val target = url ?: return
        if (target.startsWith("myapp://")) {
            router.navigate(target)
        } else {
            openInAppBrowser(target)   // you must do this yourself
        }
    }

    override fun onDestroy() {
        FreshdeskSDK.setLinkHandler(null)
        super.onDestroy()
    }
}

// In the initializedCallback passed to FreshdeskSDK.initialize(...):
initializedCallback = {
    sdkReady = true
    // Register for an already-visible SupportActivity that was created before the SDK was ready.
    currentSupportActivity?.let { FreshdeskSDK.setLinkHandler(it) }
}
```

**Contract change — this is a behavioural trap.** Freshchat's `handleLink` returned `Boolean`, letting you fall back to SDK default behaviour by returning `false`. Freshdesk's `handleLink` returns `Unit`: once a handler is set, **you are responsible for every URL**. If you ignore a URL you don't recognise, that link becomes dead. Always provide a default branch that opens the URL yourself.

Both SDKs hold a **weak reference** to the handler — keep a strong reference (e.g. the Activity itself) or handling silently reverts to default.

> **Call order:** `setLinkHandler` is ignored if called before `initialize` has run, or while the device is offline — so setting it in `onCreate` ahead of `initialize` has no effect. Set it from the `initializedCallback`, or re-set it once you know the SDK is ready.

### 15.2 User interaction listener

```kotlin
// Before
Freshchat.getInstance(this).setFreshchatUserInteractionListener(
    object : FreshchatUserInteractionListener {
        override fun onUserInteraction() { resetInactivityTimer() }
        override fun onUserLeaveHint() { /* … */ }
    }
)
```

```kotlin
// After — the SDK holds the listener weakly, so keep your own reference to it
private val userInteractionListener = object : FreshdeskUserInteractionListener {
    override fun onUserInteraction() { resetInactivityTimer() }
}

FreshdeskSDK.setFreshdeskUserInteractionListener(userInteractionListener)

FreshdeskSDK.setFreshdeskUserInteractionListener(null)   // to remove
```

Store the listener in a field of an object that lives as long as you need callbacks, such as your `Application` class or the Activity that uses it. A listener passed inline with nothing else pointing to it can be garbage-collected, and then it silently stops being called.

Direct equivalent, with one method removed: **`onUserLeaveHint()` has no Freshdesk counterpart.** If you used it to detect the user leaving the SDK, use `Activity` lifecycle callbacks on your own side instead.

### 15.3 WebView locale listener

```kotlin
// Before
Freshchat.getInstance(this).setWebviewListener(object : FreshchatWebViewListener {
    override fun onLocaleChangedByWebView(ctx: WeakReference<Context>) { reapplyAppLocale() }
})

// After — the SDK holds the listener weakly, so keep your own reference to it
class MyApp : Application() {

    private val webViewListener = object : FreshdeskWebviewListener {
        override fun onLocaleChangedByWebView() { reapplyAppLocale() }
    }

    override fun onCreate() {
        super.onCreate()
        FreshdeskSDK.setWebViewListener(webViewListener)
    }
}
```

The context parameter is gone. Two more things to know:

- **Keep your own reference to the listener.** The SDK holds it weakly, so a listener passed inline with nothing else pointing to it can be garbage-collected, and then it silently stops being called. Store it in a field of a long-lived object, such as your `Application` class, as shown above.
- **The listener can't be removed.** The setter doesn't accept `null`. Because the SDK holds the listener weakly, a listener owned by an Activity won't leak it, but after that Activity is destroyed the listener stops receiving calls. If you need it for the whole app, own it in your `Application` class.

---

## 16. Localization

### 16.1 Language selection

| | Freshchat | Freshdesk |
|---|---|---|
| Mechanism | Follows the app/device locale; SDK strings come from `freshchat_*` resources you override per `values-<lang>` | `SDKConfig.locale` (BCP-47, `""` = device locale) |
| Runtime change | `Freshchat.notifyAppLocaleChange(context)` | **NA** — re-`initialize()` with a new `SDKConfig.locale` |
| Shipped translations | None — the SDK ships only `values`, so all localization was app-supplied | Widget content is localized server-side per the portal's language settings |

**Action:** delete your `freshchat_*` string overrides. They do nothing for the Freshdesk widget. If you supported in-app language switching via `notifyAppLocaleChange`, replace it with a re-`initialize()` using the new locale.

### 16.2 `ContentConfiguration` — new capability

Freshdesk offers something Freshchat did not: code-first override of individual widget strings.

```kotlin
val content = ContentConfiguration(
    headers = HeaderContent(
        chat = "Talk to our team",
        faq = "Help Centre",
        typicallyRepliesFewMinsFallback = "We usually reply soon",
        channelResponse = ChannelResponseContent(
            offline = "We are offline — leave a message",
            online = ChannelResponseOnlineContent(
                default = "We typically reply in a few minutes",
                minutes = TimeUnitTemplates(
                    one = "Typically replies in {{time}} minute",
                    more = "Typically replies in {{time}} minutes"
                ),
                hours = TimeUnitTemplates(
                    one = "Typically replies in {{time}} hour",
                    more = "Typically replies in {{time}} hours"
                )
            )
        ),
        ticketForm = TicketFormContent(
            title = "Raise a request",
            submitBtnTitle = "Submit"
        )
    ),
    placeholders = PlaceholderContent(
        replyField = "Type your reply…",
        searchField = "Search articles…"
    ),
    privacyPolicySetting = PrivacyPolicyContent(
        privacyPolicyMessage = "We respect your privacy",
        privacyPolicyLinkText = "Privacy Policy",
        privacyPolicyLink = "https://example.com/privacy"
    )
)

// At init
FreshdeskSDK.initialize(
    applicationContext,
    SDKConfig(
        token = "<ACCOUNT_TOKEN>",
        host = "https://yourcompany.freshdesk.com",
        sdkID = "<SDK_ID>",
        contentConfiguration = content
    )
) { /* SDK ready */ }

// Or at runtime
FreshdeskSDK.setContentConfiguration(content)
FreshdeskSDK.setContentConfiguration(null)   // reset to widget defaults
```

Available structure (`com.freshworks.sdk.freshdesk.data.content`):

| Class | Fields |
|-------|--------|
| `ContentConfiguration` | `headers`, `placeholders`, `privacyPolicySetting`, `additionalContentKeys: Map<String, Any>` |
| `HeaderContent` | `chat`, `faq`, `faqMessageUs`, `faqNotAvailable`, `faqSearchNotAvailable`, `faqThankyou`, `faqUseful`, `faqNotUseful`, `typicallyRepliesFewMinsFallback`, `channelResponse`, `ticketForm` |
| `ChannelResponseContent` | `offline`, `online` |
| `ChannelResponseOnlineContent` | `default`, `minutes`, `hours` |
| `TimeUnitTemplates` | `one`, `more` |
| `TicketFormContent` | `title`, `listTitle`, `submitBtnTitle`, `confirmationMessage` |
| `PlaceholderContent` | `replyField`, `searchField` |
| `PrivacyPolicyContent` | `privacyPolicyMessage`, `privacyPolicyLinkText`, `privacyPolicyLink` |

Notes:

- All fields are nullable and default to `null`; omitted fields keep the widget default.
- Time templates must include the literal `{{time}}` placeholder. With `debugMode = true` the SDK logs warnings for missing placeholders and other misconfigurations.
- `additionalContentKeys` is an escape hatch for keys not yet modelled as typed properties. Typed fields win on key collision.
- If the support screen has already loaded, `setContentConfiguration` reloads it to apply the new text; otherwise the text is applied when it next loads.

---

## 17. Theming and appearance

```kotlin
// Before — resource-based theming
// res/values/themes.xml
// <style name="Theme.Freshchat.SelectedTheme" parent="Theme.Freshchat.Light.DarkActionBar">
//     <item name="freshchatChatBubbleRight">@drawable/my_bubble</item>
//     <item name="freshchatSendIcon">@drawable/my_send</item>
//     ...
// </style>
```

`NA` in code. There is **no theming or appearance API** in the Freshdesk Android SDK. Appearance (brand colours, button colours, launcher colour, titles, agent profile display, position and behaviour) is **configured in the Freshdesk portal** and applied automatically. The SDK's single Activity is fixed to `Theme.AppCompat.DayNight.NoActionBar`.

**Action:** this is often the largest single migration cost for apps with heavily customized Freshchat theming.

1. Inventory every `freshchat*` theme attribute you override.
2. Work with your Freshdesk admin to reproduce as much as possible through the portal's appearance settings.
3. Accept that per-drawable customization (chat bubbles, send/attach/search icons, placeholder and error images) has no equivalent, and communicate the visual change to your design stakeholders **before** cutover.
4. Delete the `Theme.Freshchat.*` overrides and any custom `freshchat*` drawables that are now unused.

---

## 18. Diagnostics

```kotlin
// Before
val name = Freshchat.getSDKVersionName()
val code = Freshchat.getSDKVersionCode()

// After
Log.d(
    "Freshdesk",
    "${FreshdeskSDK.getSDKVersionName()} " +
        "(${FreshdeskSDK.getSDKVersionCode()}) " +
        "build ${FreshdeskSDK.getSDKBuildID()}"
)
```

All three are safe to call before `initialize`. `getSDKBuildID()` is new.

Set `SDKConfig.debugMode = BuildConfig.DEBUG` for verbose logging in development.

> **Note:** the Freshdesk SDK installs a global `Thread.setDefaultUncaughtExceptionHandler` during `initialize` (wrapping and delegating to your existing handler) so it can flush SDK logs on crash. If your app installs its own handler, install yours **before** `FreshdeskSDK.initialize` so the SDK wraps it rather than the other way around, and verify your crash reporter still receives reports.

---

## 19. Migration checklist

**Phase 1 — Portal and planning**

- [ ] Obtain `token`, `host`, `sdkID` from **Admin Settings → Mobile Chat SDK**
- [ ] Decide whether JWT enforcement will be enabled
- [ ] Pre-create every custom user property as a **Contact Field** on the linked widget
- [ ] Pre-create every custom ticket property as a **Ticket Field** on the linked widget
- [ ] Configure the FCM credential in the portal
- [ ] Configure widget appearance to approximate your Freshchat theme
- [ ] Confirm `minSdk 26` is acceptable for your install base
- [ ] Review the gap list ([§6.3](#63-faqoptions-has-no-equivalent), [§6.4](#64-tag-filtering-and-parallel-conversations-have-no-equivalent), [§6.5](#65-deeplinks-have-no-equivalent), [§10](#10-unread-count), [§11](#11-messaging-and-bot-apis-with-no-equivalent), [§12.4](#124-notification-interception-has-no-equivalent), [§14.4](#144-freshchat-events-with-no-freshdesk-equivalent), [§17](#17-theming-and-appearance)) with product and design and get sign-off on what is being lost

**Phase 2 — Project setup**

- [ ] Remove the JitPack repository entry (if used only for Freshchat)
- [ ] Remove `com.github.freshworks:freshchat-android`
- [ ] Add `mavenCentral()` and `com.freshworks.sdk:freshdesk:<latest-version>`
- [ ] Raise `minSdk` to 26
- [ ] Resolve `<application>` manifest merge conflicts (`dataExtractionRules`, `fullBackupContent`, `enableOnBackInvokedCallback`)
- [ ] Rename your FileProvider authority if it collides with `${applicationId}.fileProvider`
- [ ] Add `POST_NOTIFICATIONS` for API 33+
- [ ] Delete `freshchat_*` string resource overrides
- [ ] Delete `Theme.Freshchat.*` overrides and unused `freshchat*` drawables
- [ ] Delete your `FreshchatImageLoader` implementation

**Phase 3 — Code migration**

- [ ] Move initialization from the launcher Activity to `Application.onCreate()`
- [ ] Replace `FreshchatConfig` with `SDKConfig`
- [ ] Add an `initializedCallback` and a readiness flag; gate all UI entry points on it
- [ ] Move `setNotificationConfig` (and any early `setLinkHandler` / `setPushRegistrationToken`) into the callback
- [ ] Replace `showConversations` → `openSupport`, `showFAQs` → `openKnowledgeBase`
- [ ] Replace `setUser` / `setUserProperty` / `setUserProperties` with a single `setUserProperties(Map)`
- [ ] Convert synchronous `getUser()` reads into `getUser(onFailure, userCallback)` callbacks
- [ ] Replace `identifyUser` with your JWT flow
- [ ] Replace `resetUser(context)` with `resetUser(onSuccess, onFailure)`; if it fails, keep support blocked and retry until it succeeds
- [ ] Replace `setUser(jwt)` / `restoreUser(jwt)` with `SDKConfig.jwt` + `authenticateAndUpdate(jwt)`
- [ ] Replace `getUserIdTokenStatus()` polling with a cached `USER_STATE_CHANGE` receiver
- [ ] Replace `getUnreadCountAsync` polling with a cached `UNREAD_COUNT` receiver
- [ ] Replace push detection/handling with the `remoteMessage.data` variants
- [ ] Replace the `FRESHCHAT_EVENTS` receiver with per-action `SDKEventID` receivers
- [ ] Rewrite `setCustomLinkHandler` as `setLinkHandler` with a default branch that opens unmatched URLs
- [ ] Rename the user-interaction and webview listeners; drop `onUserLeaveHint` usage
- [ ] Convert `setBotVariables` and `sendMessage` payloads into user/ticket properties
- [ ] Add `ContentConfiguration` if you need string overrides
- [ ] Remove all `MethodNotAllowedException` / `JwtException` / `FreshchatCallbackStatus` handling

**Phase 4 — Verification**

- [ ] Cold start with no network — confirm graceful degradation, no crash
- [ ] Verify UI entry points only fire after the init callback
- [ ] Verify custom user and ticket properties actually land in Freshdesk (whitelisting check)
- [ ] Verify push end to end: token registration, foreground and background delivery, tap navigation
- [ ] Verify the unread badge updates from the broadcast
- [ ] Verify the full JWT lifecycle: authenticated → expired → refresh → authenticated
- [ ] Verify logout: `dismissFreshdeskViews` → `resetUser` → re-init as a different user
- [ ] Verify logout while offline: support stays blocked, the reset retries once back online, and the next user never sees the previous user's conversations
- [ ] Verify link handling covers every URL shape, including ones you don't recognise
- [ ] Verify your crash reporter still works after the SDK installs its handler
- [ ] Test on API 26 (minimum) and the latest API level
- [ ] Test RTL and non-English locales if you support them

---

## Appendix A — Freshchat API surface with no Freshdesk equivalent

Consolidated for planning. Everything here is confirmed absent from Freshdesk Android `2.2.4`.

| Freshchat API | Area |
|---------------|------|
| `identifyUser(externalId, restoreId)` | Identity |
| `getUserIdTokenStatus()` | Identity |
| `setBotVariables(vars, botVars)` | Bots |
| `sendMessage(context, FreshchatMessage)` | Messaging |
| `ConversationOptions.filterByTags(...)` | Filtering |
| `FaqOptions` (all methods) | Filtering / FAQ UI |
| `showConversations(ctx, refId, topicName)` (as a persisted thread) | Parallel conversations |
| `getUnreadCountAsync(callback)` | Unread |
| `getUnreadCountAsync(callback, tags)` | Unread |
| `getUnreadCountAsyncWithReferenceId(...)` | Unread |
| `openFreshchatDeeplink(context, url)` | Navigation |
| `setConversationBannerMessage(context, msg)` | UI |
| `FreshchatNotificationConfig.setNotificationSound(Uri)` | Push |
| `FreshchatNotificationConfig.launchActivityOnFinish(name)` | Push |
| `FreshchatNotificationConfig.setNotificationInterceptionEnabled(true)` + intercept broadcast | Push |
| `linkifyWithPattern(regex, scheme)` | Links |
| `setImageLoader(FreshchatImageLoader)` | Media |
| `setAgentProfileClickRedirectionUri(uri)` | Navigation |
| `notifyAppLocaleChange(context)` | Localization |
| `FreshchatConfig` feature toggles (camera, gallery, file, team-member info, response expectation, event tracking) | Config |
| `FreshchatUserInteractionListener.onUserLeaveHint()` | Listeners |
| `setEdgeDebugApiLoggingEnabled(context, enabled)` | Diagnostics |
| Resource-based theming (`Theme.Freshchat.SelectedTheme`, all `freshchat*` attributes) | Theming |
| 41 of 44 `Event.EventName` values, all 29 `Event.Property` values | Events |

---

## Appendix C — Version reference

| | Freshchat Android | Freshdesk Android |
|---|---|---|
| Version documented | `6.5.10` (versionCode `6510`) | `2.2.4` |
| Package | `com.freshchat.consumer.sdk` | `com.freshworks.sdk.freshdesk` |
| Artifact | `com.github.freshworks:freshchat-android` | `com.freshworks.sdk:freshdesk` |
| Repository | JitPack | Maven Central |
| Public repo | `github.com/freshworks/freshchat-android` | `github.com/freshworks/freshdesk-android-sdk` |

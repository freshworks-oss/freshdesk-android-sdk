# Freshdesk Android SDK

Integrate Freshdesk messaging, knowledge base, and push notifications into your Android app.

**Credentials:** Admin Settings → **Mobile Chat SDK** in your Freshdesk portal.

![Mobile Chat SDK settings](https://github.com/user-attachments/assets/8b66dfbe-6822-4b97-956b-9af285809d15)

---

## Table of contents

- [Portal and server setup](#portal-and-server-setup)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [SDK initialization](#sdk-initialization)
- [Content configuration](#content-configuration)
- [Opening the SDK UI](#opening-the-sdk-ui)
- [User properties and tickets](#user-properties-and-tickets)
- [JWT authentication](#jwt-authentication)
- [Push notifications](#push-notifications)
- [SDK events](#sdk-events)
- [Analytics](#analytics)
- [Customization](#customization)
- [User session management](#user-session-management)
- [API reference](#api-reference)
- [Troubleshooting](#troubleshooting)

---

## Portal and server setup

Complete these steps in your Freshdesk portal **before** integrating the SDK in your app.

### Where to find SDK credentials

Go to **Admin Settings → Mobile Chat SDK** and open your mobile SDK configuration. Map portal values to `SDKConfig` as follows:

| Portal / admin field | `SDKConfig` field | Notes |
|----------------------|-------------------|--------|
| Token (account token) | `token` | Required |
| Host / domain | `host` | Full portal URL, e.g. `https://yourcompany.freshdesk.com` |
| SDK ID | `sdkID` | Identifies this mobile SDK instance |
| JWT encryption key | — | Used on **your backend** to sign JWTs; never embed in the app |

### Push notifications (server)

If you use FCM push:

1. Create a Firebase project and add your Android app (`google-services.json`).
2. In the Firebase console, generate and download the **FCM service-account JSON** for your project.
3. In Freshdesk, go to **Admin Settings → Mobile Chat SDK** and upload or configure that FCM credential for your SDK/app so Freshdesk can send pushes to your users.
4. In the app, forward the device token with `setPushRegistrationToken` (see [Push notifications](#push-notifications)).

### JWT (server)

1. Use the **JWT encryption key** from the same Mobile Chat SDK page on your **backend only**.
2. Sign JWTs server-side per [Authenticate users](https://support.freshdesk.com/en/support/solutions/articles/50000011580-authenticate-users) (user identity, optional `exp`, and any user/ticket properties to update).
3. Pass the token in `SDKConfig.jwt` at initialization — **required** when JWT enforcement is enabled. `authenticateAndUpdate` after login is supported only when enforcement is disabled.

---

## Requirements

| Requirement | Value |
|-------------|-------|
| Minimum SDK | 26 |
| Compile / target SDK | 35 (recommended) |
| Repository | Maven Central |

The SDK merges `INTERNET` and network-state permissions automatically. For push on **Android 13+ (API 33+)**, declare and request `POST_NOTIFICATIONS` in your app (see [Push notifications](#push-notifications)).

---

## Installation

**1. Add Maven Central** (project-level `build.gradle` / `settings.gradle`):

```groovy
repositories {
    mavenCentral()
}
```

**2. Add the dependency** (app-level). Replace `<latest-version>` with the current SDK version.

Groovy:

```groovy
dependencies {
    implementation 'com.freshworks.sdk:freshdesk:<latest-version>'
}
```

Kotlin DSL:

```kotlin
dependencies {
    implementation("com.freshworks.sdk:freshdesk:<latest-version>")
}
```

**3. Key imports**

```kotlin
import com.freshworks.sdk.freshdesk.FreshdeskSDK
import com.freshworks.sdk.freshdesk.data.SDKConfig
import com.freshworks.sdk.freshdesk.notification.NotificationConfig
import com.freshworks.sdk.freshdesk.backend.model.User
import com.freshworks.sdk.freshdesk.events.SDKEventID
import com.freshworks.sdk.freshdesk.events.UserState
import com.freshworks.sdk.freshdesk.handlers.FreshDeskSDKLinkHandler
import com.freshworks.sdk.freshdesk.handlers.FreshdeskUserInteractionListener
import com.freshworks.sdk.freshdesk.core.FreshdeskWebviewListener
// Optional — widget string overrides
import com.freshworks.sdk.freshdesk.data.content.ContentConfiguration
import com.freshworks.sdk.freshdesk.data.content.HeaderContent
import com.freshworks.sdk.freshdesk.data.content.PlaceholderContent
```

___

## Quick start

1. Call `FreshdeskSDK.initialize()` from your `Application.onCreate()` using **application context**.
2. Wait for the **initialization callback** before opening support UI.
3. Open support from an **Activity** when the user taps Help / Support.

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FreshdeskSDK.initialize(
            this,
            SDKConfig(
                token = "<Your token>",
                host = "<Your host>",
                sdkID = "<Your SDK ID>"
            )
        ) {
            // SDK is fully ready — safe to open support, topic, or knowledge base.
        }
    }
}
```

```kotlin
// In an Activity
helpButton.setOnClickListener {
    FreshdeskSDK.openSupport(this)
}
```

---

## SDK initialization

### When and where to call

- Call **`initialize` once per active SDK session** at app startup in `Application.onCreate()`.
- Pass the **Application** instance or `applicationContext`, not an Activity.
- Use the **callback** to know when the widget is loaded and ready. Do **not** call `openSupport`, `openTopic`, or `openKnowledgeBase` before the callback runs.
- Most APIs need an **active network** connection. Calls made offline are dropped (UI methods may show a no-network message).
- Non-UI methods such as `setNotificationConfig`, `setPushRegistrationToken`, `setUserProperties`, and `setContentConfiguration` can be called after `initialize()` returns; they queue until the SDK is ready.
- To **switch users**, call `resetUser()` first, then `initialize()` again with the new config.

### `SDKConfig` fields

| Field | Required | Description |
|-------|----------|-------------|
| `token` | Yes | Account token from Mobile Chat SDK settings |
| `host` | Yes | Portal URL (e.g. `https://yourcompany.freshdesk.com`) |
| `sdkID` | Yes | SDK ID from Mobile Chat SDK settings |
| `locale` | No | BCP 47 language code (`en`, `ar`, …). Empty = device locale |
| `jwt` | No | Signed JWT for authenticated users. **Required** if JWT enforcement is enabled on the widget |
| `headerProps` | No | Custom header properties for the widget |
| `contentConfiguration` | No | Override static widget UI strings (see [Content configuration](#content-configuration)) |
| `debugMode` | No | Verbose SDK logging (`true` in dev, `false` in production) |

### Example

```kotlin
FreshdeskSDK.initialize(
    context = applicationContext,
    sdkConfig = SDKConfig(
        token = "<Your token>",
        host = "<Your host>",
        sdkID = "<Your SDK ID>",
        locale = "en",
        jwt = null,
        headerProps = emptyMap(),
        contentConfiguration = null,
        debugMode = BuildConfig.DEBUG
    ),
    initializedCallback = {
        FreshdeskSDK.setNotificationConfig(
            NotificationConfig(
                soundEnabled = true,
                smallIconResId = R.drawable.ic_notification,
                largeIconResId = R.drawable.ic_notification
            )
        )
    }
)
```

---

## Content configuration

Use **`ContentConfiguration`** to override static strings in the embedded web widget (headers, placeholders, ticket-form labels, privacy banner, topic response-expectation labels, and more).

- Pass overrides at init via **`SDKConfig.contentConfiguration`**, or at runtime with **`FreshdeskSDK.setContentConfiguration(configuration)`**.
- Pass **`null`** to `setContentConfiguration` to clear overrides and use widget defaults.
- This is **separate from** `SDKConfig.locale`, which selects the widget’s base language. Content overrides replace individual strings; omitted or null fields keep the widget default for that key.
- For keys not yet modeled as typed properties, use **`ContentConfiguration.additionalContentKeys`** (`Map<String, Any>`). Typed fields win on key collision.

Public types are in **`com.freshworks.sdk.freshdesk.data.content`**: `ContentConfiguration`, `HeaderContent`, `PlaceholderContent`, `PrivacyPolicyContent`, `TicketFormContent`, `ChannelResponseContent`, `ChannelResponseOnlineContent`, and `TimeUnitTemplates`.

### Init example

```kotlin
FreshdeskSDK.initialize(
    context,
    SDKConfig(
        token = "<Your token>",
        host = "<Your host>",
        sdkID = "<Your SDK ID>",
        locale = "ar",
        contentConfiguration = ContentConfiguration(
            headers = HeaderContent(
                chat = "Talk to our team",
                ticketForm = TicketFormContent(
                    title = "Create a request",
                    submitBtnTitle = "Submit"
                )
            ),
            placeholders = PlaceholderContent(
                replyField = "Type your message…",
                searchField = "Search help articles…"
            ),
            privacyPolicySetting = PrivacyPolicyContent(
                privacyPolicyMessage = "We respect your privacy.",
                privacyPolicyLinkText = "Privacy policy",
                privacyPolicyLink = "https://example.com/privacy"
            )
        ),
        debugMode = BuildConfig.DEBUG
    )
) {
    // Ready
}
```

### Runtime updates

```kotlin
FreshdeskSDK.setContentConfiguration(
    ContentConfiguration(
        headers = HeaderContent(chat = "Message support")
    )
)

FreshdeskSDK.setContentConfiguration(null) // clear overrides
```

If the widget has **already loaded**, the SDK applies updates via an internal re-init. If the widget is **still starting**, the new configuration is stored for the next load.

### Response expectation labels (topic view)

On a topic screen, the widget shows how quickly agents typically reply. Configure these on **`HeaderContent`**:

| Kotlin property | Wire key | When used |
|-----------------|----------|-----------|
| `typicallyRepliesFewMinsFallback` | `headers.typically_replies_few_mins_fallback` | Backend cannot return a dynamic response time |
| `channelResponse.offline` | `headers.channel_response.offline` | Agents unavailable |
| `channelResponse.online.default` | `headers.channel_response.online.default` | Agents online; generic label |
| `channelResponse.online.minutes` / `.hours` | `headers.channel_response.online.minutes` / `.hours` | Online; templates with `{{time}}` |

Use **`TimeUnitTemplates`** with **`one`** (singular) and **`more`** (plural). Include the literal **`{{time}}`** in each template.

```kotlin
FreshdeskSDK.setContentConfiguration(
    ContentConfiguration(
        headers = HeaderContent(
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
            )
        )
    )
)
```

With **`debugMode = true`**, the SDK logs non-fatal warnings for common misconfigurations (for example templates missing `{{time}}`).

### Forward-compatible keys

```kotlin
ContentConfiguration(
    additionalContentKeys = mapOf(
        "custom_banner" to "Hello",
        "new_section" to mapOf("title" to "Coming soon")
    )
)
```

---

## Opening the SDK UI

Use an **Activity context** for all UI entry points so back navigation works correctly.

### Support home

Opens the main support experience (chat, topics, FAQs).

```kotlin
FreshdeskSDK.openSupport(context)
```

### Knowledge base

Opens FAQs / help articles directly (no chat home).

```kotlin
FreshdeskSDK.openKnowledgeBase(context)
```

### Topic

Opens a specific conversation topic. Provide at least one of `topicName` or `topicId`.

```kotlin
FreshdeskSDK.openTopic(
    context = context,
    topicName = "Order Support",
    topicId = "12345" // optional
)
```

**Tip:** Call `setTicketProperties()` before `openSupport()` or `openTopic()` to attach order IDs, product names, or other context for agents.

### Dismiss SDK screens

Closes any open Freshdesk UI (for example before logout).

```kotlin
FreshdeskSDK.dismissFreshdeskViews()
```

---

## User properties and tickets

User and ticket metadata are set at **runtime** only through the non-JWT APIs below. When using JWT authentication, user and ticket properties encoded in the token are supplied via **`SDKConfig.jwt`** and refreshed through **`authenticateAndUpdate()`** (see [JWT authentication](#jwt-authentication)).

### Set user properties

Updates the current user in the Freshdesk session. Use after login or profile updates when JWT enforcement is **disabled** and you are not sending property updates in the JWT.

```kotlin
FreshdeskSDK.setUserProperties(
    mapOf(
        "firstName" to "Jane",
        "lastName" to "Doe",
        "email" to "jane@example.com",
        "phone" to "+1234567890",
        "phoneCountry" to "US",
        "plan" to "premium" // custom property
    )
)
```

**Standard keys:** `firstName`, `lastName`, `email`, `phone`, `phoneCountry`. Any other keys are custom properties.

> If **JWT enforcement** is enabled on the widget, `setUserProperties` has no effect. Put user data in the JWT and use `authenticateAndUpdate()` instead.

### Set ticket properties

Attaches metadata to the **next** conversation when the user sends a message. Call immediately before opening support or a topic.

```kotlin
FreshdeskSDK.setTicketProperties(
    mapOf(
        "order_id" to "ORD-12345",
        "product_name" to "Premium Widget"
    )
)
FreshdeskSDK.openSupport(this)
```

Properties are sent to Freshdesk only after the user **sends a message** in that session. Call again before each new support flow for fresh context.

> Under JWT enforcement, use ticket fields in the JWT payload instead of `setTicketProperties`.

### Get current user

```kotlin
FreshdeskSDK.getUser(
    onFailure = { error ->
        Log.e("FreshdeskSDK", "getUser failed", error)
    },
    userCallback = { user: User ->
        Log.d("FreshdeskSDK", "User alias: ${user.alias}")
    }
)
```

Both callbacks run on the **main thread**. `onFailure` is optional.

Relevant `User` fields: `alias`, `firstName`, `lastName`, `email`, `phone`, `phoneCountryCode`, `customProps`, `restoreId`, `identifier`.

---

## JWT authentication

Freshdesk can require a server-signed JWT so only identified users can chat. See [Authenticate users](https://support.freshdesk.com/en/support/solutions/articles/50000011580-authenticate-users) for payload fields, signing, and examples.

### When to use JWT

| Scenario | Guidance |
|----------|----------|
| JWT **enforced** on the widget | **Must** pass a valid JWT in `SDKConfig.jwt` at init; init fails without it |
| Logged-in app, JWT not enforced | **Should** pass JWT for identified users; may omit for anonymous visitors |
| No login in your app | Omit `jwt` (or pass `null`) |

### Setup

1. Generate a JWT on your backend using the encryption key from **Admin Settings → Mobile Chat SDK** (see [Portal and server setup](#portal-and-server-setup)).
2. Include user identity (and optional user/ticket properties) per the authenticate-users article.
3. Pass the token in `SDKConfig.jwt` during `initialize`, or call `authenticateAndUpdate` after login.
4. Register for `SDKEventID.USER_STATE_CHANGE` (see [SDK events](#sdk-events)).
5. On `UserState.AUTH_EXPIRED`, fetch a new JWT and call `authenticateAndUpdate(newJwt)`.

### Identified vs anonymous users

- **Identified (logged-in) users:** JWT should carry stable identity fields required by your Freshdesk configuration (for example reference or user identifiers described in the authenticate-users doc). Use `authenticateAndUpdate` to refresh profile or ticket data encoded in the JWT.
- **Anonymous / pre-login users:** If your product allows it and JWT is not enforced, you may start without a JWT; once the user logs in, call `authenticateAndUpdate` with a signed token.
- **Restoring sessions:** The SDK exposes `User.restoreId` via `getUser` and `USER_CREATED` events. For cross-device restore, follow the restore flow in the [Authenticate users](https://support.freshdesk.com/en/support/solutions/articles/50000011580-authenticate-users) article when building JWT payloads.

### JWT expiry

Add an `exp` claim (Unix time in **seconds**) in the JWT if you want a bounded lifetime. When the token expires, the SDK reports `UserState.AUTH_EXPIRED`; supply a new JWT with `authenticateAndUpdate`.

### SDK behavior by auth state

Listen to `SDKEventID.USER_STATE_CHANGE` and handle states promptly—especially when JWT is enforced, the widget may not show chat or FAQ content until authentication succeeds.

```kotlin
FreshdeskSDK.initialize(
    context,
    SDKConfig(
        token = "<Your token>",
        host = "<Your host>",
        sdkID = "<Your SDK ID>",
        jwt = "<JWT from your backend>"
    )
) {
    // Ready
}
```

```kotlin
FreshdeskSDK.authenticateAndUpdate(jwt)
```

### Important rules

- If JWT is **enforced** in the portal, you **must** pass a valid JWT at init or initialization fails.
- `authenticateAndUpdate` handles authentication **and** property updates when those are encoded in the JWT.
- To switch to a **different user**, call `resetUser()` first, then `authenticateAndUpdate` or re-`initialize` with the new credentials. Do not call `authenticateAndUpdate` alone to switch users.

### User state values

| Constant | Value | Meaning |
|----------|-------|---------|
| `UserState.AUTHENTICATED` | `authenticated` | Valid JWT |
| `UserState.NOT_AUTHENTICATED` | `not_authenticated` | Invalid JWT |
| `UserState.AUTH_EXPIRED` | `auth_expired` | JWT expired — refresh and call `authenticateAndUpdate` |
| `UserState.JWT_ABSENT` | `jwt_not_present` | JWT required but not provided at init |
| `UserState.IDENTIFIER_UPDATED` | `identifier_updated` | User identifier updated |
| `UserState.UNDEFINED` | `undefined` | Default / unknown |

```kotlin
if (intent?.action == SDKEventID.USER_STATE_CHANGE) {
    when (intent.getStringExtra(SDKEventID.USER_STATE_CHANGE)) {
        UserState.AUTH_EXPIRED -> FreshdeskSDK.authenticateAndUpdate(fetchNewJwt())
        UserState.AUTHENTICATED -> { /* user ready */ }
        UserState.NOT_AUTHENTICATED -> { /* handle invalid token */ }
    }
}
```

---

## Push notifications

Your app must integrate **Firebase Cloud Messaging (FCM)**. The SDK registers device tokens with Freshdesk and displays incoming Freshdesk push payloads.

### Server-side setup

1. Enable Firebase Cloud Messaging in your Firebase project.
2. Save your app’s **FCM server key** (or the credential format required by your Freshdesk admin UI) under **Admin Settings → Mobile Chat SDK** in the portal (see [Portal and server setup](#portal-and-server-setup)).
3. Without this step, client token registration may succeed locally but pushes from Freshdesk will not reach devices.

### Client prerequisites

- Firebase project with `google-services.json` in your app module
- `FirebaseMessaging` dependency and a `FirebaseMessagingService` declared in the manifest
- Device with **Google Play services** (standard for FCM on Android)
- `POST_NOTIFICATIONS` for API 33+:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Request at runtime on Android 13+ when you need notification permission.

### 1. Register the FCM token

Call on **every** token refresh in `onNewToken`:

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FreshdeskSDK.setPushRegistrationToken(token)
    }
}
```

### 2. Configure notification appearance

Call after `initialize` (for example in the init callback). Use your app’s drawable resources for icons.

```kotlin
FreshdeskSDK.setNotificationConfig(
    NotificationConfig(
        soundEnabled = true,
        smallIconResId = R.drawable.ic_notification,
        largeIconResId = R.drawable.ic_notification,
        importance = NotificationManager.IMPORTANCE_HIGH
    )
)
```

| Field | Description |
|-------|-------------|
| `soundEnabled` | Play sound for notifications |
| `smallIconResId` | Status bar icon (required for branding) |
| `largeIconResId` | Large notification icon |
| `priority` | Priority on pre-Oreo devices |
| `importance` | Notification channel importance (Oreo+) |

### 3. Handle incoming messages

Only process payloads that belong to Freshdesk:

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    if (FreshdeskSDK.isFreshdeskSDKNotification(remoteMessage.data)) {
        FreshdeskSDK.handleFCMNotification(remoteMessage.data)
    } else {
        // Your app’s own notifications
    }
}
```

**Notes:**

- `isFreshdeskSDKNotification` requires the SDK to be initialized but not network.
- `setPushRegistrationToken` stores the token locally; upload to Freshdesk happens after the SDK has an active user session.
- Notifications are not shown while the Freshdesk support screen is already open.
- `resetUser()` clears push registration for the current SDK user.

### Manifest example (FirebaseMessagingService)

```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

## SDK events

The SDK sends events via **`LocalBroadcastManager`**. Register a `BroadcastReceiver` where you need updates (Application for app-wide badges, or Activity `onStart` / `onStop` for screen-scoped listeners).

### Event reference

| Event | Extra key | Payload | Use case |
|-------|-----------|---------|----------|
| `SDKEventID.UNREAD_COUNT` | Same as action | `Int` count | Unread badge |
| `SDKEventID.USER_STATE_CHANGE` | Same as action | `String` state | JWT lifecycle |
| `SDKEventID.USER_CREATED` | Same as action | `User` (Parcelable) | New SDK user |
| `SDKEventID.USER_CLEARED` | — | — | After `resetUser` |
| `SDKEventID.USER_AUTHENTICATED` | — | — | JWT auth success |
| `SDKEventID.MESSAGE_SENT` | Extras bundle | — | Message analytics |
| `SDKEventID.MESSAGE_RECEIVED` | Extras bundle | — | Message analytics |
| `SDKEventID.DOWNLOAD_FILE` | Extras bundle | — | File download in chat |

### Unread count example

```kotlin
class SupportActivity : AppCompatActivity() {

    private val unreadCountReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SDKEventID.UNREAD_COUNT) {
                val count = intent.getIntExtra(SDKEventID.UNREAD_COUNT, 0)
                updateBadge(count)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            unreadCountReceiver,
            IntentFilter(SDKEventID.UNREAD_COUNT)
        )
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(unreadCountReceiver)
        super.onStop()
    }
}
```

### Register multiple events

```kotlin
LocalBroadcastManager.getInstance(context).registerReceiver(
    receiver,
    IntentFilter().apply {
        addAction(SDKEventID.UNREAD_COUNT)
        addAction(SDKEventID.USER_STATE_CHANGE)
        addAction(SDKEventID.USER_CREATED)
        addAction(SDKEventID.USER_CLEARED)
        addAction(SDKEventID.USER_AUTHENTICATED)
        addAction(SDKEventID.MESSAGE_SENT)
        addAction(SDKEventID.MESSAGE_RECEIVED)
        addAction(SDKEventID.DOWNLOAD_FILE)
    }
)
```

Unregister receivers when appropriate to avoid leaks. Receivers registered only in `Application.onCreate()` may stay registered for the process lifetime.

---

## Analytics

Track custom user actions for context, campaigns, and triggered messages.

```kotlin
FreshdeskSDK.trackEvent(
    eventName = "purchase_completed",
    eventData = mapOf(
        "order_id" to "ORD-12345",
        "total_amount" to 99.99,
        "items_count" to 3
    )
)

FreshdeskSDK.trackEvent("profile_updated")
```

Use concise **snake_case** event names. `eventData` values should be primitives (`String`, `Int`, `Double`, `Boolean`, `Long`, `Float`, etc.)—not nested objects.

### Event limits (product)

Freshdesk enforces limits on the server for tracked events (typical constraints across Freshworks messaging products):

| Limit | Value |
|-------|--------|
| Unique event names per account | 121 |
| Event name length | 32 characters |
| Property key length | 32 characters |
| Property value length | 256 characters |
| Properties per event | 20 |

Confirm current limits in your Freshdesk account or admin documentation if events are rejected.

---

## Customization

### Custom link handler

By default, links in chat and FAQs open in the browser. Implement `FreshDeskSDKLinkHandler` to open in-app browsers, deep links, or custom screens.

```kotlin
class SupportActivity : AppCompatActivity(), FreshDeskSDKLinkHandler {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FreshdeskSDK.setLinkHandler(this)
    }

    override fun handleLink(url: String?) {
        url?.let { openInAppBrowser(it) }
    }

    override fun onDestroy() {
        FreshdeskSDK.setLinkHandler(null)
        super.onDestroy()
    }
}
```

The SDK holds a **weak reference** to the handler. Keep a **strong reference** (for example the Activity instance) or handling falls back to the default browser.

### WebView locale listener

If your app manages locale globally, react when the SDK WebView changes locale:

```kotlin
FreshdeskSDK.setWebViewListener(object : FreshdeskWebviewListener {
    override fun onLocaleChangedByWebView() {
        // Re-apply your app locale if needed
    }
})
```

Hold a strong reference to the listener.

### User interaction listener

Notified when the user interacts with SDK UI (taps, scrolls). Useful for inactivity timers or analytics.

```kotlin
FreshdeskSDK.setFreshdeskUserInteractionListener(object : FreshdeskUserInteractionListener {
    override fun onUserInteraction() {
        resetInactivityTimer()
    }
})
```

Pass `null` to remove. Retrieve the current listener with `getFreshdeskUserInteractionListener()`.

---

## User session management

### Reset user

Clears conversations, properties, and auth state. Call on **logout** or before switching accounts.

```kotlin
FreshdeskSDK.resetUser(
    onSuccess = { message ->
        Log.d("FreshdeskSDK", "Reset OK: $message")
        clearAppSession()
        navigateToLogin()
    },
    onFailure = { error ->
        Log.e("FreshdeskSDK", "Reset failed: $error")
        clearAppSession()
    }
)
```

Callbacks run on the **main thread**. `resetUser` requires the SDK to be fully ready and online; otherwise `onFailure` is invoked.

**Recommended logout flow:**

```kotlin
fun logoutUser() {
    FreshdeskSDK.dismissFreshdeskViews()
    FreshdeskSDK.resetUser(
        onSuccess = { clearAppSession(); navigateToLogin() },
        onFailure = { clearAppSession(); navigateToLogin() }
    )
}
```

Call `resetUser()` before `initialize()` with a different user’s `SDKConfig`.

---

## API reference

| Method | Description |
|--------|-------------|
| `initialize(context, sdkConfig, callback?)` | Start SDK; callback when widget is ready |
| `setContentConfiguration(configuration?)` | Override or clear widget UI strings at runtime |
| `openSupport(context)` | Open support home |
| `openKnowledgeBase(context)` | Open FAQs |
| `openTopic(context, topicName, topicId)` | Open a topic |
| `setUserProperties(map)` | Update user properties |
| `setTicketProperties(map)` | Attach ticket context for next message |
| `getUser(onFailure?, userCallback)` | Get current `User` |
| `authenticateAndUpdate(jwt)` | Authenticate or refresh JWT / properties |
| `setPushRegistrationToken(token)` | Register FCM token |
| `setNotificationConfig(config)` | Notification icons and behavior |
| `handleFCMNotification(data)` | Show Freshdesk push |
| `isFreshdeskSDKNotification(data)` | Filter FCM payloads |
| `trackEvent(name, eventData)` | Track custom event |
| `setLinkHandler(handler?)` | Custom URL handling |
| `setWebViewListener(listener)` | WebView locale changes |
| `setFreshdeskUserInteractionListener(listener?)` | User interaction callback |
| `getFreshdeskUserInteractionListener()` | Current interaction listener |
| `dismissFreshdeskViews()` | Close SDK UI |
| `resetUser(onSuccess, onFailure)` | Clear user session |
| `getSDKVersionName()` | SDK version string |
| `getSDKVersionCode()` | SDK version code |
| `getSDKBuildID()` | SDK build identifier |

`SDKConfig` also accepts **`contentConfiguration`** at init (same model as `setContentConfiguration`).

---

## Troubleshooting

| Issue | What to check |
|-------|----------------|
| UI methods do nothing | Wait for `initialize` callback; ensure network is available |
| `openSupport` navigation broken | Use Activity context, not Application context |
| Properties not updating | JWT enforced? Use JWT / `authenticateAndUpdate` instead of `setUserProperties` |
| Ticket properties missing | User must send a message after `setTicketProperties` |
| Widget strings not changing | Use `contentConfiguration` or `setContentConfiguration`; check `debugMode` logs |
| Push not received | FCM server key saved in portal, `google-services.json`, `setPushRegistrationToken`, `POST_NOTIFICATIONS` on API 33+ |
| Widget stuck loading | Do not clear app storage during active session; check network and JWT state |
| Link handler ignored | Strong reference to `FreshDeskSDKLinkHandler` implementation |
| Switching users fails | Call `resetUser()` before new JWT or `initialize` |
| Init fails with JWT | Enforced widget requires valid `jwt` in `SDKConfig` |

For version diagnostics:

```kotlin
Log.d("FreshdeskSDK", "${FreshdeskSDK.getSDKVersionName()} (${FreshdeskSDK.getSDKVersionCode()}) build ${FreshdeskSDK.getSDKBuildID()}")
```

---

## Learn more

- [Freshworks](https://www.freshworks.com)
- [Authenticate users (JWT)](https://support.freshdesk.com/en/support/solutions/articles/50000011580-authenticate-users)

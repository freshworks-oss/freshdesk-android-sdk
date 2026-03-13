# Freshdesk Android SDK
"Modern ticketing software that your sales and customer engagement teams will love [FreshdeskSDK](https://www.freshworks.com)"

## Installation
To include the SDK into your android application follow the steps mentioned below.

1. Add/Ensure MavenCentral repository is included for your project. In your project level `build.gradle` make sure the `repositories` block has `mavenCentral()`.
   ```groovy
     repositories {
       mavenCentral()
     }
   ```
2. Add the following dependency to your app-level `build.gradle` or `build.gradle.kts`:
   ```groovy
    dependencies {
      implementation 'com.freshworks.sdk:freshdesk:0.0.1-alpha'
    }
   ```
## Documentation
### SDK Initialization
To initialize the SDK, you can call the `initialize` method as shown below.
You can get the your credentials for the placeholders mentioned in the below snippet from the "Mobile Chat SDK" page in your portal. 
Go to Admin Settings -> Mobile Chat SDK
<img width="1901" height="898" alt="Screenshot 2025-11-13 at 12 27 20 PM" src="https://github.com/user-attachments/assets/8b66dfbe-6822-4b97-956b-9af285809d15" />
```kotlin
  FreshdeskSDK.initialize(
                context,
                SDKConfig(
                    token = "<Your token>",
                    host = "<Your host",
                    sdkID = "<Your SDK ID>",
                    locale = "ar", // Set your desired locale here
                    jwt = "<Your JWT if any>",
                    debugMode = true // Enable debug mode for logging
                )
            ) {
                // SDK initialized callback. You can perform other SDK actions safely after initialization, here.
            }
```
### List of APIs/Usage 
#### Support Home
Usage
```kotlin
  FreshdeskSDK.openSupport(context) //Pass context
```
Call this method to open the SDK into the landing page.

#### Knowledge Base
Usage
```kotlin
  FreshdeskSDK.openKnowledgeBase(context) //Pass context
```
Call this method to open the SDK into the FAQ/Knowledge base page directly.

### Topic
Usage
```kotlin
  FreshdeskSDK.openTopic(
                        context, //Activity context
                        topicName, //The name of the topic.
                        topicId //The ID of the topic if you have. (Optional)
                    )
```
Call this method to open the SDK into a particular topic directly.

### Getting unread message count
Getting unread count requires listening to the an event through a broadcast listener. The event will have the details regarding the total unread messages, which can be read. Refer to the below example.
* Register a broadcast listener, wherever appropriate in your application. An activity, or even the application class
   ```kotlin
     val unreadCountReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (intent?.action == SDKEventID.UNREAD_COUNT) {
                    val unreadCount = intent.getIntExtra(SDKEventID.UNREAD_COUNT, 0)
                    // Use unreadCount as needed
                }
            }
        }
     LocalBroadcastManager.getInstance(context).registerReceiver(
            unreadCountReceiver,
            IntentFilter(SDKEventID.UNREAD_COUNT)
        )
   ```
 * Make sure you unregister the receiver once you are done, to prevent memory leaks
   ```kotlin
      LocalBroadcastManager.getInstance(context).unregisterReceiver(
            unreadCountReceiver
        )
   ```
### Tracking user events
Freshdesk allows you to track any events performed by your users. It can be anything, ranging from updating their profile picture to adding 5 items to the cart. You can use these events as context while engaging with the user. Events can also be used to set up triggered messages or to segment users for campaigns.

Usage
```kotlin
  FreshdeskSDK.trackEvent(eventName, eventValueMap)
```

### JWT User Authentication
Freshdesk uses JSON Web Token (JWT) to allow only authenticated users to initiate a conversation with you through the Freshdesk messenger. To use this capability, follow the steps below.
1. Create a JWT with the encryption key that can be found in your SDK page in the admin portal (Admin Settings -> Mobile Chat SDK -> Your SDK)
   (To learn more about this refer to this [link](https://support.freshdesk.com/en/support/solutions/articles/50000011580-authenticate-users)
2. Initialize the SDK by passing the generated JWT alone with the `SDKConfig` object that. 
   ```kotlin
     FreshdeskSDK.initialize(
                context,
                SDKConfig(
                    token = "<Your token>",
                    host = "<Your host",
                    sdkID = "<Your SDK ID>",
                    locale = "ar", // Set your desired locale here
                    jwt = "<The token your generated from Step 1 goes here>",
                )
            ) {
                // SDK initialized callback. You can perform other SDK actions safely after initialization, here.
            }
   ```
3. Listen to JWT events through a broadcast receiver like below.
   ```kotlin
     //Define your receiver
      val userStateReceiver = object : BroadcastReceiver() {
          override fun onReceive(
              context: Context?,
              intent: Intent?
          ) {
              if (intent?.action == SDKEventID.USER_STATE_CHANGE) {
                  val userState = intent.getStringExtra(SDKEventID.USER_STATE_CHANGE)
                  when (userState) {
                      UserState.UNDEFINED, //Default
                      UserState.IDENTIFIER_UPDATED, //Unique user identifier updated for a user
                      UserState.NOT_AUTHENTICATED, //Invalid/Expired token is passed
                      UserState.AUTH_EXPIRED, //JWT passed has expired
                      UserState.JWT_ABSENT, //JWT was not passed during init for an enforced JWT SDK linked Widget
                      UserState.AUTHENTICATED -> //JWT passed is successfully authenticated or restored
                  }
              }
          }
      }
        
      //Register your receiver
      LocalBroadcastManager.getInstance(context).registerReceiver(
          userStateReceiver,
          IntentFilter(SDKEventID.USER_STATE_CHANGE)
      )
   ```
4. To update a new JWT during an expiry of a previously passed JWT, or to update the properties for a user with a new JWT, you can call the below method.
   ```kotlin
     FreshdeskSDK.authenticateAndUpdate(<JWT>)
   ```
Note:
1. When the JWT use is "enforced" in the widget settings in your admin portal, it is mandatory to pass the JWT during initialization. The SDK initialization will fail otherwise.
2. The `authenticateAndUpdate` method is responsible for both authentication of a user as well as updating the properties of an existing user or a ticket.

### Custom Link Handler
The SDK by default, opens any link in the conversation, or FAQs in a new browser session if a browser is present. However, the host application can take control of this redirection and decide how to open links. For this to work, the SDK can accept a custom implementation of a `FreshDeskSDKLinkHandler`.
For example, an Activity or Fragment can implement this `FreshDeskSDKLinkHandler` interface and override the `handleLink(url: String)` method.
Usage
```kotlin
  FreshdeskSDK.setLinkHandler(<FreshDeskSDKLinkHandler>)
```
Note: Make sure the application holds a solid reference to the implementation of the `FreshDeskSDKLinkHandler` (no weak references) or it may be garbage collected.

### Dismiss Freshdesk SDK views
To dismiss any open FreshdeskSDK screesn from anywhere in your application, use the following method.
Usage
```kotlin
  FreshdeskSDK.dismissFreshdeskViews()
```

### Reset User
The `resetUser` method allows you to clear the current user's session and data from the SDK. This is particularly useful when a user logs out of your application or when you want to switch between different user accounts. When called, this method will reset all user-specific data including conversations, user properties, and authentication state.

Usage
```kotlin
  FreshdeskSDK.resetUser(
      onSuccess = { message ->
          // Handle successful user reset
          // Perform any cleanup operations needed in your app
          Log.d("FreshdeskSDK", "User reset successful: $message")
      },
      onFailure = { errorMessage ->
          // Handle failure case
          Log.e("FreshdeskSDK", "Failed to reset user: $errorMessage")
      }
  )
```

Example - Reset user on logout:
```kotlin
  fun logoutUser() {
      FreshdeskSDK.resetUser(
          onSuccess = {
              // Clear your app's user session
              clearUserSession()
              // Navigate to login screen
              navigateToLogin()
          },
          onFailure = { error ->
              // Even on failure, you might want to clear local state
              clearUserSession()
              showErrorMessage("Failed to reset Freshdesk user: $error")
          }
      )
  }
```

Note: It is recommended to call `resetUser` before initializing the SDK with a different user's credentials or configuration.


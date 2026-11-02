package com.freshdesk.southwest

import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.freshdesk.southwest.data.DataStore
import com.freshdesk.southwest.data.DataStore.setUserAlias
import com.freshdesk.southwest.data.DataStore.sharedPreferences
import com.freshdesk.southwest.ui.activity.SOUTH_WEST
import com.freshdesk.southwest.utils.logEvent
import com.freshdesk.southwest.utils.logd
import com.freshdesk.southwest.utils.toast
import com.freshworks.logging.LogUploader
import com.freshworks.logging.log.LogEntry
import com.freshworks.logging.log.LogSession
import com.freshworks.logging.sampling.Logger
import com.freshworks.sdk.freshdesk.FreshdeskSDK
import com.freshworks.sdk.freshdesk.FreshdeskSDK.setFreshdeskUserInteractionListener
import com.freshworks.sdk.freshdesk.backend.model.User
import com.freshworks.sdk.freshdesk.core.FreshdeskWebviewListener
import com.freshworks.sdk.freshdesk.data.SDKConfig
import com.freshworks.sdk.freshdesk.events.SDKEventID
import com.freshworks.sdk.freshdesk.events.UserState.Companion.AUTH_EXPIRED
import com.freshworks.sdk.freshdesk.handlers.FreshdeskUserInteractionListener
import com.freshworks.sdk.freshdesk.notification.NotificationConfig
import com.freshworks.sdk.freshdesk.utils.changeLocale

const val TAG = "SouthWest"

class SouthWestApp : Application(), FreshdeskUserInteractionListener {

    private val _unreadCount: MutableLiveData<Int> = MutableLiveData()
    val unreadCount: LiveData<Int> = _unreadCount

    private val _userState: MutableLiveData<String> = MutableLiveData()
    val userState: LiveData<String> = _userState

    val customLocale = "ar"
    private lateinit var logger: Logger

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = this.getSharedPreferences(SOUTH_WEST, MODE_PRIVATE)
        setUpFreshdeskSDK()
        setupLogging()
        setWebViewListener()
        registerBroadcastReceiver()
        setFreshdeskUserInteractionListener(this)
    }

    fun clearUserState() {
        _userState.value = ""
    }

    private fun setupLogging() {
        logger = Logger.getNewInstance(
            applicationContext,
            "Southwest",
            object : LogUploader {
                override suspend fun uploadSessions(sessions: List<LogSession>): Boolean {
                    return true
                }

                override suspend fun uploadLogs(logs: List<LogEntry>): Boolean {
                    return true
                }
            }
        )
        logger.applyDefaultRule()
        logger.startSampling(mapOf())
        logger.scheduleUpload(this)
    }

    private fun setUpFreshdeskSDK() {
        initializeSDK(
            DataStore.getSelectedAccount(),
            this
        )
    }

    private fun setWebViewListener() {
        val webViewListener: FreshdeskWebviewListener = object : FreshdeskWebviewListener {
            override fun onLocaleChangedByWebView() {
                changeLocale(this@SouthWestApp, customLocale)
            }
        }
        FreshdeskSDK.setWebViewListener(webViewListener)
    }

    private fun registerBroadcastReceiver() {
        val myReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    SDKEventID.UNREAD_COUNT -> {
                        _unreadCount.value = intent.getIntExtra(SDKEventID.UNREAD_COUNT, 0)
                        context?.logEvent(
                            UNREAD_COUNT,
                            _unreadCount.value.toString(),
                            true
                        )
                    }

                    SDKEventID.MESSAGE_SENT -> {
                        context?.logEvent(MESSAGE_SENT, "${intent.extras}")
                    }

                    SDKEventID.MESSAGE_RECEIVED -> {
                        context?.logEvent(MESSAGE_RECEIVED, "${intent.extras}")
                    }

                    SDKEventID.DOWNLOAD_FILE -> {
                        context?.logEvent(DOWNLOAD_FILE, "${intent.extras}")
                    }

                    else -> userEvents(intent, context)
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver,
            IntentFilter().apply {
                addAction(SDKEventID.USER_CREATED)
                addAction(SDKEventID.UNREAD_COUNT)
                addAction(SDKEventID.USER_STATE_CHANGE)
                addAction(SDKEventID.MESSAGE_SENT)
                addAction(SDKEventID.MESSAGE_RECEIVED)
                addAction(SDKEventID.DOWNLOAD_FILE)
                addAction(SDKEventID.USER_CLEARED)
            }
        )
    }

    @Suppress("DEPRECATION")
    fun userEvents(intent: Intent?, context: Context?) {
        when (intent?.action) {
            SDKEventID.USER_CREATED -> {
                val user: User? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.extras?.getParcelable(SDKEventID.USER_CREATED, User::class.java)
                    } else {
                        intent.extras?.getParcelable(SDKEventID.USER_CREATED) as? User
                    }
                user?.let {
                    setUserAlias(it.alias)
                    context?.logEvent(USER_CREATED, user.alias, true)
                }
            }

            SDKEventID.USER_STATE_CHANGE -> {
                _userState.value = intent.getStringExtra(SDKEventID.USER_STATE_CHANGE)
                if (_userState.value == AUTH_EXPIRED) {
                    handleAuthExpiry()
                }
                context?.logEvent(USER_STATE, _userState.value ?: "", true)
            }

            SDKEventID.USER_CLEARED -> {
                context?.logEvent(USER_CLEARED, "")
            }

            SDKEventID.USER_AUTHENTICATED -> {
                context?.logEvent(USER_AUTHENTICATED, "")
            }
        }
    }

    private fun handleAuthExpiry() {
        logd { "User State: $userState" }
        toast("JWT has expired. Please update the JWT to continue.")
        // The Below line can be uncommented to re-authenticate the user automatically on JWT expiry
        // by passing valid jwt token.
        // FreshdeskSDK.authenticateAndUpdate(jwt = "")
    }

    override fun onUserInteraction() {
        logd { "onUserInteraction() called" }
        if (DataStore.getUserActionState()) {
            toast("User interaction!!")
        }
    }

    companion object {

        private const val UNREAD_COUNT = "UNREAD COUNT"
        private const val MESSAGE_SENT = "MESSAGE SENT"
        private const val MESSAGE_RECEIVED = "MESSAGE RECEIVED"
        private const val USER_STATE = "USER STATE"
        private const val USER_CLEARED = "USER CLEARED"
        private const val DOWNLOAD_FILE = "DOWNLOAD FILE"
        private const val USER_CREATED = "USER_CREATED"

        private const val USER_AUTHENTICATED = "USER AUTHENTICATED"

        fun initializeSDK(sdkConfig: SDKConfig, context: Context) {
            FreshdeskSDK.initialize(
                context,
                sdkConfig.copy(
                    debugMode = true
                )
            ) {
                Log.d(TAG, "SDK initialization complete")
                FreshdeskSDK.setNotificationConfig(
                    NotificationConfig(
                        soundEnabled = true,
                        smallIconResId = R.drawable.ic_southwest_notification,
                        largeIconResId = R.drawable.ic_southwest_notification,
                        importance = NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }
        }
    }
}

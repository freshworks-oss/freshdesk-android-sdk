package com.freshdesk.southwest.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshdesk.southwest.R
import com.freshdesk.southwest.SouthWestApp
import com.freshdesk.southwest.TAG
import com.freshdesk.southwest.components.buttons.ButtonText
import com.freshdesk.southwest.components.dialogs.ConfigureSwitcherDialog
import com.freshdesk.southwest.components.dialogs.ContentConfigurationDialog
import com.freshdesk.southwest.components.dialogs.LoadAccountForm
import com.freshdesk.southwest.components.dialogs.ProgressBarDialog
import com.freshdesk.southwest.components.dialogs.TextFieldDialog
import com.freshdesk.southwest.components.dialogs.UpdateAuthTokenDialog
import com.freshdesk.southwest.components.dialogs.UpdateTicketDialog
import com.freshdesk.southwest.components.dialogs.UpdateUserDialog
import com.freshdesk.southwest.components.dialogs.UserDetailDialog
import com.freshdesk.southwest.components.dialogs.demoContentConfiguration
import com.freshdesk.southwest.data.DataStore
import com.freshdesk.southwest.data.DataStore.clearUser
import com.freshdesk.southwest.data.DataStore.getSelectedAccount
import com.freshdesk.southwest.data.DataStore.saveToastEventsState
import com.freshdesk.southwest.data.DataStore.saveUserActionState
import com.freshdesk.southwest.data.DataStore.setCustomLinkHandlerState
import com.freshdesk.southwest.data.DataStore.setSelectedAccount
import com.freshdesk.southwest.data.DialogConfig
import com.freshdesk.southwest.extensions.applySDK35InsetsListener
import com.freshdesk.southwest.ui.theme.SouthWestTheme
import com.freshdesk.southwest.utils.logd
import com.freshdesk.southwest.utils.loge
import com.freshdesk.southwest.utils.logi
import com.freshdesk.southwest.utils.toMap
import com.freshdesk.southwest.utils.toast
import com.freshworks.sdk.freshdesk.FreshdeskSDK
import com.freshworks.sdk.freshdesk.FreshdeskSDK.setLinkHandler
import com.freshworks.sdk.freshdesk.backend.model.User
import com.freshworks.sdk.freshdesk.handlers.FreshDeskSDKLinkHandler

const val DELAY_MILLIS = 10000L

// TODO Remove once the initialization flow is done
@Suppress("UnusedPrivateProperty", "UnusedPrivateMember")
class SettingsActivity : ComponentActivity(), FreshDeskSDKLinkHandler {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SouthWestTheme {
                Scaffold(
                    topBar = {
                        Row(modifier = Modifier.padding(end = 40.dp)) {
                            TopAppBar(title = {
                                Text(
                                    text = stringResource(id = R.string.developer_settings),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }, navigationIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_back_arrow),
                                    contentDescription = stringResource(id = R.string.back),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            onBackPressed()
                                        },
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            })
                        }
                    },
                ) { paddingValues -> SetContent(paddingValues) }
            }
        }
        applySDK35InsetsListener()
    }

    @Composable
    private fun SetContent(paddingValues: PaddingValues) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OpenSupport()
            OpenTopic()
            ShowKnowledgeBase()
            DismissFreshdeskSDK()
            UpdateJWT()
            UpdateUserProperties()
            UpdateTicketProperties()
            ApplyDemoContentConfiguration()
            ClearContentConfiguration()
            LogUserEvent()
            GetUser()
            Configure()
            ResetUser()
            LoadAccount()
//            Ticket Forms SDK commented
//            OpenArticle()
//            TicketForm()
        }
    }

    @Composable
    fun GetUser() {
        val openUserDetailsDialog = rememberSaveable { mutableStateOf(false) }
        val user = rememberSaveable { mutableStateOf<User?>(null) }
        ButtonText(textId = R.string.get_user) {
            FreshdeskSDK.getUser(
                userCallback = {
                    Log.d(TAG, "User details: $it")
                    user.value = it
                    openUserDetailsDialog.value = true
                },
                onFailure = {
                    Log.d(TAG, "Get User Failure: $it")
                    user.value = null
                    openUserDetailsDialog.value = true
                }
            )
        }
        if (openUserDetailsDialog.value) {
            UserDetailDialog(
                user = user.value,
                config = DialogConfig(title = R.string.user_details, positiveText = R.string.ok),
                onDismissed = { openUserDetailsDialog.value = false }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OpenSupport() {
        val unreadCount = rememberSaveable { mutableStateOf("0") }
        (applicationContext as SouthWestApp).unreadCount.observeAsState().value?.let {
            unreadCount.value = it.toString()
        }

        BadgedBox(
            badge = {
                Badge(modifier = Modifier.padding(top = 10.dp)) {
                    Text(text = unreadCount.value)
                }
            },
            modifier = Modifier.padding(8.dp)
        ) {
            ButtonText(textId = R.string.open_support) {
                FreshdeskSDK.openSupport(this@SettingsActivity)
            }
        }
    }

    /*Ticket Forms SDK commented
    @Composable
    fun TicketForm() {
        ButtonText(text = TicketFormTestActivity.MENU_LABEL) {
            startActivity(Intent(this@SettingsActivity, TicketFormTestActivity::class.java))
        }
    }
     */

    @Composable
    fun OpenTopic() {
        val openDialog = rememberSaveable { mutableStateOf(false) }
        if (openDialog.value) {
            TextFieldDialog(
                config = DialogConfig(),
                textField1 = Pair(R.string.topic_name, DataStore.getConvTopicName()),
                textField2 = Pair(R.string.topic_id, DataStore.getConvTopicId()),
                onConfirmed = {
                    openDialog.value = false
                    DataStore.setConvTopicName(it.field1)
                    DataStore.setConvTopicId(it.field2)
                    FreshdeskSDK.openTopic(
                        this@SettingsActivity,
                        it.field1,
                        it.field2
                    )
                }, onDismissed = {
                    openDialog.value = false
                }
            )
        }
        ButtonText(textId = R.string.open_topic) {
            openDialog.value = true
        }
    }

    /*Ticket Forms SDK commented
    @Composable
    fun OpenArticle() {
        val openDialog = rememberSaveable { mutableStateOf(false) }
        if (openDialog.value) {
            TextFieldDialog(
                config = DialogConfig(title = R.string.open_article),
                textField1 = Pair(R.string.article_id, "4374493"),
                onConfirmed = {
                    openDialog.value = false
                    val articleId = it.field1.toIntOrNull()
                    if (articleId == null) {
                        toast(getString(R.string.inbound_event_invalid_input))
                    } else {
                        FreshdeskSDK.openArticle(this@SettingsActivity, articleId)
                    }
                },
                onDismissed = {
                    openDialog.value = false
                }
            )
        }
        ButtonText(textId = R.string.open_article) {
            openDialog.value = true
        }
    }
     */

    @Composable
    private fun ShowKnowledgeBase() {
        ButtonText(textId = R.string.show_knowledge_base) {
            FreshdeskSDK.openKnowledgeBase(this@SettingsActivity)
        }
    }

    @Composable
    fun UpdateTicketProperties() {
        val openDialog = rememberSaveable { mutableStateOf(false) }
        if (openDialog.value) {
            UpdateTicketDialog(onConfirmed = { ticketPropertiesMap, jwt ->
                if (ticketPropertiesMap.isNotEmpty()) {
                    FreshdeskSDK.setTicketProperties(ticketPropertiesMap)
                } else {
                    FreshdeskSDK.authenticateAndUpdate(jwt)
                }
                openDialog.value = false
            }) {
                openDialog.value = false
            }
        }
        ButtonText(textId = R.string.update_ticket_properties) {
            openDialog.value = true
        }
    }

    @Composable
    private fun ApplyDemoContentConfiguration() {
        val openDialog = rememberSaveable { mutableStateOf(false) }
        if (openDialog.value) {
            ContentConfigurationDialog(
                onDismiss = { openDialog.value = false },
                onApplyDemo = {
                    FreshdeskSDK.setContentConfiguration(demoContentConfiguration())
                    toast(getString(R.string.toast_demo_content_configuration_applied))
                },
                onApplyCustom = { configuration ->
                    FreshdeskSDK.setContentConfiguration(configuration)
                    toast(getString(R.string.toast_custom_content_configuration_applied))
                },
            )
        }
        ButtonText(textId = R.string.set_demo_content_configuration) {
            openDialog.value = true
        }
    }

    @Composable
    private fun ClearContentConfiguration() {
        ButtonText(textId = R.string.clear_content_configuration) {
            FreshdeskSDK.setContentConfiguration(null)
            toast(getString(R.string.toast_content_configuration_cleared))
        }
    }

    @Composable
    fun UpdateUserProperties() {
        val openDialog = rememberSaveable { mutableStateOf(false) }
        if (openDialog.value) {
            UpdateUserDialog(onConfirmed = { userPropertiesMap, jwt ->
                if (userPropertiesMap.isNotEmpty()) {
                    FreshdeskSDK.setUserProperties(userPropertiesMap)
                } else {
                    FreshdeskSDK.authenticateAndUpdate(jwt)
                }
                openDialog.value = false
            }) {
                openDialog.value = false
            }
        }
        ButtonText(textId = R.string.update_properties) {
            openDialog.value = true
        }
    }

    @Composable
    fun ResetUser() {
        val resetDialog = rememberSaveable { mutableStateOf(false) }
        ButtonText(textId = R.string.reset_user) {
            resetDialog.value = true
            FreshdeskSDK.resetUser({
                resetDialog.value = false
                (application as SouthWestApp).clearUserState()
                toast(it)
                clearUser()
            }) {
                (application as SouthWestApp).clearUserState()
                resetDialog.value = false
                toast(it)
            }
        }
        if (resetDialog.value) {
            ProgressBarDialog(R.string.resetting_user) {
                resetDialog.value = false
            }
        }
    }

    @Composable
    fun UpdateJWT() {
        val fetching = stringResource(id = R.string.undefined)
        val userState = rememberSaveable { mutableStateOf(fetching) }
        val openDialog = rememberSaveable { mutableStateOf(false) }
        val account = getSelectedAccount()
        (application as SouthWestApp).userState.observeAsState().value?.let { state ->
            userState.value = state.ifEmpty { fetching }
            logd { "User State observed: $state" }
        }
        Button(
            onClick = {
                openDialog.value = true
            }, shape = ShapeDefaults.Large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.update_jwt),
                fontWeight = FontWeight.SemiBold
            )
        }
        if (openDialog.value) {
            UpdateAuthTokenDialog(
                userState = userState.value,
                authTokenValue = getSelectedAccount().jwt ?: "",
                onValueChange = {
                    setSelectedAccount(account = account.copy(jwt = it))
                    FreshdeskSDK.authenticateAndUpdate(it)
                    openDialog.value = false
                }
            ) {
                openDialog.value = false
            }
        }
    }

    @Composable
    fun LoadAccount() {
        val openLoadAccountDialog = rememberSaveable { mutableStateOf(false) }

        if (openLoadAccountDialog.value) {
            LoadAccountForm(getSelectedAccount(), { config ->
                openLoadAccountDialog.value = false
                val initNew = fun() {
                    setSelectedAccount(config)
                    FreshdeskSDK.initialize(
                        this@SettingsActivity,
                        config
                    ) {
                        toast("SDK initialized with new SDK configuration")
                    }
                }

                logi { "Resetting current user and loading new SDK configuration" }
                FreshdeskSDK.resetUser({
                    (application as SouthWestApp).clearUserState()
                    clearUser()
                    logi { "User reset" }
                    initNew()
                }) {
                    clearUser()
                    (application as SouthWestApp).clearUserState()
                    initNew()
                    loge { "Unable to reset User $it" }
                }
            }) {
                openLoadAccountDialog.value = false
            }
        }
        ButtonText(textId = R.string.load_account) {
            openLoadAccountDialog.value = true
        }
    }

    @Composable
    private fun LogUserEvent() {
        val logUserEventDialog = rememberSaveable { mutableStateOf(false) }
        if (logUserEventDialog.value) {
            TextFieldDialog(
                config = DialogConfig(
                    title = R.string.log_user_event,
                    positiveText = R.string.send,
                    showDescription = true
                ),
                textField1 = Pair(R.string.event_name, DataStore.getInboundEventName()),
                textField2 = Pair(R.string.event_value, DataStore.getInboundEventData()),
                onConfirmed = {
                    logUserEventDialog.value = false
                    if (it.field1.isEmpty()) {
                        toast(getString(R.string.inbound_event_invalid_input))
                    } else {
                        val message = getString(R.string.event_sent, it.field1)
                        DataStore.saveInboundEventName(it.field1)
                        DataStore.saveInboundEventData(it.field2)
                        if (it.field2.isEmpty()) {
                            FreshdeskSDK.trackEvent(it.field1, emptyMap())
                            toast(message)
                        } else {
                            val eventValue = it.field2.toMap()
                            if (eventValue.isNotEmpty()) {
                                FreshdeskSDK.trackEvent(it.field1, it.field2.toMap())
                                toast(message)
                            } else {
                                toast(getString(R.string.inbound_event_invalid_input))
                            }
                        }
                    }
                },
                onDismissed = { logUserEventDialog.value = false }
            )
        }
        ButtonText(textId = R.string.log_user_event) { logUserEventDialog.value = true }
    }

    @Composable
    fun DismissFreshdeskSDK() {
        ButtonText(textId = R.string.dismiss_freshdesk_sdk) {
            toast("Freshdesk SDK will close automatically in 10 seconds.")
            Handler(Looper.getMainLooper()).postDelayed({
                toast("The Freshdesk SDK closed automatically after 10 seconds.")
                Log.d(TAG, "DismissFreshdeskSDK")
                FreshdeskSDK.dismissFreshdeskViews()
            }, DELAY_MILLIS)
            FreshdeskSDK.openSupport(this@SettingsActivity)
        }
    }

    @Composable
    fun Configure() {
        val openSwitcherDialog = rememberSaveable { mutableStateOf(false) }
        if (openSwitcherDialog.value) {
            ConfigureSwitcherDialog(
                title = R.string.configurations,
                onConfirmed = { state ->
                    openSwitcherDialog.value = false
                    saveToastEventsState(state.toastEventsState)
                    saveUserActionState(state.userActionState)
                    setCustomLinkHandlerState(state.customLinkHandlerState)
                    if (state.customLinkHandlerState) {
                        setLinkHandler(this)
                    } else {
                        setLinkHandler(null)
                    }
                },
                onDismissed = { openSwitcherDialog.value = false }
            )
        }
        ButtonText(textId = R.string.switcher_dialog) {
            openSwitcherDialog.value = true
        }
    }

    override fun handleLink(url: String?) {
        toast("Link handled by custom handler:\n$url")
    }
}
package com.freshdesk.southwest.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshdesk.southwest.R
import com.freshdesk.southwest.components.FormField
import com.freshdesk.southwest.components.buttons.ConfirmButton
import com.freshdesk.southwest.components.buttons.DismissButton
import com.freshworks.sdk.freshdesk.data.content.ChannelResponseContent
import com.freshworks.sdk.freshdesk.data.content.ContentConfiguration
import com.freshworks.sdk.freshdesk.data.content.HeaderContent
import com.freshworks.sdk.freshdesk.data.content.PlaceholderContent
import com.freshworks.sdk.freshdesk.data.content.PrivacyPolicyContent
import com.freshworks.sdk.freshdesk.data.content.TicketFormContent

private class ContentConfigurationDialogController {
    val useBuiltInDemo = mutableStateOf(false)
    val chat = mutableStateOf(ContentConfigurationDemoPresets.CHAT)
    val faq = mutableStateOf(ContentConfigurationDemoPresets.FAQ)
    val faqMessageUs = mutableStateOf(ContentConfigurationDemoPresets.FAQ_MESSAGE_US)
    val channelOffline = mutableStateOf(ContentConfigurationDemoPresets.CHANNEL_OFFLINE)
    val ticketTitle = mutableStateOf(ContentConfigurationDemoPresets.TICKET_TITLE)
    val ticketListTitle = mutableStateOf(ContentConfigurationDemoPresets.TICKET_LIST_TITLE)
    val ticketSubmit = mutableStateOf(ContentConfigurationDemoPresets.TICKET_SUBMIT)
    val ticketConfirmation = mutableStateOf(ContentConfigurationDemoPresets.TICKET_CONFIRMATION)
    val placeholderReply = mutableStateOf(ContentConfigurationDemoPresets.PLACEHOLDER_REPLY)
    val placeholderSearch = mutableStateOf(ContentConfigurationDemoPresets.PLACEHOLDER_SEARCH)
    val privacyMessage = mutableStateOf(ContentConfigurationDemoPresets.PRIVACY_MESSAGE)
    val privacyLinkText = mutableStateOf(ContentConfigurationDemoPresets.PRIVACY_LINK_TEXT)
    val privacyLink = mutableStateOf(ContentConfigurationDemoPresets.PRIVACY_LINK)

    fun resetFieldsToDemo() {
        chat.value = ContentConfigurationDemoPresets.CHAT
        faq.value = ContentConfigurationDemoPresets.FAQ
        faqMessageUs.value = ContentConfigurationDemoPresets.FAQ_MESSAGE_US
        channelOffline.value = ContentConfigurationDemoPresets.CHANNEL_OFFLINE
        ticketTitle.value = ContentConfigurationDemoPresets.TICKET_TITLE
        ticketListTitle.value = ContentConfigurationDemoPresets.TICKET_LIST_TITLE
        ticketSubmit.value = ContentConfigurationDemoPresets.TICKET_SUBMIT
        ticketConfirmation.value = ContentConfigurationDemoPresets.TICKET_CONFIRMATION
        placeholderReply.value = ContentConfigurationDemoPresets.PLACEHOLDER_REPLY
        placeholderSearch.value = ContentConfigurationDemoPresets.PLACEHOLDER_SEARCH
        privacyMessage.value = ContentConfigurationDemoPresets.PRIVACY_MESSAGE
        privacyLinkText.value = ContentConfigurationDemoPresets.PRIVACY_LINK_TEXT
        privacyLink.value = ContentConfigurationDemoPresets.PRIVACY_LINK
    }

    fun buildCustomConfiguration(): ContentConfiguration {
        val ticketForm = TicketFormContent(
            title = ticketTitle.value.nonBlankOrNull(),
            listTitle = ticketListTitle.value.nonBlankOrNull(),
            submitBtnTitle = ticketSubmit.value.nonBlankOrNull(),
            confirmationMessage = ticketConfirmation.value.nonBlankOrNull(),
        ).takeIf {
            listOf(ticketTitle.value, ticketListTitle.value, ticketSubmit.value, ticketConfirmation.value)
                .any { it.isNotBlank() }
        }
        val headers = HeaderContent(
            chat = chat.value.nonBlankOrNull(),
            faq = faq.value.nonBlankOrNull(),
            faqMessageUs = faqMessageUs.value.nonBlankOrNull(),
            channelResponse = channelOffline.value.nonBlankOrNull()?.let { ChannelResponseContent(it) },
            ticketForm = ticketForm,
        ).takeIf {
            chat.value.isNotBlank() || faq.value.isNotBlank() || faqMessageUs.value.isNotBlank() ||
                channelOffline.value.isNotBlank() || ticketForm != null
        }
        val placeholders = PlaceholderContent(
            replyField = placeholderReply.value.nonBlankOrNull(),
            searchField = placeholderSearch.value.nonBlankOrNull(),
        ).takeIf { placeholderReply.value.isNotBlank() || placeholderSearch.value.isNotBlank() }
        val privacy = PrivacyPolicyContent(
            privacyPolicyMessage = privacyMessage.value.nonBlankOrNull(),
            privacyPolicyLinkText = privacyLinkText.value.nonBlankOrNull(),
            privacyPolicyLink = privacyLink.value.nonBlankOrNull(),
        ).takeIf {
            privacyMessage.value.isNotBlank() || privacyLinkText.value.isNotBlank() ||
                privacyLink.value.isNotBlank()
        }
        return ContentConfiguration(
            headers = headers,
            placeholders = placeholders,
            privacyPolicySetting = privacy,
        )
    }
}

private fun String.nonBlankOrNull(): String? = takeIf { it.isNotBlank() }

@Composable
private fun rememberContentConfigurationDialogController() =
    remember { ContentConfigurationDialogController() }

@Composable
private fun ContentConfigurationDialogScrollableFields(c: ContentConfigurationDialogController) {
    Column(
        modifier = Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        FormField(R.string.content_cfg_chat, c.chat)
        FormField(R.string.content_cfg_faq, c.faq)
        FormField(R.string.content_cfg_faq_message_us, c.faqMessageUs)
        FormField(R.string.content_cfg_channel_offline, c.channelOffline)
        FormField(R.string.content_cfg_ticket_title, c.ticketTitle)
        FormField(R.string.content_cfg_ticket_list_title, c.ticketListTitle)
        FormField(R.string.content_cfg_ticket_submit, c.ticketSubmit)
        FormField(R.string.content_cfg_ticket_confirmation, c.ticketConfirmation)
        FormField(R.string.content_cfg_placeholder_reply, c.placeholderReply)
        FormField(R.string.content_cfg_placeholder_search, c.placeholderSearch)
        FormField(R.string.content_cfg_privacy_message, c.privacyMessage)
        FormField(R.string.content_cfg_privacy_link_text, c.privacyLinkText)
        FormField(R.string.content_cfg_privacy_link, c.privacyLink)
    }
}

@Composable
private fun FormField(labelId: Int, state: MutableState<String>) {
    FormField(
        labelId = labelId,
        value = state.value,
        onValueChange = { state.value = it },
    )
}

@Composable
private fun ContentConfigurationDialogBody(c: ContentConfigurationDialogController) {
    Column {
        Text(
            text = stringResource(R.string.content_configuration_dialog_hint),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { c.useBuiltInDemo.value = !c.useBuiltInDemo.value }
                .padding(vertical = 4.dp),
        ) {
            Checkbox(
                checked = c.useBuiltInDemo.value,
                onCheckedChange = { c.useBuiltInDemo.value = it },
            )
            Text(
                text = stringResource(R.string.content_configuration_use_demo_checkbox),
                modifier = Modifier.clickable {
                    c.useBuiltInDemo.value = !c.useBuiltInDemo.value
                },
            )
        }
        if (!c.useBuiltInDemo.value) {
            TextButton(onClick = { c.resetFieldsToDemo() }) {
                Text(stringResource(R.string.content_configuration_reset_fields))
            }
            ContentConfigurationDialogScrollableFields(c)
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ContentConfigurationDialog(
    onDismiss: () -> Unit,
    onApplyDemo: () -> Unit,
    onApplyCustom: (ContentConfiguration) -> Unit,
) {
    val c = rememberContentConfigurationDialogController()
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = stringResource(R.string.content_configuration_dialog_title),
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = { ContentConfigurationDialogBody(c) },
        confirmButton = {
            ConfirmButton(R.string.content_configuration_apply) {
                if (c.useBuiltInDemo.value) {
                    onApplyDemo()
                } else {
                    onApplyCustom(c.buildCustomConfiguration())
                }
                onDismiss()
            }
        },
        dismissButton = {
            DismissButton(onDismiss)
        },
    )
}

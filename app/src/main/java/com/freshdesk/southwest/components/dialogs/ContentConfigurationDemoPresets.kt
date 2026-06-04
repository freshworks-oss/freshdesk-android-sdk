package com.freshdesk.southwest.components.dialogs

import com.freshworks.sdk.freshdesk.data.content.ChannelResponseContent
import com.freshworks.sdk.freshdesk.data.content.ChannelResponseOnlineContent
import com.freshworks.sdk.freshdesk.data.content.ContentConfiguration
import com.freshworks.sdk.freshdesk.data.content.HeaderContent
import com.freshworks.sdk.freshdesk.data.content.PlaceholderContent
import com.freshworks.sdk.freshdesk.data.content.PrivacyPolicyContent
import com.freshworks.sdk.freshdesk.data.content.TicketFormContent
import com.freshworks.sdk.freshdesk.data.content.TimeUnitTemplates

/** Default strings used for the built-in demo and as initial field values. */
object ContentConfigurationDemoPresets {
    const val CHAT = "Talk to our team (demo)"
    const val FAQ = "Help articles (demo)"
    const val FAQ_MESSAGE_US = "Message us (demo)"
    const val CHANNEL_OFFLINE = "We are offline — leave a message (demo)"
    const val RESPONSE_DEFAULT = "We typically reply in a few minutes (demo)"
    const val RESPONSE_FALLBACK = "We usually reply soon (demo)"
    const val RESPONSE_MINUTE_ONE = "Typically replies in {{time}} minute (demo)"
    const val RESPONSE_MINUTE_MORE = "Typically replies in {{time}} minutes (demo)"
    const val TICKET_TITLE = "Ticket form (demo)"
    const val TICKET_LIST_TITLE = "Your requests (demo)"
    const val TICKET_SUBMIT = "Send (demo)"
    const val TICKET_CONFIRMATION = "Thanks, we received it (demo)"
    const val PLACEHOLDER_REPLY = "Type your reply… (demo)"
    const val PLACEHOLDER_SEARCH = "Search articles… (demo)"
    const val PRIVACY_MESSAGE = "We respect your privacy (demo)"
    const val PRIVACY_LINK_TEXT = "Privacy policy (demo)"
    const val PRIVACY_LINK = "https://example.com/privacy"
}

fun demoContentConfiguration(): ContentConfiguration =
    ContentConfiguration(
        headers = HeaderContent(
            chat = ContentConfigurationDemoPresets.CHAT,
            faq = ContentConfigurationDemoPresets.FAQ,
            faqMessageUs = ContentConfigurationDemoPresets.FAQ_MESSAGE_US,
            typicallyRepliesFewMinsFallback = ContentConfigurationDemoPresets.RESPONSE_FALLBACK,
            channelResponse = ChannelResponseContent(
                offline = ContentConfigurationDemoPresets.CHANNEL_OFFLINE,
                online = ChannelResponseOnlineContent(
                    default = ContentConfigurationDemoPresets.RESPONSE_DEFAULT,
                    minutes = TimeUnitTemplates(
                        one = ContentConfigurationDemoPresets.RESPONSE_MINUTE_ONE,
                        more = ContentConfigurationDemoPresets.RESPONSE_MINUTE_MORE,
                    ),
                ),
            ),
            ticketForm = TicketFormContent(
                title = ContentConfigurationDemoPresets.TICKET_TITLE,
                listTitle = ContentConfigurationDemoPresets.TICKET_LIST_TITLE,
                submitBtnTitle = ContentConfigurationDemoPresets.TICKET_SUBMIT,
                confirmationMessage = ContentConfigurationDemoPresets.TICKET_CONFIRMATION,
            ),
        ),
        placeholders = PlaceholderContent(
            replyField = ContentConfigurationDemoPresets.PLACEHOLDER_REPLY,
            searchField = ContentConfigurationDemoPresets.PLACEHOLDER_SEARCH,
        ),
        privacyPolicySetting = PrivacyPolicyContent(
            privacyPolicyMessage = ContentConfigurationDemoPresets.PRIVACY_MESSAGE,
            privacyPolicyLinkText = ContentConfigurationDemoPresets.PRIVACY_LINK_TEXT,
            privacyPolicyLink = ContentConfigurationDemoPresets.PRIVACY_LINK,
        ),
    )

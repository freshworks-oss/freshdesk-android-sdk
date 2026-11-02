package com.freshdesk.southwest.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.freshdesk.southwest.components.buttons.ButtonText
import com.freshdesk.southwest.components.dialogs.TicketFormDialog
import com.freshdesk.southwest.components.dialogs.TicketFormDialogConfig
import com.freshdesk.southwest.data.TicketFormSamples
import com.freshdesk.southwest.extensions.applySDK35InsetsListener
import com.freshdesk.southwest.ui.theme.SouthWestTheme
import com.freshdesk.southwest.utils.TicketFormJsonParser
import com.freshdesk.southwest.utils.toast
import com.freshworks.sdk.freshdesk.FreshdeskSDK

class TicketFormActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SouthWestTheme {
                Scaffold(
                    topBar = {
                        Row(modifier = Modifier.padding(end = 40.dp)) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = SCREEN_TITLE,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                navigationIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_back_arrow),
                                        contentDescription = stringResource(id = R.string.back),
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .clickable { onBackPressed() },
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                            )
                        }
                    },
                ) { paddingValues -> TicketFormTestContent(paddingValues) }
            }
        }
        applySDK35InsetsListener()
    }

    @Composable
    private fun TicketFormTestContent(paddingValues: PaddingValues) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OpenTicketFormButton()
            ClearTicketFormButton()
            PrefillTicketFormButton()
            DisableTicketFormFieldsButton()
            HideTicketFormFieldsButton()
            HideTicketFieldChoicesButton()
        }
    }

    @Composable
    private fun OpenTicketFormButton() {
        TicketFormAction(
            buttonText = OPEN_TICKET_FORM,
            dialogConfig = TicketFormDialogConfig(
                title = OPEN_TICKET_FORM,
                positiveText = OPEN,
                formIdOptional = true,
            ),
        ) { formId, _ ->
            FreshdeskSDK.openTicketForm(
                this@TicketFormActivity,
                formId.ifBlank { null },
            )
            toast(actionSent(OPEN_TICKET_FORM))
        }
    }

    @Composable
    private fun ClearTicketFormButton() {
        TicketFormAction(
            buttonText = CLEAR_TICKET_FORM,
            dialogConfig = TicketFormDialogConfig(
                title = CLEAR_TICKET_FORM,
                positiveText = CLEAR,
            ),
        ) { formId, _ ->
            FreshdeskSDK.clearTicketForm(this@TicketFormActivity, formId)
            toast(actionSent(CLEAR_TICKET_FORM))
        }
    }

    @Composable
    private fun PrefillTicketFormButton() {
        TicketFormAction(
            buttonText = PREFILL_TICKET_FORM,
            dialogConfig = TicketFormDialogConfig(
                title = PREFILL_TICKET_FORM,
                positiveText = PREFILL,
                initialPayload = TicketFormSamples.defaultPrefillFieldValuesJson(),
                showPayload = true,
                payloadLabel = FIELD_VALUES,
                description = PREFILL_DESCRIPTION,
            ),
        ) { formId, payload ->
            val fieldValues = TicketFormJsonParser.parseFieldValues(payload)
            if (fieldValues.isNullOrEmpty()) {
                toast(INVALID_JSON)
                return@TicketFormAction
            }
            FreshdeskSDK.prefillTicketForm(this@TicketFormActivity, formId, fieldValues)
            toast(actionSent(PREFILL_TICKET_FORM))
        }
    }

    @Composable
    private fun DisableTicketFormFieldsButton() {
        TicketFormAction(
            buttonText = DISABLE_TICKET_FORM_FIELDS,
            dialogConfig = TicketFormDialogConfig(
                title = DISABLE_TICKET_FORM_FIELDS,
                positiveText = DISABLE,
                initialPayload = TicketFormSamples.defaultFieldsListJson(),
                showPayload = true,
                payloadLabel = FIELDS_LIST,
                description = FIELDS_LIST_DESCRIPTION,
            ),
        ) { formId, payload ->
            invokeFieldsListApi(formId, payload, DISABLE_TICKET_FORM_FIELDS) { id, fields ->
                FreshdeskSDK.disableTicketFormFields(this@TicketFormActivity, id, fields)
            }
        }
    }

    @Composable
    private fun HideTicketFormFieldsButton() {
        TicketFormAction(
            buttonText = HIDE_TICKET_FORM_FIELDS,
            dialogConfig = TicketFormDialogConfig(
                title = HIDE_TICKET_FORM_FIELDS,
                positiveText = HIDE,
                initialPayload = TicketFormSamples.defaultFieldsListJson(),
                showPayload = true,
                payloadLabel = FIELDS_LIST,
                description = FIELDS_LIST_DESCRIPTION,
            ),
        ) { formId, payload ->
            invokeFieldsListApi(formId, payload, HIDE_TICKET_FORM_FIELDS) { id, fields ->
                FreshdeskSDK.hideTicketFormFields(this@TicketFormActivity, id, fields)
            }
        }
    }

    @Composable
    private fun HideTicketFieldChoicesButton() {
        TicketFormAction(
            buttonText = HIDE_TICKET_FIELD_CHOICES,
            dialogConfig = TicketFormDialogConfig(
                title = HIDE_TICKET_FIELD_CHOICES,
                positiveText = HIDE_CHOICES,
                initialPayload = TicketFormSamples.defaultHideChoicesJson(),
                showPayload = true,
                payloadLabel = CHOICES_PAYLOAD,
                description = CHOICES_DESCRIPTION,
            ),
        ) { formId, payload ->
            val choices = TicketFormJsonParser.parseChoices(payload)
            if (choices.isNullOrEmpty()) {
                toast(INVALID_JSON)
                return@TicketFormAction
            }
            FreshdeskSDK.hideTicketFormChoiceFields(this@TicketFormActivity, formId, choices)
            toast(actionSent(HIDE_TICKET_FIELD_CHOICES))
        }
    }

    private fun invokeFieldsListApi(
        formId: String,
        payload: String,
        actionName: String,
        invoke: (formId: String, fields: List<String>) -> Unit,
    ) {
        val fields = TicketFormJsonParser.parseFieldNames(payload)
        if (fields.isNullOrEmpty()) {
            toast(INVALID_JSON)
            return
        }
        invoke(formId, fields)
        toast(actionSent(actionName))
    }

    @Composable
    private fun TicketFormAction(
        buttonText: String,
        dialogConfig: TicketFormDialogConfig,
        onSubmit: (formId: String, payload: String) -> Unit,
    ) {
        val openDialog = rememberSaveable { mutableStateOf(false) }
        ButtonText(text = buttonText) {
            openDialog.value = true
        }
        if (openDialog.value) {
            TicketFormDialog(
                config = dialogConfig,
                onConfirmed = { formId, payload ->
                    if (!dialogConfig.formIdOptional && formId.isBlank()) {
                        toast(INVALID_FORM_ID)
                        return@TicketFormDialog
                    }
                    openDialog.value = false
                    onSubmit(formId, payload)
                },
                onDismissed = { openDialog.value = false },
            )
        }
    }

    private fun actionSent(action: String) = "$action sent"

    companion object {
        const val SCREEN_TITLE = "Ticket Forms"
        const val MENU_LABEL = "Ticket Forms"

        private const val OPEN_TICKET_FORM = "Open Ticket Form"
        private const val CLEAR_TICKET_FORM = "Clear Ticket Form"
        private const val PREFILL_TICKET_FORM = "Prefill Ticket Form"
        private const val DISABLE_TICKET_FORM_FIELDS = "Disable Ticket Form Fields"
        private const val HIDE_TICKET_FORM_FIELDS = "Hide Ticket Form Fields"
        private const val HIDE_TICKET_FIELD_CHOICES = "Hide Ticket Field Choices"

        private const val OPEN = "Open"
        private const val CLEAR = "Clear"
        private const val PREFILL = "Prefill"
        private const val DISABLE = "Disable"
        private const val HIDE = "Hide"
        private const val HIDE_CHOICES = "Hide Choices Fields"

        private const val FIELD_VALUES = "Field values"
        private const val FIELDS_LIST = "Fields list"
        private const val CHOICES_PAYLOAD = "Choices payload"

        private const val FIELDS_LIST_DESCRIPTION =
            "Enter a JSON array of field names ([String]). " +
                "Example: [\"subject\", \"description\", \"group\", \"priority\", \"product\", " +
                "\"ticket_type\", \"custom_fields.cf_reference_number\", \"custom_fields.cf_decimal\", " +
                "\"custom_fields.cf_date\", \"custom_fields.cf_checkbox\", \"custom_fields.cf_dp\"]"

        private const val PREFILL_DESCRIPTION =
            "Enter a JSON object of field names and values to prefill. " +
                "Example keys: subject, description, group, priority, product, status, ticket_type, customFields."

        private const val CHOICES_DESCRIPTION =
            "Enter a JSON object mapping fields to the choice values to hide. " +
                "Example: {\"priority\": [2, 3], \"custom_fields\": {\"cf_dp\": [\"Second Choice\"]}}"

        private const val INVALID_FORM_ID = "Enter a valid Form ID"
        private const val INVALID_JSON = "Invalid JSON payload"
    }
}

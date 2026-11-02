package com.freshdesk.southwest.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshdesk.southwest.components.buttons.ClearButton
import com.freshdesk.southwest.components.buttons.DismissButton
import com.freshdesk.southwest.data.TicketFormSamples
import com.freshdesk.southwest.ui.theme.FormFieldTheme

@Composable
fun TicketFormDialog(
    config: TicketFormDialogConfig,
    onConfirmed: (formId: String, payload: String) -> Unit,
    onDismissed: () -> Unit,
) {
    val formId = rememberSaveable { mutableStateOf(config.initialFormId) }
    val payload = rememberSaveable { mutableStateOf(config.initialPayload) }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = config.title,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = { TicketFormDialogBody(config, formId, payload) },
        confirmButton = {
            Button(
                onClick = { onConfirmed(formId.value.trim(), payload.value.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text(text = config.positiveText)
            }
        },
        dismissButton = {
            DismissButton { onDismissed() }
        },
    )
}

@Composable
private fun TicketFormDialogBody(
    config: TicketFormDialogConfig,
    formId: MutableState<String>,
    payload: MutableState<String>,
) {
    // Cap height like other harness dialogs so the dialog scrolls as a whole, while the
    // multiline JSON field consumes nested scroll (same idea as MultiLineFormField usage
    // in UpdateCustomPropertiesDialog on structuredBot).
    Column(
        modifier = Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TicketFormIdField(
            label = config.formIdLabel,
            value = formId.value,
            onValueChange = { formId.value = it },
            trailingIcon = {
                if (formId.value.isNotEmpty()) {
                    ClearButton { formId.value = "" }
                }
            },
        )
        config.description?.let { description ->
            Text(
                text = description,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (config.showPayload && config.payloadLabel != null) {
            TicketFormTextField(
                label = config.payloadLabel,
                value = payload.value,
                singleLine = false,
                onValueChange = { payload.value = it },
                trailingIcon = {
                    if (payload.value.isNotEmpty()) {
                        ClearButton { payload.value = "" }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TicketFormIdField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    FormFieldTheme {
        OutlinedTextField(
            value = value,
            onValueChange = { incoming ->
                // Reject mixed input (e.g. "12-34") instead of silently becoming "1234".
                if (incoming.all { char -> char.isDigit() }) {
                    onValueChange(incoming)
                }
            },
            label = { Text(text = label) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            singleLine = true,
            trailingIcon = trailingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .focusProperties {
                    up = FocusRequester.Cancel
                    down = FocusRequester.Cancel
                    left = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TicketFormTextField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    // Empty connection: multiline field keeps pointer/scroll gestures so backspace, selection,
    // and clear taps are not stolen by the parent verticalScroll (common Compose dialog issue).
    val nestedScrollConnection = remember { object : NestedScrollConnection {} }

    FormFieldTheme {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            singleLine = singleLine,
            trailingIcon = trailingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .then(
                    if (singleLine) {
                        Modifier
                    } else {
                        // Keep arrow keys for caret/line navigation inside the editor instead of
                        // Compose focus traversal jumping between FormId and the payload field.
                        Modifier
                            .height(200.dp)
                            .nestedScroll(nestedScrollConnection)
                            .focusProperties {
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            }
                    },
                ),
        )
    }
}

data class TicketFormDialogConfig(
    val title: String,
    val positiveText: String,
    val formIdLabel: String = "FormId",
    val initialFormId: String = TicketFormSamples.SAMPLE_FORM_ID,
    val initialPayload: String = "",
    val showPayload: Boolean = false,
    val payloadLabel: String? = null,
    val description: String? = null,
    val formIdOptional: Boolean = false,
)

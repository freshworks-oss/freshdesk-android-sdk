package com.freshdesk.southwest.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                TicketFormTextField(
                    label = config.formIdLabel,
                    value = formId.value,
                    onValueChange = { formId.value = it },
                    trailingIcon = { ClearButton { formId.value = "" } },
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
                        trailingIcon = { ClearButton { payload.value = "" } },
                    )
                }
            }
        },
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
private fun TicketFormTextField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable () -> Unit,
) {
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
                    if (singleLine) Modifier else Modifier.height(200.dp),
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

package com.freshdesk.southwest.components.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.freshdesk.southwest.components.buttons.ConfirmButton
import com.freshdesk.southwest.data.DialogConfig
import com.freshworks.sdk.freshdesk.backend.model.User
import com.freshworks.sdk.freshdesk.backend.model.toDisplayString

@Composable
fun UserDetailDialog(
    user: User?,
    config: DialogConfig,
    onDismissed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
        },
        title = {
            Text(
                text = stringResource(id = config.title),
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            user?.let { user ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    SpaceAndDivider()
                    UserDetailRow("Alias", user.alias)
                    SpaceAndDivider()
                    UserDetailRow("First Name", user.firstName)
                    SpaceAndDivider()
                    UserDetailRow("Last Name", user.lastName)
                    SpaceAndDivider()
                    UserDetailRow("Email", user.email)
                    SpaceAndDivider()
                    UserDetailRow("Phone Country Code", user.phoneCountryCode)
                    SpaceAndDivider()
                    UserDetailRow("Phone", user.phone)
                    SpaceAndDivider()
                    UserDetailRow("Rule Id", user.ruleId)
                    SpaceAndDivider()
                    user.customProps.forEach { (key, value) ->
                        UserDetailRow(key, value.toDisplayString())
                        SpaceAndDivider()
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "User Not Found",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        },
        confirmButton = {
            ConfirmButton(config.positiveText) {
                onDismissed.invoke()
            }
        }
    )
}

@Composable
fun UserDetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Text(
            text = if (value.isNullOrEmpty()) "-" else value,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.horizontalScroll(state = rememberScrollState())
        )
    }
}

@Composable
fun SpaceAndDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}
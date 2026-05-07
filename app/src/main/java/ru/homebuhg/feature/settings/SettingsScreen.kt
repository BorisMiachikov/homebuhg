package ru.homebuhg.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import ru.homebuhg.BuildConfig
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurringRules: () -> Unit,
    onOpenSmsRules: () -> Unit,
    onOpenExport: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var smsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsPermissionGranted = granted
        if (granted) onOpenSmsRules()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = { Text("Категории и магазины") },
                leadingContent = { Icon(Icons.Outlined.Category, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenCategories)
            )
            ListItem(
                headlineContent = { Text("Бюджеты и лимиты") },
                leadingContent = { Icon(Icons.Outlined.Savings, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenBudgets)
            )
            ListItem(
                headlineContent = { Text("Регулярные операции") },
                leadingContent = { Icon(Icons.Outlined.Repeat, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenRecurringRules)
            )
            ListItem(
                headlineContent = { Text("Правила SMS") },
                supportingContent = if (!smsPermissionGranted) {
                    {
                        Text(
                            "Требуется разрешение на получение SMS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else null,
                leadingContent = { Icon(Icons.Outlined.Message, contentDescription = null) },
                modifier = Modifier.clickable {
                    if (smsPermissionGranted) onOpenSmsRules()
                    else smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                }
            )
            ListItem(
                headlineContent = { Text("Экспорт данных") },
                leadingContent = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenExport)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Синхронизация",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when {
                !uiState.isFirebaseAvailable -> FirebaseUnavailableCard()
                uiState.isSignedIn -> SignedInSection(uiState, viewModel)
                else -> SignInSection(viewModel, uiState.error)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "HomeBuhg v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun FirebaseUnavailableCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text("Firebase не настроен", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Добавьте google-services.json для включения синхронизации",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SignedInSection(uiState: SettingsViewModel.UiState, viewModel: SettingsViewModel) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.CloudDone, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        text = uiState.email ?: "Аккаунт",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (uiState.lastSyncMs > 0L) {
                        val fmt = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                        Text(
                            text = "Синхронизировано: ${fmt.format(Date(uiState.lastSyncMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            uiState.error?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::syncNow,
                    enabled = !uiState.isSyncing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Outlined.Cloud, contentDescription = null)
                        Text(" Синхронизировать")
                    }
                }
                OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.weight(1f)) {
                    Text("Выйти")
                }
            }
        }
    }
}

@Composable
private fun SignInSection(viewModel: SettingsViewModel, error: String?) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Войти для синхронизации", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.signIn(email, password) },
                    enabled = email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Войти") }

                OutlinedButton(
                    onClick = { viewModel.signUp(email, password) },
                    enabled = email.isNotBlank() && password.length >= 6,
                    modifier = Modifier.weight(1f)
                ) { Text("Регистрация") }
            }
        }
    }
}

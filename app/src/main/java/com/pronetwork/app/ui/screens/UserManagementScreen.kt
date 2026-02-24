package com.pronetwork.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.network.UserResponse
import com.pronetwork.app.viewmodel.UserManagementUiState
import com.pronetwork.app.network.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    uiState: UserManagementUiState,
    onRefresh: () -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (UserResponse) -> Unit,
    onToggleClick: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit,
    onDismissDialog: () -> Unit,
    onCreateUser: () -> Unit,
    onUpdateUser: () -> Unit,
    onFormUsernameChange: (String) -> Unit,
    onFormPasswordChange: (String) -> Unit,
    onFormDisplayNameChange: (String) -> Unit,
    onFormRoleChange: (String) -> Unit,
    onClearMessages: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val successCreated = stringResource(R.string.user_mgmt_success_created)
    val successUpdated = stringResource(R.string.user_mgmt_success_updated)
    val successDeleted = stringResource(R.string.user_mgmt_success_deleted)
    val successToggled = stringResource(R.string.user_mgmt_success_toggled)

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            val text = when (msg) {
                "USER_CREATED" -> successCreated
                "USER_UPDATED" -> successUpdated
                "USER_DELETED" -> successDeleted
                "USER_TOGGLED" -> successToggled
                else -> msg
            }
            snackbarHostState.showSnackbar(text)
            onClearMessages()
        }
    }

    val errorNoConnection = stringResource(R.string.user_mgmt_error_no_connection)
    val errorNetwork = stringResource(R.string.user_mgmt_error_network)
    val errorNotAdmin = stringResource(R.string.user_mgmt_error_not_admin)
    val errorSessionExpired = stringResource(R.string.user_mgmt_error_session_expired)
    val errorLoadFailed = stringResource(R.string.user_mgmt_error_load_failed)
    val errorDeleteFailed = stringResource(R.string.user_mgmt_error_delete_failed)
    val errorToggleFailed = stringResource(R.string.user_mgmt_error_toggle_failed)
    val errorCannotDeleteSelf = stringResource(R.string.user_mgmt_error_cannot_delete_self)

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null && !uiState.showCreateDialog && !uiState.showEditDialog) {
            val text = when (uiState.errorMessage) {
                "NO_CONNECTION" -> errorNoConnection
                "NETWORK_ERROR" -> errorNetwork
                "NOT_ADMIN" -> errorNotAdmin
                "SESSION_EXPIRED" -> errorSessionExpired
                "LOAD_FAILED" -> errorLoadFailed
                "DELETE_FAILED" -> errorDeleteFailed
                "TOGGLE_FAILED" -> errorToggleFailed
                "CANNOT_DELETE_SELF" -> errorCannotDeleteSelf
                else -> errorNetwork
            }
            snackbarHostState.showSnackbar(text)
            onClearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.user_mgmt_add_user)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.user_mgmt_title, uiState.users.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.user_mgmt_refresh),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (uiState.isLoading && uiState.users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (uiState.users.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.user_mgmt_no_users),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.users, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        onEdit = { onEditClick(user) },
                        onToggle = { onToggleClick(user.id) },
                        onDelete = { showDeleteConfirm = user.id }
                    )
                }
            }
        }
    }

    showDeleteConfirm?.let { userId ->
        val user = uiState.users.find { it.id == userId }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(stringResource(R.string.user_mgmt_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.user_mgmt_delete_confirm_msg,
                        user?.display_name ?: ""
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick(userId)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (uiState.showCreateDialog) {
        UserFormDialog(
            title = stringResource(R.string.user_mgmt_create_title),
            username = uiState.formUsername,
            password = uiState.formPassword,
            displayName = uiState.formDisplayName,
            role = uiState.formRole,
            isUsernameEnabled = true,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onUsernameChange = onFormUsernameChange,
            onPasswordChange = onFormPasswordChange,
            onDisplayNameChange = onFormDisplayNameChange,
            onRoleChange = onFormRoleChange,
            onConfirm = onCreateUser,
            onDismiss = onDismissDialog,
            confirmText = stringResource(R.string.user_mgmt_create_btn)
        )
    }

    if (uiState.showEditDialog) {
        UserFormDialog(
            title = stringResource(R.string.user_mgmt_edit_title),
            username = uiState.formUsername,
            password = uiState.formPassword,
            displayName = uiState.formDisplayName,
            role = uiState.formRole,
            isUsernameEnabled = false,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            passwordHint = stringResource(R.string.user_mgmt_password_hint_edit),
            onUsernameChange = onFormUsernameChange,
            onPasswordChange = onFormPasswordChange,
            onDisplayNameChange = onFormDisplayNameChange,
            onRoleChange = onFormRoleChange,
            onConfirm = onUpdateUser,
            onDismiss = onDismissDialog,
            confirmText = stringResource(R.string.user_mgmt_save_btn)
        )
    }
}

@Composable
private fun UserCard(
    user: UserResponse,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = user.is_active
    val cardColor by animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colorScheme.surface
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "cardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (user.role == "ADMIN")
                                    Color(0xFF673AB7)
                                else
                                    Color(0xFF2196F3)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.display_name.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.display_name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (user.role == "ADMIN")
                        Color(0xFF673AB7).copy(alpha = 0.15f)
                    else
                        Color(0xFF2196F3).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (user.role == "ADMIN")
                            stringResource(R.string.user_mgmt_role_admin)
                        else
                            stringResource(R.string.user_mgmt_role_user),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (user.role == "ADMIN")
                            Color(0xFF673AB7)
                        else
                            Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isActive)
                            stringResource(R.string.user_mgmt_active)
                        else
                            stringResource(R.string.user_mgmt_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
                user.last_login?.let { lastLogin ->
                    Text(
                        text = stringResource(
                            R.string.user_mgmt_last_login,
                            formatDateTime(lastLogin)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggle) {
                    Text(
                        text = if (isActive)
                            stringResource(R.string.user_mgmt_toggle_disable)
                        else
                            stringResource(R.string.user_mgmt_toggle_enable),
                        color = if (isActive)
                            MaterialTheme.colorScheme.error
                        else
                            Color(0xFF4CAF50)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormDialog(
    title: String,
    username: String,
    password: String,
    displayName: String,
    role: String,
    isUsernameEnabled: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    passwordHint: String? = null,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String
) {
    val errorFieldsRequired = stringResource(R.string.user_mgmt_error_fields_required)
    val errorUsernameExists = stringResource(R.string.user_mgmt_error_username_exists)
    val errorCreateFailed = stringResource(R.string.user_mgmt_error_create_failed)
    val errorUpdateFailed = stringResource(R.string.user_mgmt_error_update_failed)
    val errorNoConnection = stringResource(R.string.user_mgmt_error_no_connection)
    val errorNetwork = stringResource(R.string.user_mgmt_error_network)
    val roleAdminLabel = stringResource(R.string.user_mgmt_role_admin)
    val roleUserLabel = stringResource(R.string.user_mgmt_role_user)
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                errorMessage?.let { msg ->
                    val text = when (msg) {
                        "FIELDS_REQUIRED" -> errorFieldsRequired
                        "USERNAME_EXISTS" -> errorUsernameExists
                        "CREATE_FAILED" -> errorCreateFailed
                        "UPDATE_FAILED" -> errorUpdateFailed
                        "NO_CONNECTION" -> errorNoConnection
                        "NETWORK_ERROR" -> errorNetwork
                        else -> errorNetwork
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.user_mgmt_username)) },
                    enabled = isUsernameEnabled && !isLoading,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text(stringResource(R.string.user_mgmt_display_name)) },
                    enabled = !isLoading,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.user_mgmt_password)) },
                    placeholder = passwordHint?.let {
                        { Text(it, style = MaterialTheme.typography.bodySmall) }
                    },
                    enabled = !isLoading,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    stringResource(R.string.user_mgmt_role),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = role == "USER",
                        onClick = { if (!isLoading) onRoleChange("USER") },
                        label = { Text(roleUserLabel) }
                    )
                    FilterChip(
                        selected = role == "ADMIN",
                        onClick = { if (!isLoading) onRoleChange("ADMIN") },
                        label = { Text(roleAdminLabel) }
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun formatDateTime(isoDate: String): String {
    return TimeUtils.utcToLocal(isoDate)
}

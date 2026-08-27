package com.makhp.pelukdiri.features.dashboard

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.features.intervention.InterventionActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSidebar(
    viewModel: ProfileViewModel,
    onClose: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf(false) }
    var showImageSourcePicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfileImage(it.toString()) }
        showImageSourcePicker = false
    }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight().width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        val scrollState = rememberScrollState()
        val context = LocalContext.current
        val isDebug = remember {
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // ... (Profile Image box remains same)
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (uiState.profileImagePath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uiState.profileImagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.profile_image_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp).fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                FloatingActionButton(
                    onClick = { showImageSourcePicker = true },
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            ProfileItem(
                label = stringResource(R.string.profile_nickname),
                value = uiState.nickname.ifEmpty { stringResource(R.string.profile_not_set) },
                onClick = { showEditNameDialog = true }
            )

            Spacer(Modifier.height(16.dp))

            ProfileItem(
                label = stringResource(R.string.profile_username),
                value = uiState.username.ifEmpty { "@username" },
                onClick = { showEditUserDialog = true }
            )

            if (isDebug) {
                Spacer(Modifier.height(32.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Debug Tools",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(12.dp))
                
                DebugActionItem(
                    label = stringResource(R.string.profile_test_lab),
                    icon = Icons.Default.Science,
                    onClick = {
                        try {
                            val intent = Intent().setClassName(
                                context.packageName,
                                "com.makhp.pelukdiri.debug.DebugTestLabActivity"
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if component name changed
                        }
                    }
                )
                
                DebugActionItem(
                    label = stringResource(R.string.profile_test_notification),
                    icon = Icons.Default.Notifications,
                    onClick = { viewModel.triggerTestNotification() }
                )
                
                DebugActionItem(
                    label = stringResource(R.string.profile_test_intervention),
                    icon = Icons.Default.Calculate,
                    onClick = {
                        if (viewModel.tryAcquireInterventionLock()) {
                            onClose()
                            val intent = Intent(context, InterventionActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, context.packageName)
                                putExtra(InterventionActivity.EXTRA_MONITORED_USAGE, 120.0)
                                putExtra(InterventionActivity.EXTRA_INTERVAL_MINUTES_AT_LAUNCH, 10.0)
                                putExtra(InterventionActivity.EXTRA_AMBIENT_LIGHT_LUX_AT_LAUNCH, 100f)
                                putExtra(InterventionActivity.EXTRA_DEVIATION, 0.5)
                                putExtra(InterventionActivity.EXTRA_DIFFICULTY_CONTROL_SIGNAL, 0.7)
                                putExtra(InterventionActivity.EXTRA_DIFFICULTY, 3)
                                putExtra(InterventionActivity.EXTRA_CHALLENGE_TYPE, InterventionChallengeType.MATH.name)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                
                DebugActionItem(
                    label = stringResource(R.string.profile_test_intervention_pattern),
                    icon = Icons.Default.Grid4x4,
                    onClick = {
                        if (viewModel.tryAcquireInterventionLock()) {
                            onClose()
                            val intent = Intent(context, InterventionActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or 
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, context.packageName)
                                putExtra(InterventionActivity.EXTRA_MONITORED_USAGE, 120.0)
                                putExtra(InterventionActivity.EXTRA_INTERVAL_MINUTES_AT_LAUNCH, 10.0)
                                putExtra(InterventionActivity.EXTRA_AMBIENT_LIGHT_LUX_AT_LAUNCH, 100f)
                                putExtra(InterventionActivity.EXTRA_DEVIATION, 0.5)
                                putExtra(InterventionActivity.EXTRA_DIFFICULTY_CONTROL_SIGNAL, 0.7)
                                putExtra(InterventionActivity.EXTRA_DIFFICULTY, 3)
                                putExtra(InterventionActivity.EXTRA_CHALLENGE_TYPE, InterventionChallengeType.PATTERN.name)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.weight(1f))
            
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.profile_close))
            }
        }
    }

    if (showEditNameDialog) {
        EditFieldDialog(
            title = stringResource(R.string.profile_edit_nickname),
            initialValue = uiState.nickname,
            onConfirm = { 
                viewModel.updateNickname(it)
                showEditNameDialog = false
            },
            onDismiss = { showEditNameDialog = false }
        )
    }

    if (showEditUserDialog) {
        EditFieldDialog(
            title = stringResource(R.string.profile_edit_username),
            initialValue = uiState.username,
            onConfirm = { 
                viewModel.updateUsername(it)
                showEditUserDialog = false
            },
            onDismiss = { showEditUserDialog = false }
        )
    }

    if (showImageSourcePicker) {
        AlertDialog(
            onDismissRequest = { showImageSourcePicker = false },
            title = { Text(stringResource(R.string.profile_select_source)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.profile_gallery)) },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                        modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                    )
                    if (uiState.profileImagePath != null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.profile_delete_photo)) },
                            leadingContent = { Icon(Icons.Default.Delete, null) },
                            modifier = Modifier.clickable { 
                                viewModel.updateProfileImage(null)
                                showImageSourcePicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun DebugActionItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProfileItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EditFieldDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.profile_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        }
    )
}

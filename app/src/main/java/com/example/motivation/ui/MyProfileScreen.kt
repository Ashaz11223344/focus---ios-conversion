package com.example.motivation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.ui.theme.InterFontFamily
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.VibrantOrange
import com.example.motivation.viewmodel.ProfileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onNavigateBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentName by profileViewModel.userName.collectAsState()
    val photoUriString by profileViewModel.userProfilePhotoUri.collectAsState()
    val profileCreatedDate by profileViewModel.profileCreatedDate.collectAsState()

    var name by remember(currentName) { mutableStateOf(currentName) }
    var selectedPhotoUri by remember(photoUriString) { mutableStateOf(photoUriString) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    // Camera launcher setup
    val cameraTempFile = remember { File(context.cacheDir, "camera_temp_edit.jpg") }
    val cameraTempUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cameraTempFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedPhotoUri = Uri.fromFile(cameraTempFile).toString()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it.toString()
        }
    }

    val joinedDateFormatted = remember(profileCreatedDate) {
        if (profileCreatedDate > 0L) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(profileCreatedDate))
        } else {
            "Not joined"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile photo picker
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clickable { showPhotoOptions = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, VibrantOrange.copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatar(
                        photoUriString = selectedPhotoUri,
                        userName = name,
                        size = 150.dp
                    )
                }
                
                // Tiny Camera overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VibrantOrange)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Edit photo",
                        tint = Color(0xFF121212),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 30) {
                        name = it
                    }
                },
                label = { 
                    Text(
                        text = "Name",
                        fontFamily = InterFontFamily,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    ) 
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = VibrantOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.3f),
                    cursorColor = VibrantOrange
                ),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    Text(
                        text = "${name.length}/30",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Metadata card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Joined",
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                        Text(
                            text = joinedDateFormatted,
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    profileViewModel.saveProfile(name.trim(), selectedPhotoUri, isUpdate = true)
                    onNavigateBack()
                },
                enabled = name.isNotBlank() && name.length <= 30,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantOrange,
                    contentColor = Color(0xFF121212),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Save Changes",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // Photo options dialog
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Profile Photo",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("Take Photo", color = MaterialTheme.colorScheme.onSurface, fontFamily = InterFontFamily) },
                        leadingContent = { Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = VibrantOrange) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            showPhotoOptions = false
                            cameraLauncher.launch(cameraTempUri)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery", color = MaterialTheme.colorScheme.onSurface, fontFamily = InterFontFamily) },
                        leadingContent = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = VibrantOrange) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            showPhotoOptions = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                    if (selectedPhotoUri != null) {
                        ListItem(
                            headlineContent = { Text("Remove Photo", color = Color(0xFFFF9494), fontFamily = InterFontFamily) },
                            leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFF9494)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showPhotoOptions = false
                                selectedPhotoUri = null
                                profileViewModel.updatePhoto(null)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptions = false }) {
                    Text("Cancel", color = VibrantOrange, fontFamily = InterFontFamily)
                }
            }
        )
    }
}

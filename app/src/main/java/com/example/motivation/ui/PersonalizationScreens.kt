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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.ui.theme.InterFontFamily
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.VibrantOrange
import com.example.motivation.viewmodel.ProfileViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameInputScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onNameSaved: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    // Camera launcher setup
    val cameraTempFile = remember { File(context.cacheDir, "camera_temp.jpg") }
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

    Scaffold(
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Your Profile",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color(0xFFFFE7D0),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Let's personalize your Focus environment",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFFFFE7D0).copy(0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Image Picker
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
                            .background(Color(0xFF1E1E1E))
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

                // Name input with character limits and counter
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 30) {
                            name = it
                        }
                    },
                    label = { 
                        Text(
                            text = "Your Name",
                            fontFamily = InterFontFamily,
                            color = Color(0xFFFFE7D0).copy(0.5f)
                        ) 
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFFE7D0),
                        unfocusedTextColor = Color(0xFFFFE7D0),
                        focusedBorderColor = VibrantOrange,
                        unfocusedBorderColor = Color(0xFF2E2E2E),
                        cursorColor = VibrantOrange
                    ),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        Text(
                            text = "${name.length}/30",
                            fontSize = 12.sp,
                            color = Color(0xFFFFE7D0).copy(0.4f),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        profileViewModel.saveProfile(name.trim(), selectedPhotoUri, isUpdate = false)
                        onNameSaved()
                    },
                    enabled = name.isNotBlank() && name.length <= 30,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantOrange,
                        contentColor = Color(0xFF121212),
                        disabledContainerColor = Color(0xFF222222),
                        disabledContentColor = Color(0xFFFFE7D0).copy(0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Continue",
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Photo picker options bottom sheet or dialog
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    text = "Profile Photo",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE7D0),
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("Take Photo", color = Color(0xFFFFE7D0), fontFamily = InterFontFamily) },
                        leadingContent = { Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = VibrantOrange) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            showPhotoOptions = false
                            cameraLauncher.launch(cameraTempUri)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery", color = Color(0xFFFFE7D0), fontFamily = InterFontFamily) },
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

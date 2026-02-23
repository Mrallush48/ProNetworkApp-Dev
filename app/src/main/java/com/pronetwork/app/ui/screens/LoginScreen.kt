package com.pronetwork.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronetwork.app.R
import com.pronetwork.app.viewmodel.LoginUiState
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLogin: () -> Unit,
    onClearError: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // === Entry Animations ===
    var logoVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    var footerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(400)
        formVisible = true
        delay(300)
        footerVisible = true
    }

    // === Pulse animation for logo ===
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // === Error message resolver ===
    val errorText = when (uiState.errorMessage) {
        "USERNAME_REQUIRED" -> stringResource(R.string.login_error_username_required)
        "PASSWORD_REQUIRED" -> stringResource(R.string.login_error_password_required)
        "INVALID_CREDENTIALS" -> stringResource(R.string.login_error_invalid_credentials)
        "ACCOUNT_DISABLED" -> stringResource(R.string.login_error_account_disabled)
        "USER_NOT_FOUND" -> stringResource(R.string.login_error_user_not_found)
        "SERVER_ERROR" -> stringResource(R.string.login_error_server)
        "NO_CONNECTION" -> stringResource(R.string.login_error_no_connection)
        "TIMEOUT" -> stringResource(R.string.login_error_timeout)
        "NETWORK_ERROR" -> stringResource(R.string.login_error_network)
        else -> uiState.errorMessage
    }

    // === Main Layout ===
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1128),
                        Color(0xFF2D1B4E),
                        Color(0xFF1A1128)
                    )
                )
            )
    ) {
        // === Background Decorative Circles ===
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .alpha(glowAlpha * 0.15f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF7C4DFF), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 100.dp)
                .alpha(glowAlpha * 0.12f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFB388FF), Color.Transparent)
                    )
                )
        )

        // === Content ===
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // ========== LOGO SECTION ==========
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(tween(800)) + slideInVertically(
                    initialOffsetY = { -60 },
                    animationSpec = tween(800, easing = EaseOutBack)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        // Outer glow ring
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(pulseScale)
                                .alpha(glowAlpha * 0.4f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF7C4DFF), Color.Transparent)
                                    )
                                )
                        )
                        // Main logo circle
                        Surface(
                            modifier = Modifier.size(88.dp),
                            shape = CircleShape,
                            shadowElevation = 20.dp,
                            tonalElevation = 8.dp,
                            color = Color.Transparent
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF5E35B1),
                                                Color(0xFF311B92)
                                            )
                                        )
                                    )
                            ) {
                                Text(
                                    text = "P",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "ProNetwork",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SPOT MANAGEMENT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB388FF),
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ========== LOGIN FORM CARD ==========
            AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { 80 },
                    animationSpec = tween(600, easing = EaseOutCubic)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A1F3D).copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = stringResource(R.string.login_welcome),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.login_subtitle),
                            fontSize = 13.sp,
                            color = Color(0xFFB0AAC0)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // === Error Message ===
                        AnimatedVisibility(visible = errorText != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4E1A1A)
                            ) {
                                Text(
                                    text = errorText ?: "",
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    ),
                                    color = Color(0xFFFF8A80),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // === Username Field ===
                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = onUsernameChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(stringResource(R.string.login_username))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFB388FF)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF7C4DFF),
                                unfocusedBorderColor = Color(0xFF4A4458),
                                focusedLabelColor = Color(0xFFB388FF),
                                unfocusedLabelColor = Color(0xFF8E8E93),
                                cursorColor = Color(0xFFB388FF),
                                focusedLeadingIconColor = Color(0xFFB388FF),
                                unfocusedLeadingIconColor = Color(0xFF8E8E93)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // === Password Field ===
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = onPasswordChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(stringResource(R.string.login_password))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFB388FF)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = onTogglePassword) {
                                    Icon(
                                        imageVector = if (uiState.isPasswordVisible)
                                            Icons.Filled.Visibility
                                        else Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFF8E8E93)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (uiState.isPasswordVisible)
                                VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    onLogin()
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF7C4DFF),
                                unfocusedBorderColor = Color(0xFF4A4458),
                                focusedLabelColor = Color(0xFFB388FF),
                                unfocusedLabelColor = Color(0xFF8E8E93),
                                cursorColor = Color(0xFFB388FF),
                                focusedLeadingIconColor = Color(0xFFB388FF),
                                unfocusedLeadingIconColor = Color(0xFF8E8E93)
                            )
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // ========== LOGIN BUTTON ==========
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onLogin()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C4DFF),
                                disabledContainerColor = Color(0xFF4A3B6B)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.login_loading),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.login_button),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ========== FOOTER ==========
            AnimatedVisibility(
                visible = footerVisible,
                enter = fadeIn(tween(500))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.login_secured),
                        fontSize = 11.sp,
                        color = Color(0xFF6E6680)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.login_version),
                        fontSize = 11.sp,
                        color = Color(0xFF4A4458)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalAppStrings
import com.example.security.AuthVerificationResult
import com.example.ui.components.D09LogoBadge
import com.example.ui.components.TvFocusableSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan

@Composable
fun UserLoginScreen(
    allowRememberSession: Boolean,
    onLoginAttempt: (code: String, rememberSession: Boolean) -> AuthVerificationResult,
    onOpenLanguagePicker: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    var code by remember { mutableStateOf("") }
    var rememberSession by remember(allowRememberSession) { mutableStateOf(allowRememberSession) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        when (val result = onLoginAttempt(code, rememberSession)) {
            is AuthVerificationResult.Success -> {
                errorMessage = null
            }
            is AuthVerificationResult.InvalidCode -> {
                code = ""
                errorMessage = strings.invalidCodeError(result.remainingAttempts)
            }
            is AuthVerificationResult.RateLimited -> {
                code = ""
                errorMessage = strings.rateLimitedError(result.waitSeconds)
            }
        }
    }

    LoginScaffoldLayout(
        isAdminMode = false,
        title = strings.userLoginTitle,
        subtitle = strings.userLoginSubtitle,
        code = code,
        errorMessage = errorMessage,
        onDigit = { digit ->
            if (code.length < 8) {
                code += digit
                errorMessage = null
            }
        },
        onBackspace = {
            if (code.isNotEmpty()) {
                code = code.dropLast(1)
                errorMessage = null
            }
        },
        onClear = {
            code = ""
            errorMessage = null
        },
        onSubmit = ::submit,
        headerActions = {
            TvFocusableSurface(
                onClick = onOpenLanguagePicker,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("login_language_button"),
            ) { _ ->
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = strings.changeLanguage,
                        tint = SkyCyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = strings.changeLanguage,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        extraOptions = {
            if (allowRememberSession) {
                TvFocusableSurface(
                    onClick = { rememberSession = !rememberSession },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remember_session_toggle"),
                ) { _ ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = strings.rememberSessionLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = rememberSession,
                            onCheckedChange = { rememberSession = it },
                        )
                    }
                }
            }
        },
        footerAction = {
            TextButton(
                onClick = onOpenAdminPortal,
                modifier = Modifier.testTag("open_admin_login_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = strings.adminPortalButton,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.adminPortalButton,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        submitTestTag = "user_login_submit_button",
        modifier = modifier,
    )
}

@Composable
fun AdminLoginScreen(
    onAdminLoginAttempt: (code: String) -> AuthVerificationResult,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val strings = LocalAppStrings.current
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        when (val result = onAdminLoginAttempt(code)) {
            is AuthVerificationResult.Success -> {
                errorMessage = null
            }
            is AuthVerificationResult.InvalidCode -> {
                code = ""
                errorMessage = strings.invalidCodeError(result.remainingAttempts)
            }
            is AuthVerificationResult.RateLimited -> {
                code = ""
                errorMessage = strings.rateLimitedError(result.waitSeconds)
            }
        }
    }

    LoginScaffoldLayout(
        isAdminMode = true,
        title = strings.adminLoginTitle,
        subtitle = strings.adminLoginSubtitle,
        code = code,
        errorMessage = errorMessage,
        onDigit = { digit ->
            if (code.length < 8) {
                code += digit
                errorMessage = null
            }
        },
        onBackspace = {
            if (code.isNotEmpty()) {
                code = code.dropLast(1)
                errorMessage = null
            }
        },
        onClear = {
            code = ""
            errorMessage = null
        },
        onSubmit = ::submit,
        headerActions = {
            TvFocusableSurface(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("admin_login_back_button"),
            ) { _ ->
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backToUserLogin,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = strings.backToUserLogin,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        extraOptions = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RoyalPurple.copy(alpha = 0.14f))
                    .border(1.dp, RoyalPurple.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = strings.adminSecurityNoticeTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = SkyCyan,
                )
                Text(
                    text = strings.adminSecurityNoticeBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        footerAction = {},
        submitTestTag = "admin_login_submit_button",
        modifier = modifier,
    )
}

@Composable
private fun LoginScaffoldLayout(
    isAdminMode: Boolean,
    title: String,
    subtitle: String,
    code: String,
    errorMessage: String?,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    headerActions: @Composable () -> Unit,
    extraOptions: @Composable () -> Unit,
    footerAction: @Composable () -> Unit,
    submitTestTag: String,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    var revealCode by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (isAdminMode) Color(0xFF1D103D) else Color(0xFF102047),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    val digit = when (event.key) {
                        Key.Zero, Key.NumPad0 -> '0'
                        Key.One, Key.NumPad1 -> '1'
                        Key.Two, Key.NumPad2 -> '2'
                        Key.Three, Key.NumPad3 -> '3'
                        Key.Four, Key.NumPad4 -> '4'
                        Key.Five, Key.NumPad5 -> '5'
                        Key.Six, Key.NumPad6 -> '6'
                        Key.Seven, Key.NumPad7 -> '7'
                        Key.Eight, Key.NumPad8 -> '8'
                        Key.Nine, Key.NumPad9 -> '9'
                        else -> null
                    }
                    when {
                        digit != null -> {
                            onDigit(digit)
                            true
                        }
                        event.key == Key.Backspace -> {
                            onBackspace()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        val isWide = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .widthIn(max = if (isWide) 840.dp else 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .border(
                    1.dp,
                    if (isAdminMode) RoyalPurple.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    RoundedCornerShape(26.dp),
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                D09LogoBadge(iconSize = 42.dp, showSubtitle = false)
                headerActions()
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LoginInfoPanel(
                            isAdminMode = isAdminMode,
                            title = title,
                            subtitle = subtitle,
                            code = code,
                            revealCode = revealCode,
                            onToggleReveal = { revealCode = !revealCode },
                            errorMessage = errorMessage,
                        )
                        extraOptions()
                        footerAction()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NumericKeypadGrid(
                            onDigit = onDigit,
                            onClear = onClear,
                            onBackspace = onBackspace,
                        )
                        Button(
                            onClick = onSubmit,
                            enabled = code.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdminMode) RoyalPurple else ElectricBlue,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag(submitTestTag),
                        ) {
                            Text(
                                text = strings.loginButton,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            } else {
                LoginInfoPanel(
                    isAdminMode = isAdminMode,
                    title = title,
                    subtitle = subtitle,
                    code = code,
                    revealCode = revealCode,
                    onToggleReveal = { revealCode = !revealCode },
                    errorMessage = errorMessage,
                )
                NumericKeypadGrid(
                    onDigit = onDigit,
                    onClear = onClear,
                    onBackspace = onBackspace,
                )
                extraOptions()
                Button(
                    onClick = onSubmit,
                    enabled = code.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdminMode) RoyalPurple else ElectricBlue,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag(submitTestTag),
                ) {
                    Text(
                        text = strings.loginButton,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    footerAction()
                }
            }
        }
    }
}

@Composable
private fun LoginInfoPanel(
    isAdminMode: Boolean,
    title: String,
    subtitle: String,
    code: String,
    revealCode: Boolean,
    onToggleReveal: () -> Unit,
    errorMessage: String?,
) {
    val strings = LocalAppStrings.current
    val accent = if (isAdminMode) RoyalPurple else SkyCyan

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isAdminMode) Icons.Filled.AdminPanelSettings else Icons.Filled.Lock,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Masked Code Display Box (keep LTR for digits/dots consistency)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                    .border(
                        1.5.dp,
                        if (errorMessage != null) LiveCrimson else accent.copy(alpha = 0.6f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (code.isEmpty()) {
                    Text(
                        text = strings.enterAccessCodeHint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                } else if (revealCode) {
                    Text(
                        text = code.chunked(1).joinToString("  "),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pin_display_text"),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pin_dots_display"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(code.length) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(accent),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleReveal,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("pin_visibility_toggle"),
                ) {
                    Icon(
                        imageVector = if (revealCode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (revealCode) strings.hideCodeLabel else strings.showCodeLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LiveCrimson.copy(alpha = 0.18f))
                    .border(1.dp, LiveCrimson.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("login_error_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = errorMessage,
                    tint = LiveCrimson,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun NumericKeypadGrid(
    onDigit: (Char) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
) {
    val strings = LocalAppStrings.current
    // Keep keypad numeric layout LTR for intuitive phone & TV remote dial pad orientation
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val rows = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
            )
            rows.forEach { rowDigits ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowDigits.forEach { digit ->
                        TvFocusableSurface(
                            onClick = { onDigit(digit) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("keypad_digit_$digit"),
                        ) { _ ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = digit.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TvFocusableSurface(
                    onClick = onClear,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("keypad_clear"),
                ) { _ ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = strings.keypadClear,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                TvFocusableSurface(
                    onClick = { onDigit('0') },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("keypad_digit_0"),
                ) { _ ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }

                TvFocusableSurface(
                    onClick = onBackspace,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("keypad_backspace"),
                ) { _ ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = strings.keypadBackspace,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

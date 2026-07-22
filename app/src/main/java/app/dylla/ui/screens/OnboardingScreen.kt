package app.dylla.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.dylla.models.Company
import app.dylla.models.FundingStage
import app.dylla.models.IndustryTemplate
import app.dylla.models.IndustryType
import app.dylla.services.PersistenceManager
import app.dylla.ui.theme.DyllaBlue

private const val TOTAL_PAGES = 9

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dylla_prefs", android.content.Context.MODE_PRIVATE) }

    var currentPage by remember { mutableIntStateOf(0) }
    var animateIn by remember { mutableStateOf(false) }

    // Profile fields
    var selectedIndustry by remember { mutableStateOf(IndustryType.OTHER) }
    var companyName by remember { mutableStateOf("") }
    var agentName by remember { mutableStateOf("") }
    var applyURL by remember { mutableStateOf("") }

    // API key
    var apiKey by remember { mutableStateOf("") }

    // Email fields
    var sendingEmail by remember { mutableStateOf("") }
    var sendingDomain by remember { mutableStateOf("") }

    // Permissions state
    var phoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        )
    }
    var calendarGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launchers
    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> phoneGranted = granted }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        calendarGranted = results.values.all { it }
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micGranted = granted }

    // Templates loaded from industry selection
    val industryTemplate = remember(selectedIndustry) {
        IndustryTemplate.forIndustry(selectedIndustry)
    }

    // Editable SMS templates per stage
    var smsTemplates by remember(selectedIndustry) {
        mutableStateOf(
            FundingStage.defaults.mapIndexed { index, stage ->
                stage.name to (industryTemplate.smsTemplates.getOrElse(index) {
                    industryTemplate.smsTemplates.lastOrNull() ?: ""
                })
            }
        )
    }

    // Editable Email templates per stage (initial + nudge)
    var emailTemplatesInitial by remember(selectedIndustry) {
        mutableStateOf(
            FundingStage.defaults.mapIndexed { index, stage ->
                stage.name to (industryTemplate.emailTemplates.getOrElse(index) {
                    industryTemplate.emailTemplates.lastOrNull() ?: ""
                })
            }
        )
    }
    var emailTemplatesNudge by remember(selectedIndustry) {
        mutableStateOf(
            FundingStage.defaults.map { stage ->
                stage.name to "Hi {name}, just following up on my last message. Would you have a few minutes to chat about options for {business}?"
            }
        )
    }

    // Finish onboarding action
    val finishOnboarding = {
        // Save API key
        if (apiKey.isNotBlank()) {
            prefs.edit().putString("api_key", apiKey).apply()
        }

        // Save email settings
        if (sendingEmail.isNotBlank()) {
            prefs.edit().putString("sending_email", sendingEmail).apply()
        }
        if (sendingDomain.isNotBlank()) {
            prefs.edit().putString("sending_domain", sendingDomain).apply()
        }

        // Create company
        val company = Company(
            name = companyName.ifBlank { "My Company" },
            industry = selectedIndustry,
            isDefault = true
        )

        // Save company
        val existingCompanies = PersistenceManager.loadCompanies()
        PersistenceManager.saveCompanies(existingCompanies + company)

        // Build stages with templates
        val stages = FundingStage.defaults.mapIndexed { index, stage ->
            stage.copy(
                autoSMS = smsTemplates.getOrNull(index)?.second ?: "",
                autoEmail = emailTemplatesInitial.getOrNull(index)?.second ?: ""
            )
        }
        PersistenceManager.saveStages(stages)

        // Save user profile
        val profile = PersistenceManager.loadUserProfile().copy(
            apiKey = apiKey,
            companies = (existingCompanies + company)
        )
        PersistenceManager.saveUserProfile(profile)

        // Save agent name
        if (agentName.isNotBlank()) {
            prefs.edit().putString("agent_name", agentName).apply()
        }
        if (applyURL.isNotBlank()) {
            prefs.edit().putString("apply_url", applyURL).apply()
        }

        // Mark complete and call callback
        onComplete()
    }

    // Trigger entrance animation
    LaunchedEffect(Unit) { animateIn = true }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F1A66),
            Color(0xFF1A4DD9),
            Color(0xFF3380F2)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button (hidden on last page)
            if (currentPage < TOTAL_PAGES - 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { finishOnboarding() }) {
                        Text(
                            text = "Skip",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(56.dp))
            }

            // Main page content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "onboarding_page"
            ) { page ->
                when (page) {
                    0 -> WelcomePage(animateIn = animateIn)
                    1 -> HowItWorksPage()
                    2 -> IndustryPickerPage(
                        selectedIndustry = selectedIndustry,
                        onIndustrySelected = { selectedIndustry = it },
                        companyName = companyName,
                        onCompanyNameChange = { companyName = it },
                        agentName = agentName,
                        onAgentNameChange = { agentName = it },
                        applyURL = applyURL,
                        onApplyURLChange = { applyURL = it }
                    )
                    3 -> PermissionsPage(
                        phoneGranted = phoneGranted,
                        calendarGranted = calendarGranted,
                        micGranted = micGranted,
                        onRequestPhone = {
                            phoneLauncher.launch(Manifest.permission.CALL_PHONE)
                        },
                        onRequestCalendar = {
                            calendarLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        },
                        onRequestMic = {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                    4 -> ApiKeyPage(
                        apiKey = apiKey,
                        onApiKeyChange = { apiKey = it }
                    )
                    5 -> EmailSetupPage(
                        sendingEmail = sendingEmail,
                        onSendingEmailChange = { sendingEmail = it },
                        sendingDomain = sendingDomain,
                        onSendingDomainChange = { sendingDomain = it }
                    )
                    6 -> SmsTemplateReviewPage(
                        stages = FundingStage.defaults,
                        templates = smsTemplates,
                        onTemplateChange = { index, newText ->
                            smsTemplates = smsTemplates.toMutableList().also {
                                it[index] = it[index].first to newText
                            }
                        }
                    )
                    7 -> EmailTemplateReviewPage(
                        stages = FundingStage.defaults,
                        initialTemplates = emailTemplatesInitial,
                        nudgeTemplates = emailTemplatesNudge,
                        onInitialChange = { index, newText ->
                            emailTemplatesInitial = emailTemplatesInitial.toMutableList().also {
                                it[index] = it[index].first to newText
                            }
                        },
                        onNudgeChange = { index, newText ->
                            emailTemplatesNudge = emailTemplatesNudge.toMutableList().also {
                                it[index] = it[index].first to newText
                            }
                        }
                    )
                    8 -> ImportPage()
                }
            }

            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(TOTAL_PAGES) { index ->
                    val width by animateDpAsState(
                        targetValue = if (index == currentPage) 24.dp else 8.dp,
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == currentPage) Color.White
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom button(s)
            if (currentPage < TOTAL_PAGES - 1) {
                Button(
                    onClick = { currentPage++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DyllaBlue
                    )
                ) {
                    Text(
                        text = "Continue",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Last page: two buttons
                Button(
                    onClick = { finishOnboarding() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DyllaBlue
                    )
                ) {
                    Text(
                        text = "Import CSV & Start",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { finishOnboarding() }) {
                    Text(
                        text = "Skip for Now",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Page 0: Welcome ──────────────────────────────────────────────────────

@Composable
private fun WelcomePage(animateIn: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.5f,
        animationSpec = tween(800),
        label = "welcome_scale"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.3f,
        animationSpec = tween(800),
        label = "icon_scale"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (animateIn) 0f else 30f,
        animationSpec = tween(800),
        label = "welcome_offset"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Animated logo circles
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size((160 * scale).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size((120 * scale).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            )
            Icon(
                imageVector = Icons.Filled.PhoneForwarded,
                contentDescription = null,
                modifier = Modifier.size((56 * iconScale).dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title + tagline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = offsetY.dp)
        ) {
            Text(
                text = "Dylla",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your personal sales CRM.",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Manage warm leads. Call, text, and email\nfrom your personal line.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(modifier = Modifier.weight(2f))
    }
}

// ── Page 1: How It Works ─────────────────────────────────────────────────

@Composable
private fun HowItWorksPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "How It Works",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StepRow(icon = Icons.Filled.Description, title = "Import Your Leads", sub = "Drop in your CSV list from any source")
            StepRow(icon = Icons.Filled.Phone, title = "Call & Connect", sub = "Tap to call from your personal number")
            StepRow(icon = Icons.Filled.Checklist, title = "Qualify & Tag", sub = "Checklist auto-picks the right stage")
            StepRow(icon = Icons.Filled.Bolt, title = "Automate", sub = "SMS + email fire based on stage")
            StepRow(icon = Icons.Filled.Notifications, title = "Follow Up", sub = "Automatic nudge emails keep leads warm")
        }

        Spacer(modifier = Modifier.weight(2f))
    }
}

@Composable
private fun StepRow(icon: ImageVector, title: String, sub: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = sub,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Page 2: Industry Picker ──────────────────────────────────────────────

@Composable
private fun IndustryPickerPage(
    selectedIndustry: IndustryType,
    onIndustrySelected: (IndustryType) -> Unit,
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    agentName: String,
    onAgentNameChange: (String) -> Unit,
    applyURL: String,
    onApplyURLChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your Industry",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pick your industry to get tailored templates.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3-column grid of industries
        val industries = IndustryType.entries.toList()
        // Manual grid using rows of 3
        val chunked = industries.chunked(3)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chunked.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { industry ->
                        val isSelected = selectedIndustry == industry
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.1f)
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        Color.White,
                                        RoundedCornerShape(12.dp)
                                    )
                                    else Modifier
                                )
                                .clickable { onIndustrySelected(industry) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = industry.emoji,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = industry.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                    // Fill remaining space if row has fewer than 3 items
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Text fields
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingTextField(
                value = companyName,
                onValueChange = onCompanyNameChange,
                placeholder = "Company Name"
            )
            OnboardingTextField(
                value = agentName,
                onValueChange = onAgentNameChange,
                placeholder = "Your Name"
            )
            OnboardingTextField(
                value = applyURL,
                onValueChange = onApplyURLChange,
                placeholder = "Apply / Booking URL"
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ── Page 3: Permissions ──────────────────────────────────────────────────

@Composable
private fun PermissionsPage(
    phoneGranted: Boolean,
    calendarGranted: Boolean,
    micGranted: Boolean,
    onRequestPhone: () -> Unit,
    onRequestCalendar: () -> Unit,
    onRequestMic: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Permissions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Dylla works best with these enabled.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PermissionCard(
                icon = Icons.Filled.Phone,
                title = "Phone",
                sub = "Tap to call leads from the dialer",
                granted = phoneGranted,
                onRequest = onRequestPhone
            )
            PermissionCard(
                icon = Icons.Filled.CalendarMonth,
                title = "Calendar",
                sub = "Save callback reminders to your calendar",
                granted = calendarGranted,
                onRequest = onRequestCalendar
            )
            PermissionCard(
                icon = Icons.Filled.Mic,
                title = "Microphone",
                sub = "Record voice drops to send as MMS",
                granted = micGranted,
                onRequest = onRequestMic
            )
        }

        Spacer(modifier = Modifier.weight(2f))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    sub: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { if (!granted) onRequest() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (granted) Color(0xFF34C759) else Color.White,
            modifier = Modifier.size(28.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = sub,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (granted) Color(0xFF34C759) else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ── Page 4: API Key ──────────────────────────────────────────────────────

@Composable
private fun ApiKeyPage(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Filled.Key,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Connect to Your Server",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Enter your API key to enable\nSMS + email automation per stage.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            placeholder = {
                Text(
                    text = "Paste API key",
                    color = Color.White.copy(alpha = 0.4f)
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.15f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can add this later in Settings.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.weight(2f))
    }
}

// ── Page 5: Email Setup ──────────────────────────────────────────────────

@Composable
private fun EmailSetupPage(
    sendingEmail: String,
    onSendingEmailChange: (String) -> Unit,
    sendingDomain: String,
    onSendingDomainChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Icon(
            imageVector = Icons.Filled.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Connect Your Email",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Set up your sending domain so emails\nland in the inbox, not spam.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Email fields card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sending Email",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            OnboardingTextField(
                value = sendingEmail,
                onValueChange = onSendingEmailChange,
                placeholder = "you@yourdomain.com"
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sending Domain",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            OnboardingTextField(
                value = sendingDomain,
                onValueChange = onSendingDomainChange,
                placeholder = "yourdomain.com"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "You can set this up later in Settings.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ── Page 6: SMS Template Review ──────────────────────────────────────────

@Composable
private fun SmsTemplateReviewPage(
    stages: List<FundingStage>,
    templates: List<Pair<String, String>>,
    onTemplateChange: (Int, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your SMS Templates",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Review and customize the texts sent\nwhen you assign a stage after a call.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        stages.forEachIndexed { index, stage ->
            TemplateCard(
                emoji = stage.emoji,
                stageName = stage.name,
                content = {
                    OutlinedTextField(
                        value = templates.getOrNull(index)?.second ?: "",
                        onValueChange = { onTemplateChange(index, it) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DyllaBlue.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            cursorColor = DyllaBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Variables: {name} {company} {agent} {applyURL}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ── Page 7: Email Template Review ────────────────────────────────────────

@Composable
private fun EmailTemplateReviewPage(
    stages: List<FundingStage>,
    initialTemplates: List<Pair<String, String>>,
    nudgeTemplates: List<Pair<String, String>>,
    onInitialChange: (Int, String) -> Unit,
    onNudgeChange: (Int, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your Email Templates",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Customize the emails sent alongside\nyour SMS and as follow-up nudges.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        stages.forEachIndexed { index, stage ->
            TemplateCard(
                emoji = stage.emoji,
                stageName = stage.name,
                content = {
                    Text(
                        text = "Initial Email",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = initialTemplates.getOrNull(index)?.second ?: "",
                        onValueChange = { onInitialChange(index, it) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DyllaBlue.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            cursorColor = DyllaBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Nudge / Follow-Up Email",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = nudgeTemplates.getOrNull(index)?.second ?: "",
                        onValueChange = { onNudgeChange(index, it) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DyllaBlue.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            cursorColor = DyllaBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Variables: {name} {company} {agent} {applyURL}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ── Page 8: Import / Ready ───────────────────────────────────────────────

@Composable
private fun ImportPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Circle with icon
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Icon(
                imageVector = Icons.Filled.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Ready to Go",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Import your first CSV lead list\nor jump straight in and add one later.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(2f))
    }
}

// ── Shared Components ────────────────────────────────────────────────────

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.4f)
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedBorderColor = Color.White.copy(alpha = 0.4f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.15f)
        )
    )
}

@Composable
private fun TemplateCard(
    emoji: String,
    stageName: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stageName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}

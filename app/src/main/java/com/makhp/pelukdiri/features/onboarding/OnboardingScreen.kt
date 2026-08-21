package com.makhp.pelukdiri.features.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.ui.components.PelukDiriLogo
import kotlinx.coroutines.launch

sealed class OnboardingStep {
    data object Intro : OnboardingStep()
    data object Terms : OnboardingStep()
    data class Permissions(val step: Int) : OnboardingStep()
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf<OnboardingStep>(OnboardingStep.Intro) }
    val context = LocalContext.current

    BackHandler {
        if (currentStep == OnboardingStep.Intro) {
            (context as? Activity)?.finish()
        } else {
            currentStep = OnboardingStep.Intro
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(targetState = currentStep, label = "OnboardingStep") { step ->
            when (step) {
                OnboardingStep.Intro -> IntroPager(
                    onSkip = { currentStep = OnboardingStep.Terms },
                    onFinish = { currentStep = OnboardingStep.Terms }
                )
                OnboardingStep.Terms -> TermsScreen(
                    onAgree = { currentStep = OnboardingStep.Permissions(1) },
                    onDisagree = { currentStep = OnboardingStep.Intro }
                )
                is OnboardingStep.Permissions -> PermissionsStepScreen(
                    step = step.step,
                    onNext = {
                        if (step.step < 3) {
                            currentStep = OnboardingStep.Permissions(step.step + 1)
                        } else {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    },
                    onBack = {
                        if (step.step > 1) {
                            currentStep = OnboardingStep.Permissions(step.step - 1)
                        } else {
                            currentStep = OnboardingStep.Terms
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IntroPager(
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4CAF50))
                ) {
                    Text(stringResource(R.string.onboarding_skip), fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) Color(0xFF4CAF50) 
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (pagerState.currentPage < 2) stringResource(R.string.onboarding_next) 
                            else stringResource(R.string.onboarding_start),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> FactsPage()
                2 -> HowItWorksPage()
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(240.dp),
            shape = RoundedCornerShape(40.dp),
            color = Color(0xFFE8F5E9)
        ) {
            Box(contentAlignment = Alignment.Center) {
                PelukDiriLogo(size = 180.dp)
            }
        }
        
        Spacer(Modifier.height(40.dp))
        
        Text(
            stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.onboarding_welcome_brand),
            style = MaterialTheme.typography.displayMedium,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            stringResource(R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun FactsPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.onboarding_facts_label),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.onboarding_facts_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            stringResource(R.string.onboarding_facts_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FactCard(stringResource(R.string.onboarding_fact_1_title), stringResource(R.string.onboarding_fact_1_desc), Icons.Default.AccessTime)
            FactCard(stringResource(R.string.onboarding_fact_2_title), stringResource(R.string.onboarding_fact_2_desc), Icons.Default.Psychology)
            FactCard(stringResource(R.string.onboarding_fact_3_title), stringResource(R.string.onboarding_fact_3_desc), Icons.Default.Bedtime)
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FactCard(title: String, desc: String, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HowItWorksPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.onboarding_how_label),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.onboarding_how_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            stringResource(R.string.onboarding_how_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                stringResource(R.string.onboarding_how_step_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            HowStepItem(1, stringResource(R.string.onboarding_how_step_1))
            HowStepItem(2, stringResource(R.string.onboarding_how_step_2))
            HowStepItem(3, stringResource(R.string.onboarding_how_step_3))
            HowStepItem(4, stringResource(R.string.onboarding_how_step_4))
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun HowStepItem(num: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = num.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsScreen(
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { PelukDiriLogo(size = 28.dp) },
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onDisagree) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onAgree,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.onboarding_terms_agree), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }

                TextButton(
                    onClick = onDisagree,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.onboarding_terms_disagree), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Text(
                stringResource(R.string.onboarding_terms_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.onboarding_terms_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)
            )

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val terms = listOf(
                    R.string.onboarding_terms_1_title to R.string.onboarding_terms_1_desc,
                    R.string.onboarding_terms_2_title to R.string.onboarding_terms_2_desc,
                    R.string.onboarding_terms_3_title to R.string.onboarding_terms_3_desc,
                    R.string.onboarding_terms_4_title to R.string.onboarding_terms_4_desc,
                    R.string.onboarding_terms_5_title to R.string.onboarding_terms_5_desc,
                    R.string.onboarding_terms_6_title to R.string.onboarding_terms_6_desc
                )
                itemsIndexed(terms) { index, term ->
                    TermItem(stringResource(term.first), stringResource(term.second), index)
                }
                
                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun TermItem(title: String, desc: String, index: Int) {
    val icon = when (index) {
        0 -> Icons.Default.Lock
        1 -> Icons.Default.BarChart
        2 -> Icons.Default.PrivacyTip
        3 -> Icons.Default.Info
        4 -> Icons.Default.Person
        else -> Icons.AutoMirrored.Filled.Article
    }
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsStepScreen(
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    val title = when (step) {
        1 -> stringResource(R.string.onboarding_perm_accessibility_title)
        2 -> stringResource(R.string.onboarding_perm_usage_title)
        else -> stringResource(R.string.onboarding_perm_notif_title)
    }
    
    val desc = when (step) {
        1 -> stringResource(R.string.onboarding_perm_accessibility_desc)
        2 -> stringResource(R.string.onboarding_perm_usage_desc)
        else -> stringResource(R.string.onboarding_perm_notif_desc_v2)
    }

    Scaffold(
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                PelukDiriLogo(modifier = Modifier.align(Alignment.Center), size = 28.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        when (step) {
                            1 -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            2 -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            3 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // Handle POST_NOTIFICATIONS request if needed, 
                                    // or just open App Settings as a fallback.
                                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    })
                                }
                            }
                        }
                        onNext()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (step == 1) Icons.Default.VerifiedUser else Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (step == 1) stringResource(R.string.onboarding_perm_button) else stringResource(R.string.onboarding_next), 
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }

                Text(
                    stringResource(R.string.onboarding_perm_privacy),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PermissionStepIndicator(1, step >= 1)
                Box(Modifier.width(24.dp).height(2.dp).clip(CircleShape).background(if (step >= 2) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant))
                PermissionStepIndicator(2, step >= 2)
                Box(Modifier.width(24.dp).height(2.dp).clip(CircleShape).background(if (step >= 3) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant))
                PermissionStepIndicator(3, step >= 3)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                textAlign = TextAlign.Center
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    val sectionTitle = when (step) {
                        1 -> stringResource(R.string.onboarding_perm_why_title)
                        2 -> stringResource(R.string.onboarding_perm_why_usage_title)
                        else -> stringResource(R.string.onboarding_perm_why_notif_title)
                    }
                    
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    when (step) {
                        1 -> {
                            WhyItem(stringResource(R.string.onboarding_perm_why_1_title), stringResource(R.string.onboarding_perm_why_1_desc), Icons.Default.VisibilityOff)
                            WhyItem(stringResource(R.string.onboarding_perm_why_2_title), stringResource(R.string.onboarding_perm_why_2_desc), Icons.Default.AppShortcut)
                            WhyItem(stringResource(R.string.onboarding_perm_why_3_title), stringResource(R.string.onboarding_perm_why_3_desc), Icons.Default.CheckCircle)
                        }
                        2 -> {
                            WhyItem(stringResource(R.string.onboarding_perm_why_usage_1_title), stringResource(R.string.onboarding_perm_why_usage_1_desc), Icons.Default.BarChart)
                            WhyItem(stringResource(R.string.onboarding_perm_why_usage_2_title), stringResource(R.string.onboarding_perm_why_usage_2_desc), Icons.Default.Apps)
                            WhyItem(stringResource(R.string.onboarding_perm_why_usage_3_title), stringResource(R.string.onboarding_perm_why_usage_3_desc), Icons.Default.SettingsSuggest)
                        }
                        3 -> {
                            WhyItem(stringResource(R.string.onboarding_perm_why_notif_1_title), stringResource(R.string.onboarding_perm_why_notif_1_desc), Icons.Default.NotificationsActive)
                            WhyItem(stringResource(R.string.onboarding_perm_why_notif_2_title), stringResource(R.string.onboarding_perm_why_notif_2_desc), Icons.Default.Assignment)
                            WhyItem(stringResource(R.string.onboarding_perm_why_notif_3_title), stringResource(R.string.onboarding_perm_why_notif_3_desc), Icons.Default.Star)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionStepIndicator(num: Int, active: Boolean) {
    Box(
        Modifier.size(32.dp).clip(CircleShape)
            .background(if (active) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(num.toString(), color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun WhyItem(title: String, desc: String, icon: ImageVector) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

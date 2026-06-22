package com.govtprep.app.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.screens.admin.*
import com.govtprep.app.ui.screens.analysis.AnalysisScreen
import com.govtprep.app.ui.screens.auth.*
import com.govtprep.app.ui.screens.exam.*
import com.govtprep.app.ui.screens.home.*
import com.govtprep.app.ui.screens.mocktests.MockTestsScreen
import com.govtprep.app.ui.screens.premium.PremiumScreen
import com.govtprep.app.ui.screens.profile.*
import com.govtprep.app.ui.screens.pyq.*
import com.govtprep.app.ui.screens.test.*
import com.govtprep.app.ui.theme.*

object Routes {
    const val LOGIN        = "login"
    const val SIGNUP       = "signup"
    const val HOME         = "home"
    const val TESTS        = "tests"
    const val ANALYSIS     = "analysis"
    const val PROFILE      = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val ADMIN        = "admin"
    const val PREMIUM      = "premium"
    const val PYQ          = "pyq"
    const val EXAM         = "exam/{slug}"
    const val TEST         = "test/{testSetId}"

    fun exam(slug: String)          = "exam/$slug"
    fun test(testSetId: String)     = "test/$testSetId"
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Transition presets
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private val tabRoutes = setOf(Routes.HOME, Routes.TESTS, Routes.ANALYSIS, Routes.PROFILE)

// Slide-in from right (drill down)
private val drillEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 4 } +
    fadeIn(tween(280))
}
// Slide-out to left (drill down exit)
private val drillExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 5 } +
    fadeOut(tween(220))
}
// Slide back from left (pop)
private val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 4 } +
    fadeIn(tween(260))
}
// Slide out to right (pop exit)
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 4 } +
    fadeOut(tween(240))
}
// Crossfade for tab switches
private val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(220))
}
private val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(180))
}

// Auth screens: slide up from bottom
private val authEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 3 } +
    fadeIn(tween(300))
}
private val authExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { -it / 5 } +
    fadeOut(tween(220))
}

@Composable
fun AppNavHost() {
    val s = S
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = remember(s) {
        listOf(
            NeuNavItem(Routes.HOME,     s.home,     Icons.Outlined.Home,     Icons.Filled.Home),
            NeuNavItem(Routes.TESTS,    s.tests,    Icons.Outlined.EditNote,  Icons.Filled.EditNote),
            NeuNavItem(Routes.ANALYSIS, s.analysis, Icons.Outlined.Insights,  Icons.Filled.Insights),
            NeuNavItem(Routes.PROFILE,  s.profile,  Icons.Outlined.Person,    Icons.Filled.Person),
        )
    }
    val showBottomBar = currentRoute in tabRoutes

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState    by authViewModel.authState.collectAsState()
    val sessionCheck by authViewModel.sessionCheck.collectAsState()
    val isAdmin      by authViewModel.isAdmin.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(NeuColors.Background)) {

        // ── Double back press → exit ──
        val activity = LocalContext.current as? Activity
        var showExitDialog by remember { mutableStateOf(false) }
        var lastBackPress  by remember { mutableLongStateOf(0L) }

        BackHandler {
            val now = System.currentTimeMillis()
            if (now - lastBackPress < 2000) showExitDialog = true
            else lastBackPress = now
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                containerColor = NeuColors.Surface,
                title = { Text("Exit App?", style = NeuType.h3) },
                text  = { Text("Are you sure you want to close Mock Master?", style = NeuType.body) },
                confirmButton = {
                    Button(onClick = { activity?.finish() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeuColors.Error)
                    ) { Text("Exit", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false; lastBackPress = 0L }) {
                        Text("Cancel", color = NeuColors.TextMuted)
                    }
                }
            )
        }

        NavHost(
            navController    = navController,
            startDestination = Routes.LOGIN,
            modifier         = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) 100.dp else 0.dp),
            enterTransition  = drillEnter,
            exitTransition   = drillExit,
            popEnterTransition  = popEnter,
            popExitTransition   = popExit,
        ) {
            // ━━ AUTH ━━
            composable(
                Routes.LOGIN,
                enterTransition = { fadeIn(tween(350)) },
                exitTransition  = authExit,
                popEnterTransition  = { fadeIn(tween(300)) },
                popExitTransition   = { fadeOut(tween(220)) }
            ) {
                LoginScreen(
                    authState = authState, sessionCheck = sessionCheck,
                    onSignIn = { e, p -> authViewModel.signIn(e, p) },
                    onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
                    onNavigateToHome = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    }
                )
            }

            composable(
                Routes.SIGNUP,
                enterTransition = authEnter,
                exitTransition  = authExit,
                popEnterTransition  = popEnter,
                popExitTransition   = popExit
            ) {
                SignupScreen(
                    authState = authState,
                    onSignUp  = { e, p, n, r -> authViewModel.signUp(e, p, n, r) },
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToHome  = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    }
                )
            }

            // ━━ BOTTOM TABS (crossfade) ━━
            composable(
                Routes.HOME,
                enterTransition    = tabEnter,
                exitTransition     = tabExit,
                popEnterTransition = tabEnter,
                popExitTransition  = tabExit
            ) {
                val homeVM: HomeViewModel = hiltViewModel()
                val state by homeVM.state.collectAsState()
                HomeScreen(
                    state = state, onRefresh = { homeVM.loadHome() },
                    onStartQuickTest = { navController.navigate(Routes.TESTS) },
                    onResumeTest = null,
                    onAnalytics  = { navController.navigate(Routes.ANALYSIS) },
                    onDailyQuiz  = { navController.navigate(Routes.TESTS) },
                    onPYQ        = { navController.navigate(Routes.PYQ) },
                    onPremiumClick = { navController.navigate(Routes.PREMIUM) }
                )
            }

            composable(
                Routes.TESTS,
                enterTransition    = tabEnter,
                exitTransition     = tabExit,
                popEnterTransition = tabEnter,
                popExitTransition  = tabExit
            ) {
                val homeVM: HomeViewModel = hiltViewModel()
                val state by homeVM.state.collectAsState()
                MockTestsScreen(
                    exams     = state.exams,
                    isLoading = state.isLoading,
                    onExamClick = { navController.navigate(Routes.exam(it.slug)) }
                )
            }

            composable(
                Routes.ANALYSIS,
                enterTransition    = tabEnter,
                exitTransition     = tabExit,
                popEnterTransition = tabEnter,
                popExitTransition  = tabExit
            ) {
                val homeVM: HomeViewModel = hiltViewModel()
                val state by homeVM.state.collectAsState()
                AnalysisScreen(
                    stats = state.stats, attempts = state.recentAttempts, isLoading = state.isLoading
                )
            }

            composable(
                Routes.PROFILE,
                enterTransition    = tabEnter,
                exitTransition     = tabExit,
                popEnterTransition = tabEnter,
                popExitTransition  = tabExit
            ) {
                val homeVM: HomeViewModel = hiltViewModel()
                val state by homeVM.state.collectAsState()
                ProfileScreen(
                    profile = state.profile, stats = state.stats, isAdmin = isAdmin,
                    onAdminClick  = { navController.navigate(Routes.ADMIN) },
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onSubmitFeedback = { msg -> homeVM.submitFeedback(msg) },
                    feedbackResult   = state.feedbackResult,
                    onClearFeedback  = { homeVM.clearFeedbackResult() }
                )
            }

            // ━━ DRILL-DOWN SCREENS (slide) ━━
            composable(Routes.EDIT_PROFILE) {
                val homeVM:    HomeViewModel    = hiltViewModel()
                val homeState by homeVM.state.collectAsState()
                val profileVM: ProfileViewModel = hiltViewModel()
                val editState by profileVM.state.collectAsState()
                EditProfileScreen(
                    profile  = homeState.profile,
                    onSaveProfile    = { name, phone -> profileVM.saveProfile(name, phone) },
                    onChangePassword = { pwd -> profileVM.changePassword(pwd) },
                    onBack           = { homeVM.loadHome(); navController.popBackStack() },
                    isSaving    = editState.isSaving,
                    saveMessage = editState.message
                )
            }

            composable(Routes.PREMIUM) {
                PremiumScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PYQ) {
                val pyqVM: PYQViewModel = hiltViewModel()
                val pyqState by pyqVM.state.collectAsState()
                PYQScreen(
                    state = pyqState,
                    vm = pyqVM,
                    onBack = { navController.popBackStack() },
                    onStartTest = { navController.navigate(Routes.test(it)) }
                )
            }

            composable(
                Routes.EXAM,
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) { entry ->
                val slug = entry.arguments?.getString("slug") ?: return@composable
                val examVM: ExamViewModel = hiltViewModel()
                val state by examVM.state.collectAsState()
                LaunchedEffect(slug) { examVM.loadExam(slug) }
                ExamScreen(
                    state = state, onBack = { navController.popBackStack() },
                    onSubExamSelect = { examVM.selectSubExam(it) },
                    onSubExamBack   = { examVM.clearSubExam() },
                    onStartTest     = { navController.navigate(Routes.test(it)) }
                )
            }

            composable(
                Routes.TEST,
                arguments       = listOf(navArgument("testSetId") { type = NavType.StringType }),
                // Test screen: slide up from bottom for immersive feel
                enterTransition = {
                    slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 2 } +
                    fadeIn(tween(320))
                },
                popExitTransition = {
                    slideOutVertically(tween(360, easing = FastOutSlowInEasing)) { it / 2 } +
                    fadeOut(tween(280))
                }
            ) { entry ->
                val testSetId = entry.arguments?.getString("testSetId") ?: return@composable
                val testVM: TestViewModel = hiltViewModel()
                val state by testVM.state.collectAsState()
                LaunchedEffect(testSetId) { testVM.loadTest(testSetId) }
                TestScreen(
                    state = state, onStart = { testVM.startTest() },
                    onSelectOption = { qId, optId -> testVM.selectOption(qId, optId) },
                    onNext   = { testVM.nextQuestion() },
                    onPrev   = { testVM.prevQuestion() },
                    onGoTo   = { testVM.goToQuestion(it) },
                    onToggleMark    = { testVM.toggleMarkForReview(it) },
                    onTogglePalette = { testVM.togglePalette() },
                    onShowSubmit    = { testVM.showSubmitDialog() },
                    onHideSubmit    = { testVM.hideSubmitDialog() },
                    onSubmit = { testVM.submitTest() },
                    onBack   = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    answeredCount   = testVM.answeredCount,
                    unansweredCount = testVM.unansweredCount,
                    markedCount     = testVM.markedCount,
                    getStatus       = { testVM.getQuestionStatus(it) }
                )
            }

            composable(Routes.ADMIN) {
                val adminVM: AdminViewModel = hiltViewModel()
                val adminState by adminVM.state.collectAsState()
                AdminScreen(
                    state  = adminState,
                    onBack = { navController.popBackStack() },
                    vm     = adminVM
                )
            }
        }

        if (showBottomBar) {
            NeuBottomBar(
                items        = navItems,
                currentRoute = currentRoute,
                onItemClick  = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

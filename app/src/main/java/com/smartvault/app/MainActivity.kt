package com.smartvault.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartvault.app.scanner.ParsedCardData
import com.smartvault.app.ui.navigation.Screen
import com.smartvault.app.ui.screens.auth.LockScreen
import com.smartvault.app.ui.screens.auth.SetupPinScreen
import com.smartvault.app.ui.screens.cards.AddEditCardScreen
import com.smartvault.app.ui.screens.cards.CardDetailScreen
import com.smartvault.app.ui.screens.cards.CardsListScreen
import com.smartvault.app.ui.screens.dashboard.DashboardScreen
import com.smartvault.app.ui.screens.expenses.AddExpenseScreen
import com.smartvault.app.ui.screens.expenses.ExpenseReportsScreen
import com.smartvault.app.ui.screens.expenses.ExpensesScreen
import com.smartvault.app.ui.screens.family.FamilyProfileScreen
import com.smartvault.app.ui.screens.family.FamilyScreen
import com.smartvault.app.ui.screens.notes.AddEditNoteScreen
import com.smartvault.app.ui.screens.notes.NotesScreen
import com.smartvault.app.ui.screens.radar.ExpiryRadarScreen
import com.smartvault.app.ui.screens.scanner.CameraScanScreen
import com.smartvault.app.ui.screens.scanner.ScanReviewScreen
import com.smartvault.app.ui.screens.search.UniversalSearchScreen
import com.smartvault.app.ui.screens.settings.BackupRestoreScreen
import com.smartvault.app.ui.screens.settings.SettingsScreen
import com.smartvault.app.ui.screens.visiting.ContactDetailScreen
import com.smartvault.app.ui.screens.visiting.VisitingCardsScreen
import com.smartvault.app.ui.theme.SmartVaultTheme

class MainActivity : FragmentActivity() {

    private lateinit var app: SmartVaultApp
    private var isAppUnlockedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as SmartVaultApp

        updateScreenshotProtection()

        setContent {
            SmartVaultTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isPinConfigured = remember { app.securityManager.isPinSetup() }
                    val isUnlocked by remember { isAppUnlockedState }

                    // Ephemeral state for camera scan review handoff
                    var currentParsedData by remember { mutableStateOf<ParsedCardData?>(null) }
                    var currentFrontImagePath by remember { mutableStateOf<String?>(null) }
                    var currentBackImagePath by remember { mutableStateOf<String?>(null) }

                    val startDestination = remember {
                        if (!isPinConfigured) {
                            Screen.SetupPin.route
                        } else if (!isUnlocked) {
                            Screen.Lock.route
                        } else {
                            Screen.Dashboard.route
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        // 1. PIN Setup Screen
                        composable(Screen.SetupPin.route) {
                            SetupPinScreen(
                                securityManager = app.securityManager,
                                onPinSetupComplete = {
                                    isAppUnlockedState.value = true
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.SetupPin.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Lock Screen
                        composable(Screen.Lock.route) {
                            LockScreen(
                                securityManager = app.securityManager,
                                onUnlocked = {
                                    isAppUnlockedState.value = true
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Lock.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 3. Dashboard Screen
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigate = { route -> navController.navigate(route) },
                                onLockApp = { lockVault(navController) },
                                onCardClick = { cardId ->
                                    navController.navigate(Screen.CardDetail.createRoute(cardId))
                                }
                            )
                        }

                        // 4. Cards List Screen
                        composable(Screen.CardsList.route) {
                            CardsListScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigate = { route -> navController.navigate(route) },
                                onLockApp = { lockVault(navController) },
                                onCardClick = { cardId ->
                                    navController.navigate(Screen.CardDetail.createRoute(cardId))
                                }
                            )
                        }

                        // 5. Card Detail Screen
                        composable(
                            route = Screen.CardDetail.route,
                            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
                            CardDetailScreen(
                                cardId = cardId,
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() },
                                onEditCard = { id -> navController.navigate(Screen.AddEditCard.createRoute(id)) },
                                onAddExpense = { id -> navController.navigate(Screen.AddExpense.createRoute(id)) },
                                onAddNote = { id -> navController.navigate(Screen.AddEditNote.createRoute(cardId = id)) },
                                onLockApp = { lockVault(navController) }
                            )
                        }

                        // 6. Add / Edit Card Screen
                        composable(
                            route = Screen.AddEditCard.route,
                            arguments = listOf(navArgument("cardId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            })
                        ) { backStackEntry ->
                            val cardId = backStackEntry.arguments?.getLong("cardId")
                            AddEditCardScreen(
                                cardId = if (cardId == 0L) null else cardId,
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() },
                                onLaunchScanner = { navController.navigate(Screen.CameraScan.route) }
                            )
                        }

                        // 7. Camera OCR Scanner Screen
                        composable(Screen.CameraScan.route) {
                            CameraScanScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onScanSuccess = { parsed, front, back ->
                                    currentParsedData = parsed
                                    currentFrontImagePath = front
                                    currentBackImagePath = back
                                    navController.navigate(Screen.ScanReview.route) {
                                        popUpTo(Screen.CameraScan.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 8. Scan Review Screen
                        composable(Screen.ScanReview.route) {
                            if (currentParsedData != null) {
                                ScanReviewScreen(
                                    parsedData = currentParsedData!!,
                                    frontImagePath = currentFrontImagePath,
                                    backImagePath = currentBackImagePath,
                                    repository = app.repository,
                                    onNavigateBack = { navController.popBackStack() },
                                    onSavedSuccess = {
                                        navController.navigate(Screen.CardsList.route) {
                                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                                        }
                                    }
                                )
                            }
                        }

                        // 9. Family Members Screen
                        composable(Screen.Family.route) {
                            FamilyScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigate = { route -> navController.navigate(route) },
                                onLockApp = { lockVault(navController) },
                                onMemberClick = { memberId ->
                                    navController.navigate(Screen.FamilyProfile.createRoute(memberId))
                                }
                            )
                        }

                        // 10. Family Profile Screen
                        composable(
                            route = Screen.FamilyProfile.route,
                            arguments = listOf(navArgument("memberId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val memberId = backStackEntry.arguments?.getLong("memberId") ?: 0L
                            FamilyProfileScreen(
                                memberId = memberId,
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() },
                                onCardClick = { cardId -> navController.navigate(Screen.CardDetail.createRoute(cardId)) },
                                onAddCard = { id -> navController.navigate(Screen.AddEditCard.createRoute()) },
                                onLockApp = { lockVault(navController) }
                            )
                        }

                        // 11. Visiting Cards Directory Screen
                        composable(Screen.VisitingCards.route) {
                            VisitingCardsScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigate = { route -> navController.navigate(route) },
                                onLockApp = { lockVault(navController) },
                                onContactClick = { contactId ->
                                    navController.navigate(Screen.ContactDetail.createRoute(contactId))
                                }
                            )
                        }

                        // 12. Contact Detail Screen
                        composable(
                            route = Screen.ContactDetail.route,
                            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
                            ContactDetailScreen(
                                contactId = contactId,
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() },
                                onLockApp = { lockVault(navController) }
                            )
                        }

                        // 13. Expense Manager Screen
                        composable(Screen.Expenses.route) {
                            ExpensesScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigate = { route -> navController.navigate(route) },
                                onLockApp = { lockVault(navController) }
                            )
                        }

                        // 14. Add Expense Screen
                        composable(
                            route = Screen.AddExpense.route,
                            arguments = listOf(navArgument("cardId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            })
                        ) { backStackEntry ->
                            val cardId = backStackEntry.arguments?.getLong("cardId")
                            AddExpenseScreen(
                                initialCardId = if (cardId == 0L) null else cardId,
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 15. Expense Reports Screen
                        composable(Screen.ExpenseReports.route) {
                            ExpenseReportsScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 16. Notes Screen
                        composable(Screen.Notes.route) {
                            NotesScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigate = { route -> navController.navigate(route) },
                                onLockApp = { lockVault(navController) },
                                onNoteClick = { noteId ->
                                    navController.navigate(Screen.AddEditNote.createRoute(noteId = noteId))
                                }
                            )
                        }

                        // 17. Add / Edit Note Screen
                        composable(
                            route = Screen.AddEditNote.route,
                            arguments = listOf(
                                navArgument("noteId") { type = NavType.LongType; defaultValue = 0L },
                                navArgument("cardId") { type = NavType.LongType; defaultValue = 0L },
                                navArgument("contactId") { type = NavType.LongType; defaultValue = 0L }
                            )
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getLong("noteId")
                            val cardId = backStackEntry.arguments?.getLong("cardId")
                            val contactId = backStackEntry.arguments?.getLong("contactId")

                            AddEditNoteScreen(
                                noteId = if (noteId == 0L) null else noteId,
                                cardId = if (cardId == 0L) null else cardId,
                                contactId = if (contactId == 0L) null else contactId,
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 18. Universal Search Screen
                        composable(Screen.UniversalSearch.route) {
                            UniversalSearchScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() },
                                onCardClick = { cardId -> navController.navigate(Screen.CardDetail.createRoute(cardId)) },
                                onContactClick = { contactId -> navController.navigate(Screen.ContactDetail.createRoute(contactId)) },
                                onNoteClick = { noteId -> navController.navigate(Screen.AddEditNote.createRoute(noteId = noteId)) }
                            )
                        }

                        // 19. Card Expiry Radar Screen
                        composable(Screen.ExpiryRadar.route) {
                            ExpiryRadarScreen(
                                repository = app.repository,
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() },
                                onCardClick = { cardId -> navController.navigate(Screen.CardDetail.createRoute(cardId)) }
                            )
                        }

                        // 20. Settings Screen
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                securityManager = app.securityManager,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenBackupRestore = { navController.navigate(Screen.BackupRestore.route) },
                                onLockApp = { lockVault(navController) }
                            )
                        }

                        // 21. Backup & Restore Screen
                        composable(Screen.BackupRestore.route) {
                            BackupRestoreScreen(
                                repository = app.repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateScreenshotProtection() {
        if (app.securityManager.isScreenshotProtectionEnabled()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onPause() {
        super.onPause()
        app.securityManager.onAppPaused()
    }

    override fun onResume() {
        super.onResume()
        updateScreenshotProtection()
        val isStillUnlocked = app.securityManager.onAppResumed()
        if (!isStillUnlocked && app.securityManager.isPinSetup()) {
            isAppUnlockedState.value = false
        }
    }

    private fun lockVault(navController: androidx.navigation.NavController) {
        app.securityManager.lockApp()
        isAppUnlockedState.value = false
        navController.navigate(Screen.Lock.route) {
            popUpTo(0) { inclusive = true }
        }
    }
}

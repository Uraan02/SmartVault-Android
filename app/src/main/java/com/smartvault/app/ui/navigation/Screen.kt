package com.smartvault.app.ui.navigation

sealed class Screen(val route: String) {
    object SetupPin : Screen("setup_pin")
    object Lock : Screen("lock")
    object Dashboard : Screen("dashboard")
    object CardsList : Screen("cards_list")
    object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: Long) = "card_detail/$cardId"
    }
    object AddEditCard : Screen("add_edit_card?cardId={cardId}") {
        fun createRoute(cardId: Long? = null) = if (cardId != null) "add_edit_card?cardId=$cardId" else "add_edit_card"
    }
    object CameraScan : Screen("camera_scan")
    object ScanReview : Screen("scan_review")
    object Family : Screen("family")
    object FamilyProfile : Screen("family_profile/{memberId}") {
        fun createRoute(memberId: Long) = "family_profile/$memberId"
    }
    object VisitingCards : Screen("visiting_cards")
    object ContactDetail : Screen("contact_detail/{contactId}") {
        fun createRoute(contactId: Long) = "contact_detail/$contactId"
    }
    object Expenses : Screen("expenses")
    object AddExpense : Screen("add_expense?cardId={cardId}") {
        fun createRoute(cardId: Long? = null) = if (cardId != null) "add_expense?cardId=$cardId" else "add_expense"
    }
    object ExpenseReports : Screen("expense_reports")
    object Notes : Screen("notes")
    object AddEditNote : Screen("add_edit_note?noteId={noteId}&cardId={cardId}&contactId={contactId}") {
        fun createRoute(noteId: Long? = null, cardId: Long? = null, contactId: Long? = null): String {
            val params = mutableListOf<String>()
            if (noteId != null) params.add("noteId=$noteId")
            if (cardId != null) params.add("cardId=$cardId")
            if (contactId != null) params.add("contactId=$contactId")
            return if (params.isNotEmpty()) "add_edit_note?${params.joinToString("&")}" else "add_edit_note"
        }
    }
    object UniversalSearch : Screen("universal_search")
    object ExpiryRadar : Screen("expiry_radar")
    object Settings : Screen("settings")
    object BackupRestore : Screen("backup_restore")
}

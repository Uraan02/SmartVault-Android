package com.smartvault.app.scanner

data class ParsedCardData(
    val suggestedCategory: String, // BANK_PAYMENT, VISITING, IDENTITY_MEMBERSHIP, PERSONAL
    val name: String? = null,
    val cardholderName: String? = null,
    val cardNumber: String? = null,
    val issuingBank: String? = null,
    val expiryDate: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val address: String? = null,
    val designation: String? = null,
    val company: String? = null,
    val rawText: String
)

object CardTextParser {

    private val BANK_KEYWORDS = listOf(
        "HBL", "MCB", "UBL", "Meezan", "Allied Bank", "Bank Alfalah", "Faysal Bank", "Askari",
        "Standard Chartered", "Chase", "Citi", "Bank of America", "Wells Fargo", "HSBC",
        "Barclays", "National Bank", "NBP", "BankIslami", "Dubai Islamic", "JS Bank",
        "Visa", "Mastercard", "UnionPay", "PayPak", "American Express", "Amex"
    )

    private val IDENTITY_KEYWORDS = listOf(
        "CNIC", "National Identity", "Identity Card", "Driving Licence", "Driving License",
        "Passport", "Government of Pakistan", "Nadra", "Citizen", "Resident", "Membership"
    )

    private val DESIGNATION_KEYWORDS = listOf(
        "Director", "Manager", "CEO", "Chief", "Founder", "President", "Engineer",
        "Doctor", "Dr.", "Advocate", "Consultant", "Partner", "Executive", "Architect",
        "Officer", "Professor", "Lecturer", "Advisor", "Specialist"
    )

    fun parse(rawText: String): ParsedCardData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var detectedBank: String? = null
        var detectedCardNumber: String? = null
        var detectedExpiry: String? = null
        var detectedPhone: String? = null
        var detectedEmail: String? = null
        var detectedWebsite: String? = null
        var detectedDesignation: String? = null
        var detectedCompany: String? = null
        var detectedPersonName: String? = null

        var isBankCard = false
        var isIdentityCard = false
        var isVisitingCard = false

        // 1. Look for Card Number (12-19 digits, possibly space or hyphen separated)
        val cardNumberRegex = Regex("""\b(?:\d[ -]*?){13,19}\b""")
        for (line in lines) {
            val match = cardNumberRegex.find(line)
            if (match != null) {
                val clean = match.value.replace(Regex("""[ -]"""), "")
                if (clean.length in 13..19 && !clean.startsWith("03") && !clean.startsWith("+")) {
                    detectedCardNumber = clean.chunked(4).joinToString(" ")
                    isBankCard = true
                    break
                }
            }
        }

        // 2. Look for Expiry (MM/YY or MM/YYYY or VALID THRU)
        val expiryRegex = Regex("""(?:VALID\s+THRU|EXPIRY|EXP|GOOD\s+THRU)?\s*([0-1][0-9]\s*[/.-]\s*(?:20)?[2-9][0-9])""", RegexOption.IGNORE_CASE)
        for (line in lines) {
            val match = expiryRegex.find(line)
            if (match != null) {
                detectedExpiry = match.groupValues[1].replace(Regex("""\s+"""), "")
                break
            }
        }

        // 3. Email regex
        val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
        for (line in lines) {
            val match = emailRegex.find(line)
            if (match != null) {
                detectedEmail = match.value
                isVisitingCard = true
                break
            }
        }

        // 4. Website regex
        val webRegex = Regex("""(?:https?://)?(?:www\.)?[a-zA-Z0-9.-]+\.(?:com|org|net|pk|io|co|ai|edu|gov)(?:/[^\s]*)?""", RegexOption.IGNORE_CASE)
        for (line in lines) {
            val match = webRegex.find(line)
            if (match != null && !match.value.contains("@")) {
                detectedWebsite = match.value
                isVisitingCard = true
                break
            }
        }

        // 5. Phone number regex
        val phoneRegex = Regex("""(?:\+?\d{1,3}[ -]?)?(?:(?:\(0?\d{1,4}\))|0?\d{1,4})[ -]?\d{3,4}[ -]?\d{3,4}""")
        for (line in lines) {
            if (line == detectedCardNumber) continue
            val match = phoneRegex.find(line)
            if (match != null && match.value.replace(Regex("""\D"""), "").length in 9..14) {
                detectedPhone = match.value.trim()
                isVisitingCard = true
                break
            }
        }

        // 6. Bank / Company Keywords
        for (line in lines) {
            for (bank in BANK_KEYWORDS) {
                if (line.contains(bank, ignoreCase = true)) {
                    detectedBank = bank
                    isBankCard = true
                    break
                }
            }
            if (detectedBank != null) break
        }

        // 7. Identity check
        for (line in lines) {
            for (idKeyword in IDENTITY_KEYWORDS) {
                if (line.contains(idKeyword, ignoreCase = true)) {
                    isIdentityCard = true
                    break
                }
            }
        }

        // 8. Designation & Company & Person Name
        for (line in lines) {
            for (desig in DESIGNATION_KEYWORDS) {
                if (line.contains(desig, ignoreCase = true)) {
                    detectedDesignation = line
                    isVisitingCard = true
                    break
                }
            }
        }

        // Extract probable person name (words with title case, not bank, not number)
        for (line in lines) {
            if (line == detectedCardNumber || line == detectedExpiry || line == detectedPhone || line == detectedEmail || line == detectedWebsite) continue
            if (line.any { it.isDigit() }) continue
            if (line.length in 3..35 && line.split(" ").size in 1..4) {
                if (detectedBank != null && line.contains(detectedBank, ignoreCase = true)) continue
                if (detectedPersonName == null) {
                    detectedPersonName = line
                } else if (detectedCompany == null && isVisitingCard) {
                    detectedCompany = line
                }
            }
        }

        val category = when {
            isBankCard -> "BANK_PAYMENT"
            isIdentityCard -> "IDENTITY_MEMBERSHIP"
            isVisitingCard -> "VISITING"
            else -> "PERSONAL"
        }

        val fallbackName = when (category) {
            "BANK_PAYMENT" -> "${detectedBank ?: "Bank"} Card"
            "VISITING" -> detectedPersonName ?: detectedCompany ?: "Visiting Card"
            "IDENTITY_MEMBERSHIP" -> "Identity Card"
            else -> detectedPersonName ?: "Personal Card"
        }

        return ParsedCardData(
            suggestedCategory = category,
            name = fallbackName,
            cardholderName = detectedPersonName,
            cardNumber = detectedCardNumber,
            issuingBank = detectedBank,
            expiryDate = detectedExpiry,
            phone = detectedPhone,
            email = detectedEmail,
            website = detectedWebsite,
            designation = detectedDesignation,
            company = detectedCompany,
            rawText = rawText
        )
    }
}

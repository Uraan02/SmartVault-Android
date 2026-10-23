package com.smartvault.app.util

import com.smartvault.app.data.local.entity.TransactionEntity
import com.smartvault.app.data.local.entity.VisitingContactEntity

object CsvExporter {
    fun exportTransactions(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Type,Category,Amount,Currency,Description,Notes\n")

        for (tx in transactions) {
            val dateStr = DateUtil.formatDateTime(tx.date)
            val desc = tx.description.replace("\"", "\"\"")
            val notes = (tx.notes ?: "").replace("\"", "\"\"")
            sb.append("${tx.id},\"$dateStr\",${tx.type},\"${tx.category}\",${tx.amount},${tx.currency},\"$desc\",\"$notes\"\n")
        }
        return sb.toString()
    }
}

object VcfExporter {
    fun exportContact(contact: VisitingContactEntity): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCARD\n")
        sb.append("VERSION:3.0\n")
        sb.append("FN:${contact.name}\n")
        if (!contact.company.isNullOrBlank()) {
            sb.append("ORG:${contact.company}\n")
        }
        if (!contact.designation.isNullOrBlank()) {
            sb.append("TITLE:${contact.designation}\n")
        }
        if (!contact.phone.isNullOrBlank()) {
            sb.append("TEL;TYPE=CELL:${contact.phone}\n")
        }
        if (!contact.email.isNullOrBlank()) {
            sb.append("EMAIL;TYPE=INTERNET:${contact.email}\n")
        }
        if (!contact.website.isNullOrBlank()) {
            sb.append("URL:${contact.website}\n")
        }
        if (!contact.address.isNullOrBlank()) {
            sb.append("ADR;TYPE=WORK:;;${contact.address};;;;\n")
        }
        if (!contact.meetingNotes.isNullOrBlank()) {
            sb.append("NOTE:${contact.meetingNotes}\n")
        }
        sb.append("END:VCARD\n")
        return sb.toString()
    }

    fun exportContacts(contacts: List<VisitingContactEntity>): String {
        return contacts.joinToString("\n") { exportContact(it) }
    }
}

package com.smartvault.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager

class SmartVaultApp : Application() {

    lateinit var securityManager: SecurityManager
        private set

    lateinit var repository: VaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        securityManager = SecurityManager(this)
        repository = VaultRepository(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "SmartVault Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for card renewals, follow-ups, and notes reminders"
                enableVibration(true)
            }

            val securityChannel = NotificationChannel(
                CHANNEL_SECURITY,
                "Security & Lock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Vault auto-lock status and security notices"
            }

            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(securityChannel)
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "channel_smartvault_reminders"
        const val CHANNEL_SECURITY = "channel_smartvault_security"
    }
}

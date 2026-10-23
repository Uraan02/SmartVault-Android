package com.smartvault.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartVaultTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    isPrivacyMode: Boolean = false,
    onTogglePrivacy: () -> Unit = {},
    onLockApp: () -> Unit = {},
    onOpenSearch: (() -> Unit)? = null,
    onOpenExpiryRadar: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    expiringCount: Int = 0
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isPrivacyMode) {
                    Text(
                        text = "🔒 Privacy Mode Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning
                    )
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "SmartVault Shield",
                        tint = IndigoPrimary
                    )
                }
            }
        },
        actions = {
            // Privacy mode toggle
            IconButton(onClick = onTogglePrivacy) {
                Icon(
                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isPrivacyMode) "Disable Privacy Mode" else "Enable Privacy Mode",
                    tint = if (isPrivacyMode) AmberWarning else MaterialTheme.colorScheme.onBackground
                )
            }

            // Expiry radar button
            if (onOpenExpiryRadar != null) {
                IconButton(onClick = onOpenExpiryRadar) {
                    BadgedBox(
                        badge = {
                            if (expiringCount > 0) {
                                Badge(
                                    containerColor = CrimsonError,
                                    contentColor = Color.White
                                ) {
                                    Text(text = expiringCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Expiry Radar",
                            tint = if (expiringCount > 0) AmberWarning else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Universal search
            if (onOpenSearch != null) {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Universal Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Settings
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Immediate Lock
            IconButton(onClick = onLockApp) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Vault",
                    tint = CrimsonError
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun SmartVaultBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("dashboard", "Home", Icons.Default.Dashboard),
            Triple("cards_list", "Cards", Icons.Default.CreditCard),
            Triple("family", "Family", Icons.Default.Groups),
            Triple("visiting_cards", "Visiting", Icons.Default.ContactPhone),
            Triple("expenses", "Expenses", Icons.Default.AccountBalanceWallet),
            Triple("notes", "Notes", Icons.Default.StickyNote2)
        )

        items.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ObsidianSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun MaskedCardNumber(
    cardNumber: String?,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRevealed by remember { mutableStateOf(false) }

    val displayText = remember(cardNumber, isPrivacyMode, isRevealed) {
        if (cardNumber.isNullOrBlank()) {
            "No card number recorded"
        } else if (isPrivacyMode) {
            "•••• •••• •••• ••••"
        } else if (isRevealed) {
            cardNumber
        } else {
            val clean = cardNumber.replace(" ", "")
            if (clean.length >= 4) {
                "•••• •••• •••• ${clean.takeLast(4)}"
            } else {
                "••••"
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurfaceVariant,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = IndigoLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TextPrimaryDark,
                modifier = Modifier.weight(1f)
            )

            if (!cardNumber.isNullOrBlank() && !isPrivacyMode) {
                IconButton(
                    onClick = { isRevealed = !isRevealed },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isRevealed) "Hide" else "Reveal",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Card Number", cardNumber)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Card number copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = IndigoLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureModal(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onScanCard: () -> Unit,
    onAddCardManual: () -> Unit,
    onAddNote: () -> Unit,
    onAddExpense: () -> Unit
) {
    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = ObsidianSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Quick Action Vault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Fast capture from anywhere in SmartVault",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = "Scan Card",
                        subtitle = "Camera OCR",
                        icon = Icons.Default.CameraAlt,
                        color = IndigoPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onScanCard()
                        }
                    )
                    QuickActionTile(
                        title = "Manual Card",
                        subtitle = "Add Record",
                        icon = Icons.Default.CreditCard,
                        color = EmeraldAccent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onAddCardManual()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = "New Note",
                        subtitle = "Checklist / Note",
                        icon = Icons.Default.StickyNote2,
                        color = AmberWarning,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onAddNote()
                        }
                    )
                    QuickActionTile(
                        title = "Add Expense",
                        subtitle = "Income / Expense",
                        icon = Icons.Default.ReceiptLong,
                        color = RosePink,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                            onAddExpense()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
fun EmptyVaultState(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ObsidianSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

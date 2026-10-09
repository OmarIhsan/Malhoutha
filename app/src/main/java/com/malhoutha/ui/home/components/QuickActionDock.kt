package com.malhoutha.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malhoutha.ui.components.GlassFloatingBar
import com.malhoutha.ui.theme.LocalMalhouthaColors
import com.malhoutha.ui.theme.LocalMalhouthaTypography

@Composable
fun QuickActionDock(
    onOpenDocument: () -> Unit,
    onCaptureSlide: () -> Unit,
    onOpenLexiconDrawer: () -> Unit,
    onOpenDecks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassFloatingBar(
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Action 1: Open Document (Primary prominent action)
            DockItem(
                icon = Icons.Rounded.FileOpen,
                label = "Open / فتح",
                accentColor = colors.clinicalTeal,
                isPrimary = true,
                onClick = onOpenDocument
            )

            // Action 2: Capture Clinical Slide
            DockItem(
                icon = Icons.Rounded.CameraAlt,
                label = "Snap Slide",
                accentColor = colors.royalIndigo,
                isPrimary = false,
                onClick = onCaptureSlide
            )

            // Action 3: Lexicon Drawer
            DockItem(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                label = "Lexicon / المعجم",
                accentColor = colors.accentPurple,
                isPrimary = false,
                onClick = onOpenLexiconDrawer
            )

            // Action 4: Vocabulary Decks
            DockItem(
                icon = Icons.Rounded.School,
                label = "Decks / البطاقات",
                accentColor = colors.warningAmber,
                isPrimary = false,
                onClick = onOpenDecks
            )
        }
    }
}
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (isPrimary) 40.dp else 36.dp)
                .clip(CircleShape)
                .background(if (isPrimary) accentColor else accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPrimary) Color.White else accentColor,
                modifier = Modifier.size(if (isPrimary) 22.dp else 20.dp)
            )
        }

        Text(
            text = label,
            style = typography.bodySmall.copy(fontSize = 10.sp, fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium),
            color = if (isPrimary) colors.textPrimary else colors.textSecondary
        )
    }
}

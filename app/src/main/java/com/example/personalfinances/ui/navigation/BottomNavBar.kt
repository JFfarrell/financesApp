package com.example.personalfinances.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.personalfinances.ui.theme.wallet

data class BottomNavItem(
    val destination: AppDestination,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(AppDestination.Home,     Icons.Default.Home,               "Home"),
    BottomNavItem(AppDestination.Calendar, Icons.AutoMirrored.Filled.List,   "Transactions"),
    BottomNavItem(AppDestination.Savings,  Icons.Default.Savings,            "Savings")
)

/** Switches to the tab with [route], keeping each tab's state and avoiding duplicate entries. */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(AppDestination.Home.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Floating navigation bar with the three tabs. The bar is transparent around the pill so screens
 * show through, and it pads for the system navigation bar.
 */
@Composable
fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val wallet = MaterialTheme.wallet

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(CircleShape)
                .background(wallet.navContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            bottomNavItems.forEach { item ->
                NavItem(
                    item = item,
                    selected = currentRoute == item.destination.route,
                    onClick = { navController.navigateToTab(item.destination.route) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(item: BottomNavItem, selected: Boolean, onClick: () -> Unit) {
    val wallet = MaterialTheme.wallet
    val color = if (selected) wallet.onNavIndicator else wallet.muted
    Column(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (selected) wallet.navIndicator else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

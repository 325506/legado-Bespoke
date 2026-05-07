package io.legado.app.ui.compose.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class LegadoBottomNavItem(
    val id: Int,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun LegadoBottomNavigation(
    items: List<LegadoBottomNavItem>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) },
                selected = selectedItem == item.id,
                onClick = { onItemSelected(item.id) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun RowScope.LegadoBottomNavigationItem(
    item: LegadoBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        icon = {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label
            )
        },
        label = { Text(text = item.label) },
        selected = selected,
        onClick = onClick,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

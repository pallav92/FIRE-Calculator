package com.finance.firecalculator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Visualizer(
        label = "Visualizer",
        selectedIcon = Icons.Filled.AutoGraph,
        unselectedIcon = Icons.Outlined.AutoGraph
    ),
    Calculator(
        label = "Plan Studio",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    ),
    Profiles(
        label = "Scenarios",
        selectedIcon = Icons.Filled.FolderShared,
        unselectedIcon = Icons.Outlined.FolderShared
    )
}

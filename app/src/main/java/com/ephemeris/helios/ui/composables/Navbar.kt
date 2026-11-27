package com.ephemeris.helios.ui.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.Routes

data class NavbarItem(
    val label: Int,
    val icon: Int,
    val filledIcon: Int,
    val route: String
)

object NavbarItems {
    val items = listOf(
        NavbarItem(
            label = R.string.home,
            icon = R.drawable.ic_home,
            filledIcon = R.drawable.ic_home_filled,
            route = Routes.Home.route
        ),
        NavbarItem(
            label=R.string.exposure,
            icon = R.drawable.ic_beach_access,
            filledIcon = R.drawable.ic_beach_access_filled,
            route = Routes.Exposure.route
        ),
        NavbarItem(
            label = R.string.sun,
            icon = R.drawable.ic_wb_sunny,
            filledIcon = R.drawable.ic_wb_sunny_filled,
            route = Routes.Sun.route
        ),
        NavbarItem(
            label = R.string.moon,
            icon = R.drawable.ic_moon_stars,
            filledIcon = R.drawable.ic_moon_stars_filled,
            route = Routes.Moon.route
        ),
        NavbarItem(
            label = R.string.maps,
            icon = R.drawable.ic_explore,
            filledIcon = R.drawable.ic_explore_filled,
            route = Routes.Maps.route
        )
    )
}


@Composable
fun Navbar(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow  // was surfaceContainer
    ) {
        NavbarItems.items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val label = stringResource(item.label)
            NavigationBarItem(
                selected = selected,
                label = { Text(label) },
                icon = {
                    Icon(
                        imageVector = if (selected) {
                            ImageVector.vectorResource(item.filledIcon)
                        } else {
                            ImageVector.vectorResource(item.icon)
                        },
                        contentDescription = label
                    )
                },
                onClick = { onSelectTab(index) }
            )
        }
    }
}
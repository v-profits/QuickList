package com.quicklist.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.quicklist.app.data.ListEntity
import com.quicklist.app.data.TYPE_SHOPPING

const val ICON_LIST = 0
const val ICON_CART = 1
const val ICON_CHECK = 2

/** Icons a list can have. The index is stored in the database (ListEntity.icon). */
val ListIconSet: List<ImageVector> = listOf(
    Icons.AutoMirrored.Filled.FormatListBulleted, // 0 - neutral default
    Icons.Default.ShoppingCart,                   // 1
    Icons.Default.CheckCircle,                    // 2
    Icons.Default.Home,
    Icons.Default.Work,
    Icons.Default.Favorite,
    Icons.Default.Star,
    Icons.Default.Flight,
    Icons.Default.School,
    Icons.Default.FitnessCenter,
    Icons.Default.Restaurant,
    Icons.Default.Pets
)

/** Lists created before icons existed have icon = -1: fall back to the old type-based icon. */
fun resolveListIconIndex(icon: Int, type: Int): Int =
    if (icon in ListIconSet.indices) icon else if (type == TYPE_SHOPPING) ICON_CART else ICON_CHECK

fun listIconFor(list: ListEntity): ImageVector = ListIconSet[resolveListIconIndex(list.icon, list.type)]

package com.quicklist.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

data class NavTarget(val listId: Long, val focus: Boolean)

@Composable
fun AppNav(vm: AppViewModel, target: NavTarget?, onTargetHandled: () -> Unit) {
    val nav = rememberNavController()

    LaunchedEffect(target) {
        if (target != null) {
            nav.navigate("list/${target.listId}?focus=${target.focus}") {
                popUpTo("home")
                launchSingleTop = true
            }
            onTargetHandled()
        }
    }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                onOpenList = { id, focus -> nav.navigate("list/$id?focus=$focus") },
                onSettings = { nav.navigate("settings") }
            )
        }
        composable(
            route = "list/{id}?focus={focus}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("focus") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            ListScreen(
                vm = vm,
                listId = entry.arguments?.getLong("id") ?: -1L,
                focusInput = entry.arguments?.getBoolean("focus") ?: false,
                onBack = { nav.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}

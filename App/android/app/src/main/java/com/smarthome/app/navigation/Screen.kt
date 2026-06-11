package com.smarthome.app.navigation

sealed class Screen(val route: String) {
    data object Splash      : Screen("splash")
    data object Login       : Screen("login")
    data object Register    : Screen("register")
    data object Otp         : Screen("otp")
    data object Dashboard   : Screen("dashboard")
    data object Device      : Screen("device/{deviceId}") {
        fun withId(deviceId: String) = "device/$deviceId"
    }
    data object Scenes      : Screen("scenes")
    data object Provisioning: Screen("provisioning")
    data object Schedules      : Screen("schedules")
    data object Notifications: Screen("notifications")
    data object Settings    : Screen("settings")
}

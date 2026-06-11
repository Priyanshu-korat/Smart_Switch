package com.smarthome.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smarthome.app.ui.notifications.NotificationsScreen
import com.smarthome.app.ui.notifications.NotificationsViewModel
import com.smarthome.app.ui.schedules.SchedulesScreen
import com.smarthome.app.ui.schedules.SchedulesViewModel
import com.smarthome.app.ui.auth.*
import com.smarthome.app.ui.dashboard.DashboardScreen
import com.smarthome.app.ui.dashboard.DashboardViewModel
import com.smarthome.app.ui.device.DeviceControlScreen
import com.smarthome.app.ui.device.DeviceControlViewModel
import com.smarthome.app.ui.provisioning.ProvisioningScreen
import com.smarthome.app.ui.provisioning.ProvisioningViewModel
import com.smarthome.app.ui.scenes.ScenesScreen
import com.smarthome.app.ui.scenes.ScenesViewModel
import com.smarthome.core.common.UiEvent

@Composable
fun AppNavGraph(
    navController: NavHostController,
    targetDestination: String? = null
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {

        // ── Splash ──────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            val vm: SplashViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.Navigate -> {
                            val route = if (event.route == "dashboard" && targetDestination != null) {
                                targetDestination
                            } else {
                                event.route
                            }
                            navController.navigate(route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                        else -> Unit
                    }
                }
            }
            SplashScreen()
        }

        // ── Login ────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            val vm: AuthViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.Navigate -> navController.navigate(event.route)
                        else -> Unit
                    }
                }
            }
            LoginScreen(
                state = state,
                onEmailChange = vm::onEmailChanged,
                onPasswordChange = vm::onPasswordChanged,
                onLoginClick = vm::login,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        // ── Register ─────────────────────────────────────────────────
        composable(Screen.Register.route) {
            val vm: AuthViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.Navigate -> navController.navigate(event.route)
                        else -> Unit
                    }
                }
            }
            RegisterScreen(
                state = state,
                onNameChange = vm::onNameChanged,
                onEmailChange = vm::onEmailChanged,
                onPasswordChange = vm::onPasswordChanged,
                onRegisterClick = vm::register,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── OTP ──────────────────────────────────────────────────────
        composable(Screen.Otp.route) {
            val vm: AuthViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.Navigate -> navController.navigate(event.route) {
                            popUpTo(Screen.Login.route)
                        }
                        else -> Unit
                    }
                }
            }
            OtpScreen(
                state = state,
                onOtpChange = vm::onOtpChanged,
                onVerifyClick = vm::verifyOtp
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            timber.log.Timber.d("AppNavGraph: Composing Dashboard screen entry")
            val vm: DashboardViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            timber.log.Timber.d("AppNavGraph: state collected: $state")
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { event ->
                    timber.log.Timber.d("AppNavGraph: collected event: $event")
                    when (event) {
                        is UiEvent.Navigate -> navController.navigate(event.route)
                        else -> Unit
                    }
                }
            }
            DashboardScreen(
                state = state,
                onToggleSwitch = vm::onToggleSwitch,
                onAddDevice = vm::onAddDeviceClick,
                onDeviceCardClick = vm::onDeviceCardClick,
                onScenesClick = { navController.navigate(Screen.Scenes.route) }
            )
        }

        // ── Device Control ────────────────────────────────────────────
        composable(Screen.Device.route) {
            val vm: DeviceControlViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.NavigateUp -> navController.popBackStack()
                        is UiEvent.ShowSnackbar -> { /* wire to scaffold snackbar host */ }
                        else -> Unit
                    }
                }
            }
            DeviceControlScreen(
                state = state,
                onToggleSwitch = vm::onToggleSwitch,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Scenes ────────────────────────────────────────────────────
        composable(Screen.Scenes.route) {
            val vm: ScenesViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.ShowSnackbar -> { /* wire to scaffold */ }
                        else -> Unit
                    }
                }
            }
            ScenesScreen(
                state = state,
                onActivateScene = vm::onActivateScene,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Provisioning ──────────────────────────────────────────────
        composable(Screen.Provisioning.route) {
            val vm: ProvisioningViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            ProvisioningScreen(
                state = state,
                onSsidChange = vm::onSsidChanged,
                onPasswordChange = vm::onPasswordChanged,
                onNextStep = vm::goToStep,
                onProvision = vm::startProvisioning
            )
        }

        // ── Notifications ─────────────────────────────────────────────
        composable(Screen.Notifications.route) {
            val vm: NotificationsViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { }
            }
            NotificationsScreen(
                state = state,
                onMarkRead = vm::onMarkRead,
                onMarkAllRead = vm::onMarkAllRead,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Schedules ─────────────────────────────────────────────────
        composable(Screen.Schedules.route) {
            val vm: SchedulesViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            LaunchedEffect(Unit) {
                vm.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.ShowSnackbar -> { /* wire to scaffold snackbar */ }
                        else -> Unit
                    }
                }
            }
            SchedulesScreen(
                state = state,
                onShowCreate = vm::onShowCreateDialog,
                onDismissCreate = vm::onDismissDialog,
                onDayToggled = vm::onDayToggled,
                onHourChanged = vm::onHourChanged,
                onMinuteChanged = vm::onMinuteChanged,
                onLabelChanged = vm::onLabelChanged,
                onTargetStateChanged = vm::onTargetStateChanged,
                onCreateSchedule = vm::onCreateSchedule,
                onDeleteSchedule = vm::onDeleteSchedule,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

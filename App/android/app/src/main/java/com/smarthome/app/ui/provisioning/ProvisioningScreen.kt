package com.smarthome.app.ui.provisioning

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ProvisioningScreen(
    state: ProvisioningState,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNextStep: (ProvisioningStep) -> Unit,
    onProvision: () -> Unit
) {
    AnimatedContent(targetState = state.currentStep, label = "provision_step") { step ->
        when (step) {
            ProvisioningStep.Instructions -> InstructionsStep(
                onNext = { onNextStep(ProvisioningStep.ConnectToDevice) }
            )
            ProvisioningStep.ConnectToDevice -> ConnectToDeviceStep(
                onNext = { onNextStep(ProvisioningStep.EnterCredentials) }
            )
            ProvisioningStep.EnterCredentials -> EnterCredentialsStep(
                state = state,
                onSsidChange = onSsidChange,
                onPasswordChange = onPasswordChange,
                onProvision = onProvision
            )
            ProvisioningStep.Provisioning -> ProvisioningProgressStep()
            ProvisioningStep.Success -> ProvisioningSuccessStep()
        }
    }
}

@Composable
private fun InstructionsStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Add a Device", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Make sure your device is powered on and blinking. It will broadcast a Wi-Fi hotspot named SmartHome_XXXXXX.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}

@Composable
private fun ConnectToDeviceStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Connect to Device", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "1. Open your phone's Wi-Fi settings\n2. Connect to the network named SmartHome_XXXXXX\n3. Come back here and tap Continue",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("I'm Connected")
        }
    }
}

@Composable
private fun EnterCredentialsStep(
    state: ProvisioningState,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onProvision: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your Home Wi-Fi", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Enter the Wi-Fi credentials for your home network. The device will connect to this.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.ssidInput,
            onValueChange = onSsidChange,
            label = { Text("Network Name (SSID)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.passwordInput,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onProvision,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.ssidInput.isNotBlank()
        ) {
            Text("Connect Device")
        }
    }
}

@Composable
private fun ProvisioningProgressStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Connecting device to your network…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ProvisioningSuccessStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color(0xFF4CAF50))
        Spacer(Modifier.height(24.dp))
        Text("Device Added!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Your device is connected and will appear on the dashboard shortly.", style = MaterialTheme.typography.bodyMedium)
    }
}

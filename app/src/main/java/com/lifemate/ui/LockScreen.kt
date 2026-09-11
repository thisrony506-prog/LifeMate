package com.lifemate.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.lifemate.utils.SecureStore
import kotlinx.coroutines.*

fun biometricAvailable(activity: FragmentActivity) = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
fun authenticateBiometric(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { if (errorCode !in setOf(BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_CANCELED)) onError(errString.toString()) }
    })
    prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Welcome back to LifeMate").setSubtitle("Unlock your personal space")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).setNegativeButtonText("Use PIN").build())
}
@Composable fun PinField(value: String, onChange: (String) -> Unit, label: String = "Your PIN") {
    OutlinedTextField(value, { onChange(it.filter(Char::isDigit).take(12)) }, Modifier.fillMaxWidth(), label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
}
@Composable fun LockScreen(activity: FragmentActivity, secure: SecureStore, biometrics: Boolean, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(30.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp)); Text("Your space, kept safe.", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp)); Text("Enter your PIN to unlock LifeMate.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp)); PinField(pin, { pin = it })
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp))
        Spacer(Modifier.height(16.dp))
        Button({ scope.launch { busy = true; val valid = withContext(Dispatchers.IO) { secure.verifyPin(pin) }; busy = false; if (valid) onUnlock() else { pin = ""; error = if (secure.retrySeconds() > 0) "Too many attempts. Try again in ${secure.retrySeconds()} seconds." else "That PIN doesn't match. Try again." } } }, enabled = !busy && pin.length >= 6, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(if (busy) "Unlocking…" else "Unlock LifeMate") }
        if (biometrics && biometricAvailable(activity)) TextButton({ authenticateBiometric(activity, onUnlock) { error = it } }) { Icon(Icons.Outlined.Fingerprint, null); Text(" Use biometrics") }
        Spacer(Modifier.height(24.dp))
        Text("Forgot your PIN? There is no backdoor. Reinstalling clears local data; restore a previously exported backup after setup.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun PinSetupDialog(secure: SecureStore, remove: Boolean, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var current by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (remove) "Turn off app lock?" else "Protect your personal space") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (secure.hasPin()) PinField(current, { current = it }, "Current PIN")
            if (!remove) { Text("Choose a 6–12 digit PIN. Remember it—there's no online reset."); PinField(pin, { pin = it }, "New PIN"); PinField(confirm, { confirm = it }, "Confirm PIN") }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton({ scope.launch {
        busy = true
        try {
            error = withContext(Dispatchers.IO) {
                when {
                    secure.hasPin() && !secure.verifyPin(current) -> if (secure.retrySeconds() > 0) "Try again in ${secure.retrySeconds()} seconds." else "Current PIN is incorrect."
                    !remove && (pin.length !in 6..12 || pin != confirm) -> "Use 6–12 digits and matching PINs."
                    else -> { if (remove) secure.removePin() else secure.setPin(pin); "" }
                }
            }
            if (error.isBlank()) onSaved()
        } catch (_: Exception) { error = "Secure storage is unavailable. Please try again." }
        finally { busy = false }
    } }, enabled = !busy) { Text(if (remove) "Turn off" else "Save PIN") } }, dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Cancel") } })
}

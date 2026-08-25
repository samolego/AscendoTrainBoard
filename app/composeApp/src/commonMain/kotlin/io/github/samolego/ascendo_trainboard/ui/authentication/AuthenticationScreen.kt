package io.github.samolego.ascendo_trainboard.ui.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.ui.components.PinInput
import io.github.samolego.ascendo_trainboard.ui.components.error.ErrorBottomBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime

@Composable
fun AuthenticatedUserView(
    username: String,
    onNavigateBack: () -> Boolean = { true }
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Verified User",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = username,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = { onNavigateBack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
            ) {
                Text(
                    text = "Nazaj na smeri",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun AuthenticationScreen(
    viewModel: AuthenticationViewModel,
    onNavigateBack: () -> Boolean
) {
    val state by viewModel.state.collectAsState()
    var remainingSeconds by remember { mutableStateOf(0L) }
    val goBack = {
        viewModel.clearError();
        onNavigateBack()
    }

    LaunchedEffect(state.timeoutUntil) {
        state.timeoutUntil?.let { targetTime ->
            // Launch in the LaunchedEffect scope
            while (true) {
                val now = now()
                val remaining = (targetTime - now).inWholeSeconds

                if (remaining <= 0) {
                    viewModel.clearTimeout()
                    remainingSeconds = 0
                    break
                }

                remainingSeconds = remaining
                delay(1000)
            }
        }
    }

    Scaffold(
        bottomBar = {
            ErrorBottomBar(
                error = state.error,
                onDismiss = viewModel::clearError
            )
        }
    ) {
        Box(
            modifier = Modifier.padding(it)
        ) {
            val scope = rememberCoroutineScope()

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { goBack() }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Spacer(modifier = Modifier.weight(1f))
                if (state.isAuthenticated) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.logout()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            }

            if (state.isAuthenticated) {
                AuthenticatedUserView(
                    username = state.username,
                    onNavigateBack = goBack,
                )
                return@Scaffold
            }

            var username by remember { mutableStateOf("") }
            var isRegistering by remember { mutableStateOf(false) }

            var pin by remember { mutableStateOf("") }
            var confirmPin by remember { mutableStateOf("") }

            val pinsMatch = !isRegistering || pin == confirmPin
            val canSubmit = username.isNotEmpty() && pin.length == 4 &&
                    (!isRegistering || (confirmPin.length == 4 && pinsMatch))


            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Person Icon",
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Uporabniško ime") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PinInput(
                        label = "PIN",
                        pin = pin,
                        onPinChange = { pin = it }
                    )

                    if (isRegistering) {
                        Spacer(modifier = Modifier.height(24.dp))

                        PinInput(
                            label = "Ponovi PIN",
                            pin = confirmPin,
                            onPinChange = { confirmPin = it },
                            isError = confirmPin.isNotEmpty() && !pinsMatch,
                            errorMessage = "PINa se ne ujemata"
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                if (isRegistering) {
                                    viewModel.register(username, pin)
                                } else {
                                    viewModel.login(username, pin)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = canSubmit && !state.isLoading && state.timeoutUntil == null
                    ) {
                        Text(
                            text = when {
                                state.isLoading -> "Nalaganje..."
                                remainingSeconds > 0L -> "Počakaj ${remainingSeconds}s"
                                isRegistering -> "Ustvari račun"
                                else -> "Prijava"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    state.error?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            isRegistering = !isRegistering
                            confirmPin = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRegistering) "Že imaš račun? Prijavi se" else "Ustvari nov račun"
                        )
                    }
                }
            }
        }
    }
}

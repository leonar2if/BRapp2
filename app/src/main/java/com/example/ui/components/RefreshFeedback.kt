package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Estado compartido para el feedback de "datos actualizados" (secciones 1.2 y 1.3):
 * un toast que se muestra 2s y un punto de frescura que queda verde 3s. Un solo
 * lugar para esta lógica, usado tanto por ClientHomeScreen como AdminHomeScreen.
 */
class RefreshFeedbackState(private val scope: CoroutineScope) {
    var toastMessage by mutableStateOf<String?>(null)
        private set
    var isFresh by mutableStateOf(false)
        private set

    private var toastJob: Job? = null
    private var freshJob: Job? = null

    fun notifyRefreshed(message: String = "Datos actualizados") {
        toastJob?.cancel()
        freshJob?.cancel()

        toastMessage = message
        isFresh = true

        toastJob = scope.launch {
            delay(2000)
            toastMessage = null
        }
        freshJob = scope.launch {
            delay(3000)
            isFresh = false
        }
    }
}

@Composable
fun rememberRefreshFeedbackState(): RefreshFeedbackState {
    val scope = rememberCoroutineScope()
    return remember { RefreshFeedbackState(scope) }
}

/** Toast/snackbar inferior (sección 1.2). Usar dentro de un Box. */
@Composable
fun BoxScope.RefreshToast(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 32.dp),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF323232),
            shadowElevation = 6.dp
        ) {
            Text(
                text = message ?: "",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

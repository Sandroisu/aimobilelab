package dev.sandroisu.aimobilelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sandroisu.aimobilelab.BuildConfig
import dev.sandroisu.aimobilelab.api.client.GeminiLlmStreamClient
import dev.sandroisu.aimobilelab.core.chat.Role
import dev.sandroisu.aimobilelab.core.llm.LlmStreamClient
import dev.sandroisu.aimobilelab.core.llm.LlmStreamClientImpl
import dev.sandroisu.aimobilelab.presentation.state.ChatMessageUi
import dev.sandroisu.aimobilelab.presentation.state.ChatScreenState
import dev.sandroisu.aimobilelab.presentation.viewmodel.ChatViewModel
import dev.sandroisu.aimobilelab.presentation.viewmodel.ChatViewModel.ChatViewModelFactory
import dev.sandroisu.aimobilelab.ui.theme.AiMobileLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val llmStreamClient: LlmStreamClient =
            GeminiLlmStreamClient(
                apiKeyProvider = { BuildConfig.GEMINI_API_KEY },
            )
        val viewModel: ChatViewModel =
            ViewModelProvider(
                this,
                ChatViewModelFactory
                    (llmStreamClient),
            )[ChatViewModel::class.java]
        setContent {
            AiMobileLabTheme {
                Chat(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Chat(viewModel: ChatViewModel) {
    val state = viewModel.screenState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state.value,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::send,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
    )
}

@Composable
fun ChatScreen(
    state: ChatScreenState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            state = listState,
        ) {
            items(state.messages, key = { it.id }) { m ->
                MessageBubble(m)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            when {
                state.canCancel -> Button(onClick = onCancel) { Text("Cancel") }
                state.canRetry -> Button(onClick = onRetry) { Text("Retry") }
                else -> Button(onClick = onSend, enabled = state.canSend) { Text("Send") }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessageUi) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = if (m.role == Role.User) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = m.text,
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .padding(12.dp),
            textAlign = if (m.role == Role.User) TextAlign.End else TextAlign.Start,
        )
    }
}

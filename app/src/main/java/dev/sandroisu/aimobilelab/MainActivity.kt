package dev.sandroisu.aimobilelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import dev.sandroisu.aimobilelab.core.llm.LLMStreamClient
import dev.sandroisu.aimobilelab.core.llm.LlmStreamClientImpl
import dev.sandroisu.aimobilelab.presentation.viewmodel.ChatViewModel
import dev.sandroisu.aimobilelab.ui.theme.AiMobileLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val llmStreamClient: LLMStreamClient = LlmStreamClientImpl()
        val viewModel: ChatViewModel = ChatViewModel(llmStreamClient = llmStreamClient)
        setContent {
            AiMobileLabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val screenState = viewModel.screenState.collectAsState()
    Column {
        LazyColumn {
            item { Text(text = screenState.value?.partialText ?: "") }

        }
        Button(onClick = { viewModel.startRequest("key1", 1) }) {
            Text(text = "Send")
        }
    }
}

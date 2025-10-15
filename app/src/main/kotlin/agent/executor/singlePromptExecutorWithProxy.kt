package agent.executor

import Config
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*


val httpClientWithProxy = createHttpClientWithOptionalProxy(Config.PROXY_URL)

val singlePromptExecutorWithProxy = SingleLLMPromptExecutor(
    OpenAILLMClient(
        apiKey = Config.OPENAI_API_KEY,
        baseClient = httpClientWithProxy
    )
)

val openRouterExecutor = SingleLLMPromptExecutor(
    llmClient = OpenRouterLLMClient(
        apiKey = "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"
    )
)

val multipleLLMExecutor = MultiLLMPromptExecutor(
    LLMProvider.OpenAI to OpenAILLMClient(
        apiKey = Config.OPENAI_API_KEY,
        baseClient = httpClientWithProxy
    ),
    LLMProvider.OpenRouter to OpenRouterLLMClient(
        apiKey = "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"
    )
)

fun createHttpClientWithOptionalProxy(proxyUrl: String?): HttpClient {
    return if (proxyUrl != null) {
        HttpClient(CIO) {
            engine {
                proxy = ProxyBuilder.http(Url(proxyUrl))
            }
        }
    } else {
        HttpClient(CIO)
    }
}
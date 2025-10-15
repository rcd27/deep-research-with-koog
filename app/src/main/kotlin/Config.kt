import io.github.cdimascio.dotenv.dotenv

object Config {
    private val dotenv = dotenv() {
        directory = "./app"
    }

    val PROXY_URL: String? = dotenv["PROXY_URL"]
    val LANGSMITH_API_KEY = dotenv["LANGSMITH_API_KEY"] ?: error("No LANGSMITH_API_KEY in .env file")
    val TAVILY_API_KEY = dotenv["TAVILY_API_KEY"] ?: error("No TAVILY_API_KEY in .env file")
    val OPENAI_API_KEY = dotenv["OPENAI_API_KEY"] ?: error("No OPENAI_API_KEY in .env file")
    val LANGSMITH_TRACING = dotenv["LANGSMITH_TRACING"] ?: false
    val LANGSMITH_PROJECT = dotenv["LANGSMITH_PROJECT"] ?: ""
}
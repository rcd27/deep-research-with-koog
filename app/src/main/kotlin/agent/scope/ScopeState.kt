package agent.scope

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("Input state for the full agent - only contains messages from user input.")
object AgentInputState // TODO: should contain input messages from user

@Serializable
@LLMDescription("Main state for the full multi-agent research system.")
data class AgentState(
    @property: LLMDescription("Research brief generated from user conversation history")
    val researchBrief: String?,
    @property: LLMDescription("Messages exchanged with the supervisor agent for coordination")
    val supervisorMessages: List<String>?,
    @property: LLMDescription("Raw unprocessed research notes collected during the research phase")
    val rawNotes: List<String>?,
    @property: LLMDescription("Processed and structured notes ready for report generation")
    val notes: List<String>,
    @property: LLMDescription("Final formatted research report")
    val finalReport: String
)

@Serializable
@LLMDescription("Schema for user clarification decision and questions.")
data class ClarifyWithUser(
    @property: LLMDescription("Whether the user needs to be asked a clarifying question.")
    val needClarification: Boolean,
    @property: LLMDescription("A question to ask the user to clarify the report scope.")
    val question: String,
    @property: LLMDescription("Verify message that we will start research after the user has provided the necessary information.")
    val verification: String
)

@Serializable
@LLMDescription("Schema for structured research brief generation.")
data class ResearchQuestion(
    @property: LLMDescription("A research question that will be used to guide the research.")
    val researchBrief: String
)
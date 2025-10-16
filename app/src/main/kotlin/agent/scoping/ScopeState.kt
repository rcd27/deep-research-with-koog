package agent.scoping

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
@LLMDescription("Schema for structured research brief generation.")
data class ResearchQuestion(
    @property: LLMDescription("A research question that will be used to guide the research.")
    val researchBrief: String
)
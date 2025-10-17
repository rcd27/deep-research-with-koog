package agent.supervisor

import agent.scoping.ResearchQuestion
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase

fun AIAgentSubgraphBuilderBase<*, *>.supervisor(
): AIAgentNodeDelegate<String, ResearchQuestion> = node<String, ResearchQuestion>("supervisor") {

    TODO("implement")
}
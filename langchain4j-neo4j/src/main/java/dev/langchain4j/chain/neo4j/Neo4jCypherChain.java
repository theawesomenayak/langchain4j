package dev.langchain4j.chain.neo4j;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.graph.neo4j.Neo4jGraph;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Builder;
import org.neo4j.driver.Record;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

public class Neo4jCypherChain implements Chain<String, String> {

    private static final PromptTemplate CYPHER_PROMPT_TEMPLATE = PromptTemplate.from("""
            Based on the Neo4j graph schema below, write a Cypher query that would answer the user's question:
            {{schema}}

            Question: {{question}}
            Cypher query:
            """);

    private static final PromptTemplate RESPONSE_PROMPT_TEMPLATE = PromptTemplate.from("""
            Based on the the question, Cypher query, and Cypher response, write a natural language response:
            Question: {{question}}
            Cypher query: {{query}}
            Cypher Response: {{response}}
            """);

    private final Neo4jGraph graph;
    private final ChatLanguageModel chatLanguageModel;

    @Builder
    public Neo4jCypherChain(Neo4jGraph graph, ChatLanguageModel chatLanguageModel) {

        this.graph = ensureNotNull(graph, "graph");
        this.chatLanguageModel = ensureNotNull(chatLanguageModel, "chatLanguageModel");
    }

    @Override
    public String execute(String question) {

        String schema = this.graph.getSchema();
        Prompt cypherPrompt = CYPHER_PROMPT_TEMPLATE.apply(Map.of("schema", schema, "question", question));
        String query = this.chatLanguageModel.generate(cypherPrompt.text());
        List<Record> response = this.graph.executeRead(query);
        Prompt responsePrompt = RESPONSE_PROMPT_TEMPLATE.apply(Map.of("question", question, "query", query, "response", response));
        return this.chatLanguageModel.generate(responsePrompt.text());
    }
}

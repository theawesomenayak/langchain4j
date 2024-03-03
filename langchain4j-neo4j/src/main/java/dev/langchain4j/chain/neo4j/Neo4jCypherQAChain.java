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
import java.util.Optional;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

public class Neo4jCypherQAChain implements Chain<String, String> {

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
    private final ChatLanguageModel cypherLanguageModel;
    private final ChatLanguageModel responseLanguageModel;

    @Builder
    public Neo4jCypherQAChain(Neo4jGraph graph, ChatLanguageModel chatLanguageModel, ChatLanguageModel cypherLanguageModel, ChatLanguageModel responseLanguageModel) {

        this.graph = ensureNotNull(graph, "graph");
        if (null == chatLanguageModel && null == cypherLanguageModel) {
            throw new IllegalArgumentException("Either chatLanguageModel or cypherLanguageModel must be provided");
        }
        if (null == chatLanguageModel && null == responseLanguageModel) {
            throw new IllegalArgumentException("Either chatLanguageModel or responseLanguageModel must be provided");
        }
        if (null != cypherLanguageModel && null != responseLanguageModel && null != chatLanguageModel) {
            throw new IllegalArgumentException("You can specify either chatLanguageModel and cypherLanguageModel, or only chatLanguageModel, but not all three");
        }
        if (null != chatLanguageModel && null != cypherLanguageModel) {
            throw new IllegalArgumentException("Either chatLanguageModel or cypherLanguageModel must be provided, but not both");
        }
        if (null != chatLanguageModel && null != responseLanguageModel) {
            throw new IllegalArgumentException("Either chatLanguageModel or responseLanguageModel must be provided, but not both");
        }
        this.chatLanguageModel = chatLanguageModel;
        this.cypherLanguageModel = cypherLanguageModel;
        this.responseLanguageModel = responseLanguageModel;
    }

    @Override
    public String execute(String question) {

        String schema = graph.getSchema();
        Prompt cypherPrompt = CYPHER_PROMPT_TEMPLATE.apply(Map.of("schema", schema, "question", question));
        String query = Optional.ofNullable(cypherLanguageModel).orElse(chatLanguageModel).generate(cypherPrompt.text());
        List<Record> response = graph.executeRead(query);
        Prompt responsePrompt = RESPONSE_PROMPT_TEMPLATE.apply(Map.of("question", question, "query", query, "response", response));
        return Optional.ofNullable(responseLanguageModel).orElse(chatLanguageModel).generate(responsePrompt.text());
    }
}

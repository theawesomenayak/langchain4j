package dev.langchain4j.chain.neo4j;

import dev.langchain4j.graph.neo4j.Neo4jGraph;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Neo4JCypherQAChainTest {

    @Mock
    private static Neo4jGraph neo4jGraph;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private ChatLanguageModel cypherLanguageModel;

    @Mock
    private ChatLanguageModel responseLanguageModel;

    @Test
    void executeShouldThrowExceptionWhenBothChatAndCypherLanguageModelsAreNull() {

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Neo4jCypherQAChain.builder().graph(neo4jGraph).responseLanguageModel(responseLanguageModel).build();
        });

        // then
        assertEquals("Either chatLanguageModel or cypherLanguageModel must be provided", exception.getMessage());
    }

    @Test
    void executeShouldThrowExceptionWhenBothChatAndResponseLanguageModelsAreNull() {

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Neo4jCypherQAChain.builder().graph(neo4jGraph).cypherLanguageModel(cypherLanguageModel).build();
        });

        // then
        assertEquals("Either chatLanguageModel or responseLanguageModel must be provided", exception.getMessage());
    }

    @Test
    void executeShouldThrowExceptionWhenAllLanguageModelsAreProvided() {

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Neo4jCypherQAChain.builder().graph(neo4jGraph).chatLanguageModel(chatLanguageModel).cypherLanguageModel(cypherLanguageModel).responseLanguageModel(responseLanguageModel).build();
        });

        // then
        assertEquals("You can specify either chatLanguageModel and cypherLanguageModel, or only chatLanguageModel, but not all three", exception.getMessage());
    }

    @Test
    void executeShouldThrowExceptionWhenBothChatAndCypherLanguageModelsAreProvided() {

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Neo4jCypherQAChain.builder().graph(neo4jGraph).chatLanguageModel(chatLanguageModel).cypherLanguageModel(cypherLanguageModel).build();
        });

        // then
        assertEquals("Either chatLanguageModel or cypherLanguageModel must be provided, but not both", exception.getMessage());
    }

    @Test
    void executeShouldThrowExceptionWhenBothChatAndResponseLanguageModelsAreProvided() {

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Neo4jCypherQAChain.builder().graph(neo4jGraph).chatLanguageModel(chatLanguageModel).responseLanguageModel(responseLanguageModel).build();
        });

        // then
        assertEquals("Either chatLanguageModel or responseLanguageModel must be provided, but not both", exception.getMessage());
    }

    @Test
    void executeShouldReturnExpectedResponseWhenOnlyChatLanguageModelIsUsed() {

        // given
        doReturn("schema").when(neo4jGraph).getSchema();

        when(chatLanguageModel.generate(anyString()))
                .thenReturn("MATCH (n:Person) RETURN n.name AS name")
                .thenReturn("The name of the person is John.");

        Neo4jCypherQAChain chain = Neo4jCypherQAChain.builder().graph(neo4jGraph).chatLanguageModel(chatLanguageModel).build();

        String question = "What is the name of the person?";

        // when
        String actualResponse = chain.execute(question);

        // then
        assertEquals("The name of the person is John.", actualResponse);
    }

    @Test
    void executeShouldReturnExpectedResponseWhenCypherAndResponseLanguageModelsAreUsed() {

        // given
        doReturn("schema").when(neo4jGraph).getSchema();
        doReturn("MATCH (n:Person) RETURN n.name AS name").when(cypherLanguageModel).generate(anyString());
        doReturn("The name of the person is John.").when(responseLanguageModel).generate(anyString());

        Neo4jCypherQAChain chain = Neo4jCypherQAChain.builder().graph(neo4jGraph).cypherLanguageModel(cypherLanguageModel).responseLanguageModel(responseLanguageModel).build();

        String question = "What is the name of the person?";

        // when
        String actualResponse = chain.execute(question);

        // then
        assertEquals("The name of the person is John.", actualResponse);
    }
}
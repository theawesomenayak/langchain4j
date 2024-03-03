package dev.langchain4j.chain.neo4j;

import dev.langchain4j.graph.neo4j.Neo4jGraph;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class Neo4jCypherChainTest {

    @Container
    private static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.16.0"))
            .withoutAuthentication()
            .withLabsPlugins("apoc");

    private static Neo4jGraph neo4jGraph;
    private static Neo4jCypherChain chain;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @BeforeAll
    static void startContainer() {

        neo4jContainer.start();
        Driver driver = GraphDatabase.driver(neo4jContainer.getBoltUrl(), AuthTokens.none());
        neo4jGraph = Neo4jGraph.builder().driver(driver).build();
        neo4jGraph.executeWrite("CREATE (n:Person {name: 'John'})");
        neo4jGraph.refreshSchema();
    }

    @AfterAll
    static void stopContainer() {

        neo4jGraph.close();
        neo4jContainer.stop();
    }

    @Test
    void executeShouldReturnExpectedResponse() {

        // given
        when(chatLanguageModel.generate(anyString()))
                .thenReturn("MATCH (n:Person) RETURN n.name AS name")
                .thenReturn("The name of the person is John.");

        Neo4jCypherChain chain = Neo4jCypherChain.builder().graph(neo4jGraph).chatLanguageModel(chatLanguageModel).build();

        String question = "What is the name of the person?";

        // when
        String actualResponse = chain.execute(question);

        // then
        assertEquals("The name of the person is John.", actualResponse);
    }
}
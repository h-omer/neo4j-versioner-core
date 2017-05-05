package org.homer.graph.versioner.procedure;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.harness.junit.Neo4jRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * InitTest class, it contains all the method used to test Init class methods
 */
public class InitTest {

    // This rule starts a Neo4j instance
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()

            // This is the function we want to test
            .withProcedure(Init.class);

    @Test
    public void shouldCreateAnEmptyEntityWithoutAState() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig())) {
            // Given
            Session session = driver.session();

            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity')");
            Node entity = session.run("MATCH (e:Entity) RETURN e").single().get("e").asNode();
            StatementResult entityEmptyResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateEmptyResult = session.run("MATCH (s:State) RETURN s");

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0l));
            assertThat(entityEmptyResult.single().get("props").asMap().isEmpty(), equalTo(true));
            assertThat(stateEmptyResult.hasNext(), equalTo(false));
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithoutAState() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig())) {
            // Given
            Session session = driver.session();

            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity', {key:'value'})");
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateEmptyResult = session.run("MATCH (s:State) RETURN s");

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0l));
            assertThat(entityResult.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(stateEmptyResult.hasNext(), equalTo(false));
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithAStateAndItsProperties() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig())) {
            // Given
            Session session = driver.session();

            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'})");
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateResult = session.run("MATCH (s:State) RETURN s");
            StatementResult stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");
            StatementResult currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            StatementResult hasStatusResult = session.run("MATCH (e:Entity)-[:HAS_STATE]->(s:State) RETURN id(e) as id");
            StatementResult contextResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) return s.context as context");

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0l));
            assertThat(entityResult.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(stateResult.single().get("s").asNode().id(), equalTo(1l));
            assertThat(stateProps.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(currentResult.single().get("id").asLong(), equalTo(0l));
            assertThat(hasStatusResult.single().get("id").asLong(), equalTo(0l));
            assertThat(contextResult.single().get("context").asString(), equalTo("Initial State"));
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithAStateAndItsPropertiesAndAContext() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig())) {
            // Given
            Session session = driver.session();

            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'}, 'context')");
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateResult = session.run("MATCH (s:State) RETURN s");
            StatementResult stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");
            StatementResult currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            StatementResult hasStatusResult = session.run("MATCH (e:Entity)-[:HAS_STATE]->(s:State) RETURN id(e) as id");
            StatementResult contextResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) return s.context as context");

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0l));
            assertThat(entityResult.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(stateResult.single().get("s").asNode().id(), equalTo(1l));
            assertThat(stateProps.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(currentResult.single().get("id").asLong(), equalTo(0l));
            assertThat(hasStatusResult.single().get("id").asLong(), equalTo(0l));
            assertThat(contextResult.single().get("context").asString(), equalTo("context"));
        }
    }
}
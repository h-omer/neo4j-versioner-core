package org.homer.versioner.core.procedure;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.harness.junit.Neo4jRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.homer.versioner.core.Utility.convertEpochToLocalDateTime;
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

    /*------------------------------*/
    /*             init             */
    /*------------------------------*/
    @Test
    public void shouldCreateAnEntityAndAStateNodeWithoutPropsIfEmptyMapIsPassed() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity')");
            StatementResult stateResult = session.run("MATCH (s:State) RETURN s");
            StatementResult currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            Node state = stateResult.single().get("s").asNode();

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0L));
            assertThat(state.id(), equalTo(1L));
            assertThat(currentResult.single().get("id").asLong(), equalTo(0L));
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithAState() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity', {key:'value'})");
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateResult = session.run("MATCH (s:State) RETURN s");
            StatementResult currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            Node state = stateResult.single().get("s").asNode();

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0L));
            assertThat(entityResult.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(state.id(), equalTo(1L));
            assertThat(currentResult.single().get("id").asLong(), equalTo(0L));
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithAStateAndItsProperties() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'})");
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateResult = session.run("MATCH (s:State) RETURN s");
            StatementResult stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");
            StatementResult currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            StatementResult hasStatusResult = session.run("MATCH (e:Entity)-[:HAS_STATE]->(s:State) RETURN id(e) as id");

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0L));
            assertThat(entityResult.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(stateResult.single().get("s").asNode().id(), equalTo(1L));
            assertThat(stateProps.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(currentResult.single().get("id").asLong(), equalTo(0L));
            assertThat(hasStatusResult.single().get("id").asLong(), equalTo(0L));
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithAStateAndItsPropertiesWithAdditionalLabelButNoDate() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'}, 'Error')");
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateResult = session.run("MATCH (s:State) RETURN s");
            Node state = stateResult.single().get("s").asNode();
            StatementResult stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");
            StatementResult currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            StatementResult hasStatusResult = session.run("MATCH (e:Entity)-[:HAS_STATE]->(s:State) RETURN id(e) as id");

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0L));
            assertThat(entityResult.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(state.id(), equalTo(1L));
            assertThat(stateProps.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(currentResult.single().get("id").asLong(), equalTo(0L));
            assertThat(hasStatusResult.single().get("id").asLong(), equalTo(0L));
            assertThat(state.hasLabel("Error"), equalTo(true));
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithAStateAndItsPropertiesWithAdditionalLabelAndDate() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // When
            StatementResult result = session.run("CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'}, 'Error', localdatetime('1988-10-27T02:46:40'))");
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateResult = session.run("MATCH (s:State) RETURN s");
            Node state = stateResult.single().get("s").asNode();
            StatementResult stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");
            StatementResult currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            StatementResult hasStatusResult = session.run("MATCH (e:Entity)-[:HAS_STATE]->(s:State) RETURN id(e) as id");
            StatementResult hasStatusDateResult = session.run("MATCH (e:Entity)-[rel:CURRENT]->(s:State) RETURN rel.date as date");

            // Then
            assertThat(result.single().get("node").asNode().id(), equalTo(0L));
            assertThat(entityResult.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(state.id(), equalTo(1L));
            assertThat(stateProps.single().get("props").asMap().isEmpty(), equalTo(false));
            assertThat(currentResult.single().get("id").asLong(), equalTo(0L));
            assertThat(hasStatusResult.single().get("id").asLong(), equalTo(0L));
            assertThat(state.hasLabel("Error"), equalTo(true));
            assertThat(hasStatusDateResult.single().get("date").asLocalDateTime(), equalTo(convertEpochToLocalDateTime(593920000000L)));
        }
    }

    @Test
    public void shouldCreateAnRNodeConnectedToTheEntity() throws Throwable {

        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {

            StatementResult result = session.run("CALL graph.versioner.init('Entity')");
            StatementResult rPath = session.run("MATCH rPath = (:R)-[:FOR]->(:Entity) RETURN rPath");

            Assertions.assertThat(result.single().get("node").asNode().id()).isEqualTo(0L);
            Assertions.assertThat(rPath)
                    .hasSize(1)
                    .allMatch(path -> path.get("rPath").asPath().length() == 1);
        }
    }
}
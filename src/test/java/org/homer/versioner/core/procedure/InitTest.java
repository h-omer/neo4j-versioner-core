package org.homer.versioner.core.procedure;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.harness.junit.rule.Neo4jRule;

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
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // When
            Result result = session.run("CALL graph.versioner.init('Entity')");
            Result stateResult = session.run("MATCH (s:State) RETURN s");
            Result currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
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
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // When
            Result result = session.run("CALL graph.versioner.init('Entity', {key:'value'})");
            Result entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            Result stateResult = session.run("MATCH (s:State) RETURN s");
            Result currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
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
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // When
            Result result = session.run("CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'})");
            Result entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            Result stateResult = session.run("MATCH (s:State) RETURN s");
            Result stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");
            Result currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            Result hasStatusResult = session.run("MATCH (e:Entity)-[:HAS_STATE]->(s:State) RETURN id(e) as id");

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
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // When
            Result result = session.run("CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'}, 'Error')");
            Result entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            Result stateResult = session.run("MATCH (s:State) RETURN s");
            Node state = stateResult.single().get("s").asNode();
            Result stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");
            Result currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            Result hasStatusResult = session.run("MATCH (e:Entity)-[:HAS_STATE]->(s:State) RETURN id(e) as id");

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
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // When
            Result result = session.run("CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'}, 'Error', localdatetime('1988-10-27T02:46:40'))");
            Result entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            Result stateResult = session.run("MATCH (s:State) RETURN s");
            Node state = stateResult.single().get("s").asNode();
            Result stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");
            Result currentResult = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN id(e) as id");
            Result hasStatusResult = session.run("MATCH (e:Entity)-[:HAS_STATE]->(s:State) RETURN id(e) as id");
            Result hasStatusDateResult = session.run("MATCH (e:Entity)-[rel:CURRENT]->(s:State) RETURN rel.date as date");

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

        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {

            Result result = session.run("CALL graph.versioner.init('Entity')");
            Result rPath = session.run("MATCH rPath = (:R)-[:FOR]->(:Entity) RETURN rPath");

            Assertions.assertThat(result.single().get("node").asNode().id()).isEqualTo(0L);
            Assertions.assertThat(rPath.list())
                    .hasSize(1)
                    .allMatch(path -> path.get("rPath").asPath().length() == 1);
        }
    }
}
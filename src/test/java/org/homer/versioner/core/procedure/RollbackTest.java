package org.homer.versioner.core.procedure;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.harness.junit.rule.Neo4jRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * RollbackTest class, it contains all the method used to test Rollback class methods
 */
public class RollbackTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()

            // This is the function we want to test
            .withProcedure(Rollback.class);

    /*------------------------------*/
    /*           rollback           */
    /*------------------------------*/

    @Test
    public void shouldGetOldNodeAfterARollbackOnATwoStateEntityNode() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE]->(s:Test) CREATE (sc)-[:PREVIOUS {date:localdatetime('1988-10-26T00:00:00')}]->(s)");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback(e) YIELD node RETURN node");

            // Then
            boolean failure = true;

            while (result.hasNext()) {
                failure = false;
                assertThat(result.next().get("node").asNode().hasLabel("Test"), equalTo(true));
            }

            if (failure) {
                fail();
            }
        }
    }

    @Test
    public void shouldUseExistingRollbackRelationshipToRollBackAgain() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]->(s:State) CREATE (sc)-[:PREVIOUS {date:localdatetime('1988-10-26T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-25T00:00:00'), endDate:localdatetime('1988-10-26T00:00:00')}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (new:State)<-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]-(e:Entity)-[:HAS_STATE {startDate:localdatetime('1988-10-25T00:00:00'), endDate:localdatetime('1988-10-26T00:00:00')}]->(old:State) CREATE (new)-[:PREVIOUS {date:localdatetime('1988-10-25T00:00:00')}]->(old)");

            // When
            session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback(e) YIELD node RETURN node");
            Result finalResult = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback(e) YIELD node RETURN node");

            // Then
            boolean failure = true;

            while (finalResult.hasNext()) {
                failure = false;
                assertThat(finalResult.next().get("node").asNode().hasLabel("Test"), equalTo(true));
            }

            if (failure) {
                fail();
            }
        }
    }

    @Test
    public void shouldGetNullIfThereIsNoPreviousState() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback(e) YIELD node RETURN node");

            // Then
            Assertions.assertThat(result.list()).isEmpty();
        }
    }

    /*------------------------------*/
    /*          rollback.to         */
	/*------------------------------*/

    @Test
    public void shouldRollbackToTheGivenStateNode() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE]->(s:Test) CREATE (sc)-[:PREVIOUS {date:localdatetime('1988-10-26T00:00:00')}]->(s)");

            // When
            Result result = session.run("MATCH (e:Entity)--(s:Test) WITH e, s CALL graph.versioner.rollback.to(e, s) YIELD node RETURN node");

            // Then
            boolean failure = true;

            while (result.hasNext()) {
                failure = false;
                assertThat(result.next().get("node").asNode().hasLabel("Test"), equalTo(true));
            }

            if (failure) {
                fail();
            }
        }
    }

    @Test
    public void shouldGetNullIfTheGivenNodeHasAlreadyBeenRolledBack() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE]->(s:Test) CREATE (sc)-[:PREVIOUS {date:localdatetime('1988-10-26T00:00:00')}]->(s)");
            session.run("MATCH (p:State)<-[:PREVIOUS]-(s:State) CREATE (p)<-[:ROLLBACK]-(s)");

            // When
            Result result = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) WITH e, s CALL graph.versioner.rollback.to(e, s) YIELD node RETURN node");

            // Then
            Assertions.assertThat(result.list()).isEmpty();
        }
    }

    @Test
    public void shouldGetNullIfTheGivenNodeIsTheCurrentState() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");

            // When
            Result result = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) WITH e, s CALL graph.versioner.rollback.to(e, s) YIELD node RETURN node");

            // Then
            Assertions.assertThat(result.list()).isEmpty();
        }
    }

    @Test (expected = ClientException.class)
    public void shouldNotRollbackToTheGivenStateSinceItsADifferentEntityState() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE]->(s:Test) CREATE (sc)-[:PREVIOUS {date:localdatetime('1988-10-26T00:00:00')}]->(s)");
            session.run("CREATE (e:EntityBis {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:EntityBis)-[:CURRENT]->(s:State) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");

            Result result = session.run("MATCH (e:Entity), (:EntityBis)-[:HAS_STATE]->(s:State) WITH e, s CALL graph.versioner.rollback.to(e, s) YIELD node RETURN node");
        }
    }

    /*-------------------------------*/
	/*         rollback.nth          */
	/*-------------------------------*/

    @Test
    public void shouldRollbackNthWorkCorrectly() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(:State:Error {key:'initialValue'})"
                    + "-[:PREVIOUS {date: localdatetime('1988-10-26T00:00:00')}]->(:State {key:'value'})-[:PREVIOUS {date: localdatetime('1988-10-25T00:00:00')}]->(:State:Test {key:'testValue'})");
            session.run("MATCH (e:Entity)-[:CURRENT]->(s:Error) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity), (s:State {key:'value'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity), (s:Test) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-25T00:00:00'), endDate:localdatetime('1988-10-26T00:00:00')}]->(s)");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback.nth(e, 2) YIELD node RETURN node");

            // Then
            boolean failure = true;

            while (result.hasNext()) {
                failure = false;
                assertThat(result.next().get("node").asNode().hasLabel("Test"), equalTo(true));
            }

            session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN s");

            while(!failure && result.hasNext()) {
                assertThat(result.next().get("node").asNode().hasLabel("Test"), equalTo(true));
            }

            if (failure) {
                fail();
            }
        }
    }

    @Test
    public void shouldRollbackNthToZeroWorkCorrectly() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(:State:Error {key:'initialValue'})"
                    + "-[:PREVIOUS]->(:State {key:'value'})-[:PREVIOUS {date: localdatetime('1988-10-26T00:00:00')}]->(:State:Test {key:'testValue'})");
            session.run("MATCH (e:Entity)-[:CURRENT]->(s:Error) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity), (s:State {key:'value'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-26T00:00:00'), endDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e:Entity), (s:Test) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-25T00:00:00'), endDate:localdatetime('1988-10-26T00:00:00')}]->(s)");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback.nth(e, 0) YIELD node RETURN node");

            // Then
            Assertions.assertThat(result.list()).isEmpty();

            result = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN s");

            Assertions.assertThat(result.list())
                    .hasSize(1)
                    .allMatch(node -> node.get("s").asNode().hasLabel("Error"));
        }
    }
}

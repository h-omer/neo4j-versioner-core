package org.homer.versioner.core.procedure;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.harness.junit.Neo4jRule;

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
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:593900000000, endDate:59391000000}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE]->(s:Test) CREATE (sc)-[:PREVIOUS {date:593900000000}]->(s)");

            // When
            StatementResult result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback(e) YIELD node RETURN node");

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
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:593900000000, endDate:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE {startDate:593900000000, endDate:593910000000}]->(s:State) CREATE (sc)-[:PREVIOUS {date:593900000000}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:593800000000, endDate:593900000000}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (new:State)<-[:HAS_STATE {startDate:593900000000, endDate:593910000000}]-(e:Entity)-[:HAS_STATE {startDate:593800000000, endDate:593900000000}]->(old:State) CREATE (new)-[:PREVIOUS {date:593800000000}]->(old)");

            // When
            session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback(e) YIELD node RETURN node");
            StatementResult finalResult = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback(e) YIELD node RETURN node");

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
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");

            // When
            StatementResult result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback(e) YIELD node RETURN node");

            // Then
            Assertions.assertThat(result).isEmpty();
        }
    }

    /*------------------------------*/
    /*          rollback.to         */
	/*------------------------------*/

    @Test
    public void shouldRollbackToTheGivenStateNode() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:593900000000, endDate:59391000000}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE]->(s:Test) CREATE (sc)-[:PREVIOUS {date:593900000000}]->(s)");

            // When
            StatementResult result = session.run("MATCH (e:Entity)--(s:Test) WITH e, s CALL graph.versioner.rollback.to(e, s) YIELD node RETURN node");

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
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:593900000000, endDate:59391000000}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE]->(s:Test) CREATE (sc)-[:PREVIOUS {date:593900000000}]->(s)");
            session.run("MATCH (p:State)<-[:PREVIOUS]-(s:State) CREATE (p)<-[:ROLLBACK]-(s)");

            // When
            StatementResult result = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) WITH e, s CALL graph.versioner.rollback.to(e, s) YIELD node RETURN node");

            // Then
            Assertions.assertThat(result).isEmpty();
        }
    }

    @Test
    public void shouldGetNullIfTheGivenNodeIsTheCurrentState() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");

            // When
            StatementResult result = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) WITH e, s CALL graph.versioner.rollback.to(e, s) YIELD node RETURN node");

            // Then
            Assertions.assertThat(result).isEmpty();
        }
    }

    @Test (expected = ClientException.class)
    public void shouldNotRollbackToTheGivenStateSinceItsADifferentEntityState() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:593900000000, endDate:59391000000}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (sc:State)<-[:CURRENT]-(e:Entity)-[:HAS_STATE]->(s:Test) CREATE (sc)-[:PREVIOUS {date:593900000000}]->(s)");
            session.run("CREATE (e:EntityBis {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:EntityBis)-[:CURRENT]->(s:State) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");

            StatementResult result = session.run("MATCH (e:Entity), (:EntityBis)-[:HAS_STATE]->(s:State) WITH e, s CALL graph.versioner.rollback.to(e, s) YIELD node RETURN node");
        }
    }

    /*-------------------------------*/
	/*         rollback.nth          */
	/*-------------------------------*/

    @Test
    public void shouldRollbackNthWorkCorrectly() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(:State:Error {key:'initialValue'})"
                    + "-[:PREVIOUS {date: 593900000000}]->(:State {key:'value'})-[:PREVIOUS {date: 593800000000}]->(:State:Test {key:'testValue'})");
            session.run("MATCH (e:Entity)-[:CURRENT]->(s:Error) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity), (s:State {key:'value'}) CREATE (e)-[:HAS_STATE {startDate:593900000000, endDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity), (s:Test) CREATE (e)-[:HAS_STATE {startDate:593800000000, endDate:593900000000}]->(s)");

            // When
            StatementResult result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback.nth(e, 2) YIELD node RETURN node");

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
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(:State:Error {key:'initialValue'})"
                    + "-[:PREVIOUS]->(:State {key:'value'})-[:PREVIOUS {date: 593900000000}]->(:State:Test {key:'testValue'})");
            session.run("MATCH (e:Entity)-[:CURRENT]->(s:Error) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity), (s:State {key:'value'}) CREATE (e)-[:HAS_STATE {startDate:593900000000, endDate:593910000000}]->(s)");
            session.run("MATCH (e:Entity), (s:Test) CREATE (e)-[:HAS_STATE {startDate:593800000000, endDate:593900000000}]->(s)");

            // When
            StatementResult result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.rollback.nth(e, 0) YIELD node RETURN node");

            // Then
            Assertions.assertThat(result).isEmpty();

            result = session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) RETURN s");

            Assertions.assertThat(result)
                    .hasSize(1)
                    .allMatch(node -> node.get("s").asNode().hasLabel("Error"));
        }
    }
}

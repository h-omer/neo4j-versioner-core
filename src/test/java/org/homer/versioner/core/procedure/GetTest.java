package org.homer.versioner.core.procedure;

import org.hamcrest.CoreMatchers;
import org.homer.versioner.core.Utility;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.harness.junit.rule.Neo4jRule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * GetTest class, it contains all the method used to test Get class methods
 */
public class GetTest extends GenericProcedureTest{
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()

            // This is the function we want to test
            .withProcedure(Get.class).withProcedure(Update.class);

    /*------------------------------*/
    /*       get.current.path       */
    /*------------------------------*/

    @After
    public void clean() {
    }

    @Test
    public void shouldGetCurrentPathByGivenEntity() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            Node entity = session.run("MATCH (e:Entity) RETURN e").single().get("e").asNode();
            Node state = session.run("MATCH (s:State) RETURN s").single().get("s").asNode();

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.current.path(e) YIELD path RETURN path");

            Path current = result.single().get("path").asPath();
            Iterator<Relationship> relsIterator = current.relationships().iterator();
            Map<String, Object> rels = new HashMap<>();
            while (relsIterator.hasNext()) {
                Relationship support = relsIterator.next();
                rels.put(support.type(), support);
            }

            // Then
            assertThat(current.contains(entity), equalTo(true));
            assertThat(rels.containsKey(Utility.CURRENT_TYPE), equalTo(true));
            assertThat(current.contains(state), equalTo(true));
        }
    }

    /*------------------------------*/
    /*      get.current.state       */
	/*------------------------------*/

    //@Test
    public void shouldGetCurrentStateByGivenEntity() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            Node state = session.run("MATCH (s:State) RETURN s").single().get("s").asNode();

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.current.state(e) YIELD node RETURN node");

            // Then
            assertThat(result.single().get("node").asNode(), equalTo(state));
        }
    }

    /*------------------------------*/
	/*            get.all           */
	/*------------------------------*/

    //@Test
    public void shouldGetAllStateNodesByGivenEntity() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            session.run("MATCH (e)-[hs:HAS_STATE]->(s) CREATE (e)-[:HAS_STATE {startDate: localdatetime('1988-10-26T00:00:00'), endDate: hs.startDate}]->(:State{key:'oldState'})");
            session.run("MATCH (s1:State {key:'oldState'}), (s2:State {key:'initialValue'}) CREATE (s1)<-[:PREVIOUS {date: localdatetime('1988-10-26T00:00:00')}]-(s2) ");
            Node entity = session.run("MATCH (e:Entity) RETURN e").single().get("e").asNode();
            Node stateNew = session.run("MATCH (s:State {key:'initialValue'}) RETURN s").single().get("s").asNode();
            Node stateOld = session.run("MATCH (s:State {key:'oldState'}) RETURN s").single().get("s").asNode();

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.all(e) YIELD path RETURN path");

            Path current = result.single().get("path").asPath();
            Iterator<Relationship> relsIterator = current.relationships().iterator();
            Map<String, Object> rels = new HashMap<>();
            while (relsIterator.hasNext()) {
                Relationship support = relsIterator.next();
                rels.put(support.type(), support);
            }

            // Then
            assertThat(current.contains(entity), equalTo(true));
            assertThat(current.contains(stateNew), equalTo(true));
            assertThat(rels.containsKey(Utility.PREVIOUS_TYPE), equalTo(true));
            assertThat(current.contains(stateOld), equalTo(true));
        }
    }

    /*
    @Test
    public void shouldGetAllStateNodesByGivenEntityMultipleStates() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity)-[:CURRENT]->(s:State) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");

            // When
            session.run("MATCH (e:Entity) WITH e CALL graph.versioner.update(e, {key:'newValue'}, 'Error') YIELD node RETURN node");
            session.run("MATCH (e:Entity) WITH e CALL graph.versioner.update(e, {key:'newerValue'}, 'Error') YIELD node RETURN node");
            session.run("MATCH (e:Entity) WITH e CALL graph.versioner.update(e, {key:'newestValue'}, 'Error') YIELD node RETURN node");
            StatementResult countStateResult = session.run("MATCH (s:State) RETURN count(*) as ss");
            StatementResult allResult = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.all(e) YIELD path RETURN nodes(path) as ns");
            StatementResult allResult2 = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.all(e) YIELD path RETURN path");
            // Then
            final Value ss = countStateResult.single().get("ss");
            assertThat(ss.asInt(), CoreMatchers.equalTo(4));
            final Value ns = allResult.single().get("ns");
            assertThat(ns.asList().size(), CoreMatchers.equalTo(5));
            final Iterable<Node> path = allResult2.single().get("path").asPath().nodes();
            assertThat(((Collection<Node>)path).size(), CoreMatchers.equalTo(5));
        }
    }
     */

    @Test
    public void shouldGetAllStateNodesByGivenEntityWithOnlyOneCurrentState() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s)");
            Node entity = session.run("MATCH (e:Entity) RETURN e").single().get("e").asNode();
            Node stateNew = session.run("MATCH (s:State {key:'initialValue'}) RETURN s").single().get("s").asNode();

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.all(e) YIELD path RETURN path");

            Path current = result.single().get("path").asPath();
            Iterator<Relationship> relsIterator = current.relationships().iterator();
            Map<String, Object> rels = new HashMap<>();
            while (relsIterator.hasNext()) {
                Relationship support = relsIterator.next();
                rels.put(support.type(), support);
            }

            // Then
            assertThat(current.contains(entity), equalTo(true));
            assertThat(current.contains(stateNew), equalTo(true));
        }
    }

    /*------------------------------*/
	/*         get.by.label         */
	/*------------------------------*/

    //@Test
    public void shouldGetOneErrorStateNodeByGivenErrorLabel() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Error {key:'initialValue'})");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.by.label(e, 'Error') YIELD node RETURN node");

            // Then
            boolean failure = true;

            while (result.hasNext()) {
                failure = false;
                assertThat(result.next().get("node").asNode().hasLabel("Error"), equalTo(true));
            }

            if (failure) {
                fail();
            }
        }
    }

    //@Test
    public void shouldGetAllErrorStateNodeByGivenErrorLabel() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Error {key:'initialValue'})");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Error {key:'initialValue'})");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Error {key:'initialValue'})");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.by.label(e, 'Error') YIELD node RETURN node");

            // Then
            boolean failure = true;

            while (result.hasNext()) {
                failure = false;
                assertThat(result.next().get("node").asNode().hasLabel("Error"), equalTo(true));
            }

            if (failure) {
                fail();
            }
        }
    }

    /*------------------------------*/
	/*         get.by.date          */
	/*------------------------------*/

    //@Test
    public void shouldGetSpecificStateNodeByGivenEntityAndDate() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:HAS_STATE {startDate:localdatetime('1988-10-27T00:00:00')}]->(s:State:Error {key:'initialValue'})");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-28T00:00:00')}]->(s:State:Test {key:'initialValue'})");
            session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE {startDate:localdatetime('1988-10-29T00:00:00')}]->(s:State {key:'initialValue'})");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.by.date(e, localdatetime('1988-10-28T00:00:00')) YIELD node RETURN node");

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

    /*--------------------------------*/
	/*         get.nth.state          */
	/*--------------------------------*/

    //@Test
    public void shouldGetNthStateNodeOfGivenEntity() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (:Entity {key:'immutableValue'})-[:CURRENT]->(:State:Error {key:'initialValue'})"
                    + "-[:PREVIOUS]->(:State {key:'value'})-[:PREVIOUS]->(:State:Test {key:'testValue'})");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.nth.state(e, 2) YIELD node RETURN node");

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

    //@Test
    public void shouldGetNthZeroStateNodeOfGivenEntity() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.builder().build()); Session session = driver.session()) {
            // Given
            session.run("CREATE (:Entity {key:'immutableValue'})-[:CURRENT]->(:State:Error {key:'initialValue'})"
                    + "-[:PREVIOUS]->(:State {key:'value'})-[:PREVIOUS]->(:State:Test {key:'testValue'})");

            // When
            Result result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.nth.state(e, 0) YIELD node RETURN node");

            // Then
            boolean failure = true;

            while (result.hasNext()) {
                failure = false;
                assertThat(result.next().get("node").asNode().hasLabel("Error"), equalTo(true));
            }

            if (failure) {
                fail();
            }
        }
    }
}

package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.Utility;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.neo4j.harness.junit.Neo4jRule;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * GetTest class, it contains all the method used to test Get class methods
 */
public class GetTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()

            // This is the function we want to test
            .withProcedure(Get.class);

    @Test
    public void shouldGetCurrentPathByGivenEntity() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig())) {
            // Given
            Session session = driver.session();
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            session.run("MATCH (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'}) CREATE (e)-[:HAS_STATE {startDate:593910000000}]->(s)");
            Node entity = session.run("MATCH (e:Entity) RETURN e").single().get("e").asNode();
            Node state = session.run("MATCH (s:State) RETURN s").single().get("s").asNode();

            // When
            StatementResult result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.current.path(e) YIELD path RETURN path");

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
            assertThat(rels.containsKey(Utility.HAS_STATE_TYPE), equalTo(true));
            assertThat(current.contains(state), equalTo(true));
        }
    }

    @Test
    public void shouldGetCurrentStateByGivenEntity() {
        // This is in a try-block, to make sure we close the driver after the test
        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig())) {
            // Given
            Session session = driver.session();
            session.run("CREATE (e:Entity {key:'immutableValue'})-[:CURRENT {date:593910000000}]->(s:State {key:'initialValue'})");
            Node state = session.run("MATCH (s:State) RETURN s").single().get("s").asNode();

            // When
            StatementResult result = session.run("MATCH (e:Entity) WITH e CALL graph.versioner.get.current.state(e) YIELD node RETURN node");

            // Then
            assertThat(result.single().get("node").asNode(), equalTo(state));
        }
    }
}

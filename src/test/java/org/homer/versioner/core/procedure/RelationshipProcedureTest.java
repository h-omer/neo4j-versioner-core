package org.homer.versioner.core.procedure;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;
import org.neo4j.harness.junit.Neo4jRule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.homer.versioner.core.Utility.convertEpochToLocalDateTime;

/**
 * RelationshipProcedureTest class, it contains all the method used to test RelationshipProcedure class methods
 */
public class RelationshipProcedureTest extends GenericProcedureTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()

            // This is the function we want to test
            .withProcedure(RelationshipProcedure.class);

    /*-------------------------------------------*/
    /*            relationship.create            */
    /*-------------------------------------------*/

    @Test
    public void shouldCreateTheRelationshipAndTheNewCurrentStateBetweenEntities() throws Throwable {

        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {

            // Given
            Node entityA = initEntity(session);
            Node entityB = initEntity(session);
            String testType = "testType";

            // When
            String query = "MATCH (a:Entity), (b:Entity) WHERE id(a) = %d AND id(b) = %d WITH a, b CALL graph.versioner.relationship.create(a, b, '%s') YIELD relationship RETURN relationship";
            Relationship relationship = session.run(String.format(query, entityA.id(), entityB.id(), testType)).single().get("relationship").asRelationship();

            // Then
            String querySourceCurrent = "MATCH (e:Entity)-[:CURRENT]-(s:State) WHERE id(e) = %d RETURN s";
            String queryDestinationR = "MATCH (e:Entity)<-[:FOR]-(r:R) WHERE id(e) = %d RETURN r";
            Node sourceCurrent = session.run(String.format(querySourceCurrent, entityA.id())).single().get("s").asNode();
            Node destinationR = session.run(String.format(queryDestinationR, entityB.id())).single().get("r").asNode();
            Relationship expected = new InternalRelationship(0L, sourceCurrent.id(), destinationR.id(), testType);

            assertThat(relationship).isEqualToIgnoringGivenFields(expected, "id");
        }
    }

    @Test
    public void shouldNotCreateTheRelationshipIfSourceIsNotAnEntity() throws Throwable {

        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {

            // Given
            Node entityA = session.run("CREATE (e:Entity) RETURN e").single().get("e").asNode(); //Not an entity because missing states and R
            Node entityB = initEntity(session);
            String testType = "testType";

            // When
            String query = "MATCH (a:Entity), (b:Entity) WHERE id(a) = %d AND id(b) = %d WITH a, b CALL graph.versioner.relationship.create(a, b, '%s') YIELD relationship RETURN relationship";
            assertThat(session.run(String.format(query, entityA.id(), entityB.id(), testType)));
            Throwable thrown = catchThrowable(() -> session.run(String.format(query, entityA.id(), entityB.id(), testType)));

	        //Then
	        assertThat(thrown).hasMessageContaining("The given node is not a Versioner Core Entity");
        }
    }

    @Test
    public void shouldNotCreateTheRelationshipIfDestinationIsNotAnEntity() throws Throwable {

        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {

            // Given
            Node entityA = initEntity(session);
            Node entityB = session.run("CREATE (e:Entity) RETURN e").single().get("e").asNode(); //Not an entity because missing states and R
            String testType = "testType";

            // When
            String query = "MATCH (a:Entity), (b:Entity) WHERE id(a) = %d AND id(b) = %d WITH a, b CALL graph.versioner.relationship.create(a, b, '%s') YIELD relationship RETURN relationship";
            assertThat(session.run(String.format(query, entityA.id(), entityB.id(), testType)));
            Throwable thrown = catchThrowable(() -> session.run(String.format(query, entityA.id(), entityB.id(), testType)));

	        //Then
	        assertThat(thrown).hasMessageContaining("The given node is not a Versioner Core Entity");
        }
    }

    @Test
    public void shouldCreateTheRelationshipsAssociatedToANewStateEachHavingOwnLabelAndProps() {

        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {

            // Given
            final Node entityA = initEntity(session);
            final Node entityB = initEntity(session);
            final Node entityC = initEntity(session);
            final Node entityD = initEntity(session);
            final Node entityE = initEntity(session);

            final String testType = "testType";
            final Long date = 593920000000L;
            final String dateString = convertEpochToLocalDateTime(date).toString();

            // When
            final String query = "WITH [%d, %d, %d, %d] as list MATCH (a:Entity), (b:Entity) WHERE id(a) = %d AND any(item in list where id(b) = item) WITH a, collect(b) as bs CALL graph.versioner.relationships.create(a, bs, [{versionerLabel:\"b\",name:\"b\"},{versionerLabel:\"c\",name:\"c\"},{versionerLabel:\"d\",name:\"d\"},{versionerLabel:\"e\",name:\"e\"}]) YIELD relationship RETURN relationship";
            session.run(String.format(query,entityB.id(), entityC.id(), entityD.id(), entityE.id(), entityA.id(), dateString));

            // Then
            final String querySourceCurrent = "MATCH (e:Entity)-[r:CURRENT]->(:State)-[l:%s]->(:R) WHERE id(e) = %d RETURN l";
            final Relationship bRelationship = session.run(String.format(querySourceCurrent, "b", entityA.id())).single().get("l").asRelationship();

            assertThat(bRelationship)
                    .matches(rel -> rel.containsKey("name") && rel.get("name").asString().equals("b"));

            final Relationship cRelationship = session.run(String.format(querySourceCurrent, "c", entityA.id())).single().get("l").asRelationship();

            assertThat(cRelationship)
                    .matches(rel -> rel.containsKey("name") && rel.get("name").asString().equals("c"));


            final Relationship dRelationship = session.run(String.format(querySourceCurrent, "d", entityA.id())).single().get("l").asRelationship();

            assertThat(dRelationship)
                    .matches(rel -> rel.containsKey("name") && rel.get("name").asString().equals("d"));

            final Relationship eRelationship = session.run(String.format(querySourceCurrent, "e", entityA.id())).single().get("l").asRelationship();

            assertThat(eRelationship)
                    .matches(rel -> rel.containsKey("name") && rel.get("name").asString().equals("e"));
        }
    }

    @Test
    public void shouldCreateTheRelationshipAssociatedToANewStateHavingRequestedDate() {

        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {

            // Given
            final Node entityA = initEntity(session);
            final Node entityB = initEntity(session);
            final String testType = "testType";
            final Long date = 593920000000L;
            final String dateString = convertEpochToLocalDateTime(date).toString();

            // When
            final String query = "MATCH (a:Entity), (b:Entity) WHERE id(a) = %d AND id(b) = %d WITH a, b CALL graph.versioner.relationship.create(a, b, '%s', {}, localdatetime('%s')) YIELD relationship RETURN relationship";
            session.run(String.format(query, entityA.id(), entityB.id(), testType, dateString));

            // Then
            final String querySourceCurrent = "MATCH (e:Entity)-[r:CURRENT]->(:State)-[:%s]->(:R) WHERE id(e) = %d RETURN r";
            final Relationship currentRelationship = session.run(String.format(querySourceCurrent, testType, entityA.id())).single().get("r").asRelationship();

            assertThat(currentRelationship)
                    .matches(rel -> rel.containsKey("date") && rel.get("date").asLocalDateTime().equals(convertEpochToLocalDateTime(date)));
        }
    }

    @Test
    public void shouldCreateTheRelationshipInANewCurrentStatePreservingTheOldOne() {

        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {

            // Given
            Node entityA = initEntity(session);
            Node entityB = initEntity(session);
            String testType = "testType";
            Long entityACurrentId = session.run(String.format("MATCH (e:Entity)-[:CURRENT]->(s:State) WHERE id(e) = %d RETURN s", entityA.id())).single().get("s").asNode().id();

            // When
            String query = "MATCH (a:Entity), (b:Entity) WHERE id(a) = %d AND id(b) = %d WITH a, b CALL graph.versioner.relationship.create(a, b, '%s') YIELD relationship RETURN relationship";
            Relationship relationship = session.run(String.format(query, entityA.id(), entityB.id(), testType)).single().get("relationship").asRelationship();

            // Then
            String querySourceStates = "MATCH (:R)<-[r:%s]-(s1:State)-[:PREVIOUS]->(s2:State) WHERE id(r) = %d RETURN s1, s2";
            StatementResult result = session.run(String.format(querySourceStates, testType, relationship.id()));

            assertThat(result)
                    .hasSize(1)
                    .allMatch(r -> r.get("s1").asNode().id() != r.get("s2").asNode().id() && r.get("s2").asNode().id() == entityACurrentId);
        }
    }

    @Test
    public void shouldCreateTwoRelationshipsInTwoDifferentStates() {

        try (Driver driver = GraphDatabase
                .driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {

            // Given
            Node entityA = initEntity(session);
            Node entityB = initEntity(session);
            Node entityC = initEntity(session);
            String testType1 = "testType1";
            String testType2 = "testType2";

            // When
            String query = "MATCH (a:Entity), (b:Entity) WHERE id(a) = %d AND id(b) = %d WITH a, b CALL graph.versioner.relationship.create(a, b, '%s') YIELD relationship RETURN relationship";
            session.run(String.format(query, entityA.id(), entityB.id(), testType1));
            session.run(String.format(query, entityA.id(), entityC.id(), testType2));

            // Then
            String querySourceStates = "MATCH (e:Entity)-[:CURRENT]->(:State)-[r]->(:R) WHERE id(e) = %d RETURN r";
            Stream<Relationship> result = session.run(String.format(querySourceStates, entityA.id())).list().stream()
                    .map(r -> r.get("r").asRelationship());

            Relationship expectedRelationship1 = new InternalRelationship(0L, 0L, 0L, testType1);
            Relationship expectedRelationship2 = new InternalRelationship(0L, 0L, 0L, testType2);

            assertThat(result)
                    .hasSize(2)
                    .usingElementComparatorOnFields("type")
                    .containsExactlyInAnyOrder(expectedRelationship1, expectedRelationship2);
        }
    }
}

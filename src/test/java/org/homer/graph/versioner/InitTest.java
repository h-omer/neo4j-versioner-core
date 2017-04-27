package org.homer.graph.versioner;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;

import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by mfalcier on 23/03/17.
 */
public class InitTest {

    // This rule starts a Neo4j instance
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()

            // This is the function we want to test
            .withProcedure( Init.class );

    @Test
    public void shouldCreateAnEmptyEntityWithoutAState() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try( Driver driver = GraphDatabase
                .driver( neo4j.boltURI() , Config.build().withEncryptionLevel( Config.EncryptionLevel.NONE ).toConfig() ) )
        {
            // Given
            Session session = driver.session();

            // When
            StatementResult result = session.run( "CALL graph.versioner.init('Entity')" );
            StatementResult entityEmptyResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateEmptyResult = session.run("MATCH (s:State) RETURN s");

            // Then
            assertThat( result.single().get( "id" ).asLong(), equalTo(0l) );
            assertThat( entityEmptyResult.single().get( "props" ).asMap().isEmpty(), equalTo(true));
            assertThat( stateEmptyResult.hasNext(), equalTo(false));

            // Closing session
            session.close();
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithoutAState() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try( Driver driver = GraphDatabase
                .driver( neo4j.boltURI() , Config.build().withEncryptionLevel( Config.EncryptionLevel.NONE ).toConfig() ) )
        {
            // Given
            Session session = driver.session();

            // When
            StatementResult result = session.run( "CALL graph.versioner.init('Entity', {key:'value'})" );
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateEmptyResult = session.run("MATCH (s:State) RETURN s");

            // Then
            assertThat( result.single().get( "id" ).asLong(), equalTo(0l) );
            assertThat( entityResult.single().get( "props" ).asMap().isEmpty(), equalTo(false));
            assertThat( stateEmptyResult.hasNext(), equalTo(false));

            // Closing session
            session.close();
        }
    }

    @Test
    public void shouldCreateAnEntityWithPropertiesWithAStateAndItsProperties() throws Throwable {
        // This is in a try-block, to make sure we close the driver after the test
        try( Driver driver = GraphDatabase
                .driver( neo4j.boltURI() , Config.build().withEncryptionLevel( Config.EncryptionLevel.NONE ).toConfig() ) )
        {
            // Given
            Session session = driver.session();

            // When
            StatementResult result = session.run( "CALL graph.versioner.init('Entity', {key:'value'}, {key:'value'})" );
            StatementResult entityResult = session.run("MATCH (e:Entity) RETURN properties(e) as props");
            StatementResult stateResult = session.run("MATCH (s:State) RETURN s");
            StatementResult stateProps = session.run("MATCH (s:State) RETURN properties(s) as props");

            // Then
            assertThat( result.single().get( "id" ).asLong(), equalTo(0l) );
            assertThat( entityResult.single().get( "props" ).asMap().isEmpty(), equalTo(false));
            assertThat( stateResult.single().get( "s" ).asNode().id(), equalTo(1l));
            assertThat( stateProps.single().get( "props" ).asMap().isEmpty(), equalTo(false));

            // Closing session
            session.close();
        }
    }

}
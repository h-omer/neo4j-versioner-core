package org.homer.graph.versioner;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;

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
    public void shouldCreateANode() throws Throwable
    {
        // This is in a try-block, to make sure we close the driver after the test
        try( Driver driver = GraphDatabase
                .driver( neo4j.boltURI() , Config.build().withEncryptionLevel( Config.EncryptionLevel.NONE ).toConfig() ) )
        {
            // Given
            Session session = driver.session();

            // When
            StatementResult result = session.run( "CALL graph.versioner.init('Entity', ['provaE1','provaE2'], ['provaS1','provaS2'])" );

            // Then
            assertThat( result.single().get( "id" ).asLong(), equalTo(1l) );
        }
    }

}
package org.homer.core.versioner.builders;

import org.homer.core.versioner.procedure.Init;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

/**
 * DiffBuilderTest class, it contains all the method used to test procedure builders
 */
public class InitBuilderTest {
    @Test
    public void shouldBuildCorrectProcedureInstance() {
        GraphDatabaseService db = mock(GraphDatabaseService.class);
        Log log = mock(Log.class);

        Optional<Init> result = new InitBuilder().withDb(db).withLog(log).build();

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().db, is(db));
        assertThat(result.get().log, is(log));
    }
}

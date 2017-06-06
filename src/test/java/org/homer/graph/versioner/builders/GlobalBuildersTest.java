package org.homer.graph.versioner.builders;

import org.homer.graph.versioner.procedure.Init;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

/**
 * Created by alberto on 6/6/17.
 */
public class GlobalBuildersTest {
    @Test
    public void shouldBuildCorrectInstance() {
        GraphDatabaseService db = mock(GraphDatabaseService.class);
        Log log = mock(Log.class);

        Optional<Init> result = new InitBuilder().withDb(db).withLog(log).build();

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().db, is(db));
        assertThat(result.get().log, is(log));
    }
}

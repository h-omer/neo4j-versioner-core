package org.homer.versioner.core.builders;

import org.homer.versioner.core.procedure.Diff;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * DiffBuilderTest class, it contains all the method used to test procedure builders
 */
public class DiffBuilderTest {
    @Test
    public void shouldBuildCorrectProcedureInstance() {
        Optional<Diff> result = new DiffBuilder().build();

        assertThat(result.isPresent(), is(true));
    }
}

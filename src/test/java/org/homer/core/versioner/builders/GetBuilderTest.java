package org.homer.core.versioner.builders;

import org.homer.core.versioner.procedure.Get;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * DiffBuilderTest class, it contains all the method used to test procedure builders
 */
public class GetBuilderTest {
    @Test
    public void shouldBuildCorrectProcedureInstance() {
        Optional<Get> result = new GetBuilder().build();

        assertThat(result.isPresent(), is(true));
    }
}

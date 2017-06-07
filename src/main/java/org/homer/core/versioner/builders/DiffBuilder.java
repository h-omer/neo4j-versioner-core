package org.homer.core.versioner.builders;

import org.homer.core.versioner.procedure.Diff;

import java.util.Optional;

/**
 * DiffBuilder class, used to create a new instance of the current procedure
 */
public class DiffBuilder {
    /**
     * It builds the {@link Diff} procedure
     *
     * @return procedure
     */
    public Optional<Diff> build(){
        return Optional.of(new Diff());
    }
}

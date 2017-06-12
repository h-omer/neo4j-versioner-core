package org.homer.versioner.core.builders;

import org.homer.versioner.core.procedure.Diff;

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

package org.homer.core.versioner.builders;

import org.homer.core.versioner.procedure.Get;

import java.util.Optional;

/**
 * GetBuilder class, used to create a new instance of the current procedure
 */
public class GetBuilder {

    /**
     * It builds the {@link Get} procedure
     *
     * @return procedure
     */
    public Optional<Get> build(){
        return Optional.of(new Get());
    }
}

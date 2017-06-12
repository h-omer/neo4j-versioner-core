package org.homer.versioner.core.builders;

import org.homer.versioner.core.procedure.Get;

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

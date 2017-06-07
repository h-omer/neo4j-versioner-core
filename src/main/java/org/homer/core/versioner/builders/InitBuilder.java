package org.homer.core.versioner.builders;

import org.homer.core.versioner.procedure.Init;

import java.util.Optional;

/**
 * InitBuilder class, used to create a new instance of the current procedure
 */
public class InitBuilder extends CoreProcedureBuilder<Init> {

    /**
     * Constructor method
     */
    public InitBuilder() {
        super(Init.class);
    }

    @Override
    public Optional<Init> build(){
        return super.instantiate();
    }

}

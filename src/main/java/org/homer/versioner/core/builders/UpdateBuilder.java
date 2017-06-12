package org.homer.versioner.core.builders;

import org.homer.versioner.core.procedure.Update;

import java.util.Optional;

/**
 * UpdateBuilder class, used to create a new instance of the current procedure
 */
public class UpdateBuilder extends CoreProcedureBuilder<Update> {

    /**
     * Constructor method
     */
    public UpdateBuilder() {
        super(Update.class);
    }

    @Override
    public Optional<Update> build(){
        return super.instantiate();
    }
}

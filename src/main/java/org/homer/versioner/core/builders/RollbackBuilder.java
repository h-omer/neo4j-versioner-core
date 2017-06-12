package org.homer.versioner.core.builders;

import org.homer.versioner.core.procedure.Rollback;

import java.util.Optional;

/**
 * RollbackBuilder class, used to create a new instance of the current procedure
 */
public class RollbackBuilder extends CoreProcedureBuilder<Rollback> {

    /**
     * Constructor method
     *
     */
    public RollbackBuilder() {
        super(Rollback.class);
    }

    @Override
    public Optional<Rollback> build(){
        return super.instantiate();
    }
}

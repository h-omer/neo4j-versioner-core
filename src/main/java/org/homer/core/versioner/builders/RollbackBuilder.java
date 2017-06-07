package org.homer.core.versioner.builders;

import org.homer.core.versioner.procedure.Rollback;

import java.util.Optional;

/**
 * RollbackBuilder class, used to create a new instance of the current procedure
 */
public class RollbackBuilder extends CoreProcedureBuilder<Rollback> {

    /**
     * Constructor method
     *
     * @param clazz
     */
    public RollbackBuilder(Class<Rollback> clazz) {
        super(Rollback.class);
    }

    @Override
    public Optional<Rollback> build(){
        return super.instantiate();
    }
}

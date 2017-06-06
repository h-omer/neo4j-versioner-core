package org.homer.graph.versioner.builders;

import org.homer.graph.versioner.procedure.Rollback;

import java.util.Optional;

/**
 * Created by alberto on 6/6/17.
 */
public class RollbackBuilder extends CoreProcedureBuilder<Rollback> {

    public RollbackBuilder(Class<Rollback> clazz) {
        super(Rollback.class);
    }

    @Override
    public Optional<Rollback> build(){
        return super.instantiate();
    }
}

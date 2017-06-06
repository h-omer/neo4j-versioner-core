package org.homer.graph.versioner.builders;

import org.homer.graph.versioner.procedure.Update;

import java.util.Optional;

/**
 * Created by alberto on 6/6/17.
 */
public class UpdateBuilder extends CoreProcedureBuilder<Update> {

    public UpdateBuilder() {
        super(Update.class);
    }

    @Override
    public Optional<Update> build(){
        return super.instantiate();
    }
}

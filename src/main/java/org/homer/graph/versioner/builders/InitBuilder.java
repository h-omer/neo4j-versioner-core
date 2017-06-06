package org.homer.graph.versioner.builders;

import org.homer.graph.versioner.procedure.Init;

import java.util.Optional;

/**
 * Created by alberto on 6/6/17.
 */
public class InitBuilder extends CoreProcedureBuilder<Init> {

    public InitBuilder() {
        super(Init.class);
    }

    @Override
    public Optional<Init> build(){
        return super.instantiate();
    }

}

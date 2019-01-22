package org.homer.versioner.core.builders;

import org.homer.versioner.core.procedure.RelationshipProcedure;

import java.util.Optional;

/**
 * RelationshipProcedureBuilder class, used to create a new instance of the current procedure
 */
public class RelationshipProcedureBuilder extends CoreProcedureBuilder<RelationshipProcedure> {

    /**
     * Constructor method
     */
    public RelationshipProcedureBuilder() {
        super(RelationshipProcedure.class);
    }

    @Override
    public Optional<RelationshipProcedure> build(){
        return super.instantiate();
    }
}

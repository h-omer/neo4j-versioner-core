package org.homer.versioner.core.builders;

import org.homer.versioner.core.procedure.Relationship;

import java.util.Optional;

/**
 * RelationshipBuilder class, used to create a new instance of the current procedure
 */
public class RelationshipBuilder extends CoreProcedureBuilder<Relationship> {

    /**
     * Constructor method
     */
    public RelationshipBuilder() {
        super(Relationship.class);
    }

    @Override
    public Optional<Relationship> build(){
        return super.instantiate();
    }
}

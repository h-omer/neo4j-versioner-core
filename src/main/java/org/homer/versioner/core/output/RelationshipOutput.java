package org.homer.versioner.core.output;

import org.neo4j.graphdb.Relationship;

public class RelationshipOutput {
    public Relationship relationship;

    public RelationshipOutput(Relationship relationship) {
        this.relationship = relationship;
    }
}

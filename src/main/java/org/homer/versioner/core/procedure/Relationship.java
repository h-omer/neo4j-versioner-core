package org.homer.versioner.core.procedure;

import org.homer.versioner.core.core.CoreProcedure;
import org.homer.versioner.core.output.NodeOutput;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;

/**
 * Relationship class, it contains all the Procedures needed to create versioned relationships between Entities
 */
public class Relationship extends CoreProcedure {

    //TODO add properties parameter to relationship
    @Procedure(value = "graph.versioner.relationship.create", mode = Mode.WRITE)
    @Description("graph.versioner.relationship.create(entityA, entityB, type, date) - Create a relationship from entitySource to entityDestination with the given type for the specified date.")
    public Stream<NodeOutput> update(
            @Name("entitySource") Node entitySource,
            @Name("entityDestination") Node entityDestination,
            @Name(value = "type", defaultValue = "") String type,
            @Name(value = "date", defaultValue = "0") long date) {

        //TODO check entitySource and entityDestination are entities
        //TODO create new current state cloning the existing one
        //TODO set to new current state the parameter date
        //TODO create the relationship between the current state and the R node of the entityDestination
        //TODO set the type of the new relationship to the parameter one

        return Stream.empty();
    }
}

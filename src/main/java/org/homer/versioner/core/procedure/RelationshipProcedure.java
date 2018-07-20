package org.homer.versioner.core.procedure;

import org.apache.commons.lang3.StringUtils;
import org.homer.versioner.core.builders.UpdateBuilder;
import org.homer.versioner.core.core.CoreProcedure;
import org.homer.versioner.core.exception.VersionerCoreException;
import org.homer.versioner.core.output.RelationshipOutput;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.homer.versioner.core.Utility.*;

/**
 * RelationshipProcedure class, it contains all the Procedures needed to create versioned relationships between Entities
 */
public class RelationshipProcedure extends CoreProcedure {

    //TODO add properties parameter to relationship
    @Procedure(value = "graph.versioner.relationship.create", mode = Mode.WRITE)
    @Description("graph.versioner.relationship.create(entityA, entityB, type, date) - Create a relationship from entitySource to entityDestination with the given type for the specified date.")
    public Stream<RelationshipOutput> relationshipCreate(
            @Name("entitySource") Node entitySource,
            @Name("entityDestination") Node entityDestination,
            @Name(value = "type", defaultValue = "") String type,
            @Name(value = "date", defaultValue = "null") LocalDateTime date) {

        isEntityOrThrowException(entitySource);
        isEntityOrThrowException(entityDestination);

        Optional<Node> sourceCurrentState = createNewSourceState(entitySource, defaultToNow(date));
        Optional<Node> destinationRNode = getRNode(entityDestination);

        if (sourceCurrentState.isPresent() && destinationRNode.isPresent()) {
            return streamOfRelationships(createRelationship(sourceCurrentState.get(), destinationRNode.get(), type));
        } else {
            return Stream.empty();
        }
    }

    protected static Relationship createRelationship(Node source, Node destination, String type) {

        return source.createRelationshipTo(destination, RelationshipType.withName(type));
    }

    private Optional<Node> getRNode(Node entity) {
        return Optional.ofNullable(entity.getSingleRelationship(RelationshipType.withName("FOR"), Direction.INCOMING))
                .map(Relationship::getStartNode);
    }

    private Optional<Node> createNewSourceState(Node entitySource, LocalDateTime date) throws VersionerCoreException {

        Update updateProcedure = new UpdateBuilder().withLog(log).withDb(db).build().orElseThrow(() -> new VersionerCoreException("Unable to initialize update procedure"));
        return updateProcedure.patch(entitySource, Collections.emptyMap(), StringUtils.EMPTY, date)
                .map(n -> n.node)
                .findFirst();
    }
}

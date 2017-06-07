package org.homer.core.versioner.procedure;

import org.homer.core.versioner.core.CoreProcedure;
import org.homer.core.versioner.Utility;
import org.homer.core.versioner.output.NodeOutput;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Calendar;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.homer.core.versioner.Utility.*;
import static org.neo4j.procedure.Mode.WRITE;

/**
 * Rollback class, it contains all the Procedures needed to rollback Entities' States nodes in the database
 */
public class Rollback extends CoreProcedure {

    @Procedure(value = "graph.versioner.rollback", mode = WRITE)
    @Description("graph.versioner.rollback(entity, date) - Rollback the given Entity to its previous State")
    public Stream<NodeOutput> rollback(
            @Name("entity") Node entity,
            @Name(value = "date", defaultValue = "0") long date) {

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;

        // Getting the CURRENT rel if it exists
        Spliterator<Relationship> currentRelIterator = entity.getRelationships(RelationshipType.withName(CURRENT_TYPE), Direction.OUTGOING).spliterator();
        Optional<Relationship> currentRelationshipOptional = StreamSupport.stream(currentRelIterator, false).filter(Objects::nonNull).findFirst();

        Optional<Node> newState = currentRelationshipOptional.map(currentRelationship -> {

            Node currentState = currentRelationship.getEndNode();

            return getFirstAvailableRollbackNode(currentState).map(rollbackState -> {
                Long currentDate = (Long) currentRelationship.getProperty("date");

                // Creating the rollback state, from the previous one
                Node result = Utility.cloneNode(db, rollbackState);

                //Creating ROLLBACK_TYPE relationship
                result.createRelationshipTo(rollbackState, RelationshipType.withName(ROLLBACK_TYPE));

                // Updating CURRENT state
                result = currentStateUpdate(entity, instantDate, currentRelationship, currentState, currentDate, result);


                log.info(LOGGER_TAG + "Rollback executed for Entity with id {}, adding a State with id {}", entity.getId(), result.getId());
                return Optional.of(result);
            }).orElseGet(() -> {
                log.info(LOGGER_TAG + "Failed rollback for Entity with id {}, only one CURRENT State available", entity.getId());
                return Optional.empty();
            });
        }).orElseGet(() -> {
            log.info(LOGGER_TAG + "Failed rollback for Entity with id {}, there is no CURRENT State available", entity.getId());
            return Optional.empty();
        });

        return Stream.of(new NodeOutput(newState));
    }

    @Procedure(value = "graph.versioner.rollback.to", mode = WRITE)
    @Description("graph.versioner.rollback.to(entity, state, date) - Rollback the given Entity to the given State")
    public Stream<NodeOutput> rollbackTo(
            @Name("entity") Node entity,
            @Name("state") Node state,
            @Name(value = "date", defaultValue = "0") long date) {

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;
        Optional<Node> newState = Optional.empty();

        // If the given State is the CURRENT one, null must be returned
        Spliterator<Relationship> currentRelIterator = state.getRelationships(RelationshipType.withName(CURRENT_TYPE), Direction.INCOMING).spliterator();
        Optional<Relationship> currentRelationshipOptional = StreamSupport.stream(currentRelIterator, false).filter(Objects::nonNull).findFirst();
        if (!currentRelationshipOptional.isPresent()) {
            // If the given State already has a ROLLBACK relationship, null must be returned
            Spliterator<Relationship> rollbackRelIterator = state.getRelationships(RelationshipType.withName(ROLLBACK_TYPE), Direction.OUTGOING).spliterator();
            Optional<Relationship> rollbackRelationshipOptional = StreamSupport.stream(rollbackRelIterator, false).filter(Objects::nonNull).findFirst();
            if (!rollbackRelationshipOptional.isPresent()) {
                // Otherwise, the node can be rolled back
                currentRelIterator = entity.getRelationships(RelationshipType.withName(CURRENT_TYPE), Direction.OUTGOING).spliterator();
                currentRelationshipOptional = StreamSupport.stream(currentRelIterator, false).filter(Objects::nonNull).findFirst();

                newState = currentRelationshipOptional.map(currentRelationship -> {
                    Node currentState = currentRelationship.getEndNode();
                    Long currentDate = (Long) currentRelationship.getProperty("date");

                    // Creating the rollback state, from the previous one
                    Node result = Utility.cloneNode(db, state);

                    //Creating ROLLBACK_TYPE relationship
                    result.createRelationshipTo(state, RelationshipType.withName(ROLLBACK_TYPE));

                    // Updating CURRENT state
                    result = currentStateUpdate(entity, instantDate, currentRelationship, currentState, currentDate, result);

                    log.info(LOGGER_TAG + "Rollback executed for Entity with id {}, adding a State with id {}", entity.getId(), result.getId());

                    return Optional.of(result);
                }).orElseGet(() -> {
                    log.info(LOGGER_TAG + "Failed rollback for Entity with id {}, there is no CURRENT State available", entity.getId());
                    return Optional.empty();
                });
            }
        }

        return Stream.of(new NodeOutput(newState));
    }

    /**
     * This method returns the first available State node, by a given State node to rollback
     *
     * @param state state to rollback
     * @return the first available rollback node
     */
    private Optional<Node> getFirstAvailableRollbackNode(Node state) {
        return StreamSupport.stream(state.getRelationships(RelationshipType.withName(ROLLBACK_TYPE), Direction.OUTGOING).spliterator(), false).findFirst()
                // Recursive iteration for ROLLBACKed State node
                .map(e -> getFirstAvailableRollbackNode(e.getEndNode()))
                // No ROLLBACK relationship found
                .orElse(StreamSupport.stream(state.getRelationships(RelationshipType.withName(PREVIOUS_TYPE), Direction.OUTGOING).spliterator(), false)
                        .findFirst().map(Relationship::getEndNode));
    }
}


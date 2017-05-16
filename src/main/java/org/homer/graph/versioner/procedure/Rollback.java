package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.output.NodeOutput;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.homer.graph.versioner.Utility.*;
import static org.neo4j.procedure.Mode.WRITE;

/**
 * Rollback class, it contains all the Procedures needed to rollback Entities' States nodes in the database
 */
public class Rollback {

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    @Procedure(value = "graph.versioner.rollback", mode = WRITE)
    @Description("graph.versioner.rollback(entity) - Rollback the given Entity to its previous State")
    public Stream<NodeOutput> rollback(
            @Name("entity") Node entity) {

        long instantDate = Calendar.getInstance().getTimeInMillis();

        // Getting the CURRENT rel if it exist
        Spliterator<Relationship> currentRelIterator = entity.getRelationships(RelationshipType.withName(CURRENT_TYPE), Direction.OUTGOING).spliterator();
        Node newState = StreamSupport.stream(currentRelIterator, false).map(currentRel -> {
            Node currentState = currentRel.getEndNode();
            Long currentDate = (Long) currentRel.getProperty("date");

            Node rollbackState = getFirstAvailableRollbackNode(currentState);
            Node result = null;
            if (!rollbackState.equals(currentState)) {
                // Creating the rollback state, from the previous one
                List<String> labelNames = new ArrayList<>();
                Spliterator<Label> labelsIterator = rollbackState.getLabels().spliterator();
                StreamSupport.stream(labelsIterator, false).forEach(label -> labelNames.add(label.name()));
                result = setProperties(db.createNode(labels(labelNames)), rollbackState.getAllProperties());

                //Creating ROLLBACK_TYPE relationship
                result.createRelationshipTo(rollbackState, RelationshipType.withName(ROLLBACK_TYPE));

                // Creating PREVIOUS relationship between the current and the new State
                result.createRelationshipTo(currentState, RelationshipType.withName(PREVIOUS_TYPE)).setProperty(DATE_PROP, currentDate);

                // Updating the HAS_STATE rel for the current node, adding endDate
                currentState.getRelationships(RelationshipType.withName(HAS_STATE_TYPE), Direction.INCOMING)
                        .forEach(hasStatusRel -> hasStatusRel.setProperty(END_DATE_PROP, instantDate));

                // Refactoring current relationship and adding the new ones
                currentRel.delete();
                entity.createRelationshipTo(result, RelationshipType.withName(CURRENT_TYPE)).setProperty(DATE_PROP, instantDate);
                entity.createRelationshipTo(result, RelationshipType.withName(HAS_STATE_TYPE)).setProperty(START_DATE_PROP, instantDate);
                log.info(LOGGER_TAG + "Rollback executed for Entity with id {}, adding a State with id {}", entity.getId(), result.getId());
            } else {
                log.info(LOGGER_TAG + "Failed rollback for Entity with id {}, only one CURRENT State available", entity.getId());
            }

            return result;
        }).filter(Objects::nonNull).findFirst().orElse(null);

        return Stream.of(new NodeOutput(newState));
    }

    /**
     * This method returns the first available State node, by a given State node to rollback
     *
     * @param state state to rollback
     * @return the first available rollback node
     */
    private Node getFirstAvailableRollbackNode(Node state) {
        if (StreamSupport.stream(state.getRelationships(RelationshipType.withName(ROLLBACK_TYPE), Direction.OUTGOING).spliterator(), false).count() == 0) {
            // No ROLLBACK relationship found
            if (StreamSupport.stream(state.getRelationships(RelationshipType.withName(PREVIOUS_TYPE), Direction.OUTGOING).spliterator(), false).count() == 1) {
                // A PREVIOUS relationship is available
                return StreamSupport.stream(state.getRelationships(RelationshipType.withName(PREVIOUS_TYPE), Direction.OUTGOING).spliterator(), false).findFirst().get().getEndNode();
            } else {
                // Only one State, without a previous one
                return state;
            }
        } else {
            // Recursive iteration for ROLLBACKed State node
            return getFirstAvailableRollbackNode(StreamSupport.stream(state.getRelationships(RelationshipType.withName(ROLLBACK_TYPE), Direction.OUTGOING).spliterator(), false).findFirst().get().getEndNode());
        }
    }
}


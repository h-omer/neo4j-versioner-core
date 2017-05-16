package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.output.NodeOutput;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.homer.graph.versioner.Utility.*;

/**
 * Update class, it contains all the Procedures needed to update Entities' States
 */
public class Update {

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    @Procedure(value = "graph.versioner.update", mode = Mode.WRITE)
    @Description("graph.versioner.update(entity, {key:value,...}, additionalLabel, date) - Add a new State to the given Entity.")
    public Stream<NodeOutput> update(
            @Name("entity") Node entity,
            @Name("stateProps") Map<String, Object> stateProps,
            @Name(value = "additionalLabel", defaultValue = "") String additionalLabel,
            @Name(value = "date", defaultValue = "0") long date) {

        // Creating the new State
        List<String> labelNames = new ArrayList<>(Collections.singletonList(STATE_LABEL));
        if (!additionalLabel.isEmpty()) {
            labelNames.add(additionalLabel);
        }
        Node newState = setProperties(db.createNode(labels(labelNames)), stateProps);

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;

        // Getting the CURRENT rel if it exist
        Spliterator<Relationship> currentRelIterator = entity.getRelationships(RelationshipType.withName(CURRENT_TYPE), Direction.OUTGOING).spliterator();
        StreamSupport.stream(currentRelIterator, false).forEach(currentRel -> {
            Node currentState = currentRel.getEndNode();
            Long currentDate = (Long) currentRel.getProperty("date");

            // Creating PREVIOUS relationship between the current and the new State
            newState.createRelationshipTo(currentState, RelationshipType.withName(PREVIOUS_TYPE)).setProperty(DATE_PROP, currentDate);

            // Updating the HAS_STATE rel for the current node, adding endDate
            currentState.getRelationships(RelationshipType.withName(HAS_STATE_TYPE), Direction.INCOMING)
                    .forEach(hasStatusRel -> hasStatusRel.setProperty(END_DATE_PROP, instantDate));

            // Refactoring current relationship and adding the new ones
            currentRel.delete();
        });

        entity.createRelationshipTo(newState, RelationshipType.withName(CURRENT_TYPE)).setProperty(DATE_PROP, instantDate);
        entity.createRelationshipTo(newState, RelationshipType.withName(HAS_STATE_TYPE)).setProperty(START_DATE_PROP, instantDate);

        log.info(LOGGER_TAG + "Updated Entity with id {}, adding a State with id {}", entity.getId(), newState.getId());

        return Stream.of(new NodeOutput(newState));
    }

    @Procedure(value = "graph.versioner.patch", mode = Mode.WRITE)
    @Description("graph.versioner.patch(entity, {key:value,...}, additionalLabel, date) - Add a new State to the given Entity, starting from the previous one. It will update all the properties, not labels.")
    public Stream<NodeOutput> patch(
            @Name("entity") Node entity,
            @Name("stateProps") Map<String, Object> stateProps,
            @Name(value = "additionalLabel", defaultValue = "") String additionalLabel,
            @Name(value = "date", defaultValue = "0") long date) {

        // Creating the new State
        List<String> labelNames = new ArrayList<>(Collections.singletonList(STATE_LABEL));
        if (!additionalLabel.isEmpty()) {
            labelNames.add(additionalLabel);
        }

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;

        // Getting the CURRENT rel if it exist
        Spliterator<Relationship> currentRelIterator = entity.getRelationships(RelationshipType.withName(CURRENT_TYPE), Direction.OUTGOING).spliterator();
        Node newState = StreamSupport.stream(currentRelIterator, false).map(currentRel -> {
            Node currentState = currentRel.getEndNode();
            Long currentDate = (Long) currentRel.getProperty("date");

            // Patching the current node into the new one.
            Map<String, Object> patchedProps = currentState.getAllProperties();
            patchedProps.putAll(stateProps);
            Node result = setProperties(db.createNode(labels(labelNames)), patchedProps);

            // Creating PREVIOUS relationship between the current and the new State
            result.createRelationshipTo(currentState, RelationshipType.withName(PREVIOUS_TYPE)).setProperty(DATE_PROP, currentDate);

            // Updating the HAS_STATE rel for the current node, adding endDate
            currentState.getRelationships(RelationshipType.withName(HAS_STATE_TYPE), Direction.INCOMING)
                    .forEach(hasStatusRel -> hasStatusRel.setProperty(END_DATE_PROP, instantDate));

            // Refactoring current relationship and adding the new ones
            currentRel.delete();
            entity.createRelationshipTo(result, RelationshipType.withName(CURRENT_TYPE)).setProperty(DATE_PROP, instantDate);
            entity.createRelationshipTo(result, RelationshipType.withName(HAS_STATE_TYPE)).setProperty(START_DATE_PROP, instantDate);
            return result;
        }).findFirst().orElseGet(() -> {
            Node result = setProperties(db.createNode(labels(labelNames)), stateProps);
            entity.createRelationshipTo(result, RelationshipType.withName(CURRENT_TYPE)).setProperty(DATE_PROP, instantDate);
            entity.createRelationshipTo(result, RelationshipType.withName(HAS_STATE_TYPE)).setProperty(START_DATE_PROP, instantDate);
            return result;
        });

        log.info(LOGGER_TAG + "Patched Entity with id {}, adding a State with id {}", entity.getId(), newState.getId());

        return Stream.of(new NodeOutput(newState));
    }
}

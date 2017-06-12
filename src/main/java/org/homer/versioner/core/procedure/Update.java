package org.homer.versioner.core.procedure;

import org.homer.versioner.core.core.CoreProcedure;
import org.homer.versioner.core.output.NodeOutput;
import org.homer.versioner.core.Utility;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Update class, it contains all the Procedures needed to update Entities' States
 */
public class Update extends CoreProcedure {

    @Procedure(value = "graph.versioner.update", mode = Mode.WRITE)
    @Description("graph.versioner.update(entity, {key:value,...}, additionalLabel, date) - Add a new State to the given Entity.")
    public Stream<NodeOutput> update(
            @Name("entity") Node entity,
            @Name("stateProps") Map<String, Object> stateProps,
            @Name(value = "additionalLabel", defaultValue = "") String additionalLabel,
            @Name(value = "date", defaultValue = "0") long date) {

        // Creating the new State
        List<String> labelNames = new ArrayList<>(Collections.singletonList(Utility.STATE_LABEL));
        if (!additionalLabel.isEmpty()) {
            labelNames.add(additionalLabel);
        }
        Node result = Utility.setProperties(db.createNode(Utility.labels(labelNames)), stateProps);

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;

        // Getting the CURRENT rel if it exist
        Spliterator<Relationship> currentRelIterator = entity.getRelationships(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING).spliterator();
        StreamSupport.stream(currentRelIterator, false).forEach(currentRel -> {
            Node currentState = currentRel.getEndNode();
            Long currentDate = (Long) currentRel.getProperty("date");

            // Creating PREVIOUS relationship between the current and the new State
            result.createRelationshipTo(currentState, RelationshipType.withName(Utility.PREVIOUS_TYPE)).setProperty(Utility.DATE_PROP, currentDate);

            // Updating the HAS_STATE rel for the current node, adding endDate
            currentState.getRelationships(RelationshipType.withName(Utility.HAS_STATE_TYPE), Direction.INCOMING)
                    .forEach(hasStatusRel -> hasStatusRel.setProperty(Utility.END_DATE_PROP, instantDate));

            // Refactoring current relationship and adding the new ones
            currentRel.delete();
        });

        // Connecting the new current state to the Entity
        Utility.addCurrentState(result, entity, instantDate);

        log.info(Utility.LOGGER_TAG + "Updated Entity with id {}, adding a State with id {}", entity.getId(), result.getId());

        return Stream.of(new NodeOutput(result));
    }

    @Procedure(value = "graph.versioner.patch", mode = Mode.WRITE)
    @Description("graph.versioner.patch(entity, {key:value,...}, additionalLabel, date) - Add a new State to the given Entity, starting from the previous one. It will update all the properties, not labels.")
    public Stream<NodeOutput> patch(
            @Name("entity") Node entity,
            @Name("stateProps") Map<String, Object> stateProps,
            @Name(value = "additionalLabel", defaultValue = "") String additionalLabel,
            @Name(value = "date", defaultValue = "0") long date) {

        // Creating the new State
        List<String> labelNames = new ArrayList<>(Collections.singletonList(Utility.STATE_LABEL));
        if (!additionalLabel.isEmpty()) {
            labelNames.add(additionalLabel);
        }

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;

        // Getting the CURRENT rel if it exist
        Spliterator<Relationship> currentRelIterator = entity.getRelationships(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING).spliterator();
        Node newState = StreamSupport.stream(currentRelIterator, false).map(currentRel -> {
            Node currentState = currentRel.getEndNode();
            Long currentDate = (Long) currentRel.getProperty("date");

            // Patching the current node into the new one.
            Map<String, Object> patchedProps = currentState.getAllProperties();
            patchedProps.putAll(stateProps);
            Node result = Utility.setProperties(db.createNode(Utility.labels(labelNames)), patchedProps);

            // Updating CURRENT state
            result = Utility.currentStateUpdate(entity, instantDate, currentRel, currentState, currentDate, result);

            return result;
        }).findFirst().orElseGet(() -> {
            Node result = Utility.setProperties(db.createNode(Utility.labels(labelNames)), stateProps);

            // Connecting the new current state to the Entity
            Utility.addCurrentState(result, entity, instantDate);

            return result;
        });

        log.info(Utility.LOGGER_TAG + "Patched Entity with id {}, adding a State with id {}", entity.getId(), newState.getId());

        return Stream.of(new NodeOutput(newState));
    }
}

package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.Utility;
import org.homer.graph.versioner.output.NodeOutput;
import org.neo4j.graphdb.*;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Update class, it contains all the Procedures needed to update Entities' States
 */
public class Update {

    @Context
    public GraphDatabaseService db;

    @Procedure(value = "graph.versioner.update", mode = Mode.WRITE)
    @Description("graph.versioner.update(entity, {key:value,...}, additionalLabel, date) - Add a new State to the given Entity.")
    public Stream<NodeOutput> update(
            @Name("entity") Node entity,
            @Name("stateProps") Map<String, Object> stateProps,
            @Name(value = "additionalLabel", defaultValue = "") String additionalLabel,
            @Name(value = "date", defaultValue = "0") long date) {


        // Creating the new State
        List<String> labelNames = new ArrayList<String>();
        labelNames.add(Utility.STATE_LABEL);
        if (!additionalLabel.isEmpty()) {
            labelNames.add(additionalLabel);
        }
        Node newState = Utility.setProperties(db.createNode(Utility.labels(labelNames)), stateProps);

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;

        // Getting the CURRENT rel if it exist
        boolean currentExist = false;
        Iterator<Relationship> currentRelIterator = entity.getRelationships(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING).iterator();
        while (currentRelIterator.hasNext()) {
            currentExist = true;
            Relationship currentRel = currentRelIterator.next();
            Node currentState = currentRel.getEndNode();
            Long currentDate = (Long) currentRel.getProperty("date");

            // Creating PREVIOUS relationship between the current and the new State
            newState.createRelationshipTo(currentState, RelationshipType.withName(Utility.PREVIOUS_TYPE)).setProperty(Utility.DATE_PROP, currentDate);

            // Updating the HAS_STATUS rel for the current node, adding endDate
            Iterator<Relationship> hasStatusRelIterator = currentState.getRelationships(RelationshipType.withName(Utility.HAS_STATE_TYPE), Direction.INCOMING).iterator();
            while (hasStatusRelIterator.hasNext()) {
                Relationship hasStatusRel = hasStatusRelIterator.next();
                hasStatusRel.setProperty(Utility.END_DATE_PROP, instantDate);

            }

            // Refactoring current relationship and adding the new ones
            currentRel.delete();
            entity.createRelationshipTo(newState, RelationshipType.withName(Utility.CURRENT_TYPE)).setProperty(Utility.DATE_PROP, instantDate);
            entity.createRelationshipTo(newState, RelationshipType.withName(Utility.HAS_STATE_TYPE)).setProperty(Utility.START_DATE_PROP, instantDate);
        }

        if (!currentExist) {
            entity.createRelationshipTo(newState, RelationshipType.withName(Utility.CURRENT_TYPE)).setProperty(Utility.DATE_PROP, instantDate);
            entity.createRelationshipTo(newState, RelationshipType.withName(Utility.HAS_STATE_TYPE)).setProperty(Utility.START_DATE_PROP, instantDate);
        }

        return Stream.of(new NodeOutput(newState));
    }
}

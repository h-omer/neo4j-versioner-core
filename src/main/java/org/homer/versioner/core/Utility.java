package org.homer.versioner.core;

import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Utility class, it contains some common utility methods and constants
 */
public class Utility {

    public static final String STATE_LABEL = "State";
    public static final String CURRENT_TYPE = "CURRENT";
    public static final String HAS_STATE_TYPE = "HAS_STATE";
    public static final String PREVIOUS_TYPE = "PREVIOUS";
    public static final String ROLLBACK_TYPE = "ROLLBACK";
    public static final String DATE_PROP = "date";
    public static final String START_DATE_PROP = "startDate";
    public static final String END_DATE_PROP = "endDate";
    public static final String LOGGER_TAG = "[graph-versioner] - ";
    /*   DIFF OPERATIONS   */
    public static final String DIFF_OPERATION_REMOVE = "REMOVE";
    public static final String DIFF_OPERATION_ADD = "ADD";
    public static final String DIFF_OPERATION_UPDATE = "UPDATE";
    public static final List<String> DIFF_OPERATIONS_SORTING = Arrays.asList(DIFF_OPERATION_REMOVE, DIFF_OPERATION_UPDATE, DIFF_OPERATION_ADD);


    /**
     * Sets a {@link Map} of properties to a {@link Node}
     *
     * @param node  passed node
     * @param props properties to be set
     * @return a node with properties
     */
    public static Node setProperties(Node node, Map<String, Object> props) {
        props.forEach(node::setProperty);
        return node;
    }

    /**
     * Sets a {@link List} of label names into a {@link Label[]}
     *
     * @param labelNames a {@link List} of label names
     * @return {@link Label[]}
     */
    public static Label[] labels(List<String> labelNames) {
        Label[] result;
        if (Objects.isNull(labelNames)) {
            result = new Label[0];
        } else {
            result = labelNames.stream().filter(Objects::nonNull).map(Label::label).toArray(Label[]::new);
        }
        return result;
    }

    /**
     * Sets a {@link String} as a singleton Array {@link Label[]}
     *
     * @param labelName a {@link String} representing the unique label
     * @return {@link Label[]}
     */
    public static Label[] labels(String labelName) {
        return new Label[]{Label.label(labelName)};
    }

    /**
     * Creates a new node copying properties and labels form a given one
     *
     * @param db   a {@link GraphDatabaseService} representing the database where the node will be created
     * @param node a {@link Node} representing the node to clone
     * @return {@link Node}
     */
    public static Node cloneNode(GraphDatabaseService db, Node node) {
        List<String> labelNames = new ArrayList<>();
        Spliterator<Label> labelsIterator = node.getLabels().spliterator();
        StreamSupport.stream(labelsIterator, false).forEach(label -> labelNames.add(label.name()));
        return setProperties(db.createNode(labels(labelNames)), node.getAllProperties());
    }

    /**
     * Updates an Entity node CURRENT State with the new current one, it will handle all the
     * HAS_STATE / CURRENT / PREVIOUS relationships creation/update
     *
     * @param entity              a {@link Node} representing the Entity
     * @param instantDate         the new current State date
     * @param currentRelationship a {@link Relationship} representing the current CURRENT relationship
     * @param currentState        a {@link Node} representing the current State
     * @param currentDate         the current State date
     * @param result              a {@link Node} representing the new current State
     * @return {@link Node}
     */
    public static Node currentStateUpdate(Node entity, long instantDate, Relationship currentRelationship, Node currentState, Long currentDate, Node result) {
        // Creating PREVIOUS relationship between the current and the new State
        result.createRelationshipTo(currentState, RelationshipType.withName(PREVIOUS_TYPE)).setProperty(DATE_PROP, currentDate);

        // Updating the HAS_STATE rel for the current node, adding endDate
        currentState.getRelationships(RelationshipType.withName(HAS_STATE_TYPE), Direction.INCOMING)
                .forEach(hasStatusRel -> hasStatusRel.setProperty(END_DATE_PROP, instantDate));

        // Refactoring current relationship and adding the new ones
        currentRelationship.delete();

        // Connecting the new current state to the Entity
        addCurrentState(result, entity, instantDate);

        return result;
    }

    /**
     * Connects a new State {@link Node} as the Current one, to the given Entity
     *
     * @param state       a {@link Node} representing the new current State
     * @param entity      a {@link Node} representing the Entity
     * @param instantDate the new current State date
     */
    public static void addCurrentState(Node state, Node entity, long instantDate) {
        entity.createRelationshipTo(state, RelationshipType.withName(CURRENT_TYPE)).setProperty(DATE_PROP, instantDate);
        entity.createRelationshipTo(state, RelationshipType.withName(HAS_STATE_TYPE)).setProperty(START_DATE_PROP, instantDate);
    }
}

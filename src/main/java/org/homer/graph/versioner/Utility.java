package org.homer.graph.versioner;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

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
	 * @param db a {@link GraphDatabaseService} representing the database where the node will be created
     * @param node a {@link Node} representing the node to clone
     * @return {@link Node}
     */
    public static Node cloneNode(GraphDatabaseService db, Node node) {
		List<String> labelNames = new ArrayList<>();
		Spliterator<Label> labelsIterator = node.getLabels().spliterator();
		StreamSupport.stream(labelsIterator, false).forEach(label -> labelNames.add(label.name()));
		return setProperties(db.createNode(labels(labelNames)), node.getAllProperties());
    }
}

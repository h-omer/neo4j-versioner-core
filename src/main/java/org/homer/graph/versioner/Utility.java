package org.homer.graph.versioner;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility class, it contains some common utility methods and constants
 */
public class Utility {

    public static final String STATE_LABEL = "State";
    public static final String CURRENT_TYPE = "CURRENT";
    public static final String HAS_STATE_TYPE = "HAS_STATE";
    public static final String PREVIOUS_TYPE = "PREVIOUS";
    public static final String DATE_PROP = "date";
    public static final String START_DATE_PROP = "startDate";
    public static final String END_DATE_PROP = "endDate";
    public static final String CONTEXT_PROP = "context";

    /**
     * Sets a {@link Map} of properties to a {@link Node}
     *
     * @param node passed node
     * @param props properties to be set
     * @return a node with properties
     */
    public static Node setProperties(Node node, Map<String, Object> props) {
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            node.setProperty(entry.getKey(), entry.getValue());
        }

        return node;
    }

    /**
     * Sets a {@link List} of label names into a {@link Label[]}
     * @param labelNames a {@link List} of label names
     * @return {@link Label[]}
     */
    public static Label[] labels(Object labelNames) {
        if (labelNames == null) return new Label[0];
        if (labelNames instanceof List) {
            List names = (List) labelNames;
            Label[] labels = new Label[names.size()];
            int i = 0;
            for (Object l : names) {
                if (l == null) continue;
                labels[i++] = Label.label(l.toString());
            }
            if (i <= labels.length) return Arrays.copyOf(labels, i);
            return labels;
        }
        return new Label[]{Label.label(labelNames.toString())};
    }
}

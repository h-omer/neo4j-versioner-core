package org.homer.graph.versioner;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by marco.falcier on 03/05/17.
 */
public class Utility {

    public static final String STATE_LABEL = "State";
    public static final String CURRENT_TYPE = "CURRENT";
    public static final String HAS_STATUS_TYPE = "HAS_STATUS";
    public static final String PREVIOUS_TYPE = "PREVIOUS";
    public static final String DATE_PROP = "date";
    public static final String START_DATE_PROP = "startDate";
    public static final String END_DATE_PROP = "endDate";
    public static final String CONTEXT_PROP = "context";

    public static Node setProperties(Node node, Map<String, Object> props) {
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            node.setProperty(entry.getKey(), entry.getValue());
        }

        return node;
    }

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

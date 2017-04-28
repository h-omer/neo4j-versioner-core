package org.homer.graph.versioner;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by marco.falcier on 23/03/17.
 */
public class Init {

    @Context
    public GraphDatabaseService db;

    public static final String STATE_LABEL = "State";
    public static final String CURRENT_TYPE = "CURRENT";
    public static final String HAS_NEXT_TYPE = "HAS_NEXT";
    public static final String DATE_PROP = "date";
    public static final String START_DATE_PROP = "startDate";

    @Procedure(value = "graph.versioner.init", mode = Mode.WRITE)
    @Description("graph.versioner.init(entityLabel, ['entityProp1','entityProp1',...], ['stateProp1','stateProp1',...]) - Create an Entity node with an initial State.")
    public Stream<Output> init(
            @Name("entityLabel") String entityLabel,
            @Name(value = "entityProps", defaultValue = "{}") Map<String, Object> entityProps,
            @Name(value = "stateProps", defaultValue = "{}") Map<String, Object> stateProps) {

        List<String> labelNames = new ArrayList<String>();
        labelNames.add(entityLabel);
        Node entity = this.setProperties(db.createNode(this.labels(labelNames)), entityProps);

        if (!stateProps.isEmpty()) {
            labelNames = new ArrayList<String>();
            labelNames.add(STATE_LABEL);
            Node state = this.setProperties(db.createNode(this.labels(labelNames)), stateProps);

            long date = Calendar.getInstance().getTimeInMillis();
            entity.createRelationshipTo(state, RelationshipType.withName(CURRENT_TYPE)).setProperty(DATE_PROP, date);
            entity.createRelationshipTo(state, RelationshipType.withName(HAS_NEXT_TYPE)).setProperty(START_DATE_PROP, date);
        }

        return Stream.of(new Output(entity.getId()));
    }

    private Label[] labels(Object labelNames) {
        if (labelNames==null) return new Label[0];
        if (labelNames instanceof List) {
            List names = (List) labelNames;
            Label[] labels = new Label[names.size()];
            int i = 0;
            for (Object l : names) {
                if (l==null) continue;
                labels[i++] = Label.label(l.toString());
            }
            if (i <= labels.length) return Arrays.copyOf(labels,i);
            return labels;
        }
        return new Label[]{Label.label(labelNames.toString())};
    }

    private Node setProperties(Node node, Map<String, Object> props) {
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            node.setProperty(entry.getKey(), entry.getValue());
        }

        return node;
    }
}

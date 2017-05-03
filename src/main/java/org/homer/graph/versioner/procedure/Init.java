package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.output.NodeId;
import org.homer.graph.versioner.Utility;
import org.neo4j.graphdb.GraphDatabaseService;
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

    @Procedure(value = "graph.versioner.init", mode = Mode.WRITE)
    @Description("graph.versioner.init(entityLabel, {key:value,...}, {key:value,...}, context) - Create an Entity node with an initial State.")
    public Stream<NodeId> init(
            @Name("entityLabel") String entityLabel,
            @Name(value = "entityProps", defaultValue = "{}") Map<String, Object> entityProps,
            @Name(value = "stateProps", defaultValue = "{}") Map<String, Object> stateProps,
            @Name(value = "context", defaultValue = "") String context ) {

        List<String> labelNames = new ArrayList<String>();
        labelNames.add(entityLabel);
        Node entity = Utility.setProperties(db.createNode(Utility.labels(labelNames)), entityProps);

        if (!stateProps.isEmpty()) {
            labelNames = new ArrayList<String>();
            labelNames.add(Utility.STATE_LABEL);
            Node state = Utility.setProperties(db.createNode(Utility.labels(labelNames)), stateProps);
            if (!context.isEmpty()) {
                state.setProperty(Utility.CONTEXT_PROP, context);
            } else {
                state.setProperty(Utility.CONTEXT_PROP, "Initial State");
            }

            long date = Calendar.getInstance().getTimeInMillis();
            entity.createRelationshipTo(state, RelationshipType.withName(Utility.CURRENT_TYPE)).setProperty(Utility.DATE_PROP, date);
            entity.createRelationshipTo(state, RelationshipType.withName(Utility.HAS_STATUS_TYPE)).setProperty(Utility.START_DATE_PROP, date);
        }

        return Stream.of(new NodeId(entity.getId()));
    }
}

package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.core.CoreProcedure;
import org.homer.graph.versioner.output.NodeOutput;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.homer.graph.versioner.Utility.*;

/**
 * Init class, it contains all the Procedures needed to initialize an Entity node
 */
public class Init extends CoreProcedure{

    @Procedure(value = "graph.versioner.init", mode = Mode.WRITE)
    @Description("graph.versioner.init(entityLabel, {key:value,...}, {key:value,...}, additionalLabel, date) - Create an Entity node with an optional initial State.")
    public Stream<NodeOutput> init(
            @Name("entityLabel") String entityLabel,
            @Name(value = "entityProps", defaultValue = "{}") Map<String, Object> entityProps,
            @Name(value = "stateProps", defaultValue = "{}") Map<String, Object> stateProps,
            @Name(value = "additionalLabel", defaultValue = "") String additionalLabel,
            @Name(value = "date", defaultValue = "0") long date) {

        List<String> labelNames = new ArrayList<>(singletonList(entityLabel));

        Node entity = setProperties(db.createNode(labels(labelNames)), entityProps);

        if (!stateProps.isEmpty()) {
            labelNames = new ArrayList<>(singletonList(STATE_LABEL));
            if (!additionalLabel.isEmpty()) {
                labelNames.add(additionalLabel);
            }
            Node state = setProperties(db.createNode(labels(labelNames)), stateProps);

            long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;

            // Connecting the new current state to the Entity
            addCurrentState(state, entity, instantDate);
        }

        log.info(LOGGER_TAG + "Created a new Entity with label {} and id {}", entityLabel, entity.getId());

        return Stream.of(new NodeOutput(entity));
    }
}

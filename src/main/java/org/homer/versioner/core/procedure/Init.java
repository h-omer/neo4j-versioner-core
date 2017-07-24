package org.homer.versioner.core.procedure;

import org.homer.versioner.core.Utility;
import org.homer.versioner.core.core.CoreProcedure;
import org.homer.versioner.core.output.NodeOutput;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

/**
 * Init class, it contains all the Procedures needed to initialize an Entity node
 */
public class Init extends CoreProcedure {

    @Procedure(value = "graph.versioner.init", mode = Mode.WRITE)
    @Description("graph.versioner.init(entityLabel, {key:value,...}, {key:value,...}, additionalLabel, date) - Create an Entity node with an optional initial State.")
    public Stream<NodeOutput> init(
            @Name("entityLabel") String entityLabel,
            @Name(value = "entityProps", defaultValue = "{}") Map<String, Object> entityProps,
            @Name(value = "stateProps", defaultValue = "{}") Map<String, Object> stateProps,
            @Name(value = "additionalLabel", defaultValue = "") String additionalLabel,
            @Name(value = "date", defaultValue = "0") long date) {

        List<String> labelNames = new ArrayList<>(singletonList(entityLabel));

        Node entity = Utility.setProperties(db.createNode(Utility.labels(labelNames)), entityProps);

        labelNames = new ArrayList<>(singletonList(Utility.STATE_LABEL));
        if (!additionalLabel.isEmpty()) {
            labelNames.add(additionalLabel);
        }
        Node state = Utility.setProperties(db.createNode(Utility.labels(labelNames)), stateProps);

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;

        // Connecting the new current state to the Entity
            Utility.addCurrentState(state, entity, instantDate);

        log.info(Utility.LOGGER_TAG + "Created a new Entity with label {} and id {}", entityLabel, entity.getId());

        return Stream.of(new NodeOutput(entity));
    }
}

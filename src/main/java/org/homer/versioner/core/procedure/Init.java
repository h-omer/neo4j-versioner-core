package org.homer.versioner.core.procedure;

import org.homer.versioner.core.Utility;
import org.homer.versioner.core.core.CoreProcedure;
import org.homer.versioner.core.output.NodeOutput;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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

        Node entity = createNode(entityProps, singletonList(entityLabel));

        Node state = createNode(stateProps, getStateLabels(additionalLabel));

        connectWithCurrentRelationship(entity, state, date);

        log.info(Utility.LOGGER_TAG + "Created a new Entity with label {} and id {}", entityLabel, entity.getId());

        createRNodeAndAssociateTo(entity);

        return Utility.streamOfNodes(entity);
    }

    private Node createNode(Map<String, Object> properties, List<String> labels)  {

        return Utility.setProperties(db.createNode(Utility.labels(labels)), properties);
    }

    private List<String> getStateLabels(String label) {

        return Stream.of(Utility.STATE_LABEL, label)
                .filter(l -> Objects.nonNull(l) && !l.isEmpty())
                .collect(Collectors.toList());
    }

    private void connectWithCurrentRelationship(Node entity, Node state, long date) {

        long instantDate = (date == 0) ? Calendar.getInstance().getTimeInMillis() : date;
        Utility.addCurrentState(state, entity, instantDate);
    }

    private void createRNodeAndAssociateTo(Node entity) {

        Node rNode = db.createNode(Label.label("R"));
        rNode.createRelationshipTo(entity, RelationshipType.withName("FOR"));
    }
}

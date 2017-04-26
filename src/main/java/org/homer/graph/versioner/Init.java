package org.homer.graph.versioner;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by marco.falcier on 23/03/17.
 */
public class Init {

    @Context
    public GraphDatabaseService db;

    @Procedure(value = "graph.versioner.init", mode = Mode.WRITE)
    @Description("graph.versioner.init(entityLabel, ['entityProp1','entityProp1',...], ['stateProp1','stateProp1',...]) - Create an Entity node with an initial State.")
    public Stream<Output> init(
            @Name("entityLabel") String entityLabel,
            @Name("entityProps") List<Map<String, Object>> entityProps,
            @Name("stateProps") List<Map<String, Object>> stateProps) {

        List<String> labelNames = new ArrayList<String>();
        labelNames.add(entityLabel);
        Node node = db.createNode(this.labels(labelNames));

        return Stream.of(new Output(node.getId()));
    }

    public class Output {
        public long id;

        public Output(long id) {
            this.id = id;
        }
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
}

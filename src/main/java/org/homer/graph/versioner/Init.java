package org.homer.graph.versioner;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.List;
import java.util.stream.Stream;

/**
 * Created by marco.falcier on 23/03/17.
 */
public class Init {

    @Procedure(value = "graph.versioner.init", mode = Mode.WRITE)
    @Description("graph.versioner.init(entityLabel, ['entityProp1','entityProp1',...], ['stateProp1','stateProp1',...]) - Create an Entity node with an initial State.")
    public Stream<Output> init(
            @Name("entityLabel") String entityLabel,
            @Name("entityProps") List<String> entityProps,
            @Name("stateProps") List<String> stateProps) {



        return Stream.of(new Output(1l));
    }

    public class Output {
        public long id;

        public Output(long id) {
            this.id = id;
        }
    }
}

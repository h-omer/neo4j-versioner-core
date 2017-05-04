package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.Utility;
import org.homer.graph.versioner.output.PathOutput;
import org.neo4j.graphdb.*;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

/**
 * Get class, it contains all the Procedures needed to retrieve Entities and States nodes from the database
 */
public class Get {

    @Context
    public GraphDatabaseService db;

    @Procedure(value = "graph.versioner.get.current", mode = Mode.DEFAULT)
    @Description("graph.versioner.get.current(entity) - Get a the current Path (Entity, State and rels) for the given Entity")
    public Stream<PathOutput> getCurrent(
            @Name("entity") Node entity) {

        ResourceIterator<Path> result = this.db.execute(
                "MATCH p=(e)-[c:" + Utility.CURRENT_TYPE + "]->(s:" + Utility.STATE_LABEL + ")<-[hs:" + Utility.HAS_STATE_TYPE + "]-(e) " +
                        "WHERE id(e)=" + entity.getId() +
                        " RETURN p").columnAs("p");

        return result.stream().map(PathOutput::new);
    }
}

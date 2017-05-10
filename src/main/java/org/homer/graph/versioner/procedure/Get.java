package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.output.NodeOutput;
import org.homer.graph.versioner.output.PathOutput;
import org.neo4j.graphdb.*;
import org.neo4j.procedure.*;

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.neo4j.procedure.Mode.*;
import static org.homer.graph.versioner.Utility.*;
/**
 * Get class, it contains all the Procedures needed to retrieve Entities and States nodes from the database
 */
public class Get {

    @Context
    public GraphDatabaseService db;

    @Procedure(value = "graph.versioner.get.current.path", mode = DEFAULT)
    @Description("graph.versioner.get.current.path(entity) - Get a the current Path (Entity, State and rels) for the given Entity.")
    public Stream<PathOutput> getCurrentPath(
            @Name("entity") Node entity) {

        String query = "MATCH p=(e)-[c:%s]->(s:%s)<-[hs:%s]-(e) WHERE id(e)=%d RETURN p";

        ResourceIterator<Path> result = this.db.execute(String.format(query,
                CURRENT_TYPE,
                STATE_LABEL,
                HAS_STATE_TYPE,
                entity.getId()
        )).columnAs("p");

        return result.stream().map(PathOutput::new);
    }

    @Procedure(value = "graph.versioner.get.current.state", mode = DEFAULT)
    @Description("graph.versioner.get.current.state(entity) - Get the current State node for the given Entity.")
    public Stream<NodeOutput> getCurrentState(
            @Name("entity") Node entity) {

		Spliterator<Relationship> iterator = entity.getRelationships(RelationshipType.withName(CURRENT_TYPE), Direction.OUTGOING).spliterator();
		return StreamSupport.stream(iterator, false).map(currentState -> new NodeOutput(currentState.getEndNode()));
    }

    @Procedure(value = "graph.versioner.get.all", mode = DEFAULT)
    @Description("graph.versioner.get.all(entity) - Get all the State nodes for the given Entity.")
    public Stream<PathOutput> getAllState(
            @Name("entity") Node entity) {

        String query = "MATCH (e)-[hs:%s]->(s:%s)-[p:%s]->(sn:%s) WHERE id(e)=%d " +
                "WITH s, p, sn " +
                "MATCH r=(s)-[p]->(sn) " +
                "RETURN r";

        ResourceIterator<Path> result = this.db.execute(String.format(query,
                HAS_STATE_TYPE,
                STATE_LABEL,
                PREVIOUS_TYPE,
                STATE_LABEL,
                entity.getId()
        )).columnAs("r");

        return result.stream().map(PathOutput::new);
    }
}

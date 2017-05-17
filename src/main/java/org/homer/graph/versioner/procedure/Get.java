package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.output.NodeOutput;
import org.homer.graph.versioner.output.PathOutput;
import org.neo4j.graphdb.*;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.homer.graph.versioner.Utility.*;
import static org.neo4j.procedure.Mode.DEFAULT;

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

        return getCurrentPathResourceIterator(entity).stream().map(PathOutput::new);
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

        ResourceIterator<Path> result;

        Spliterator<Relationship> hasStateRelsIterator = entity.getRelationships(RelationshipType.withName(HAS_STATE_TYPE), Direction.OUTGOING).spliterator();

        if (StreamSupport.stream(hasStateRelsIterator, false).count() != 1) {
            String query = "MATCH path=(e)-[%s]->(:%s)-[:%s*]->(:%s) WHERE id(e)=%d " +
                    "RETURN path, length(path) ORDER BY length(path) DESC LIMIT 1";

            result = this.db.execute(String.format(query,
                    CURRENT_TYPE,
                    STATE_LABEL,
                    PREVIOUS_TYPE,
                    STATE_LABEL,
                    entity.getId()
            )).columnAs("path");
        } else {
            result = getCurrentPathResourceIterator(entity);
        }

        return result.stream().map(PathOutput::new);
    }

    @Procedure(value = "graph.versioner.get.by.label", mode = DEFAULT)
    @Description("graph.versioner.get.by.label(entity, label) - Get State nodes with the given label, by the given Entity node")
    public Stream<NodeOutput> getAllStateByLabel(
            @Name("entity") Node entity,
            @Name("label") String label) {

        Collection<Node> result = new ArrayList<>();

        Spliterator<Relationship> hasStateRelsIterator = entity.getRelationships(RelationshipType.withName(HAS_STATE_TYPE), Direction.OUTGOING).spliterator();

        StreamSupport.stream(hasStateRelsIterator, false).forEach(relationship -> {
            Node state = relationship.getEndNode();

            if (state.hasLabel(Label.label(label))) {
                result.add(state);
            }
        });

        return result.parallelStream().map(NodeOutput::new);
    }

    @Procedure(value = "graph.versioner.get.by.date", mode = DEFAULT)
    @Description("graph.versioner.get.by.date(entity, date) - Get State node by the given Entity node, created at the given date")
    public Stream<NodeOutput> getStateByDate(
            @Name("entity") Node entity,
            @Name("date") long date) {

        Collection<Node> result = new ArrayList<>();

        Spliterator<Relationship> hasStateRelsIterator = entity.getRelationships(RelationshipType.withName(HAS_STATE_TYPE), Direction.OUTGOING).spliterator();

        StreamSupport.stream(hasStateRelsIterator, false).forEach(relationship -> {
            Node state = relationship.getEndNode();

            if (relationship.getProperty("startDate").equals(date)) {
                result.add(state);
            }
        });

        return result.parallelStream().map(NodeOutput::new);
    }

    private ResourceIterator<Path> getCurrentPathResourceIterator(Node node) {
        String query = "MATCH path=(e)-[c:%s]->(s:%s) WHERE id(e)=%d RETURN path";

        return this.db.execute(String.format(query,
                CURRENT_TYPE,
                STATE_LABEL,
                node.getId()
        )).columnAs("path");
    }
}

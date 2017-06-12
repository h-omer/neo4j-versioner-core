package org.homer.versioner.core.procedure;

import org.homer.versioner.core.Utility;
import org.homer.versioner.core.output.NodeOutput;
import org.homer.versioner.core.output.PathOutput;
import org.neo4j.cypher.internal.compiler.v3_2.commands.expressions.PathValueBuilder;
import org.neo4j.graphdb.*;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.neo4j.procedure.Mode.DEFAULT;

/**
 * Get class, it contains all the Procedures needed to retrieve Entities and States nodes from the database
 */
public class Get {

    @Procedure(value = "graph.versioner.get.current.path", mode = DEFAULT)
    @Description("graph.versioner.get.current.path(entity) - Get the current Path (Entity, State and rels) for the given Entity.")
    public Stream<PathOutput> getCurrentPath(
            @Name("entity") Node entity) {

		PathValueBuilder builder = new PathValueBuilder().addNode(entity);

		Optional.ofNullable(entity.getSingleRelationship(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING)).map(builder::addOutgoingRelationship);

        return Stream.of(builder.result()).map(PathOutput::new);
    }

    @Procedure(value = "graph.versioner.get.current.state", mode = DEFAULT)
    @Description("graph.versioner.get.current.state(entity) - Get the current State node for the given Entity.")
    public Stream<NodeOutput> getCurrentState(
            @Name("entity") Node entity) {

        return Stream.of(Optional.ofNullable(entity.getSingleRelationship(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING))
				.map(Relationship::getEndNode).map(NodeOutput::new).orElse(null));
    }

    @Procedure(value = "graph.versioner.get.all", mode = DEFAULT)
    @Description("graph.versioner.get.all(entity) - Get all the State nodes for the given Entity.")
    public Stream<PathOutput> getAllState(
            @Name("entity") Node entity) {

		PathValueBuilder builder = new PathValueBuilder();
		builder.addNode(entity);
		builder.addOutgoingRelationship(entity.getSingleRelationship(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING));
		StreamSupport.stream(entity.getRelationships(RelationshipType.withName(Utility.HAS_STATE_TYPE), Direction.OUTGOING).spliterator(), false)
				//.sorted((a, b) -> -1 * Long.compare((long)a.getProperty(START_DATE_PROP), (long)b.getProperty(START_DATE_PROP)))
				.forEach(rel ->
					Optional.ofNullable(rel.getEndNode().getSingleRelationship(RelationshipType.withName(Utility.PREVIOUS_TYPE), Direction.OUTGOING))
							.map(builder::addOutgoingRelationship)
		);

		return Stream.of(new PathOutput(builder.result()));
    }

    @Procedure(value = "graph.versioner.get.by.label", mode = DEFAULT)
    @Description("graph.versioner.get.by.label(entity, label) - Get State nodes with the given label, by the given Entity node")
    public Stream<NodeOutput> getAllStateByLabel(
            @Name("entity") Node entity,
            @Name("label") String label) {

		return StreamSupport.stream(entity.getRelationships(RelationshipType.withName(Utility.HAS_STATE_TYPE), Direction.OUTGOING).spliterator(), false)
				.map(Relationship::getEndNode)
				.filter(node -> node.hasLabel(Label.label(label)))
				.map(NodeOutput::new);
    }

    @Procedure(value = "graph.versioner.get.by.date", mode = DEFAULT)
    @Description("graph.versioner.get.by.date(entity, date) - Get State node by the given Entity node, created at the given date")
    public Stream<NodeOutput> getStateByDate(
            @Name("entity") Node entity,
            @Name("date") long date) {

		return StreamSupport.stream(entity.getRelationships(RelationshipType.withName(Utility.HAS_STATE_TYPE), Direction.OUTGOING).spliterator(), false)
				.filter(relationship -> relationship.getProperty(Utility.START_DATE_PROP).equals(date))
				.map(Relationship::getEndNode)
				.map(NodeOutput::new);
    }
}

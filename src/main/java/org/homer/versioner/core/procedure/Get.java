package org.homer.versioner.core.procedure;

import org.homer.versioner.core.Utility;
import org.homer.versioner.core.output.NodeOutput;
import org.homer.versioner.core.output.PathOutput;
import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.time.LocalDateTime;
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

        PathImpl.Builder builder = new PathImpl.Builder(entity);

        builder = Optional.ofNullable(entity.getSingleRelationship(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING))
                .map(builder::push)
                .orElse(new PathImpl.Builder(entity));

        return Stream.of(builder.build()).map(PathOutput::new);
    }

    @Procedure(value = "graph.versioner.get.current.state", mode = DEFAULT)
    @Description("graph.versioner.get.current.state(entity) - Get the current State node for the given Entity.")
    public Stream<NodeOutput> getCurrentState(
            @Name("entity") Node entity) {

        return Stream.of(Optional.ofNullable(entity.getSingleRelationship(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING))
                .map(Relationship::getEndNode).map(NodeOutput::new).orElse(null));
    }


    //fix for error Node[xyz] not connected to this relationship[xyz]
    @Procedure(value = "graph.versioner.get.all", mode = DEFAULT)
    @Description("graph.versioner.get.all(entity) - Get all the State nodes for the given Entity.")
    public Stream<PathOutput> getAllState(
            @Name("entity") Node entity) {

        PathImpl.Builder builder = new PathImpl.Builder(entity)
                .push(entity.getSingleRelationship(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING));
        builder = StreamSupport.stream(entity.getRelationships(Direction.OUTGOING, RelationshipType.withName(Utility.HAS_STATE_TYPE)).spliterator(), false)
                .reduce(
                        builder,
                        (build, rel) -> Optional.ofNullable(rel.getEndNode().getSingleRelationship(RelationshipType.withName(Utility.PREVIOUS_TYPE), Direction.OUTGOING))
                                            .map(build::push)
                                            .orElse(build),
                        (a, b) -> a);
        return Stream.of(new PathOutput(builder.build()));
    }

    @Procedure(value = "graph.versioner.get.by.label", mode = DEFAULT)
    @Description("graph.versioner.get.by.label(entity, label) - Get State nodes with the given label, by the given Entity node")
    public Stream<NodeOutput> getAllStateByLabel(
            @Name("entity") Node entity,
            @Name("label") String label) {

        return StreamSupport.stream(entity.getRelationships(Direction.OUTGOING, RelationshipType.withName(Utility.HAS_STATE_TYPE)).spliterator(), false)
                .map(Relationship::getEndNode)
                .filter(node -> node.hasLabel(Label.label(label)))
                .map(NodeOutput::new);
    }

    @Procedure(value = "graph.versioner.get.by.date", mode = DEFAULT)
    @Description("graph.versioner.get.by.date(entity, date) - Get State node by the given Entity node, created at the given date")
    public Stream<NodeOutput> getStateByDate(
            @Name("entity") Node entity,
            @Name("date") LocalDateTime date) {

        return StreamSupport.stream(entity.getRelationships(Direction.OUTGOING, RelationshipType.withName(Utility.HAS_STATE_TYPE)).spliterator(), false)
                .filter(relationship -> relationship.getProperty(Utility.START_DATE_PROP).equals(date))
                .map(Relationship::getEndNode)
                .map(NodeOutput::new);
    }

	@Procedure(value = "graph.versioner.get.nth.state", mode = DEFAULT)
	@Description("graph.versioner.get.nth.state(entity, nth) - Get the nth State node for the given Entity.")
	public Stream<NodeOutput> getNthState(
			@Name("entity") Node entity,
			@Name("nth") long nth) {

    	return getCurrentState(entity)
				.findFirst()
				.flatMap(currentState -> getNthStateFrom(currentState.node, nth))
				.map(Utility::streamOfNodes)
				.orElse(Stream.empty());
	}

	private Optional<Node> getNthStateFrom(Node state, long nth) {

		return Stream.iterate(Optional.of(state), s -> s.flatMap(this::jumpToPreviousState))
				.limit(nth + 1)
				.reduce((a, b) -> b) //get only the last value (apply jumpToPreviousState n times
				.orElse(Optional.empty());
	}

	private Optional<Node> jumpToPreviousState(Node state) {

    	return StreamSupport.stream(state.getRelationships(Direction.OUTGOING, RelationshipType.withName(Utility.PREVIOUS_TYPE)).spliterator(), false)
				.findFirst()
				.map(Relationship::getEndNode);
	}
}

package org.homer.graph.versioner.procedure;

import org.homer.graph.versioner.Utility;
import org.homer.graph.versioner.output.NodeOutput;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Calendar;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.homer.graph.versioner.Utility.*;
import static org.neo4j.procedure.Mode.WRITE;

/**
 * Rollback class, it contains all the Procedures needed to rollback Entities' States nodes in the database
 */
public class Rollback {

	@Context public GraphDatabaseService db;

	@Context public Log log;

	@Procedure(value = "graph.versioner.rollback", mode = WRITE) @Description("graph.versioner.rollback(entity) - Rollback the given Entity to its previous State") public Stream<NodeOutput> rollback(
			@Name("entity") Node entity) {

		long instantDate = Calendar.getInstance().getTimeInMillis();

		// Getting the CURRENT rel if it exists
		Spliterator<Relationship> currentRelIterator = entity.getRelationships(RelationshipType.withName(CURRENT_TYPE), Direction.OUTGOING).spliterator();
		Optional<Relationship> currentRelationshipOptional = StreamSupport.stream(currentRelIterator, false).filter(Objects::nonNull).findFirst();

		Optional<Node> newState = currentRelationshipOptional.map(currentRelationship -> {

			Node currentState = currentRelationship.getEndNode();

			return getFirstAvailableRollbackNode(currentState).map(rollbackState -> {
				Long currentDate = (Long) currentRelationship.getProperty("date");

				// Creating the rollback state, from the previous one
				Node result = Utility.cloneNode(db, rollbackState);

				//Creating ROLLBACK_TYPE relationship
				result.createRelationshipTo(rollbackState, RelationshipType.withName(ROLLBACK_TYPE));

				// Creating PREVIOUS relationship between the current and the new State
				result.createRelationshipTo(currentState, RelationshipType.withName(PREVIOUS_TYPE)).setProperty(DATE_PROP, currentDate);

				// Updating the HAS_STATE rel for the current node, adding endDate
				currentState.getRelationships(RelationshipType.withName(HAS_STATE_TYPE), Direction.INCOMING)
						.forEach(hasStatusRel -> hasStatusRel.setProperty(END_DATE_PROP, instantDate));

				// Refactoring current relationship and adding the new ones
				currentRelationship.delete();
				entity.createRelationshipTo(result, RelationshipType.withName(CURRENT_TYPE)).setProperty(DATE_PROP, instantDate);
				entity.createRelationshipTo(result, RelationshipType.withName(HAS_STATE_TYPE)).setProperty(START_DATE_PROP, instantDate);

				log.info(LOGGER_TAG + "Rollback executed for Entity with id {}, adding a State with id {}", entity.getId(), result.getId());
				return Optional.of(result);
			}).orElseGet(() -> {
				log.info(LOGGER_TAG + "Failed rollback for Entity with id {}, only one CURRENT State available", entity.getId());
				return Optional.empty();
			});
		}).orElseGet(() -> {
			log.info(LOGGER_TAG + "Failed rollback for Entity with id {}, there is no CURRENT State available", entity.getId());
			return Optional.empty();
		});

		return Stream.of(new NodeOutput(newState));
	}

	/**
	 * This method returns the first available State node, by a given State node to rollback
	 *
	 * @param state state to rollback
	 * @return the first available rollback node
	 */
	private Optional<Node> getFirstAvailableRollbackNode(Node state) {
		return StreamSupport.stream(state.getRelationships(RelationshipType.withName(ROLLBACK_TYPE), Direction.OUTGOING).spliterator(), false).findFirst()
				// Recursive iteration for ROLLBACKed State node
				.map(e -> getFirstAvailableRollbackNode(e.getEndNode()))
				// No ROLLBACK relationship found
				.orElse(StreamSupport.stream(state.getRelationships(RelationshipType.withName(PREVIOUS_TYPE), Direction.OUTGOING).spliterator(), false)
						.findFirst().map(Relationship::getEndNode));
	}
}


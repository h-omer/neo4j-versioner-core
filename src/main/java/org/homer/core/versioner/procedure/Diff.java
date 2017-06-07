package org.homer.core.versioner.procedure;

import org.homer.core.versioner.Utility;
import org.homer.core.versioner.output.DiffOutput;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.*;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.DEFAULT;

/**
 * Diff class, it contains all the Procedures needed to diff States nodes in the database
 */
public class Diff {

    @Procedure(value = "graph.versioner.diff", mode = DEFAULT)
    @Description("graph.versioner.diff(stateFrom, stateTo) - Get a list of differences that must be applied to stateFrom in order to convert it into stateTo")
    public Stream<DiffOutput> diff(
            @Name("stateFrom") Node stateFrom,
            @Name("stateTo") Node stateTo) {
        return diffBetweenStates(Optional.of(stateFrom), Optional.of(stateTo));
    }

    @Procedure(value = "graph.versioner.diff.from.previous", mode = DEFAULT)
    @Description("graph.versioner.diff.from.previous(state) - Get a list of differences that must be applied to the previous statusof the given one in order to become the given state")
    public Stream<DiffOutput> diffFromPrevious(
            @Name("state") Node state) {

		Optional<Node> stateFrom = Optional.ofNullable(state.getSingleRelationship(RelationshipType.withName(Utility.PREVIOUS_TYPE), Direction.OUTGOING)).map(Relationship::getEndNode);

		Stream<DiffOutput> result = Stream.empty();

		if(stateFrom.isPresent()){
			result = diffBetweenStates(stateFrom, Optional.of(state));
		}

		return result;
    }

	@Procedure(value = "graph.versioner.diff.from.current", mode = DEFAULT)
	@Description("graph.versioner.diff.from.current(state) - Get a list of differences that must be applied to the given state in order to become the current entity state")
	public Stream<DiffOutput> diffFromCurrent(
			@Name("state") Node state) {

		Optional<Node> currentState = Optional.ofNullable(state.getSingleRelationship(RelationshipType.withName(Utility.HAS_STATE_TYPE), Direction.INCOMING))
				.map(Relationship::getStartNode).map(entity -> entity.getSingleRelationship(RelationshipType.withName(Utility.CURRENT_TYPE), Direction.OUTGOING))
				.map(Relationship::getEndNode);

		Stream<DiffOutput> result = Stream.empty();

		if(currentState.isPresent() && !currentState.equals(Optional.of(state))){
			result = diffBetweenStates(Optional.of(state), currentState);
		}

		return result;
	}

	/**
	 * It returns a {@link Stream<DiffOutput>} by the given nodes
	 *
	 * @param from
	 * @param to
	 * @return a {@link Stream<DiffOutput>}
	 */
	private Stream<DiffOutput> diffBetweenStates(Optional<Node> from, Optional<Node> to) {
		List<DiffOutput> diffs = new ArrayList<>();

		Map<String, Object> propertiesFrom = from.map(Node::getAllProperties).orElse(Collections.emptyMap());
		Map<String, Object> propertiesTo = to.map(Node::getAllProperties).orElse(Collections.emptyMap());

		//Getting updated and removed properties
		propertiesFrom.forEach((key, value) -> {
			Optional<Object> foundValue = Optional.ofNullable(propertiesTo.get(key));
			String operation = foundValue.map(val -> compareObj(val, value) ? "" : Utility.DIFF_OPERATION_UPDATE).orElse(Utility.DIFF_OPERATION_REMOVE);
			if(!operation.isEmpty()){
				diffs.add(new DiffOutput(operation, key, value, foundValue.orElse(null)));
			}
		});

		//Getting added properties
		propertiesTo.forEach((key, value) -> {
			if(!propertiesFrom.containsKey(key)) {
				diffs.add(new DiffOutput(Utility.DIFF_OPERATION_ADD, key, null, value));
			}
		});

		return diffs.stream().sorted((a, b) -> Integer.compare(Utility.DIFF_OPERATIONS_SORTING.indexOf(a.operation), Utility.DIFF_OPERATIONS_SORTING.indexOf(b.operation)));
	}

	/**
	 * It compares 2 objects and return true if equals, false instead
	 *
	 * @param val
	 * @param value
	 * @return true if equals, false instead
	 */
	private boolean compareObj(Object val, Object value){
		if(val.getClass().isArray() && value.getClass().isArray()){
			return (val instanceof boolean[] && value instanceof boolean[] && Arrays.equals((boolean[])val, (boolean[])value))
					|| (val instanceof byte[] && value instanceof byte[] && Arrays.equals((byte[])val, (byte[])value))
					|| (val instanceof short[] && value instanceof short[] && Arrays.equals((short[])val, (short[])value))
					|| (val instanceof int[] && value instanceof int[] && Arrays.equals((int[])val, (int[])value))
					|| (val instanceof long[] && value instanceof long[] && Arrays.equals((long[])val, (long[])value))
					|| (val instanceof float[] && value instanceof float[] && Arrays.equals((float[])val, (float[])value))
					|| (val instanceof double[] && value instanceof double[] && Arrays.equals((double[])val, (double[])value))
					|| (val instanceof char[] && value instanceof char[] && Arrays.equals((char[])val, (char[])value))
					|| (val instanceof String[] && value instanceof String[] && Arrays.equals((String[])val, (String[])value));
		} else {
			return val.equals(value);
		}
	}
}


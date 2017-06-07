package org.homer.core.versioner.procedure;

import org.homer.core.versioner.Utility;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;

import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * RollbackTest class, it contains all the method used to test Rollback class methods
 */
public class DiffTest {
	@Rule public Neo4jRule neo4j = new Neo4jRule()

			// This is the function we want to test
			.withProcedure(Diff.class);

    /*--------------------------*/
	/*           diff           */
    /*--------------------------*/

	@Test public void shouldDiffGetTheDiffBetweenTwoStatesCorrectly() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			session.run("CREATE (s:State:From {keep:'keep', update:'old', delete:'delete'})");
			session.run("CREATE (s:State:To {keep:'keep', update:'new', new:'new'})");

			// When
			StatementResult result = session
					.run("MATCH (stateTo:State:To), (stateFrom:State:From) WITH stateFrom, stateTo CALL graph.versioner.diff(stateFrom, stateTo) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_REMOVE, "delete", "delete", "null"));
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_UPDATE, "update", "old", "new"));
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_ADD, "new", "null", "new"));
			assertThat(result.hasNext(), is(false));
		}
	}

	@Test public void shouldDiffGetTheDiffBetweenTwoStatesCorrectlyWithDifferentObjectTypes() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			session.run("CREATE (s:State:From {a:1, b:1, c:'stringA', d:'string', e: [1, 2], f: [1, 2]})");
			session.run("CREATE (s:State:To {a:2, b:1, c:'stringB', d:'string', e:[1, 2], f: [1, 3]})");

			// When
			StatementResult result = session
					.run("MATCH (stateTo:State:To), (stateFrom:State:From) WITH stateFrom, stateTo CALL graph.versioner.diff(stateFrom, stateTo) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.next().values().stream().map(value -> value.asObject().toString()).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_UPDATE, "a", "1", "2"));
			assertThat(result.next().values().stream().map(value -> value.asObject().toString()).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_UPDATE, "c", "stringA", "stringB"));
			assertThat(result.next().values().stream().map(value -> value.asObject().toString()).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_UPDATE, "f", "[1, 2]", "[1, 3]"));
			assertThat(result.hasNext(), is(false));
		}
	}

	@Test public void shouldDiffGetNoDiffIfStatesAreEqual() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			session.run("CREATE (s:State:From {keep:'keep', update:'update', delete:'delete'})");
			session.run("CREATE (s:State:To {keep:'keep', update:'update', delete:'delete'})");

			// When
			StatementResult result = session
					.run("MATCH (stateTo:State:To), (stateFrom:State:From) WITH stateFrom, stateTo CALL graph.versioner.diff(stateFrom, stateTo) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.hasNext(), is(false));
		}
	}

	@Test public void shouldDiffGetNoDiffIfOneOfTheTwoStatesIsNull() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			// An empty database

			// When
			StatementResult result = session
					.run("MATCH (stateTo:Entity), (stateFrom:State:From) WITH stateFrom, stateTo CALL graph.versioner.diff(stateFrom, stateTo) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.hasNext(), is(false));
		}
	}

	/*----------------------------------------*/
	/*           diff.from.previous           */
    /*----------------------------------------*/

	@Test public void shouldDiffFromPreviousGetTheDiffFromPreviousCorrectly() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			session.run("CREATE (s:State:From {keep:'keep', update:'old', delete:'delete'})");
			session.run("CREATE (s:State:To {keep:'keep', update:'new', new:'new'})");
			session.run("MATCH (from:From), (to:To) CREATE (from)<-[:PREVIOUS]-(to)");

			// When
			StatementResult result = session
					.run("MATCH (stateTo:State:To) WITH stateTo CALL graph.versioner.diff.from.previous(stateTo) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_REMOVE, "delete", "delete", "null"));
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_UPDATE, "update", "old", "new"));
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_ADD, "new", "null", "new"));
			assertThat(result.hasNext(), is(false));
		}
	}

	@Test public void shouldDiffFromPreviousGetNoDiffIfNoPreviousState() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			session.run("CREATE (s:State:To {keep:'keep', update:'update', delete:'delete'})");

			// When
			StatementResult result = session
					.run("MATCH (stateTo:To) WITH stateTo CALL graph.versioner.diff.from.previous(stateTo) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.hasNext(), is(false));
		}
	}

	@Test public void shouldDiffFromPreviousGetNoDiffIfOneOfTheTwoStatesAreNull() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			// An empty database

			// When
			StatementResult result = session
					.run("MATCH (stateFrom:State:From) WITH stateFrom CALL graph.versioner.diff.from.previous(stateFrom) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.hasNext(), is(false));
		}
	}

	/*---------------------------------------*/
	/*           diff.from.current           */
    /*---------------------------------------*/

	@Test public void shouldDiffFromCurrentGetTheDiffFromCurrentCorrectly() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			session.run("CREATE (:Entity)");
			session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE]->(s:State:From {keep:'keep', update:'old', delete:'delete'})");
			session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE]->(s:State:To {keep:'keep', update:'new', new:'new'})");
			session.run("MATCH (to:To)-[:HAS_STATE]-(e:Entity) CREATE (e)-[:CURRENT]->(to)");

			// When
			StatementResult result = session
					.run("MATCH (stateFrom:State:From) WITH stateFrom CALL graph.versioner.diff.from.current(stateFrom) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_REMOVE, "delete", "delete", "null"));
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_UPDATE, "update", "old", "new"));
			assertThat(result.next().values().stream().map(Value::asString).collect(Collectors.toList()), contains(Utility.DIFF_OPERATION_ADD, "new", "null", "new"));
			assertThat(result.hasNext(), is(false));
		}
	}

	@Test public void shouldDiffFromCurrentGetNoDiffIfGivenCurrentState() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			session.run("CREATE (:Entity)");
			session.run("MATCH (e:Entity) CREATE (e)-[:HAS_STATE]->(s:State:To {keep:'keep', update:'new', new:'new'})");
			session.run("MATCH (to:To)-[:HAS_STATE]-(e:Entity) CREATE (e)-[:CURRENT]->(to)");

			// When
			StatementResult result = session
					.run("MATCH (stateTo:To) WITH stateTo CALL graph.versioner.diff.from.current(stateTo) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.hasNext(), is(false));
		}
	}

	@Test public void shouldDiffFromCurrentGetNoDiffIfInputStateDoesNotExist() {
		// This is in a try-block, to make sure we close the driver after the test
		try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryption().toConfig()); Session session = driver.session()) {
			// Given
			// A db with no states

			// When
			StatementResult result = session
					.run("MATCH (stateTo:Entity) WITH stateTo CALL graph.versioner.diff.from.current(stateTo) YIELD operation, label, oldValue, newValue RETURN operation, label, oldValue, newValue");

			// Then
			assertThat(result.hasNext(), is(false));
		}
	}

}

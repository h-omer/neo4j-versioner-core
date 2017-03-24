package it.homer.neo4j.graphversioner.dataloader;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.util.Objects;
import java.util.stream.IntStream;

import static org.neo4j.driver.v1.Values.parameters;

public class DataLoader {

	private Session session;

	public DataLoader(Session session){
		this.session = session;
	}

	public void load(Integer entitiesNumber, Integer statesNumber) {
		IntStream.rangeClosed(1, entitiesNumber).forEach(i -> this.loadEntity(statesNumber));
	}

	public void loadEntity(Integer statesNumber) {
		StatementResult result = session.run( "CREATE (a:Entity {name: {name}}) RETURN id(a)",
				parameters( "name", Utils.generateRandomString(10) ) );

		Integer entityId = result.single().get(0).asInt();

		Pair<Long, Long> prevState = null;

		for(int i = 0; i < statesNumber; i++) {
			prevState = this.loadState(entityId, prevState);
		}

		if(!Objects.isNull(prevState)) {
			session.run(
					"MATCH (a) WHERE id(a) = {entityId} WITH a MATCH (b) WHERE id(b) = {stateId} WITH a, b CREATE (a)-[:CURRENT {date: {date}}]->(b)",
					parameters("entityId", entityId, "stateId", prevState.getFirst(), "date", prevState.getSecond()));
		}
	}

	public Pair<Long, Long> loadState(Integer entityId, Pair<Long, Long> prevState) {
		StatementResult result;
		if(!Objects.isNull(prevState)) {
			result = session.run(
					"CREATE (s:State {code: {code}}) " +
					"WITH s MATCH (e) WHERE id(e) = {entityId} " +
					"WITH s, e CREATE (s)<-[:HAS_STATE {startDate: {endDate}}]-(e) " +
					"WITH s MATCH (ps) WHERE id(ps) = {prevId} " +
					"WITH s, ps CREATE (s)<-[:NEXT {date: {endDate}}]-(ps) " +
					"WITH s, ps MATCH (ps)<-[hs:HAS_STATE]-(:Entity) SET hs.endDate = {endDate} " +
					"RETURN id(s), hs.endDate",
					parameters("code", Utils.generateRandomString(2),
							"entityId", entityId,
							"endDate", Utils.generateRandomMilliseconds(prevState.getSecond()),
							"prevId", prevState.getFirst()
					));
		} else {
			result = session.run(
					"CREATE (s:State {code: {code}}) " +
					"WITH s MATCH (e) WHERE id(e) = {entityId} " +
					"WITH s, e CREATE (s)<-[hs:HAS_STATE {startDate: {startDate}}]-(e) " +
					"RETURN id(s), hs.startDate",
					parameters("code", Utils.generateRandomString(2),
							"entityId", entityId,
							"startDate", Utils.generateRandomMilliseconds(0L)
					));
		}
		Record record = result.single();
		return new Pair<>(record.get(0).asLong(), record.get(1).asLong());
	}
}

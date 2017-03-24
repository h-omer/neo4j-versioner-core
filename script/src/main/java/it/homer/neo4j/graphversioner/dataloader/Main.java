package it.homer.neo4j.graphversioner.dataloader;

import org.neo4j.driver.v1.*;

public class Main {


	public static void main(String[] args) {
		Integer entitiesNumber = args.length > 0 ? Integer.parseInt(args[0]) : Utils.DEFAULT_ENTITY_NUMBER;
		Integer statesNumber = args.length > 1 ? Integer.parseInt(args[1]) : Utils.DEFAULT_STATES_NUMBER;

		System.out.println(String.format("Starting to create %s entities with %s states each", entitiesNumber, statesNumber));

		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "password" ) );
		Session session = driver.session();

		DataLoader dataLoader = new DataLoader(session);
		dataLoader.load(entitiesNumber, statesNumber);

		session.close();
		driver.close();
	}
}

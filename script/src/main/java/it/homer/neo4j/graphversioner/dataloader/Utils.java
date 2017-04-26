package it.homer.neo4j.graphversioner.dataloader;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Utils {
 	public static Integer DEFAULT_ENTITY_NUMBER = 10;
	public static Integer DEFAULT_STATES_NUMBER = 100;

	private static SecureRandom random = new SecureRandom();

	public static String generateRandomString(int size) {
		return new BigInteger(size * 5, random).toString(32);
	}

	public static long generateRandomMilliseconds(long offset) {
		return offset + random.nextInt();
	}
}

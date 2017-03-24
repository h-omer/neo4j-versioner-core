package it.homer.neo4j.graphversioner.dataloader;

public class Pair<T, S> {
	private T first;
	private S second;

	public Pair(T first, S s) {
		this.first = first;
		this.second = s;
	}

	public T getFirst() {
		return first;
	}

	public void setFirst(T first) {
		this.first = first;
	}

	public S getSecond() {
		return second;
	}

	public void setSecond(S second) {
		this.second = second;
	}
}

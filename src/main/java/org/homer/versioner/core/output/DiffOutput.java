package org.homer.versioner.core.output;

public class DiffOutput{
	public String operation;

	public String label;

	public Object oldValue;

	public Object newValue;

	public DiffOutput(String operation, String label, Object oldValue, Object newValue) {
		this.operation = operation;
		this.label = label;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
}

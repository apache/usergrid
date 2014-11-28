package org.apache.usergrid.android.sdk;

/**
 * Represents an incrementing counter, including the counter's
 * name and increment value.
 * 
 * You can use a counter when creating a number of similar 
 * Event entity instances. An Event entity's "counters" property
 * is a Map in which the key is the counter name and value is the 
 * increment value.
 */
public class CounterIncrement {

	private String counterName;
	private long counterIncrementValue;
	
	
	/**
	 * Constructs an instance, specifying the increment value
	 * as 1.
	 */
	public CounterIncrement() {
		this.counterIncrementValue = 1;
	}

	/**
	 * Constructs an instance with the specified counter name
	 * and increment value.
	 * 
	 * @param counterName The counter name.
	 * @param counterIncrementValue The counter's increment value.
	 */
	public CounterIncrement(String counterName, long counterIncrementValue) {
		this.counterName = counterName;
		this.counterIncrementValue = counterIncrementValue;
	}
	
	public String getCounterName() {
		return this.counterName;
	}
	
	public void setCounterName(String counterName) {
		this.counterName = counterName;
	}
	
	public long getCounterIncrementValue() {
		return this.counterIncrementValue;
	}
	
	public void setCounterIncrementValue(long counterIncrementValue) {
		this.counterIncrementValue = counterIncrementValue;
	}
}

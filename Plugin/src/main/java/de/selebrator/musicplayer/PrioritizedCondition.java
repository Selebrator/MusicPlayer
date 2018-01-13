package de.selebrator.musicplayer;

public class PrioritizedCondition extends Condition {
	public static final long DEFAULT_PRIORITY = 0L;
	private long priority = DEFAULT_PRIORITY;

	public long getPriority() {
		return this.priority;
	}
}

package de.selebrator.musicplayer;

import org.mvel2.MVEL;

import java.util.*;

public class Condition {
	public static final String DEFAULT_CONDITION = "true";

	private String name;
	private String condition = DEFAULT_CONDITION;

	public static <T extends Condition> boolean containsPlayable(List<T> conditions, Map<String, Object> context) {
		return conditions.stream()
				.anyMatch(condition -> condition.canPlay(context));
	}

	public boolean canPlay(Map<String, Object> context) {
		Object result = MVEL.eval(this.condition, context);
		return result instanceof Boolean && result.equals(true);
	}

	public String getCondition() {
		return this.condition;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

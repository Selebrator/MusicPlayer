package de.selebrator.musicplayer;

import java.util.*;

public class Listener extends PrioritizedCondition {
	private String type;
	private List<String> songs;

	public List<String> getSongs() {
		return this.songs;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return "listener/" + this.getName();
	}
}

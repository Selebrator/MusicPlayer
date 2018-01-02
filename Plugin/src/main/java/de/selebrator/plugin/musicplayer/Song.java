package de.selebrator.plugin.musicplayer;

import org.bukkit.SoundCategory;

import java.time.*;

public class Song extends PrioritizedCondition {
	public static final SoundCategory DEFAULT_SOUND_CATEGORY = SoundCategory.MUSIC;
	public static final long ERROR_TICK_DURATION = -1L;

	private SoundCategory category = DEFAULT_SOUND_CATEGORY;
	private String sound;
	private String duration;
	private long ticks = ERROR_TICK_DURATION;

	//in ticks
	public long getTickDuration() {
		if(this.ticks <= ERROR_TICK_DURATION) {
			Duration d = Duration.parse(this.duration);
			this.ticks = d.getSeconds() * 20 + (long) (d.getNano() * 1.0e-9d * 20.0d);
		}

		if(this.ticks <= ERROR_TICK_DURATION)
			throw new DateTimeException("A song must have a positive length");

		return this.ticks;
	}

	public String getSound() {
		return this.sound;
	}

	public SoundCategory getCategory() {
		return this.category;
	}

	@Override
	public String toString() {
		return "song/" + this.getName();
	}
}

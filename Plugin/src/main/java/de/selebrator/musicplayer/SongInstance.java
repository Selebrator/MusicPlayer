package de.selebrator.musicplayer;

import de.selebrator.musicplayer.event.SongEndEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.*;

public class SongInstance {

	private Song song;
	private Player player;
	private Location location;
	private BukkitTask timeoutTask;
	private BukkitTask outOfRangeTask;

	public SongInstance(Song song, Player player) {
		this.song = song;
		this.player = player;
	}

	public Song getSong() {
		return song;
	}

	private String getLocationString() {
		Location loc = this.location == null ? this.player.getLocation() : this.location;
		return "[" + loc.getBlock().getLocation().toVector() + "]";
	}

	public void start() {
		MusicPlayerPlugin.fine("START " + this + " for " + this.player.getName() + " at " + this.getLocationString());
		this.location = this.player.getLocation();

		this.timeoutTask = new BukkitRunnable() {
			@Override
			public void run() {
				stop(false);
			}
		}.runTaskLaterAsynchronously(MusicPlayerPlugin.instance, this.getSong().getTickDuration());

		this.outOfRangeTask = new BukkitRunnable() {
			@Override
			public void run() {
				if(location.distance(player.getLocation()) > 63)
					stop(false);
			}
		}.runTaskTimerAsynchronously(MusicPlayerPlugin.instance, 0, 20);

		this.player.playSound(this.location, this.getSong().getSound(), this.getSong().getCategory(), 1, 1);
	}

	public void stop(boolean silencing) {
		MusicPlayerPlugin.fine("STOP " + this);
		SongEndEvent event = new SongEndEvent(this.player, this);
		Bukkit.getServer().getPluginManager().callEvent(event);
		this.timeoutTask.cancel();
		this.outOfRangeTask.cancel();
		this.location = null;

		if(silencing)
			this.player.stopSound(this.getSong().getSound(), this.getSong().getCategory());
	}

	@Override
	public String toString() {
		return this.song + "@" + Integer.toHexString(this.hashCode());
	}
}

package de.selebrator.plugin.musicplayer.event;

import de.selebrator.plugin.musicplayer.SongInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class SongEndEvent extends PlayerEvent {
	private static final HandlerList handlers = new HandlerList();
	private SongInstance songInstance;

	public SongEndEvent(Player player, SongInstance songInstance) {
		super(player);
		this.songInstance = songInstance;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public SongInstance getSongInstance() {
		return songInstance;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}

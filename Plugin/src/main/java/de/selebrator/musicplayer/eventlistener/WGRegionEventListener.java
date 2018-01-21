package de.selebrator.musicplayer.eventlistener;

import com.mewin.WGRegionEvents.events.RegionEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.selebrator.musicplayer.*;
import org.bukkit.event.*;
import org.bukkit.event.Listener;

public class WGRegionEventListener implements Listener {

	private MusicPlayerPlugin plugin;

	public WGRegionEventListener(MusicPlayerPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onRegionEvent(RegionEvent event) {
		this.plugin.onEvent(event);
	}
}

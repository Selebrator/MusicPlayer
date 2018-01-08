package de.selebrator.plugin.musicplayer.eventlistener;

import com.mewin.WGRegionEvents.events.RegionEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.selebrator.plugin.musicplayer.*;
import org.bukkit.event.*;
import org.bukkit.event.Listener;

public class WGRegionEventListener implements Listener {

	private MusicPlayerPlugin plugin;

	public WGRegionEventListener(MusicPlayerPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onRegionEvent(RegionEvent event) {
		ProtectedRegion region = event.getRegion();
		ContextBuilder cb = ContextBuilder.defaults(event.getPlayer())
				.append(event)
				.append("event_region", region)
				.append("event_region_name", region.getId());

		this.plugin.onEvent(event, cb.getContext());
	}
}

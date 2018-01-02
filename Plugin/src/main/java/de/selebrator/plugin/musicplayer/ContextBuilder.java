package de.selebrator.plugin.musicplayer;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.*;
import java.util.stream.Collectors;

public class ContextBuilder {
	private final Map<String, Object> context;

	public ContextBuilder() {
		this.context = new HashMap<>();
	}

	public static ContextBuilder defaults(Player player) {
		return new ContextBuilder()
				.append(player)
				.append(player.getWorld())
				.append(player.getLocation());
	}

	public Map<String, Object> getContext() {
		return this.context;
	}

	public ContextBuilder append(String key, Object value) {
		this.context.put(key, value);
		return this;
	}

	public ContextBuilder append(Player player) {
		return this.append(player, "player");
	}

	public ContextBuilder append(Player player, String prefix) {
		this.context.put(prefix, player);
		this.context.put(prefix + "_name", player.getName());
		this.context.put(prefix + "_health", player.getHealth());
		return this;
	}

	public ContextBuilder append(World world) {
		return this.append(world, "world");
	}

	public ContextBuilder append(World world, String prefix) {
		this.context.put(prefix, world);
		this.context.put(prefix + "_name", world.getName());
		this.context.put(prefix + "_time", world.getTime());
		this.context.put(prefix + "_dimension", world.getEnvironment());
		this.context.put(prefix + "_type", world.getWorldType());
		this.context.put(prefix + "_hasStorm", world.hasStorm());
		this.context.put(prefix + "_isThundering", world.isThundering());
		this.context.put(prefix + "_isNightTime", 13000 < world.getTime() && world.getTime() <= 23000);
		this.context.put(prefix + "_isDayTime", !(13000 < world.getTime() && world.getTime() <= 23000));
		return this;
	}

	public ContextBuilder append(Location location) {
		return this.append(location, "location");
	}

	public ContextBuilder append(Location location, String prefix) {
		Set<ProtectedRegion> regions = MusicPlayerPlugin.worldGuard.getRegionManager(location.getWorld()).getApplicableRegions(location).getRegions();
		this.context.put(prefix + "_regions_names", regions.stream().map(ProtectedRegion::getId).collect(Collectors.toList()));
		return this;
	}

	public ContextBuilder append(Event event) {
		this.context.put("event", event);
		return this;
	}
}

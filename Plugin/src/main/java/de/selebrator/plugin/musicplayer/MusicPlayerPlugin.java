package de.selebrator.plugin.musicplayer;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.selebrator.plugin.musicplayer.event.*;
import de.selebrator.plugin.musicplayer.eventlistener.WGRegionEventListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MusicPlayerPlugin extends JavaPlugin implements org.bukkit.event.Listener {

	public static WorldGuardPlugin worldGuard;
	public static MusicPlayerPlugin instance;

	public Config config;
	public Map<Player, List<SongInstance>> currentSongs = new HashMap<>();

	public static void info(String msg) {
		log(Level.INFO, msg);
	}

	public static void fine(String msg) {
		log(Level.FINE, msg);
	}

	public static void finer(String msg) {
		log(Level.FINER, msg);
	}

	public static void finest(String msg) {
		log(Level.FINEST, msg);
	}

	public static void log(Level level, String msg) {
		if(level.intValue() >= MusicPlayerPlugin.instance.config.getLogLevel())
			MusicPlayerPlugin.instance.getLogger().info(msg);
	}

	public Optional<Listener> getNextListener(List<Listener> conditions, Map<String, Object> context) {
		if(conditions == null || conditions.isEmpty())
			return Optional.empty();

		return conditions.stream()
				.sorted(Comparator.comparing(PrioritizedCondition::getPriority).reversed())
				.filter(songCondition -> songCondition.canPlay(context))
				.findFirst();
	}

	public Optional<Song> getNextSong(List<Song> songs, Map<String, Object> context) {
		if(songs == null || songs.isEmpty())
			return Optional.empty();
		Map<Long, List<Song>> songsByPriority = songs.stream()
				.collect(Collectors.groupingBy(PrioritizedCondition::getPriority));

		long maxPlayablePriority = songsByPriority.keySet().stream()
				.filter(priority -> Condition.containsPlayable(songsByPriority.get(priority), context))
				.reduce(Math::max).orElse(-1L);
		finest("Highest priority with playable songs is " + maxPlayablePriority + ": " + songsByPriority.get(maxPlayablePriority) + ".");

		List<Song> maxPlayablePrioritySongs = songsByPriority.get(maxPlayablePriority).stream()
				.filter(song -> song.canPlay(context))
				.collect(Collectors.toList());

		finest("Of that playable are " + maxPlayablePrioritySongs.size() + ": " + maxPlayablePrioritySongs + ".");
		if(maxPlayablePrioritySongs.isEmpty())
			return Optional.empty();

		finest("Choosing random entry...");

		return Optional.ofNullable(maxPlayablePrioritySongs.get(ThreadLocalRandom.current().nextInt(0, maxPlayablePrioritySongs.size())));
	}

	@Override
	public void onEnable() {
		try {
			Path path = Paths.get(this.getDataFolder().getAbsolutePath() + "/config.json");
			this.config = Config.load(path);
		} catch(IOException e) {
			this.getLogger().severe("Missing config.json");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		Bukkit.getPluginManager().registerEvents(this, this);
		if(Bukkit.getPluginManager().isPluginEnabled("WGRegionEvents"))
			Bukkit.getPluginManager().registerEvents(new WGRegionEventListener(this), this);
		worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
		instance = this;
	}

	@Override
	public void onDisable() {

	}

	@Override
	public void onLoad() {

	}

	//events
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		ContextBuilder cb = ContextBuilder.defaults(event.getPlayer())
				.append(event)
				.append(event.getFrom(), "event_from");

		onEvent(event, cb.getContext());
	}

	@EventHandler
	public void onSilence(SilenceEvent event) {
		onEvent(event, ContextBuilder.defaults(event.getPlayer()).getContext());
	}

	@EventHandler
	public void onSongEnd(SongEndEvent event) {
		this.prepareCache(event.getPlayer());
		this.currentSongs.get(event.getPlayer()).remove(event.getSongInstance());
		if(this.currentSongs.get(event.getPlayer()).isEmpty())
			Bukkit.getPluginManager().callEvent(new SilenceEvent(event.getPlayer()));
	}

	public void onEvent(PlayerEvent bukkitEvent, Map<String, Object> context) {
		finer("Triggered " + bukkitEvent.getEventName() + ".");
		List<Listener> listeners = this.config.eventRegistry.getOrDefault(bukkitEvent.getEventName(), Collections.emptyList());
		finer("Found " + listeners.size() + " listeners: " + listeners.toString() + ".");

		Optional<Listener> chosenEvent = getNextListener(listeners, context);
		finer("Chose " + chosenEvent + ".");
		if(!chosenEvent.isPresent())
			return;

		finer("Searching for " + chosenEvent.get().getSongs().size() + " songs: " + chosenEvent.get().getSongs() + ".");
		List<Song> songs = chosenEvent.get().getSongs().stream()
				.map(songName -> this.config.getSongByName(songName))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
		finer("Found " + songs.size() + " songs: " + songs + ".");

		Optional<Song> chosenSong = getNextSong(songs, context);
		finer("Chose " + chosenSong + ".");
		if(!chosenSong.isPresent())
			return;

		this.startSong(bukkitEvent.getPlayer(), chosenSong.get());
	}

	private void prepareCache(Player player) {
		if(this.currentSongs == null)
			this.currentSongs = new HashMap<>();
		this.currentSongs.computeIfAbsent(player, p -> new ArrayList<>());
	}

	private void startSong(Player player, Song song) {
		SongInstance toStart = new SongInstance(song, player);
		this.prepareCache(player);

		this.currentSongs.get(player).add(toStart);

		for(SongInstance value : this.currentSongs.get(player)) {
			if(value == toStart)
				continue;
			value.stop(true);
		}

		toStart.start();
	}
}

package de.selebrator.musicplayer;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.selebrator.musicplayer.command.ReloadConfigCommand;
import de.selebrator.musicplayer.event.*;
import de.selebrator.musicplayer.eventlistener.WGRegionEventListener;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvel2.MVEL;

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

	public Map<String, Object> createContext(PlayerEvent event) {
		Player player = event.getPlayer();
		Location location = player.getLocation();
		Set<ProtectedRegion> regions = MusicPlayerPlugin.worldGuard.getRegionManager(location.getWorld()).getApplicableRegions(location).getRegions();
		List<String> regions_names = regions.stream().map(ProtectedRegion::getId).collect(Collectors.toList());

		//the variables used to create the context
		Map<String, Object> contextVars = new HashMap<>(2);
		contextVars.put("event", event);
		contextVars.put("regions_names", regions_names);

		Map<String, Object> context = new HashMap<>();
		for(Class clazz = event.getClass(); !clazz.equals(Event.class); clazz = clazz.getSuperclass()) {
			Map<String, Object> subContext = new HashMap<>();
			if(!this.config.context.containsKey(clazz.getSimpleName()))
				continue;

			this.config.context.get(clazz.getSimpleName()).forEach((var, condition) -> subContext.put(var, MVEL.eval(condition, contextVars)));
			context.putAll(subContext);
		}

		return context;
	}

	@Override
	public void onEnable() {
		if(!loadConfiguration()) return;

		this.getCommand("musicplayerreload").setExecutor(new ReloadConfigCommand(this));

		Bukkit.getPluginManager().registerEvents(this, this);
		if(Bukkit.getPluginManager().isPluginEnabled("WGRegionEvents"))
			Bukkit.getPluginManager().registerEvents(new WGRegionEventListener(this), this);
		worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
		instance = this;
	}

	public boolean loadConfiguration() {
		Path path = Paths.get(this.getDataFolder().getAbsolutePath() + "/config.json");
		if(Files.notExists(path)) {
			this.getLogger().info("Missing config.json. Creating default.");
			try {
				//noinspection ResultOfMethodCallIgnored
				this.getDataFolder().mkdir();
				Files.copy(MusicPlayerPlugin.class.getResourceAsStream("/config.json"), path);
			} catch(IOException e) {
				this.getLogger().info("Could not copy default config.");
			}
		}
		try {
			this.config = Config.load(path);
			return true;
		} catch(IOException e) {
			this.getLogger().severe("Could not load config.");
			Bukkit.getPluginManager().disablePlugin(this);
			return false;
		}
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
		onEvent(event);
	}

	@EventHandler
	public void onSilence(SilenceEvent event) {
		onEvent(event);
	}

	@EventHandler
	public void onSongEnd(SongEndEvent event) {
		if(!event.getPlayer().isOnline()) {
			this.currentSongs.remove(event.getPlayer());
			return;
		}
		this.prepareCache(event.getPlayer());
		this.currentSongs.get(event.getPlayer()).remove(event.getSongInstance());
		if(this.currentSongs.get(event.getPlayer()).isEmpty())
			Bukkit.getPluginManager().callEvent(new SilenceEvent(event.getPlayer()));
	}

	public void onEvent(PlayerEvent bukkitEvent) {
		Map<String, Object> context = createContext(bukkitEvent);
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

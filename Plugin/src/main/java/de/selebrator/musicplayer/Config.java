package de.selebrator.musicplayer;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Config {

	public static final Gson GSON = new Gson();

	public static final Level DEFAULT_LOG_LEVEL = Level.INFO;

	public Map<String, Map<String, String>> context = new HashMap<>();
	private Map<String, Song> songs = new HashMap<>();
	private Map<String, Listener> listeners = new HashMap<>();
	public Map<String, List<Listener>> eventRegistry;
	private int log_level = DEFAULT_LOG_LEVEL.intValue();

	private Config() {}

	public static Config load(Path path) throws IOException {
		Config out = GSON.fromJson(Files.newBufferedReader(path), Config.class);

		out.songs.forEach((name, condition) -> condition.setName(name));
		out.listeners.forEach((name, condition) -> condition.setName(name));

		out.eventRegistry = out.listeners.values().stream()
				.collect(Collectors.groupingBy(Listener::getType));

		return out;
	}

	public Optional<Song> getSongByName(String songName) {
		return Optional.ofNullable(this.songs.get(songName));
	}

	public int getLogLevel() {
		return this.log_level;
	}
}

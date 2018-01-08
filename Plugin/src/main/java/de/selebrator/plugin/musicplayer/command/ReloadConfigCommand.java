package de.selebrator.plugin.musicplayer.command;

import de.selebrator.plugin.musicplayer.MusicPlayerPlugin;
import org.bukkit.command.*;

public class ReloadConfigCommand implements CommandExecutor {

	private MusicPlayerPlugin plugin;

	public ReloadConfigCommand(MusicPlayerPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		boolean success = this.plugin.loadConfiguration();

		String prefix = "§r§8[§a" + this.plugin.getName() + "§8]§r";
		String path = "§r/config.json§r";

		String msg = prefix + (success ? " §aSuccessfully loaded " : " §cFailed to load ") + path + (success ? "§a" : "§c") + ".";

		if(!success)
			msg = msg + " Disabling " + this.plugin.getName() + ".";

		sender.sendMessage(msg);

		return true;
	}
}

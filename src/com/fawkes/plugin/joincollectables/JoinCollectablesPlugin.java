package com.fawkes.plugin.joincollectables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.fawkes.plugin.collectables.CollectablesPlugin;

public class JoinCollectablesPlugin extends JavaPlugin implements Listener {

	private CollectablesPlugin plugin;

	String monthaward;
	String dayaward;

	int monthlevel;

	Random rand = new Random();

	boolean specialDay = false;

	FileConfiguration calendar;

	@Override
	public void onEnable() {

		saveDefaultConfig();

		/* Load calendar yaml from url */
		try {

			URL website = new URL(this.getConfig().getString("calendar"));

			ReadableByteChannel rbc = Channels.newChannel(website.openStream());

			File file = new File("calendar.yml");

			@SuppressWarnings("resource")
			FileOutputStream fos = new FileOutputStream(file);

			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

			calendar = YamlConfiguration.loadConfiguration(file);

		} catch (IOException e) {
			Bukkit.getLogger().severe("Failed to fetch calendar, disabling.");
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
			return;

		}

		// gets the month and day and gets their respective awards
		String monthpath;
		String daypath;

		Calendar c = Calendar.getInstance();

		monthpath = c.get(Calendar.YEAR) + "." + (c.get(Calendar.MONTH) + 1);

		if (!calendar.contains(monthpath + ".month")) {
			// this shouldn't really ever happen
			Bukkit.getLogger().severe("This month does not have an entry, disabling.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;

		}

		daypath = monthpath + "." + c.get(Calendar.DAY_OF_MONTH);

		monthaward = calendar.getString(monthpath + ".month");

		monthlevel = c.getActualMaximum(Calendar.DAY_OF_MONTH) - c.get(Calendar.DAY_OF_MONTH) + 1;

		// if it's the first day of the month, give them something special
		if (c.get(Calendar.DAY_OF_MONTH) == 1) {
			monthlevel = 99;

		}

		if (calendar.contains(daypath)) {
			specialDay = true;
			dayaward = calendar.getString(daypath);

		}

		// registers this plugin's listeners
		Bukkit.getPluginManager().registerEvents(this, this);

		Bukkit.getLogger().info("Enabling with month award: " + monthaward + "and day award: " + dayaward);

		// loads the plugin for API usage
		plugin = this.getServer().getServicesManager().load(CollectablesPlugin.class);

	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {

		// if the player hasn't played before, give them the new player award!
		if (!e.getPlayer().hasPlayedBefore()) {
			try {
				plugin.giveAward(e.getPlayer().getUniqueId(), "newplayer", 1);

			} catch (SQLException er) {
				Bukkit.getLogger()
						.severe("Could not give 'New Player' award to player \"" + e.getPlayer().getName() + "\"");
				er.printStackTrace();

			}
		}

		// check if the player has this month's award
		if (!plugin.hasAward(e.getPlayer().getUniqueId(), monthaward)) {
			try {
				plugin.giveAward(e.getPlayer().getUniqueId(), monthaward, getRandLevel());

			} catch (SQLException er) {
				Bukkit.getLogger()
						.severe("Could not give 'Monthly Award' award to player \"" + e.getPlayer().getName() + "\"");
				er.printStackTrace();

			}

		}

		if (specialDay) {
			if (!plugin.hasAward(e.getPlayer().getUniqueId(), dayaward)) {
				try {
					plugin.giveAward(e.getPlayer().getUniqueId(), dayaward, getRandLevel());

				} catch (SQLException er) {
					Bukkit.getLogger()
							.severe("Could not give 'Day Award' award to player \"" + e.getPlayer().getName() + "\"");
					er.printStackTrace();

				}

			}

		}

	}

	public int getRandLevel() {
		return 1 + rand.nextInt(99);

	}

}
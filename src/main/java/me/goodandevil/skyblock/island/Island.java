package me.goodandevil.skyblock.island;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;

import me.goodandevil.skyblock.SkyBlock;
import me.goodandevil.skyblock.api.event.island.IslandBiomeChangeEvent;
import me.goodandevil.skyblock.api.event.island.IslandLocationChangeEvent;
import me.goodandevil.skyblock.api.event.island.IslandMessageChangeEvent;
import me.goodandevil.skyblock.api.event.island.IslandOpenEvent;
import me.goodandevil.skyblock.api.event.island.IslandPasswordChangeEvent;
import me.goodandevil.skyblock.api.event.island.IslandRoleChangeEvent;
import me.goodandevil.skyblock.api.event.island.IslandWeatherChangeEvent;
import me.goodandevil.skyblock.api.utils.APIUtil;
import me.goodandevil.skyblock.ban.Ban;
import me.goodandevil.skyblock.ban.BanManager;
import me.goodandevil.skyblock.config.FileManager;
import me.goodandevil.skyblock.config.FileManager.Config;
import me.goodandevil.skyblock.playerdata.PlayerData;
import me.goodandevil.skyblock.upgrade.Upgrade;
import me.goodandevil.skyblock.utils.StringUtil;
import me.goodandevil.skyblock.visit.Visit;
import me.goodandevil.skyblock.visit.VisitManager;
import me.goodandevil.skyblock.world.WorldManager;

public class Island {

	private final SkyBlock skyblock;
	private final me.goodandevil.skyblock.api.island.Island apiWrapper;

	private Map<IslandRole, List<Setting>> islandSettings = new HashMap<>();
	private List<Location> islandLocations = new ArrayList<>();
	private Set<UUID> coopPlayers = new HashSet<>();

	private UUID uuid;
	private Level level;
	private int size;

	public Island(UUID uuid, org.bukkit.Location islandNormalLocation, org.bukkit.Location islandNetherLocation) {
		this.skyblock = SkyBlock.getInstance();

		IslandManager islandManager = skyblock.getIslandManager();
		WorldManager worldManager = skyblock.getWorldManager();
		FileManager fileManager = skyblock.getFileManager();

		this.uuid = uuid;
		this.size = fileManager.getConfig(new File(skyblock.getDataFolder(), "config.yml")).getFileConfiguration()
				.getInt("Island.Size.Minimum");

		if (this.size > 1000) {
			this.size = 50;
		}

		islandLocations.add(new Location(Location.World.Normal, Location.Environment.Island, islandNormalLocation));
		islandLocations.add(new Location(Location.World.Nether, Location.Environment.Island, islandNetherLocation));

		File configFile = new File(skyblock.getDataFolder().toString() + "/island-data");

		Config config = fileManager.getConfig(new File(configFile, uuid + ".yml"));
		Config defaultSettingsConfig = fileManager.getConfig(new File(skyblock.getDataFolder(), "settings.yml"));
		Config mainConfig = fileManager.getConfig(new File(skyblock.getDataFolder(), "config.yml"));

		FileConfiguration mainConfigLoad = mainConfig.getFileConfiguration();

		if (fileManager.isFileExist(new File(configFile, uuid + ".yml"))) {
			FileConfiguration configLoad = config.getFileConfiguration();

			if (configLoad.getString("Size") != null) {
				size = configLoad.getInt("Size");
			}

			if (configLoad.getString("Settings") != null) {
				configLoad.set("Settings", null);
			}

			for (Location.World worldList : Location.World.values()) {
				for (Location.Environment environmentList : Location.Environment.values()) {
					if (environmentList != Location.Environment.Island) {
						Location spawnLocation = new Location(worldList, environmentList, fileManager.getLocation(
								config, "Location." + worldList.name() + ".Spawn." + environmentList.name(), true));

						if (spawnLocation.getLocation().getWorld() == null) {
							spawnLocation.getLocation().setWorld(worldManager.getWorld(worldList));
						}

						islandLocations.add(spawnLocation);
					}
				}
			}

			Config settingsDataConfig = null;

			if (fileManager.isFileExist(new File(skyblock.getDataFolder().toString() + "/setting-data",
					getOwnerUUID().toString() + ".yml"))) {
				settingsDataConfig = fileManager.getConfig(new File(
						skyblock.getDataFolder().toString() + "/setting-data", getOwnerUUID().toString() + ".yml"));
			}

			for (IslandRole roleList : IslandRole.values()) {
				List<Setting> settings = new ArrayList<>();

				for (String settingList : defaultSettingsConfig.getFileConfiguration()
						.getConfigurationSection("Settings." + roleList.name()).getKeys(false)) {
					if (settingsDataConfig == null) {
						settings.add(new Setting(settingList, defaultSettingsConfig.getFileConfiguration()
								.getBoolean("Settings." + roleList.name() + "." + settingList)));
					} else {
						settings.add(new Setting(settingList, settingsDataConfig.getFileConfiguration()
								.getBoolean("Settings." + roleList.name() + "." + settingList)));
					}
				}

				islandSettings.put(roleList, settings);
			}
		} else {
			islandLocations.add(new Location(Location.World.Normal, Location.Environment.Main,
					islandNormalLocation.clone().add(0.5D, 0.0D, 0.5D)));
			islandLocations.add(new Location(Location.World.Nether, Location.Environment.Main,
					islandNetherLocation.clone().add(0.5D, 0.0D, 0.5D)));
			islandLocations.add(new Location(Location.World.Normal, Location.Environment.Visitor,
					islandNormalLocation.clone().add(0.5D, 0.0D, 0.5D)));
			islandLocations.add(new Location(Location.World.Nether, Location.Environment.Visitor,
					islandNetherLocation.clone().add(0.5D, 0.0D, 0.5D)));

			fileManager.setLocation(config, "Location.Normal.Island", islandNormalLocation, true);
			fileManager.setLocation(config, "Location.Nether.Island", islandNetherLocation, true);
			fileManager.setLocation(config, "Location.Normal.Spawn.Main",
					islandNormalLocation.clone().add(0.5D, 0.0D, 0.5D), true);
			fileManager.setLocation(config, "Location.Nether.Spawn.Main",
					islandNetherLocation.clone().add(0.5D, 0.0D, 0.5D), true);
			fileManager.setLocation(config, "Location.Normal.Spawn.Visitor",
					islandNormalLocation.clone().add(0.5D, 0.0D, 0.5D), true);
			fileManager.setLocation(config, "Location.Nether.Spawn.Visitor",
					islandNetherLocation.clone().add(0.5D, 0.0D, 0.5D), true);

			configFile = config.getFile();
			FileConfiguration configLoad = config.getFileConfiguration();

			configLoad.set("Visitor.Open", mainConfigLoad.getBoolean("Island.Visitor.Open"));
			configLoad.set("Biome.Type", mainConfigLoad.getString("Island.Biome.Default.Type").toUpperCase());
			configLoad.set("Weather.Synchronised", mainConfigLoad.getBoolean("Island.Weather.Default.Synchronised"));
			configLoad.set("Weather.Time", mainConfigLoad.getInt("Island.Weather.Default.Time"));
			configLoad.set("Weather.Weather", mainConfigLoad.getString("Island.Weather.Default.Weather").toUpperCase());
			configLoad.set("Ownership.Original", uuid.toString());

			for (IslandRole roleList : IslandRole.values()) {
				List<Setting> settings = new ArrayList<>();

				for (String settingList : defaultSettingsConfig.getFileConfiguration()
						.getConfigurationSection("Settings." + roleList.name()).getKeys(false)) {
					settings.add(new Setting(settingList, defaultSettingsConfig.getFileConfiguration()
							.getBoolean("Settings." + roleList.name() + "." + settingList)));
				}

				islandSettings.put(roleList, settings);
			}

			save();

			PlayerData playerData = skyblock.getPlayerDataManager().getPlayerData(Bukkit.getServer().getPlayer(uuid));
			playerData.setPlaytime(0);
			playerData.setOwner(uuid);
			playerData.setMemberSince(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
			playerData.save();

			if (mainConfigLoad.getBoolean("Island.Spawn.Protection")) {
				Bukkit.getServer().getScheduler().runTask(skyblock, new Runnable() {
					@Override
					public void run() {
						islandNormalLocation.clone().subtract(0.0D, 1.0D, 0.0D).getBlock().setType(Material.STONE);
						islandManager.setSpawnProtection(islandNormalLocation);
						islandManager.setSpawnProtection(islandNetherLocation);
					}
				});
			}
		}

		if (!mainConfigLoad.getBoolean("Island.Coop.Unload")) {
			File coopDataFile = new File(skyblock.getDataFolder().toString() + "/coop-data",
					getOwnerUUID().toString() + ".yml");

			if (fileManager.isFileExist(coopDataFile)) {
				Config coopDataConfig = fileManager.getConfig(coopDataFile);
				FileConfiguration coopDataConfigLoad = coopDataConfig.getFileConfiguration();

				if (coopDataConfigLoad.getString("CoopPlayers") != null) {
					for (String coopPlayerList : coopDataConfigLoad.getStringList("CoopPlayers")) {
						coopPlayers.add(UUID.fromString(coopPlayerList));
					}
				}
			}
		}

		level = new Level(getOwnerUUID(), skyblock);

		VisitManager visitManager = skyblock.getVisitManager();

		if (!visitManager.hasIsland(getOwnerUUID())) {
			visitManager.createIsland(getOwnerUUID(),
					new org.bukkit.Location[] { getLocation(Location.World.Normal, Location.Environment.Island),
							getLocation(Location.World.Nether, Location.Environment.Island) },
					size, getRole(IslandRole.Member).size() + getRole(IslandRole.Operator).size() + 1,
					visitManager.getIslandSafeLevel(uuid), level, getMessage(IslandMessage.Signature), isOpen());
		}

		BanManager banManager = skyblock.getBanManager();

		if (!banManager.hasIsland(getOwnerUUID())) {
			banManager.createIsland(getOwnerUUID());
		}

		this.apiWrapper = new me.goodandevil.skyblock.api.island.Island(this);
	}

	public UUID getOwnerUUID() {
		return uuid;
	}

	public void setOwnerUUID(UUID uuid) {
		getVisit().setOwnerUUID(uuid);
		this.uuid = uuid;
	}

	public UUID getOriginalOwnerUUID() {
		return UUID
				.fromString(
						skyblock.getFileManager()
								.getConfig(new File(new File(skyblock.getDataFolder().toString() + "/island-data"),
										uuid.toString() + ".yml"))
								.getFileConfiguration().getString("Ownership.Original"));
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		if (size > 1000 || size < 0) {
			size = 50;
		}

		this.size = size;
		skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().set("Size", size);
	}

	public double getRadius() {
		return (size / 2) + 0.5;
	}

	public boolean hasPassword() {
		return skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getString("Ownership.Password") != null;
	}

	public String getPassword() {
		return skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getString("Ownership.Password");
	}

	public void setPassword(String password) {
		IslandPasswordChangeEvent islandPasswordChangeEvent = new IslandPasswordChangeEvent(getAPIWrapper(), password);
		Bukkit.getServer().getPluginManager().callEvent(islandPasswordChangeEvent);

		skyblock.getFileManager()
				.getConfig(new File(new File(skyblock.getDataFolder().toString() + "/island-data"),
						uuid.toString() + ".yml"))
				.getFileConfiguration().set("Ownership.Password", islandPasswordChangeEvent.getPassword());
	}

	public org.bukkit.Location getLocation(Location.World world, Location.Environment environment) {
		for (Location islandLocationList : islandLocations) {
			if (islandLocationList.getWorld() == world && islandLocationList.getEnvironment() == environment) {
				return islandLocationList.getLocation();
			}
		}

		return null;
	}

	public void setLocation(Location.World world, Location.Environment environment, org.bukkit.Location location) {
		for (Location islandLocationList : islandLocations) {
			if (islandLocationList.getWorld() == world && islandLocationList.getEnvironment() == environment) {
				Bukkit.getServer().getPluginManager()
						.callEvent(new IslandLocationChangeEvent(getAPIWrapper(),
								new me.goodandevil.skyblock.api.island.IslandLocation(
										APIUtil.fromImplementation(environment), APIUtil.fromImplementation(world),
										location)));

				FileManager fileManager = skyblock.getFileManager();
				fileManager.setLocation(
						fileManager.getConfig(new File(new File(skyblock.getDataFolder().toString() + "/island-data"),
								getOwnerUUID().toString() + ".yml")),
						"Location." + world.name() + ".Spawn." + environment.name(), location, true);

				islandLocationList.setLocation(location);

				break;
			}
		}
	}

	public Biome getBiome() {
		return Biome.valueOf(skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getString("Biome.Type"));
	}

	public String getBiomeName() {
		return StringUtil
				.capatilizeUppercaseLetters(WordUtils.capitalize(getBiome().name().toLowerCase()).replace("_", " "));
	}

	public void setBiome(Biome biome) {
		IslandBiomeChangeEvent islandBiomeChangeEvent = new IslandBiomeChangeEvent(getAPIWrapper(), biome);
		Bukkit.getServer().getPluginManager().callEvent(islandBiomeChangeEvent);

		skyblock.getFileManager()
				.getConfig(new File(new File(skyblock.getDataFolder().toString() + "/island-data"),
						uuid.toString() + ".yml"))
				.getFileConfiguration().set("Biome.Type", islandBiomeChangeEvent.getBiome().name());
	}

	public boolean isWeatherSynchronized() {
		return skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getBoolean("Weather.Synchronised");
	}

	public void setWeatherSynchronized(boolean sync) {
		IslandWeatherChangeEvent islandWeatherChangeEvent = new IslandWeatherChangeEvent(getAPIWrapper(), getWeather(),
				getTime(), sync);
		Bukkit.getServer().getPluginManager().callEvent(islandWeatherChangeEvent);

		skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().set("Weather.Synchronised", sync);
	}

	public WeatherType getWeather() {
		return WeatherType.valueOf(skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getString("Weather.Weather"));
	}

	public String getWeatherName() {
		return WordUtils.capitalize(getWeather().name().toLowerCase());
	}

	public void setWeather(WeatherType weatherType) {
		IslandWeatherChangeEvent islandWeatherChangeEvent = new IslandWeatherChangeEvent(getAPIWrapper(), weatherType,
				getTime(), isWeatherSynchronized());
		Bukkit.getServer().getPluginManager().callEvent(islandWeatherChangeEvent);

		skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().set("Weather.Weather", weatherType.name());
	}

	public int getTime() {
		return skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getInt("Weather.Time");
	}

	public void setTime(int time) {
		IslandWeatherChangeEvent islandWeatherChangeEvent = new IslandWeatherChangeEvent(getAPIWrapper(), getWeather(),
				time, isWeatherSynchronized());
		Bukkit.getServer().getPluginManager().callEvent(islandWeatherChangeEvent);

		skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().set("Weather.Time", time);
	}

	public Set<UUID> getCoopPlayers() {
		return coopPlayers;
	}

	public void addCoopPlayer(UUID uuid) {
		coopPlayers.add(uuid);
	}

	public void removeCoopPlayer(UUID uuid) {
		coopPlayers.remove(uuid);
	}

	public boolean isCoopPlayer(UUID uuid) {
		return coopPlayers.contains(uuid);
	}

	public Set<UUID> getRole(IslandRole role) {
		Set<UUID> islandRoles = new HashSet<>();

		if (role == IslandRole.Owner) {
			islandRoles.add(getOwnerUUID());
		} else {
			Config config = skyblock.getFileManager().getConfig(
					new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"));
			FileConfiguration configLoad = config.getFileConfiguration();

			if (configLoad.getString(role.name() + "s") != null) {
				for (String playerList : configLoad.getStringList(role.name() + "s")) {
					islandRoles.add(UUID.fromString(playerList));
				}
			}
		}

		return islandRoles;
	}

	public boolean setRole(IslandRole role, UUID uuid) {
		if (!(role == IslandRole.Visitor || role == IslandRole.Coop || role == IslandRole.Owner)) {
			if (!hasRole(role, uuid)) {
				if (role == IslandRole.Member) {
					if (hasRole(IslandRole.Operator, uuid)) {
						Bukkit.getServer().getPluginManager().callEvent(new IslandRoleChangeEvent(getAPIWrapper(),
								Bukkit.getServer().getOfflinePlayer(uuid), APIUtil.fromImplementation(role)));
						removeRole(IslandRole.Operator, uuid);
					}
				} else if (role == IslandRole.Operator) {
					if (hasRole(IslandRole.Member, uuid)) {
						Bukkit.getServer().getPluginManager().callEvent(new IslandRoleChangeEvent(getAPIWrapper(),
								Bukkit.getServer().getOfflinePlayer(uuid), APIUtil.fromImplementation(role)));
						removeRole(IslandRole.Member, uuid);
					}
				}

				Config config = skyblock.getFileManager()
						.getConfig(new File(new File(skyblock.getDataFolder().toString() + "/island-data"),
								getOwnerUUID().toString() + ".yml"));
				File configFile = config.getFile();
				FileConfiguration configLoad = config.getFileConfiguration();

				List<String> islandMembers;

				if (configLoad.getString(role.name() + "s") == null) {
					islandMembers = new ArrayList<>();
				} else {
					islandMembers = configLoad.getStringList(role.name() + "s");
				}

				islandMembers.add(uuid.toString());
				configLoad.set(role.name() + "s", islandMembers);

				try {
					configLoad.save(configFile);
				} catch (IOException e) {
					e.printStackTrace();
				}

				getVisit().setMembers(getRole(IslandRole.Member).size() + getRole(IslandRole.Operator).size() + 1);

				return true;
			}
		}

		return false;
	}

	public boolean removeRole(IslandRole role, UUID uuid) {
		if (!(role == IslandRole.Visitor || role == IslandRole.Coop || role == IslandRole.Owner)) {
			if (hasRole(role, uuid)) {
				Config config = skyblock.getFileManager().getConfig(new File(
						new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"));
				File configFile = config.getFile();
				FileConfiguration configLoad = config.getFileConfiguration();
				List<String> islandMembers = configLoad.getStringList(role.name() + "s");

				islandMembers.remove(uuid.toString());
				configLoad.set(role.name() + "s", islandMembers);

				try {
					configLoad.save(configFile);
				} catch (IOException e) {
					e.printStackTrace();
				}

				getVisit().setMembers(getRole(IslandRole.Member).size() + getRole(IslandRole.Operator).size() + 1);

				return true;
			}
		}

		return false;
	}

	public boolean hasRole(IslandRole role, UUID uuid) {
		if (role == IslandRole.Owner) {
			return getOwnerUUID().equals(uuid);
		}

		return getRole(role).contains(uuid);
	}

	public void setUpgrade(Upgrade.Type type, boolean status) {
		skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().set("Upgrade." + type.name(), status);
	}

	public boolean hasUpgrade(Upgrade.Type type) {
		if (skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getString("Upgrade." + type.name()) == null) {
			return false;
		}

		return true;
	}

	public boolean isUpgrade(Upgrade.Type type) {
		return skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getBoolean("Upgrade." + type.name());
	}

	public Setting getSetting(IslandRole role, String setting) {
		if (islandSettings.containsKey(role)) {
			for (Setting settingList : islandSettings.get(role)) {
				if (settingList.getName().equalsIgnoreCase(setting)) {
					return settingList;
				}
			}
		}

		return null;
	}

	public List<Setting> getSettings(IslandRole role) {
		if (islandSettings.containsKey(role)) {
			return islandSettings.get(role);
		}

		return null;
	}

	public void setOpen(boolean open) {
		IslandOpenEvent islandOpenEvent = new IslandOpenEvent(getAPIWrapper(), open);
		Bukkit.getServer().getPluginManager().callEvent(islandOpenEvent);

		if (!islandOpenEvent.isCancelled()) {
			skyblock.getFileManager().getConfig(
					new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
					.getFileConfiguration().set("Visitor.Open", open);
			getVisit().setOpen(open);
		}
	}

	public boolean isOpen() {
		return skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"))
				.getFileConfiguration().getBoolean("Visitor.Open");
	}

	public List<String> getMessage(IslandMessage message) {
		List<String> islandMessage = new ArrayList<>();

		Config config = skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"));
		FileConfiguration configLoad = config.getFileConfiguration();

		if (configLoad.getString("Visitor." + message.name() + ".Message") != null) {
			islandMessage = configLoad.getStringList("Visitor." + message.name() + ".Message");
		}

		return islandMessage;
	}

	public String getMessageAuthor(IslandMessage message) {
		Config config = skyblock.getFileManager().getConfig(
				new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"));
		FileConfiguration configLoad = config.getFileConfiguration();

		if (configLoad.getString("Visitor." + message.name() + ".Author") != null) {
			return configLoad.getString("Visitor." + message.name() + ".Author");
		}

		return "";
	}

	public void setMessage(IslandMessage message, String author, List<String> lines) {
		IslandMessageChangeEvent islandMessageChangeEvent = new IslandMessageChangeEvent(getAPIWrapper(),
				APIUtil.fromImplementation(message), lines, author);
		Bukkit.getServer().getPluginManager().callEvent(islandMessageChangeEvent);

		if (!islandMessageChangeEvent.isCancelled()) {
			Config config = skyblock.getFileManager().getConfig(
					new File(new File(skyblock.getDataFolder().toString() + "/island-data"), uuid.toString() + ".yml"));
			FileConfiguration configLoad = config.getFileConfiguration();
			configLoad.set("Visitor." + message.name() + ".Message", islandMessageChangeEvent.getLines());
			configLoad.set("Visitor." + message.name() + ".Author", islandMessageChangeEvent.getAuthor());

			if (message == IslandMessage.Signature) {
				getVisit().setSignature(lines);
			}
		}
	}

	public Visit getVisit() {
		return skyblock.getVisitManager().getIsland(getOwnerUUID());
	}

	public Ban getBan() {
		return skyblock.getBanManager().getIsland(getOwnerUUID());
	}

	public Level getLevel() {
		return level;
	}

	public void save() {
		FileManager fileManager = skyblock.getFileManager();

		Config config = fileManager
				.getConfig(new File(skyblock.getDataFolder().toString() + "/island-data", uuid.toString() + ".yml"));

		try {
			config.getFileConfiguration().save(config.getFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		config = fileManager
				.getConfig(new File(skyblock.getDataFolder().toString() + "/setting-data", uuid.toString() + ".yml"));
		FileConfiguration configLoad = config.getFileConfiguration();

		for (IslandRole roleList : islandSettings.keySet()) {
			for (Setting settingList : islandSettings.get(roleList)) {
				configLoad.set("Settings." + roleList + "." + settingList.getName(), settingList.getStatus());
			}
		}

		try {
			configLoad.save(config.getFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!fileManager.getConfig(new File(skyblock.getDataFolder(), "config.yml")).getFileConfiguration()
				.getBoolean("Island.Coop.Unload")) {
			config = fileManager
					.getConfig(new File(skyblock.getDataFolder().toString() + "/coop-data", uuid.toString() + ".yml"));
			configLoad = config.getFileConfiguration();

			List<String> coopPlayersAsString = new ArrayList<>();

			for (UUID coopPlayerList : coopPlayers) {
				coopPlayersAsString.add(coopPlayerList.toString());
			}

			configLoad.set("CoopPlayers", coopPlayersAsString);

			try {
				configLoad.save(config.getFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public me.goodandevil.skyblock.api.island.Island getAPIWrapper() {
		return apiWrapper;
	}
}
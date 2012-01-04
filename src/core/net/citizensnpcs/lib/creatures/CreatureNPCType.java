package net.citizensnpcs.lib.creatures;

import java.lang.reflect.Constructor;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.citizensnpcs.Settings;
import net.citizensnpcs.lib.CraftNPC;
import net.citizensnpcs.utils.Messaging;
import net.citizensnpcs.utils.StringUtils;

import org.bukkit.Location;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public enum CreatureNPCType {
	EVIL(EvilCreatureNPC.class, "Evil", SpawnValidator.DEFAULT_SPAWNIN,
			SpawnValidator.DEFAULT_SPAWNON);
	private final int max, spawnChance;
	private final String[] possible;
	private final boolean spawn;
	private Constructor<? extends CreatureNPC> constructor;
	private final SpawnValidator spawnIn, spawnOn;
	private final Spawner spawner = DefaultSpawner.INSTANCE;
	private final String name;

	CreatureNPCType(Class<? extends CreatureNPC> instance, String name,
			SpawnValidator spawnIn, SpawnValidator spawnOn) {
		name = StringUtils.capitalise(name.toLowerCase());
		try {
			this.constructor = instance.getConstructor(CraftNPC.class);
		} catch (Exception ex) {
			Messaging.log("Unable to get constructor for", name + ".");
		}
		this.spawn = Settings.getBoolean("Spawn" + name + "s");
		this.max = Settings.getInt("Max" + name + "s");
		this.spawnChance = Settings.getInt(name + "SpawnChance");
		this.possible = Settings.getString(name + "Names").split(",");
		this.spawnIn = spawnIn;
		this.spawnOn = spawnOn;
		this.name = name;
	}

	public boolean canSpawn(int spawned) {
		return max > spawned;
	}

	public String chooseRandomName() {
		return possible[random.nextInt(possible.length)];
	}

	public Constructor<? extends CreatureNPC> getEntityConstructor() {
		return constructor;
	}

	public boolean isSpawn() {
		return constructor != null && spawn;
	}

	public boolean shouldSpawn() {
		return this.spawnChance > random.nextInt(100);
	}

	public CreatureNPC spawn(Location location) {
		CraftNPC handle = spawner.spawn(this, location);
		try {
			return constructor.newInstance(handle);
		} catch (Exception ex) {
			return null;
		}
	}

	public boolean validSpawnPosition(org.bukkit.World world, int x, int y,
			int z) {
		return spawnOn.isValid(world.getBlockTypeIdAt(x, y - 1, z))
				&& spawnIn.isValid(world.getBlockTypeIdAt(x, y, z))
				&& spawnIn.isValid(world.getBlockTypeIdAt(x, y + 1, z));
	}

	private static final Random random = new Random();

	private static final Map<String, CreatureNPCType> mapping = Maps
			.newHashMap();

	private static final List<CreatureNPCType> spawning = Lists.newArrayList();

	public static CreatureNPCType fromName(String mob) {
		return mapping.get(mob);
	}

	public static CreatureNPCType getRandomType(Random random) {
		return spawning.size() == 0 ? null : spawning.get(random
				.nextInt(spawning.size()));
	}

	public static boolean hasSpawning() {
		return spawning.size() > 0;
	}

	static {
		for (CreatureNPCType type : EnumSet.allOf(CreatureNPCType.class)) {
			mapping.put(type.name, type);
			if (type.isSpawn())
				spawning.add(type);
		}
	}
}
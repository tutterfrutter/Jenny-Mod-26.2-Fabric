package com.trolmastercard.sexmod.port.registry;

import com.trolmastercard.sexmod.port.JennyPort;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;

public final class ModSounds {
	private static final Map<String, SoundEvent> JENNY_SOUNDS = new HashMap<>();
	private static final Map<String, SoundEvent> GALATH_SOUNDS = new HashMap<>();
	private static final Map<String, SoundEvent> MISC_SOUNDS = new HashMap<>();

	private static final Map<String, Integer> SOUND_COUNTS = Map.ofEntries(
		Map.entry("aftersessionmoan", 5),
		Map.entry("ahh", 10),
		Map.entry("bjmoan", 13),
		Map.entry("giggle", 5),
		Map.entry("happyoh", 3),
		Map.entry("heavybreathing", 8),
		Map.entry("hmph", 4),
		Map.entry("huh", 2),
		Map.entry("lightbreathing", 12),
		Map.entry("lipsound", 10),
		Map.entry("mmm", 9),
		Map.entry("moan", 8),
		Map.entry("sadoh", 2),
		Map.entry("sigh", 2)
	);
	private static final Map<String, Integer> MISC_SOUND_COUNTS = Map.ofEntries(
		Map.entry("bedrustle", 2),
		Map.entry("belljingle", 1),
		Map.entry("cuminflation", 1),
		Map.entry("plob", 1),
		Map.entry("pounding", 36),
		Map.entry("slap", 2),
		Map.entry("smallinserts", 5),
		Map.entry("touch", 2)
	);
	private static final Map<String, Integer> GALATH_SOUND_COUNTS = Map.ofEntries(
		Map.entry("aaa", 2),
		Map.entry("ahh", 8),
		Map.entry("breathing", 7),
		Map.entry("dialog", 6),
		Map.entry("giggle", 4),
		Map.entry("hmph", 3),
		Map.entry("huh", 3),
		Map.entry("lightcharge", 5),
		Map.entry("moan", 8),
		Map.entry("orgasm", 5),
		Map.entry("strongcharge", 4),
		Map.entry("uuh", 7)
	);

	private ModSounds() {
	}

	public static void initialize() {
		SOUND_COUNTS.forEach((category, count) -> {
			for (int index = 0; index < count; index++) {
				register(category, category + index);
			}
		});

		// The original archive contains this second set inside the
		// aftersessionmoan directory. Keep its original identifiers intact.
		for (int index = 0; index < 13; index++) {
			register("aftersessionmoan.bjmoan", "bjmoan" + index);
		}

		MISC_SOUND_COUNTS.forEach((category, count) -> {
			for (int index = 0; index < count; index++) {
				registerMisc(category, index);
			}
		});
		GALATH_SOUND_COUNTS.keySet().forEach(ModSounds::registerGalath);
	}

	private static void register(String category, String clip) {
		String path = "girls.jenny." + category + "." + clip;
		var id = JennyPort.id(path);
		JENNY_SOUNDS.put(path, Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
	}

	private static void registerMisc(String category, int index) {
		String path = "misc." + category + "." + category + index;
		var id = JennyPort.id(path);
		MISC_SOUNDS.put(path, Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
	}

	private static void registerGalath(String category) {
		String path = "girls.galath." + category;
		var id = JennyPort.id(path);
		GALATH_SOUNDS.put(path, Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
	}

	public static SoundEvent jenny(String category, int index) {
		String path = "girls.jenny." + category + "." + category + index;
		SoundEvent sound = JENNY_SOUNDS.get(path);

		if (sound == null) {
			throw new IllegalArgumentException("Unknown Jenny sound: " + path);
		}

		return sound;
	}

	public static int count(String category) {
		return SOUND_COUNTS.getOrDefault(category, 0);
	}

	public static SoundEvent misc(String category, int index) {
		int count = MISC_SOUND_COUNTS.getOrDefault(category, 0);

		if (count == 0) {
			throw new IllegalArgumentException("Unknown misc sound category: " + category);
		}

		String path = "misc." + category + "." + category + Math.floorMod(index, count);
		return MISC_SOUNDS.get(path);
	}

	public static int miscCount(String category) {
		return MISC_SOUND_COUNTS.getOrDefault(category, 0);
	}

	public static SoundEvent galath(String category, int index) {
		int count = GALATH_SOUND_COUNTS.getOrDefault(category, 0);

		if (count == 0) {
			throw new IllegalArgumentException("Unknown Galath sound category: " + category);
		}

		String path = "girls.galath." + category;
		return GALATH_SOUNDS.get(path);
	}

	public static int galathCount(String category) {
		return GALATH_SOUND_COUNTS.getOrDefault(category, 0);
	}
}

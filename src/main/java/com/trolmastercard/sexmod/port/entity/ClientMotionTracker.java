package com.trolmastercard.sexmod.port.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side movement measurement for remote entities. GeckoLib's
 * walkAnimation.speed() and the entity's deltaMovement stay zero on the
 * client for mobs it doesn't own, so walking detection has to be measured
 * from position deltas between render frames instead.
 *
 * Multiple calls within one frame share the same sample (the first caller
 * samples, later callers get the cached value), which keeps debug logging
 * from corrupting the measurement.
 */
public final class ClientMotionTracker {
	private static final Map<UUID, Vec3> LAST_POSITIONS = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_NANOS = new ConcurrentHashMap<>();
	private static final Map<UUID, Double> CACHED_SPEED = new ConcurrentHashMap<>();

	private static final long FRAME_WINDOW_NANOS = 5_000_000; // 5 ms

	private ClientMotionTracker() {
	}

	/// Blocks per second the entity moved since the previous render frame (smoothed).
	public static double speed(Entity entity) {
		UUID id = entity.getUUID();
		long now = System.nanoTime();
		Long lastNanos = LAST_NANOS.get(id);

		if (lastNanos != null && now - lastNanos < FRAME_WINDOW_NANOS) {
			return CACHED_SPEED.getOrDefault(id, 0.0);
		}

		Vec3 previous = LAST_POSITIONS.put(id, entity.position());
		LAST_NANOS.put(id, now);

		if (previous == null) {
			return 0.0;
		}

		double seconds = Math.min((now - lastNanos) / 1_000_000_000.0, 0.25);

		if (seconds <= 0.0005) {
			return 0.0;
		}

		double instant = entity.position().distanceTo(previous) / seconds;
		double speed = Math.min(instant, 10.0);

		// short exponential smoothing on top of the frame sample
		double previousSmoothed = CACHED_SPEED.getOrDefault(id, 0.0);
		double smoothed = instant > 0.05
			? previousSmoothed * 0.6 + speed * 0.4
			: previousSmoothed * 0.15 + speed * 0.85;

		CACHED_SPEED.put(id, smoothed);
		return smoothed;
	}
}

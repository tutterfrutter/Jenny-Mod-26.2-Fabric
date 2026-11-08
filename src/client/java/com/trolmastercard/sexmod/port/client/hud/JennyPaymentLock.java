package com.trolmastercard.sexmod.port.client.hud;

import com.trolmastercard.sexmod.port.entity.JennyEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/**
 * Locks local input immediately after choosing a paid action. The server owns
 * the actual velocity stop; camera rotation deliberately remains unrestricted.
 */
public final class JennyPaymentLock {
	private static int entityId = -1;
	private static int waitTicks;
	private static boolean sawPayment;

	private JennyPaymentLock() {
	}

	public static void initialize() {
		ClientTickEvents.START_CLIENT_TICK.register(JennyPaymentLock::lockKeys);
		ClientTickEvents.END_CLIENT_TICK.register(JennyPaymentLock::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
	}

	public static void begin(int requestedEntityId) {
		entityId = requestedEntityId;
		waitTicks = 0;
		sawPayment = false;
	}

	public static boolean isActive() {
		return entityId >= 0;
	}

	public static int getEntityId() {
		return entityId;
	}

	private static void lockKeys(Minecraft client) {
		if (!isActive() || client.player == null) {
			return;
		}

		client.options.keyUp.setDown(false);
		client.options.keyDown.setDown(false);
		client.options.keyLeft.setDown(false);
		client.options.keyRight.setDown(false);
		client.options.keyJump.setDown(false);
		client.options.keyShift.setDown(false);
		client.options.keySprint.setDown(false);
	}

	private static void tick(Minecraft client) {
		if (!isActive() || client.player == null || client.level == null) {
			return;
		}

		client.player.input = new net.minecraft.client.player.ClientInput();
		client.player.input.keyPresses = Input.EMPTY;
		client.player.setDeltaMovement(Vec3.ZERO);

		Entity entity = client.level.getEntity(entityId);
		boolean payment = entity instanceof JennyEntity jenny
			&& jenny.getActionState() == JennyEntity.ActionState.PAYMENT;

		if (payment) {
			sawPayment = true;
			return;
		}

		waitTicks++;

		if (sawPayment || waitTicks > 10) {
			reset();
		}
	}

	private static void reset() {
		com.trolmastercard.sexmod.port.JennyPort.LOGGER.info("[sexmod-lockreset] payment lock reset at tick {}", net.minecraft.client.Minecraft.getInstance().level != null ? net.minecraft.client.Minecraft.getInstance().level.getGameTime() : -1);
		entityId = -1;
		waitTicks = 0;
		sawPayment = false;
	}
}

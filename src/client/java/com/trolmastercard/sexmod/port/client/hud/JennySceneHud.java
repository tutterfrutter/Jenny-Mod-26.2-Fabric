package com.trolmastercard.sexmod.port.client.hud;

import com.trolmastercard.sexmod.port.JennyPort;
import com.trolmastercard.sexmod.port.entity.GalathEntity;
import com.trolmastercard.sexmod.port.entity.JennyEntity;
import com.trolmastercard.sexmod.port.network.GalathPayloads;
import com.trolmastercard.sexmod.port.network.JennyPayloads;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class JennySceneHud {
	private static final Identifier HORNY_METER =
		JennyPort.id("textures/gui/hornymeter.png");
	private static final Identifier BUTTONS =
		JennyPort.id("textures/gui/buttons.png");
	private static final Identifier HUD_ID = JennyPort.id("jenny_scene_hud");
	private static final HudElement ELEMENT = JennySceneHud::render;

	private static int entityId = -1;
	private static CameraType previousCameraType;
	private static boolean previousNoPhysics;
	private static boolean noPhysicsCaptured;
	private static boolean shiftDown;
	private static boolean jumpDown;
	private static boolean fastCommandKnown;
	private static boolean lastFastCommand;
	private static boolean completionTriggered;
	private static float appearance;
	private static float displayedArousal;

	private JennySceneHud() {
	}

	public static void initialize() {
		HudElementRegistry.addLast(HUD_ID, ELEMENT);
		ClientTickEvents.START_CLIENT_TICK.register(JennySceneHud::lockMovementAndReadControls);
		ClientTickEvents.END_CLIENT_TICK.register(JennySceneHud::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(client));
	}

	public static void open(int requestedEntityId) {
		Minecraft client = Minecraft.getInstance();

		if (entityId < 0) {
			previousCameraType = client.options.getCameraType();

			if (client.player != null) {
				previousNoPhysics = client.player.noPhysics;
				noPhysicsCaptured = true;
			}
		}

		entityId = requestedEntityId;
		appearance = 0.0f;
		displayedArousal = 0.0f;
		completionTriggered = false;
		fastCommandKnown = false;
		lastFastCommand = false;
		client.options.setCameraType(CameraType.FIRST_PERSON);
		client.gui.setScreen(null);
	}

	public static void close(int requestedEntityId) {
		if (entityId != requestedEntityId) {
			return;
		}

		reset(Minecraft.getInstance());
	}

	public static boolean isActive() {
		return entityId >= 0;
	}

	public static int getEntityId() {
		return entityId;
	}

	private static void reset(Minecraft client) {
		com.trolmastercard.sexmod.port.JennyPort.LOGGER.info("[sexmod-hudreset] scene hud reset");
		entityId = -1;
		shiftDown = false;
		jumpDown = false;
		fastCommandKnown = false;
		lastFastCommand = false;
		completionTriggered = false;
		appearance = 0.0f;
		displayedArousal = 0.0f;

		if (noPhysicsCaptured && client.player != null) {
			client.player.noPhysics = previousNoPhysics;
		}

		previousNoPhysics = false;
		noPhysicsCaptured = false;

		if (previousCameraType != null) {
			client.options.setCameraType(previousCameraType);
			previousCameraType = null;
		}
	}

	private static void lockMovementAndReadControls(Minecraft client) {
		if (entityId < 0) {
			return;
		}

		var window = client.getWindow();
		shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
		jumpDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_SPACE);

		client.options.keyUp.setDown(false);
		client.options.keyDown.setDown(false);
		client.options.keyLeft.setDown(false);
		client.options.keyRight.setDown(false);
		client.options.keyJump.setDown(false);
		client.options.keyShift.setDown(false);
		client.options.keySprint.setDown(false);
	}

	private static void tick(Minecraft client) {
		if (entityId < 0 || client.player == null || client.level == null) {
			return;
		}

		client.player.input = new ClientInput();
		client.player.input.keyPresses = Input.EMPTY;
		client.player.setDeltaMovement(Vec3.ZERO);
		client.player.noPhysics = true;

		JennyEntity jenny = getJenny(client);

		if (jenny != null) {
			tickJenny(jenny);
			return;
		}

		GalathEntity galath = getGalath(client);

		if (galath != null) {
			tickGalath(galath);
		}
	}

	private static void tickJenny(JennyEntity jenny) {
		JennyEntity.ActionState action = jenny.getActionState();

		if (action.isControllableLoop()) {
			appearance = Math.min(1.0f, appearance + 0.04f);

			if (!fastCommandKnown || shiftDown != lastFastCommand) {
				fastCommandKnown = true;
				lastFastCommand = shiftDown;

				if (shiftDown) {
					send(JennyEntity.SceneCommand.FAST);
				} else {
					send(JennyEntity.SceneCommand.SLOW);
				}
			}

			if (jumpDown && !completionTriggered && jenny.getSceneArousal() >= 0.999f) {
				completionTriggered = true;
				send(JennyEntity.SceneCommand.FINISH);
			}
		}
	}

	private static void tickGalath(GalathEntity galath) {
		GalathEntity.ActionState action = galath.getActionState();

		if (!action.isControllableLoop()) {
			return;
		}

		appearance = Math.min(1.0f, appearance + 0.04f);

		if (!fastCommandKnown || shiftDown != lastFastCommand) {
			fastCommandKnown = true;
			lastFastCommand = shiftDown;

			if (shiftDown) {
				send(GalathEntity.SceneCommand.FAST);
			} else {
				send(GalathEntity.SceneCommand.SLOW);
			}
		}

		if (jumpDown && !completionTriggered && galath.getSceneArousal() >= 0.999f) {
			completionTriggered = true;
			send(GalathEntity.SceneCommand.FINISH);
		}
	}

	private static JennyEntity getJenny(Minecraft client) {
		Entity entity = client.level == null ? null : client.level.getEntity(entityId);
		return entity instanceof JennyEntity jenny ? jenny : null;
	}

	private static GalathEntity getGalath(Minecraft client) {
		Entity entity = client.level == null ? null : client.level.getEntity(entityId);
		return entity instanceof GalathEntity galath ? galath : null;
	}

	private static void send(JennyEntity.SceneCommand command) {
		ClientPlayNetworking.send(new JennyPayloads.SceneCommand(entityId, command.ordinal()));
	}

	private static void send(GalathEntity.SceneCommand command) {
		ClientPlayNetworking.send(new GalathPayloads.SceneCommand(entityId, command.ordinal()));
	}

	private static void render(GuiGraphicsExtractor gfx, DeltaTracker tickCounter) {
		if (entityId < 0) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		JennyEntity jenny = getJenny(client);
		GalathEntity galath = getGalath(client);
		boolean controllable = jenny != null
			? jenny.getActionState().isControllableLoop()
			: galath != null && galath.getActionState().isControllableLoop();

		if ((jenny == null && galath == null) || (appearance <= 0.0f && !controllable)) {
			return;
		}

		float targetArousal = jenny != null ? jenny.getSceneArousal() : galath.getSceneArousal();
		displayedArousal += (targetArousal - displayedArousal) * 0.2f;
		displayedArousal = Math.clamp(displayedArousal, 0.0f, 1.0f);

		renderButtons(gfx);
		renderMeter(gfx);
	}

	private static void renderButtons(GuiGraphicsExtractor gfx) {
		gfx.pose().pushMatrix();
		gfx.pose().scale(0.35f, 0.35f);

		int jumpTextureV = completionTriggered ? 162 : 108;
		gfx.blit(
			RenderPipelines.GUI_TEXTURED,
			BUTTONS,
			240,
			160,
			0.0f,
			jumpTextureV,
			256,
			52,
			256,
			256
		);

		if (!completionTriggered) {
			int shiftX = Math.round(lerp(-200.0f, 98.0f, appearance));
			int shiftTextureV = shiftDown ? 54 : 0;
			gfx.blit(
				RenderPipelines.GUI_TEXTURED,
				BUTTONS,
				shiftX,
				405,
				0.0f,
				shiftTextureV,
				158,
				54,
				256,
				256
			);
		}

		gfx.pose().popMatrix();
	}

	private static void renderMeter(GuiGraphicsExtractor gfx) {
		gfx.pose().pushMatrix();
		gfx.pose().scale(0.75f, 0.75f);

		int backgroundY = Math.round(lerp(-200.0f, 10.0f, appearance));
		gfx.blit(
			RenderPipelines.GUI_TEXTURED,
			HORNY_METER,
			10,
			backgroundY,
			0.0f,
			0.0f,
			146,
			175,
			256,
			256
		);

		int fillHeight = Math.round(160.0f * displayedArousal);
		int fillTextureV = Math.round(167.0f - 159.0f * displayedArousal);
		int fillTargetV = Math.round(178.0f - 160.0f * displayedArousal);
		int fillY = Math.round(lerp(-45.0f, fillTargetV, appearance));

		if (!completionTriggered && fillHeight > 0) {
			gfx.blit(
				RenderPipelines.GUI_TEXTURED,
				HORNY_METER,
				67,
				fillY,
				159.0f,
				fillTextureV,
				32,
				fillHeight,
				256,
				256
			);

			int sideY = Math.round(149.0f + 29.0f * displayedArousal);
			int sideTextureV = Math.round(141.0f + 28.0f * displayedArousal);
			int sideHeight = Math.max(1, Math.round(29.0f - 28.0f * displayedArousal));
			int animatedSideY = Math.round(lerp(-58.0f, sideY, appearance));

			gfx.blit(
				RenderPipelines.GUI_TEXTURED,
				HORNY_METER,
				18,
				animatedSideY,
				212.0f,
				sideTextureV,
				28,
				sideHeight,
				256,
				256
			);
			gfx.blit(
				RenderPipelines.GUI_TEXTURED,
				HORNY_METER,
				120,
				animatedSideY,
				212.0f,
				sideTextureV,
				28,
				sideHeight,
				256,
				256
			);
		}

		gfx.pose().popMatrix();
	}

	private static float lerp(float start, float end, float delta) {
		return start + (end - start) * delta;
	}
}

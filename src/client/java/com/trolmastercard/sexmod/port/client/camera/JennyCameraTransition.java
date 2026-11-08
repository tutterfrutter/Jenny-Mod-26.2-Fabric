package com.trolmastercard.sexmod.port.client.camera;

import com.trolmastercard.sexmod.port.client.hud.JennySceneHud;
import com.trolmastercard.sexmod.port.entity.GalathEntity;
import com.trolmastercard.sexmod.port.entity.JennyEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

/**
 * Scripted/camera-driven first-person framing for the scene system.
 *
 * Feeds (by priority):
 * <ul>
 *   <li>doggy: the payer rig's animated Head2 world matrix (position + authored
 *       forward), registered by {@code JennyRenderer} each render pass;</li>
 *   <li>boyCam: same authored-bone contract, used by the Galath renderer;</li>
 *   <li>Galath cowgirl/threesome head tracking;</li>
 *   <li>the scripted CameraTransition lerp payload.</li>
 * </ul>
 *
 * The writes happen from the CameraMixin directly after the vanilla camera
 * aligns with the (first-person-locked) player entity.
 */
public final class JennyCameraTransition {
	/// Contract implemented by the CameraMixin shadow view of the camera.
	public interface MutableCamera {
		Vec3 position();
		float xRot();
		float yRot();
		void set(Vec3 position, float xRot, float yRot);
	}

	private static boolean active;
	private static long startNanos;
	private static long durationNanos;
	private static double startX;
	private static double startY;
	private static double startZ;
	private static float startYaw;
	private static float startPitch;
	private static double targetX;
	private static double targetY;
	private static double targetZ;
	private static float targetYaw;
	private static float targetPitch;

	private static int doggyCamEntityId = -1;
	private static Vec3 doggyCamPos = null;
	private static Vec3 doggyCamForward = null;
	private static int boyCamEntityId = -1;
	private static Vec3 boyCamForward = null;
	private static Vec3 jennyLookTarget = null;

	private static int galathCowgirlEntityId = -1;
	private static double galathCowgirlHeadX;
	private static double galathCowgirlHeadY;
	private static double galathCowgirlHeadZ;
	private static boolean galathCowgirlHeadReady;
	private static double galathCowgirlSmoothedX;
	private static double galathCowgirlSmoothedY;
	private static double galathCowgirlSmoothedZ;
	private static boolean galathCowgirlSmoothingReady;
	private static long galathCowgirlLastFrameNanos;
	private static boolean galathThreesomeTracking;
	private static double galathThreesomeReferenceX;
	private static double galathThreesomeReferenceY;
	private static double galathThreesomeReferenceZ;

	private JennyCameraTransition() {
	}

	public static void initialize() {
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			active = false;
			resetDoggyCam();
			resetBoyCam();
			resetGalathCowgirlCamera();
		});
	}

	// ------------------------------------------------------------------
	// Doggy: camera driven by the payer rig's animated Head2 matrix
	// ------------------------------------------------------------------

	public static void updateDoggyCam(net.minecraft.world.entity.Entity jenny, Vec3 camPos, org.joml.Vector3f camForward) {
		if (doggyCamEntityId != jenny.getId()) {
			doggyCamEntityId = jenny.getId();
		}

		doggyCamPos = camPos;
		doggyCamForward = new Vec3(camForward.x, camForward.y, camForward.z);
	}

	private static void resetDoggyCam() {
		doggyCamEntityId = -1;
		doggyCamPos = null;
		doggyCamForward = null;
	}

	private static boolean applyDoggyCam(MutableCamera camera) {
		Minecraft client = Minecraft.getInstance();

		if (doggyCamEntityId < 0 || client.player == null || client.level == null) {
			return false;
		}

		Entity entity = client.level.getEntity(doggyCamEntityId);

		// keep the doggy feed alive only while that entity still is in a doggy
		// pose; the vanilla camera returns as soon as the scene ends
		if (!(entity instanceof JennyEntity jenny)
			|| !jenny.getActionState().isDoggyScene()
			|| jenny.getScenePlayer() != client.player
			|| doggyCamPos == null
			|| doggyCamPos.distanceTo(jenny.position()) > 12.0) {
			resetDoggyCam();
			return false;
		}

		float yaw;
		float pitch;

		if (doggyCamForward != null) {
			// authored rig orientation, straight from the Head2 matrix
			yaw = (float)Math.toDegrees(Math.atan2(-doggyCamForward.x, doggyCamForward.z));
			pitch = Mth.clamp((float)-Math.toDegrees(Math.asin(Mth.clamp(doggyCamForward.y, -1.0f, 1.0f))), -89.0f, 89.0f);
		} else {
			Vec3 delta = jenny.position().add(0.0, jenny.getBbHeight() * 0.55, 0.0).subtract(doggyCamPos);
			double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
			yaw = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
			pitch = Mth.clamp((float)-Math.toDegrees(Math.atan2(delta.y, horizontal)), -85.0f, 85.0f);
		}

		camera.set(doggyCamPos, pitch, yaw);
		return true;
	}

	// ------------------------------------------------------------------
	// boyCam (Galath et al)
	// ------------------------------------------------------------------

	public static void updateJennyCamera(net.minecraft.world.entity.Entity jenny, Vec3 boyCamPos, org.joml.Vector3f camForward) {
		if (boyCamEntityId != jenny.getId()) {
			boyCamEntityId = jenny.getId();
		}

		jennyLookTarget = null;
		boyCamForward = new Vec3(camForward.x, camForward.y, camForward.z);
	}

	public static void updateJennyCameraPosition(
		net.minecraft.world.entity.Entity jenny,
		double x, double y, double z
	) {
		// kept for compatibility; unused since the authored-matrix path
	}

	private static void resetBoyCam() {
		boyCamEntityId = -1;
		jennyLookTarget = null;
		boyCamForward = null;
	}

	private static boolean applyBoyCamCamera(MutableCamera camera) {
		Minecraft client = Minecraft.getInstance();

		if (boyCamEntityId < 0
			|| client.player == null
			|| client.level == null) {
			return false;
		}

		Entity entity = client.level.getEntity(boyCamEntityId);

		if (!(entity instanceof com.trolmastercard.sexmod.port.entity.GalathEntity galath)
			|| !galath.getActionState().isScene()
			|| galath.getScenePlayer() != client.player) {
			resetBoyCam();
			return false;
		}

		return false; // galath scenes use their own cowgirl/threesome tracking
	}

	// ------------------------------------------------------------------
	// Galath cowgirl/threesome head tracking
	// ------------------------------------------------------------------

	public static void updateGalathCowgirlHead(GalathEntity galath, Vector3d headPosition) {
		updateGalathHead(
			galath,
			headPosition,
			isCowgirl(galath.getActionState()),
			false
		);
	}

	public static void updateGalathThreesomeHead(
		GalathEntity galath,
		Vector3d headPosition
	) {
		updateGalathHead(
			galath,
			headPosition,
			isThreesome(galath.getActionState()),
			true
		);
	}

	private static void updateGalathHead(
		GalathEntity galath,
		Vector3d headPosition,
		boolean validAction,
		boolean threesome
	) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null
			|| galath.getScenePlayer() != client.player
			|| !validAction
			|| headPosition.distance(
				new Vector3d(galath.getX(), galath.getY(), galath.getZ())
			) > 6.0) {
			return;
		}

		if (galathCowgirlEntityId != galath.getId()) {
			galathCowgirlEntityId = galath.getId();
			galathCowgirlSmoothingReady = false;
			galathCowgirlLastFrameNanos = 0L;
			galathThreesomeTracking = false;
		}

		if (threesome && !galathThreesomeTracking) {
			galathThreesomeReferenceX = headPosition.x;
			galathThreesomeReferenceY = headPosition.y;
			galathThreesomeReferenceZ = headPosition.z;
			galathCowgirlSmoothingReady = false;
			galathThreesomeTracking = true;
		} else if (!threesome) {
			galathThreesomeTracking = false;
		}

		galathCowgirlHeadX = headPosition.x;
		galathCowgirlHeadY = headPosition.y;
		galathCowgirlHeadZ = headPosition.z;
		galathCowgirlHeadReady = true;
	}

	private static void applyGalathCowgirlCamera(MutableCamera camera) {
		Minecraft client = Minecraft.getInstance();

		if (!galathCowgirlHeadReady
			|| client.player == null
			|| client.level == null) {
			resetGalathCowgirlCamera();
			return;
		}

		Entity entity = client.level.getEntity(galathCowgirlEntityId);

		if (!(entity instanceof GalathEntity galath)
			|| galath.getScenePlayer() != client.player
			|| (!isCowgirl(galath.getActionState())
				&& !isThreesome(galath.getActionState()))) {
			resetGalathCowgirlCamera();
			return;
		}

		long now = System.nanoTime();

		if (!galathCowgirlSmoothingReady) {
			Vec3 current = camera.position();
			galathCowgirlSmoothedX = current.x;
			galathCowgirlSmoothedY = current.y;
			galathCowgirlSmoothedZ = current.z;
			galathCowgirlSmoothingReady = true;
			galathCowgirlLastFrameNanos = now;
		}

		double deltaSeconds = Math.min(
			0.05,
			(now - galathCowgirlLastFrameNanos) / 1_000_000_000.0
		);
		galathCowgirlLastFrameNanos = now;
		double blend = 1.0 - Math.exp(-14.0 * deltaSeconds);
		galathCowgirlSmoothedX += (galathCowgirlHeadX - galathCowgirlSmoothedX) * blend;
		galathCowgirlSmoothedY += (galathCowgirlHeadY - galathCowgirlSmoothedY) * blend;
		galathCowgirlSmoothedZ += (galathCowgirlHeadZ - galathCowgirlSmoothedZ) * blend;

		Vec3 cameraPosition;
		float cameraXRot;
		float cameraYRot;

		if (isThreesome(galath.getActionState())) {
			Vec3 anchor = camera.position();
			double yawRadians = Math.toRadians(camera.yRot());
			cameraPosition = new Vec3(
				anchor.x
					- Math.sin(yawRadians)
					+ galathCowgirlSmoothedX
					- galathThreesomeReferenceX,
				anchor.y
					+ galathCowgirlSmoothedY
					- galathThreesomeReferenceY,
				anchor.z
					+ Math.cos(yawRadians)
					+ galathCowgirlSmoothedZ
					- galathThreesomeReferenceZ
			);
			cameraXRot = camera.xRot();
			cameraYRot = camera.yRot();
		} else {
			cameraPosition = new Vec3(
				galathCowgirlSmoothedX,
				galathCowgirlSmoothedY,
				galathCowgirlSmoothedZ
			);
			cameraXRot = camera.xRot();
			cameraYRot = camera.yRot();
		}

		camera.set(cameraPosition, cameraXRot, cameraYRot);
	}

	private static void resetGalathCowgirlCamera() {
		galathCowgirlEntityId = -1;
		galathCowgirlHeadReady = false;
		galathCowgirlSmoothingReady = false;
		galathCowgirlLastFrameNanos = 0L;
		galathThreesomeTracking = false;
	}

	private static boolean isCowgirl(GalathEntity.ActionState action) {
		return action == GalathEntity.ActionState.COWGIRL_INTRO
			|| action == GalathEntity.ActionState.COWGIRL_SLOW
			|| action == GalathEntity.ActionState.COWGIRL_FAST
			|| action == GalathEntity.ActionState.COWGIRL_CUM;
	}

	private static boolean isThreesome(GalathEntity.ActionState action) {
		return action == GalathEntity.ActionState.THREESOME_SLOW
			|| action == GalathEntity.ActionState.THREESOME_FAST
			|| action == GalathEntity.ActionState.THREESOME_CUM;
	}

	// ------------------------------------------------------------------
	// Scripted camera transition payload
	// ------------------------------------------------------------------

	public static void start(
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		int durationTicks
	) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;

		if (player == null) {
			active = false;
			return;
		}

		Vec3 startPos = player.getEyePosition();

		startX = startPos.x;
		startY = startPos.y;
		startZ = startPos.z;
		startYaw = player.getYRot();
		startPitch = player.getXRot();
		targetX = x;
		targetY = y;
		targetZ = z;
		targetYaw = yaw;
		targetPitch = pitch;
		durationNanos = Math.max(1, durationTicks) * 50_000_000L;
		startNanos = System.nanoTime();
		active = true;
	}

	public static void apply(MutableCamera camera) {
		if (applyDoggyCam(camera)) {
			return;
		}

		if (applyBoyCamCamera(camera)) {
			return;
		}

		applyGalathCowgirlCamera(camera);

		if (!active) {
			return;
		}

		double progress = Mth.clamp(
			(double)(System.nanoTime() - startNanos) / durationNanos,
			0.0,
			1.0
		);
		// Quintic smootherstep starts and stops without a visible velocity jump.
		double smooth = progress * progress * progress
			* (progress * (progress * 6.0 - 15.0) + 10.0);
		float yawDelta = Mth.wrapDegrees(targetYaw - startYaw);
		Vec3 current = camera.position();
		camera.set(
			new Vec3(
				Mth.lerp(smooth, current.x, targetX),
				Mth.lerp(smooth, current.y, targetY),
				Mth.lerp(smooth, current.z, targetZ)
			),
			Mth.lerp((float)smooth, camera.xRot(), targetPitch),
			camera.yRot() + yawDelta * (float)smooth
		);

		if (progress >= 1.0) {
			active = false;
		}
	}
}

package com.trolmastercard.sexmod.port.mixin.client;

import com.trolmastercard.sexmod.port.client.camera.JennyCameraTransition;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

/**
 * Minecraft 26.2 removed the public camera <code>setPosition</code> /
 * <code>setRotation</code> methods and made {@link Camera#setRemoved}-style
 * state private. This mixin performs the same job the old accessor did:
 * after vanilla finishes aligning the camera with the (already
 * first-person-locked) player entity, we override position and rotation for
 * the scripted scene moves. Everything derived afterwards (cull frustum,
 * projection, render state) is computed from the overridden values.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private Vec3 position;
	@Shadow
	private float xRot;
	@Shadow
	private float yRot;

	@Inject(
		method = "alignWithEntity(F)V",
		at = @At("RETURN"),
		cancellable = false
	)
	private void sexmod$applySceneCamera(float partialTicks, CallbackInfo callback) {
		JennyCameraTransition.apply(new JennyCameraTransition.MutableCamera() {
			@Override
			public Vec3 position() {
				return CameraMixin.this.position;
			}

			@Override
			public float xRot() {
				return CameraMixin.this.xRot;
			}

			@Override
			public float yRot() {
				return CameraMixin.this.yRot;
			}

			@Override
			public void set(Vec3 position, float xRot, float yRot) {
				CameraMixin.this.position = position;
				CameraMixin.this.xRot = xRot;
				CameraMixin.this.yRot = yRot;
			}
		});
	}
}

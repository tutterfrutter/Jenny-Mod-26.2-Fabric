package com.trolmastercard.sexmod.port.mixin.client;

import com.trolmastercard.sexmod.port.client.hud.JennyPaymentLock;
import com.trolmastercard.sexmod.port.client.hud.JennySceneHud;
import com.trolmastercard.sexmod.port.entity.JennyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemRendererMixin {
	@Inject(
		method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
			+ "Lnet/minecraft/client/player/LocalPlayer;I)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void sexmod$hideHeldItemsDuringScene(
		float frameInterp,
		com.mojang.blaze3d.vertex.PoseStack poseStack,
		net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
		LocalPlayer player,
		int lightCoords,
		CallbackInfo callback
	) {
		if (JennySceneHud.isActive()) {
			callback.cancel();
		}
	}

	@Inject(
		method = "renderItem",
		at = @At("HEAD"),
		cancellable = true
	)
	private void sexmod$hideVanillaPaymentHands(
		net.minecraft.world.entity.LivingEntity mob,
		net.minecraft.world.item.ItemStack itemStack,
		net.minecraft.world.item.ItemDisplayContext type,
		com.mojang.blaze3d.vertex.PoseStack poseStack,
		net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		CallbackInfo callback
	) {
		if (sexmod$getLocalJennyPayment() != null && mob instanceof LocalPlayer) {
			callback.cancel();
		}
	}

	private static JennyEntity sexmod$getLocalJennyPayment() {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || client.level == null) {
			return null;
		}

		if (JennyPaymentLock.isActive()) {
			net.minecraft.world.entity.Entity entity =
				client.level.getEntity(JennyPaymentLock.getEntityId());

			if (entity instanceof JennyEntity jenny
				&& jenny.getActionState() == JennyEntity.ActionState.PAYMENT) {
				return jenny;
			}
		}

		return client.level.getEntities(
				net.minecraft.world.level.entity.EntityTypeTest.forClass(JennyEntity.class),
				client.player.getBoundingBox().inflate(64.0),
				jenny -> jenny.getActionState() == JennyEntity.ActionState.PAYMENT
					&& jenny.getOwner() == client.player
			)
			.stream()
			.findFirst()
			.orElse(null);
	}
}

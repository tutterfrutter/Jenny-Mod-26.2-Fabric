package com.trolmastercard.sexmod.port.network;

import com.trolmastercard.sexmod.port.entity.JennyEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.Entity;

public final class JennyNetworking {
	public static final double DOGGY_SCENE_PLAYER_ANCHOR = 2.15;

	private JennyNetworking() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(JennyPayloads.OpenMenu.TYPE, JennyPayloads.OpenMenu.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(
			JennyPayloads.OpenSceneControls.TYPE,
			JennyPayloads.OpenSceneControls.CODEC
		);
		PayloadTypeRegistry.clientboundPlay().register(
			JennyPayloads.CloseSceneControls.TYPE,
			JennyPayloads.CloseSceneControls.CODEC
		);
		PayloadTypeRegistry.clientboundPlay().register(
			JennyPayloads.CameraTransition.TYPE,
			JennyPayloads.CameraTransition.CODEC
		);
		PayloadTypeRegistry.serverboundPlay().register(JennyPayloads.SelectAction.TYPE, JennyPayloads.SelectAction.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(JennyPayloads.SceneCommand.TYPE, JennyPayloads.SceneCommand.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(JennyPayloads.SelectAction.TYPE, (payload, context) ->
			context.server().execute(() -> {
				Entity entity = context.player().level().getEntity(payload.entityId());

				if (entity instanceof JennyEntity jenny
					&& context.player().distanceToSqr(jenny) <= 64.0) {
					jenny.handleMenuAction(context.player(), JennyEntity.MenuAction.byId(payload.actionId()));
				}
			})
		);

		ServerPlayNetworking.registerGlobalReceiver(JennyPayloads.SceneCommand.TYPE, (payload, context) ->
			context.server().execute(() -> {
				Entity entity = context.player().level().getEntity(payload.entityId());

				if (entity instanceof JennyEntity jenny) {
					jenny.handleSceneCommand(
						context.player(),
						JennyEntity.SceneCommand.byId(payload.commandId())
					);
				}
			})
		);

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			JennyEntity.endSceneForDisconnect(handler.player)
		);
	}
}

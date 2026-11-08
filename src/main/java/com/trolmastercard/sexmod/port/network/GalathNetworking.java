package com.trolmastercard.sexmod.port.network;

import com.trolmastercard.sexmod.port.entity.GalathEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.Entity;

public final class GalathNetworking {
	private GalathNetworking() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(GalathPayloads.OpenMenu.TYPE, GalathPayloads.OpenMenu.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(
			GalathPayloads.OpenSceneControls.TYPE,
			GalathPayloads.OpenSceneControls.CODEC
		);
		PayloadTypeRegistry.clientboundPlay().register(
			GalathPayloads.CloseSceneControls.TYPE,
			GalathPayloads.CloseSceneControls.CODEC
		);
		PayloadTypeRegistry.serverboundPlay().register(
			GalathPayloads.SelectAction.TYPE,
			GalathPayloads.SelectAction.CODEC
		);
		PayloadTypeRegistry.serverboundPlay().register(
			GalathPayloads.SceneCommand.TYPE,
			GalathPayloads.SceneCommand.CODEC
		);

		ServerPlayNetworking.registerGlobalReceiver(GalathPayloads.SelectAction.TYPE, (payload, context) ->
			context.server().execute(() -> {
				Entity entity = context.player().level().getEntity(payload.entityId());

				if (entity instanceof GalathEntity galath
					&& context.player().distanceToSqr(galath) <= 100.0) {
					galath.handleMenuAction(context.player(), GalathEntity.MenuAction.byId(payload.actionId()));
				}
			})
		);

		ServerPlayNetworking.registerGlobalReceiver(GalathPayloads.SceneCommand.TYPE, (payload, context) ->
			context.server().execute(() -> {
				Entity entity = context.player().level().getEntity(payload.entityId());

				if (entity instanceof GalathEntity galath) {
					galath.handleSceneCommand(context.player(), GalathEntity.SceneCommand.byId(payload.commandId()));
				}
			})
		);

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			GalathEntity.endSceneForDisconnect(handler.player)
		);
	}
}

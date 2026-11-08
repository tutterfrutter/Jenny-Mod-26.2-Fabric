package com.trolmastercard.sexmod.port.client;

import com.trolmastercard.sexmod.port.client.hud.JennySceneHud;
import com.trolmastercard.sexmod.port.client.hud.JennyPaymentLock;
import com.trolmastercard.sexmod.port.client.camera.JennyCameraTransition;
import com.trolmastercard.sexmod.port.client.render.JennyRenderer;
import com.trolmastercard.sexmod.port.client.render.GalathRenderer;
import com.trolmastercard.sexmod.port.client.render.ManglelieRenderer;
import com.trolmastercard.sexmod.port.client.screen.GalathMenuScreen;
import com.trolmastercard.sexmod.port.client.screen.JennyMenuScreen;
import com.trolmastercard.sexmod.port.network.JennyPayloads;
import com.trolmastercard.sexmod.port.network.GalathPayloads;
import com.trolmastercard.sexmod.port.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class JennyPortClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.JENNY, JennyRenderer::new);
		EntityRendererRegistry.register(ModEntities.GALATH, GalathRenderer::new);
		EntityRendererRegistry.register(ModEntities.MANGLELIE, ManglelieRenderer::new);
		JennySceneHud.initialize();
		JennyPaymentLock.initialize();
		JennyCameraTransition.initialize();
		ClientPlayNetworking.registerGlobalReceiver(JennyPayloads.OpenMenu.TYPE, (payload, context) ->
			context.client().execute(() ->
				context.client().gui.setScreen(new JennyMenuScreen(payload.entityId()))
			)
		);
		ClientPlayNetworking.registerGlobalReceiver(JennyPayloads.OpenSceneControls.TYPE, (payload, context) ->
			context.client().execute(() -> JennySceneHud.open(payload.entityId()))
		);
		ClientPlayNetworking.registerGlobalReceiver(JennyPayloads.CloseSceneControls.TYPE, (payload, context) ->
			context.client().execute(() -> JennySceneHud.close(payload.entityId()))
		);
		ClientPlayNetworking.registerGlobalReceiver(JennyPayloads.CameraTransition.TYPE, (payload, context) ->
			context.client().execute(() -> JennyCameraTransition.start(
				payload.x(),
				payload.y(),
				payload.z(),
				payload.yaw(),
				payload.pitch(),
				payload.durationTicks()
			))
		);
		ClientPlayNetworking.registerGlobalReceiver(GalathPayloads.OpenMenu.TYPE, (payload, context) ->
			context.client().execute(() ->
				context.client().gui.setScreen(new GalathMenuScreen(payload.entityId()))
			)
		);
		ClientPlayNetworking.registerGlobalReceiver(GalathPayloads.OpenSceneControls.TYPE, (payload, context) ->
			context.client().execute(() -> JennySceneHud.open(payload.entityId()))
		);
		ClientPlayNetworking.registerGlobalReceiver(GalathPayloads.CloseSceneControls.TYPE, (payload, context) ->
			context.client().execute(() -> JennySceneHud.close(payload.entityId()))
		);
	}
}

package com.trolmastercard.sexmod.port.client.render;

import com.trolmastercard.sexmod.port.JennyPort;
import com.trolmastercard.sexmod.port.client.camera.JennyCameraTransition;
import com.trolmastercard.sexmod.port.entity.GalathEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.cache.model.GeoBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class GalathRenderer extends GeoEntityRenderer<GalathEntity, LivingEntityRenderState> {
	private static final Identifier MANGLELIE_TEXTURE =
		JennyPort.id("textures/entity/manglelie/manglelie.png");

	public GalathRenderer(EntityRendererProvider.Context context) {
		super(context, new GalathModel());
		shadowRadius = 0.6f;
		withRenderLayer(new GalathSceneLayer(this));
	}

	@Override
	public void addRenderData(
		GalathEntity galath,
		@Nullable Void ignored,
		LivingEntityRenderState state,
		float partialTick
	) {
		super.addRenderData(galath, ignored, state, partialTick);

		Player partner = galath.getScenePlayer();
		boolean scene = galath.getActionState().isScene();
		boolean combined = galath.usesCombinedManglelieModel();
		Optional<Identifier> skin = JennyRenderer.skinTextureOf(partner);

		state.addGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE, scene && !combined);
		state.addGeckolibData(SceneRenderTickets.PARTNER_SLIM,
			JennyRenderer.skinModelOf(partner) == PlayerModelType.SLIM);
		state.addGeckolibData(SceneRenderTickets.RIG_HEAD_HIDDEN, scene && partner != null
			&& partner == Minecraft.getInstance().player);
		state.addGeckolibData(SceneRenderTickets.PARTNER_SKIN, skin);
		state.addGeckolibData(SceneRenderTickets.RIG_TRANSLUCENT, isAnalAction(galath.getActionState()));
		state.addGeckolibData(SceneRenderTickets.COMBINED_MANGLELIE, combined);
		state.addGeckolibData(SceneRenderTickets.SHOW_WINGS,
			galath.getActionState() == GalathEntity.ActionState.RIDE);
		state.addGeckolibData(SceneRenderTickets.SCENE_ENTITY_ID, galath.getId());
	}

	@Override
	public @Nullable RenderType getRenderType(
		LivingEntityRenderState state,
		Identifier texture
	) {
		if (state.getGeckolibData(SceneRenderTickets.RIG_TRANSLUCENT)) {
			return RenderTypes.entityTranslucent(texture);
		}

		return super.getRenderType(state, texture);
	}

	@Override
	public int getRenderColor(GalathEntity animatable, @Nullable Void ignored, float partialTick) {
		int color = super.getRenderColor(animatable, ignored, partialTick);

		if (isAnalAction(animatable.getActionState())) {
			// the original darkened the player rig during the anal scenes
			return (color & 0x00FFFFFF) | 0x59000000;
		}

		return color;
	}

	private static boolean isAnalAction(GalathEntity.ActionState action) {
		return action == GalathEntity.ActionState.ANAL_SLOW
			|| action == GalathEntity.ActionState.ANAL_FAST
			|| action == GalathEntity.ActionState.ANAL_CUM;
	}

	private static final class GalathSceneLayer extends SubtreeTextureGeoLayer<GalathEntity> {
		private GalathSceneLayer(GeoRenderer<GalathEntity, Void, LivingEntityRenderState> renderer) {
			super(renderer);
		}

		@Override
		protected @Nullable Identifier subtreeOverrideTexture(
			String rootBoneName,
			LivingEntityRenderState state
		) {
			return switch (rootBoneName) {
				case "steve" -> state.getGeckolibData(SceneRenderTickets.PARTNER_SKIN).orElse(null);
				default -> null;
			};
		}

		@Override
		protected boolean subtreeNeedsLightBoost(String rootBoneName, LivingEntityRenderState state) {
			return false;
		}

		@Override
		protected void applyMainPassVisibility(
			RenderPassInfo<LivingEntityRenderState> renderPassInfo,
			BoneSnapshots snapshots
		) {
			LivingEntityRenderState state = renderPassInfo.renderState();
			boolean sceneScene = state.getGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE)
				|| state.getGeckolibData(SceneRenderTickets.COMBINED_MANGLELIE);
			boolean nipplesVisible = sceneScene;

			skipBoneTree(snapshots, "coin", true, true);
			skipBoneTree(snapshots, "girlCam", true, true);
			skipBoneTree(snapshots, "girlCam2", true, true);
			skipBoneTree(snapshots, "boyCam", true, true);
			skipBoneTree(snapshots, "wings", !state.getGeckolibData(SceneRenderTickets.SHOW_WINGS), true);
			skipBoneTree(snapshots, "nippleR", !nipplesVisible, false);
			skipBoneTree(snapshots, "nippleL", !nipplesVisible, false);
			skipBoneTree(snapshots, "braBoobL", nipplesVisible, false);
			skipBoneTree(snapshots, "braBoobR", nipplesVisible, false);
			skipBoneTree(snapshots, "slip", nipplesVisible, false);
			skipBoneTree(snapshots, "steve", true, true);
			skipBoneTree(snapshots, "cock", true, true);
		}

		@Override
		protected boolean isBoneVisibleInOverride(
			String rootName,
			String boneName,
			java.util.Set<String> ancestors,
			LivingEntityRenderState state
		) {
			boolean scene = state.getGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE);
			boolean combined = state.getGeckolibData(SceneRenderTickets.COMBINED_MANGLELIE);
			boolean slim = state.getGeckolibData(SceneRenderTickets.PARTNER_SLIM);
			boolean headHidden = state.getGeckolibData(SceneRenderTickets.RIG_HEAD_HIDDEN);

			return switch (rootName) {
				case "steve" -> scene && !combined && switch (boneName) {
					case "Head2" -> !headHidden;
					case "leftArmAlex", "leftLowerArmAlex", "rightArmAlex", "rightLowerArmAlex" -> slim;
					case "leftArmSteve", "leftLowerArmSteve", "rightArmSteve", "rightLowerArmSteve" -> !slim;
					default -> true;
				};
				case "cock" -> scene && !combined;
				default -> false;
			};
		}
	}
}

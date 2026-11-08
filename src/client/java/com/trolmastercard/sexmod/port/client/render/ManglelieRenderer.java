package com.trolmastercard.sexmod.port.client.render;

import com.trolmastercard.sexmod.port.JennyPort;
import com.trolmastercard.sexmod.port.client.camera.JennyCameraTransition;
import com.trolmastercard.sexmod.port.entity.GalathEntity;
import com.trolmastercard.sexmod.port.entity.ManglelieEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import net.minecraft.world.entity.player.PlayerModelType;

public final class ManglelieRenderer extends GeoEntityRenderer<ManglelieEntity, LivingEntityRenderState> {
	private static final Identifier GALATH_TEXTURE =
		JennyPort.id("textures/entity/galath/galath.png");

	public ManglelieRenderer(EntityRendererProvider.Context context) {
		super(context, new ManglelieModel());
		shadowRadius = 0.4f;
		JennyRenderer.registerAnatomyTexture();
		withRenderLayer(new ManglelieSceneLayer(this));
	}

	@Override
	public void addRenderData(
		ManglelieEntity manglelie,
		@Nullable Void ignored,
		LivingEntityRenderState state,
		float partialTick
	) {
		super.addRenderData(manglelie, ignored, state, partialTick);

		GalathEntity galath = manglelie.getGalath();
		boolean combined = manglelie.usesCombinedSceneModel();
		var player = galath == null ? null : galath.getScenePlayer();
		Optional<Identifier> skin = JennyRenderer.skinTextureOf(player);

		state.addGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE, combined);
		state.addGeckolibData(SceneRenderTickets.PARTNER_SKIN, skin);
		state.addGeckolibData(SceneRenderTickets.PARTNER_SLIM,
			JennyRenderer.skinModelOf(galath == null ? null : galath.getScenePlayer()) == PlayerModelType.SLIM);
		state.addGeckolibData(SceneRenderTickets.SKIP_BODY_BRANCH, combined);
		state.addGeckolibData(SceneRenderTickets.RIG_HEAD_HIDDEN,
			combined && player != null && player == Minecraft.getInstance().player);
		state.addGeckolibData(SceneRenderTickets.SCENE_ENTITY_ID, manglelie.getId());
	}

	private static final class ManglelieSceneLayer extends SubtreeTextureGeoLayer<ManglelieEntity> {
		private ManglelieSceneLayer(GeoRenderer<ManglelieEntity, Void, LivingEntityRenderState> renderer) {
			super(renderer);
		}

		@Override
		protected @Nullable Identifier subtreeOverrideTexture(
			String rootBoneName,
			LivingEntityRenderState state
		) {
			if (!state.getGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE)) {
				return null;
			}

			return switch (rootBoneName) {
				case "body" -> GALATH_TEXTURE; // combined asset carries a Galath reference branch
				case "cock" -> JennyRenderer.PARTNER_ANATOMY_TEXTURE;
				case "steve" -> state.getGeckolibData(SceneRenderTickets.PARTNER_SKIN).orElse(null);
				default -> null;
			};
		}

		@Override
		protected boolean subtreeNeedsLightBoost(String rootBoneName, LivingEntityRenderState state) {
			return "cock".equals(rootBoneName);
		}

		@Override
		protected void applyMainPassVisibility(
			RenderPassInfo<LivingEntityRenderState> renderPassInfo,
			BoneSnapshots snapshots
		) {
			LivingEntityRenderState state = renderPassInfo.renderState();
			boolean combined = state.getGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE);

			if (!combined) {
				// stand-alone Manglelie (follow-around mode)
				skipBoneTree(snapshots, "steve", true, true);
				skipBoneTree(snapshots, "body", true, true);
				skipBoneTree(snapshots, "boyCam", true, true);
				skipBoneTree(snapshots, "girlCam", true, true);
				skipBoneTree(snapshots, "skirt", true, false);
				skipBoneTree(snapshots, "sideRSkirt", true, false);
				skipBoneTree(snapshots, "sideLSkirt", true, false);
				return;
			}

			// combined three-person scene model
			skipBoneTree(snapshots, "body", true, true);
			skipBoneTree(snapshots, "boyCam", true, true);
			skipBoneTree(snapshots, "girlCam", true, true);
			skipBoneTree(snapshots, "skirt", true, false);
			skipBoneTree(snapshots, "cheekRBelowSkirt", true, false);
			skipBoneTree(snapshots, "cheekLBelowSkirt", true, false);
			skipBoneTree(snapshots, "sideRNoSkirt", true, false);
			skipBoneTree(snapshots, "sideLNoSkirt", true, false);
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
			boolean combined = state.getGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE);
			boolean headHidden = state.getGeckolibData(SceneRenderTickets.RIG_HEAD_HIDDEN);

			if (!combined) {
				return false;
			}

			return switch (rootName) {
				case "body" -> !"body".equals(boneName); // hide the empty reference root, render children
				case "steve" -> switch (boneName) {
					case "Head2" -> !headHidden;
					case "leftArmAlex", "leftLowerArmAlex", "rightArmAlex", "rightLowerArmAlex" -> state.getGeckolibData(SceneRenderTickets.PARTNER_SLIM);
					case "leftArmSteve", "leftLowerArmSteve", "rightArmSteve", "rightLowerArmSteve" -> !state.getGeckolibData(SceneRenderTickets.PARTNER_SLIM);
					default -> true;
				};
				case "cock" -> true;
				default -> false;
			};
		}
	}
}

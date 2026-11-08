package com.trolmastercard.sexmod.port.client.render;

import com.trolmastercard.sexmod.port.JennyPort;
import com.trolmastercard.sexmod.port.entity.JennyEntity;
import com.trolmastercard.sexmod.port.client.camera.JennyCameraTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.geckolib.cache.model.GeoBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class JennyRenderer extends GeoEntityRenderer<JennyEntity, LivingEntityRenderState> {
	static final Identifier PARTNER_ANATOMY_TEXTURE =
		JennyPort.id("dynamic/partner_anatomy");
	private static boolean anatomyTextureRegistered;

	/*
	 * The original GeckoLib 3 renderer hid these helper bones unless a scene
	 * or a vanilla armor layer explicitly needed them. (Bone-level visibility
	 * is applied by the scene layer each pass.)
	 */
	private static final String[] DEFAULT_HIDDEN_BONES = {
		"items",
		"boyCam",
		"girlCam",
		"armorHelmet",
		"armorShoulderR",
		"armorShoulderL",
		"armorChest",
		"armorBoobs",
		"armorBootyR",
		"armorBootyL",
		"armorPantsLowL",
		"armorPantsLowR",
		"armorPantsUpR",
		"armorPantsUpL",
		"armorHip",
		"armorShoesL",
		"armorShoesR"
	};

	static Optional<Identifier> skinTextureOf(@Nullable Player player) {
		if (player instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer
			&& clientPlayer.getSkin() != null) {
			return Optional.of(clientPlayer.getSkin().body().texturePath());
		}

		return Optional.empty();
	}

	static PlayerModelType skinModelOf(@Nullable Player player) {
		if (player instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer
			&& clientPlayer.getSkin() != null) {
			return clientPlayer.getSkin().model();
		}

		return PlayerModelType.WIDE;
	}

	public JennyRenderer(EntityRendererProvider.Context context) {
		super(context, new JennyModel());
		shadowRadius = 0.45f;
		registerAnatomyTexture();
		withRenderLayer(new JennySceneLayer(this));
		withRenderLayer(new BlockAndItemGeoLayer<JennyEntity, Void, LivingEntityRenderState>(context, this) {
			@Override
			public void addRenderData(
				JennyEntity jenny,
				@Nullable Void ignored,
				LivingEntityRenderState state,
				float partialTick
			) {
				List<BlockAndItemGeoLayer.RenderData> contents = List.of();

				if (jenny.shouldRenderOfferedItem()) {
					ItemStackRenderState itemState = new ItemStackRenderState();
					itemModelResolver.updateForTopItem(
						itemState,
						jenny.getOfferedItem().copyWithCount(1),
						ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
						jenny.level(),
						jenny,
						0
					);
					contents = List.of(BlockAndItemGeoLayer.RenderData.item(
						"itemRenderer",
						ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
						itemState
					));
				}

				state.addGeckolibData(BlockAndItemGeoLayer.CONTENTS, contents);
			}

			@Override
			protected List<BlockAndItemGeoLayer.RenderData> getRelevantBones(
				JennyEntity ignoredEntity,
				@Nullable Void ignoredObject,
				LivingEntityRenderState ignoredState,
				float ignoredPartialTick
			) {
				return List.of();
			}
		});
	}

	@Override
	public void addRenderData(
		JennyEntity jenny,
		@Nullable Void ignored,
		LivingEntityRenderState state,
		float partialTick
	) {
		super.addRenderData(jenny, ignored, state, partialTick);

		Player partner = jenny.getActionState() == JennyEntity.ActionState.PAYMENT
			? jenny.getOwner()
			: jenny.getScenePlayer();
		boolean partnerRigActive = jenny.getActionState().isPartnerScene()
			|| jenny.getActionState() == JennyEntity.ActionState.PAYMENT;
		Optional<Identifier> skin = skinTextureOf(partner);

		state.addGeckolibData(SceneRenderTickets.DRESSED, jenny.isDressed());
		state.addGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE, partnerRigActive);
		state.addGeckolibData(SceneRenderTickets.PAYMENT_FILTER,
			jenny.getActionState() == JennyEntity.ActionState.PAYMENT);
		state.addGeckolibData(SceneRenderTickets.PARTNER_SLIM,
			skinModelOf(partner) == PlayerModelType.SLIM);
		state.addGeckolibData(SceneRenderTickets.RIG_HEAD_HIDDEN, shouldHideRigHead(jenny));
		state.addGeckolibData(SceneRenderTickets.PARTNER_SKIN, skin);
		state.addGeckolibData(SceneRenderTickets.OFFERED_ITEM_VISIBLE, jenny.shouldRenderOfferedItem());
		state.addGeckolibData(SceneRenderTickets.SCENE_ENTITY_ID, jenny.getId());
		state.addGeckolibData(SceneRenderTickets.SCENE_IS_DOGGY, jenny.getActionState().isDoggyScene());
	}

	private static boolean shouldHideRigHead(JennyEntity jenny) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null
			|| jenny.getActionState() == JennyEntity.ActionState.PAYMENT) {
			return false;
		}

		if (!jenny.getActionState().isPartnerScene()
			|| jenny.getScenePlayer() != client.player) {
			return true;
		}

		/*
		 * The head would cover the normal first-person paizuri view. It is
		 * only useful after the original animation moves the camera to
		 * Jenny's side.
		 */
		if (isPaizuriAction(jenny.getActionState())) {
			return !isPaizuriSideView(jenny, client);
		}

		return true;
	}

	private static boolean isPaizuriAction(JennyEntity.ActionState action) {
		return action == JennyEntity.ActionState.PAIZURI_START
			|| action == JennyEntity.ActionState.PAIZURI_SLOW
			|| action == JennyEntity.ActionState.PAIZURI_FAST
			|| action == JennyEntity.ActionState.PAIZURI_CUM;
	}

	private static boolean isPaizuriSideView(
		JennyEntity jenny,
		Minecraft client
	) {
		double yaw = Math.toRadians(jenny.yBodyRot + 90.0f);
		double sideX = -Math.sin(yaw);
		double sideZ = Math.cos(yaw);
		var camera = client.gameRenderer.mainCamera().position();
		double deltaX = camera.x - jenny.getX();
		double deltaZ = camera.z - jenny.getZ();
		double lateralDistance = deltaX * sideX + deltaZ * sideZ;
		return Math.abs(lateralDistance) > 0.35;
	}

	static void registerAnatomyTexture() {
		if (anatomyTextureRegistered) {
			return;
		}

		DynamicTexture texture = new DynamicTexture(
			(java.util.function.Supplier<String>)() -> "sexmod_partner_anatomy", 64, 64, true);
		NativeImage image = texture.getPixels();

		if (image != null) {
			// Exact colors and pixel rectangles used by the original 1.12.2 renderer.
			image.fillRect(0, 0, 4, 3, 0xFFF5C7A5);
			image.fillRect(4, 0, 3, 3, 0xFFF59DA9);
		}

		Minecraft.getInstance().getTextureManager().register(PARTNER_ANATOMY_TEXTURE, texture);
		texture.upload();
		anatomyTextureRegistered = true;
	}


	/// Positions + orients the first-person camera from an authored boyCam bone:
	/// walks the bone chain exactly like the GeckoLib render walk (per-bone
	/// snapshot translate/rotate/scale) on top of the entity's world transform,
	/// then reads the pivot position and the authored forward direction.
	static org.joml.Matrix4f computeBoneWorldMatrix(
		com.geckolib.animatable.GeoAnimatable animatable,
		GeoBone bone,
		float bodyYaw
	) {
		com.mojang.blaze3d.vertex.PoseStack temp = new com.mojang.blaze3d.vertex.PoseStack();
		com.mojang.blaze3d.vertex.PoseStack.Pose pose = temp.last();

		if (animatable instanceof net.minecraft.world.entity.Entity entity) {
			pose.pose().translation((float)entity.getX(), (float)entity.getY(), (float)entity.getZ());
		}

		pose.pose().rotate(com.mojang.math.Axis.YP.rotationDegrees(180.0f - bodyYaw));
		com.geckolib.util.RenderUtil.transformToBone(temp, bone);
		return new org.joml.Matrix4f(temp.last().pose());
	}

	private static final class JennySceneLayer extends SubtreeTextureGeoLayer<JennyEntity> {
		private JennySceneLayer(GeoRenderer<JennyEntity, Void, LivingEntityRenderState> renderer) {
			super(renderer);
		}

		@Override
		protected @Nullable Identifier subtreeOverrideTexture(
			String rootBoneName,
			LivingEntityRenderState state
		) {
			return switch (rootBoneName) {
				case "steve" -> state.getGeckolibData(SceneRenderTickets.PARTNER_SKIN).orElse(null);
				case "cock" -> PARTNER_ANATOMY_TEXTURE;
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
			boolean offeringItem = renderPassInfo.renderState().getGeckolibData(SceneRenderTickets.OFFERED_ITEM_VISIBLE);

			for (String hiddenBone : DEFAULT_HIDDEN_BONES) {
				skipBoneTree(snapshots, hiddenBone, true, true);
			}

			skipBoneTree(snapshots, "items", !offeringItem, false);
			skipBoneTree(snapshots, "itemRenderer", !offeringItem, false);

			// overridden subtrees are drawn exclusively by the per-bone tasks
			skipBoneTree(snapshots, "steve", true, true);
			skipBoneTree(snapshots, "cock", true, true);

			// DOGGY: the payer's rig body is their first-person body - place the
			// camera ON the rig's animated Head2 matrix (position + authored
			// forward) so the POV sits over her hips like the original mod.
			Minecraft dgClient = Minecraft.getInstance();
			boolean dgFirstPerson = dgClient.player != null;

			if (dgFirstPerson
				&& renderPassInfo.renderState().getGeckolibData(SceneRenderTickets.SCENE_IS_DOGGY)
				&& dgClient.level != null
				&& dgClient.level.getEntity(renderPassInfo.renderState().getGeckolibData(SceneRenderTickets.SCENE_ENTITY_ID)) instanceof JennyEntity jennyE
				&& jennyE.isOwnedBy(dgClient.player)
				&& renderPassInfo.model().getBone("Head2").isPresent()) {
				com.geckolib.cache.model.GeoBone rigHead = renderPassInfo.model().getBone("Head2").get();

				renderPassInfo.addBoneUpdater((info, snaps) -> {
					org.joml.Matrix4f camWorld = computeBoneWorldMatrix(
						jennyE,
						rigHead,
						jennyE.yBodyRot
					);
					Vec3 camPos = new Vec3(camWorld.m30(), camWorld.m31(), camWorld.m32());
					org.joml.Vector3f camForward = new org.joml.Vector3f(0f, 0f, 1f);
					camWorld.transformDirection(camForward);

					com.trolmastercard.sexmod.port.client.camera.JennyCameraTransition
						.updateDoggyCam(jennyE, camPos, camForward);
				});
			}
		}

		@Override
		protected boolean isBoneVisibleInOverride(
			String rootName,
			String boneName,
			java.util.Set<String> ancestors,
			LivingEntityRenderState state
		) {
			boolean partnerActive = state.getGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE);
			boolean paymentFilter = state.getGeckolibData(SceneRenderTickets.PAYMENT_FILTER);
			boolean slim = state.getGeckolibData(SceneRenderTickets.PARTNER_SLIM);
			if (!partnerActive) {
				return false;
			}

			boolean doggyScene = state.getGeckolibData(SceneRenderTickets.SCENE_IS_DOGGY);

			// For the payer's first-person render the rig cubes sit right around
			// the camera: hide them for that player only (clean view), while
			// other viewers keep the full rig. The doggy is the opposite: its
			// rig body forms the on-bed pose, so it stays visible for everyone
			// except the Head2/arm swaps the owner shouldn't see.
			Minecraft inst = Minecraft.getInstance();
			boolean firstPersonOwner = inst != null
				&& inst.player != null
				&& inst.level != null
				&& inst.level.getEntity(state.getGeckolibData(SceneRenderTickets.SCENE_ENTITY_ID)) instanceof JennyEntity jennyE
				&& jennyE.isOwnedBy(inst.player);

			if (rootName.equals("cock")) {
				return true;
			}

			// doggy too: the rig's own torso/head cubes sit directly in the payer's
			// camera (the rig body is posed between them and Jenny), so the
			// owner sees through the rig; other viewers keep the full rig body.
			return !firstPersonOwner;
		}
	}
}

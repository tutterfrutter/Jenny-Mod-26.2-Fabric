package com.trolmastercard.sexmod.port.client.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Re-renders selected bone subtrees with a replacement texture. Replaces the
 * GeckoLib 4 dynamic entity renderer: the subtree is hidden in the main
 * model pass (via bone snapshots) and re-drawn in a per-bone render task
 * with the chosen texture.
 *
 * Visibility inside the re-render is decided by the renderer-specific
 * {@link #isBoneVisibleInOverride}; the snapshot flags only exist to
 * suppress the main-pass draw, so both mechanisms never fight.
 */
abstract class SubtreeTextureGeoLayer<T extends GeoAnimatable> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
	private static final int FULL_BRIGHT = 15728880;
	private static final String[] SUBTREE_ROOTS = {"steve", "cock", "body", "body2"};

	protected SubtreeTextureGeoLayer(GeoRenderer<T, Void, LivingEntityRenderState> renderer) {
		super(renderer);
	}

	/// Main-pass visibility decisions (suppression of overridden trees, cosmetic hiding, etc).
	protected abstract void applyMainPassVisibility(
		RenderPassInfo<LivingEntityRenderState> renderPassInfo,
		BoneSnapshots snapshots
	);

	/// Whether a bone (of the given subtree root) draws its cubes inside the override render.
	protected abstract boolean isBoneVisibleInOverride(
		String rootName,
		String boneName,
		Set<String> ancestorNames,
		LivingEntityRenderState renderState
	);

	/// The subtree root re-rendered with the given texture during this pass (null = none).
	protected abstract @Nullable Identifier subtreeOverrideTexture(String rootBoneName, LivingEntityRenderState renderState);

	protected abstract boolean subtreeNeedsLightBoost(String rootBoneName, LivingEntityRenderState renderState);

	@Override
	public void preRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
		if (!renderPassInfo.willRender()) {
			return;
		}

		renderPassInfo.addBoneUpdater((info, snapshots) -> applyMainPassVisibility(info, snapshots));
	}

	@Override
	public void addPerBoneRender(
		RenderPassInfo<LivingEntityRenderState> renderPassInfo,
		BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer
	) {
		if (!renderPassInfo.willRender()) {
			return;
		}

		LivingEntityRenderState state = renderPassInfo.renderState();
		BakedGeoModel model = renderPassInfo.model();

		for (String rootName : SUBTREE_ROOTS) {
			Identifier texture = subtreeOverrideTexture(rootName, state);

			if (texture == null) {
				continue;
			}

			final String resolvedRoot = rootName;
			final Identifier resolvedTexture = texture;
			model.getBone(rootName).ifPresent(bone ->
				consumer.accept(bone, (rpi, bone2, tasks) ->
					renderSubtree(rpi, bone2, tasks, resolvedRoot, resolvedTexture, subtreeNeedsLightBoost(rootName, state), state)));
		}
	}

	/**
	 * Re-render the whole subtree using the replacement texture. Runs inside
	 * the posed window, so bone snapshots reflect the current animation.
	 */
	protected void renderSubtree(
		RenderPassInfo<LivingEntityRenderState> renderPassInfo,
		GeoBone rootBone,
		SubmitNodeCollector renderTasks,
		String rootName,
		Identifier texture,
		boolean boostLight,
		LivingEntityRenderState renderState
	) {
		RenderType renderType = this.renderer.getRenderType(renderState, texture);

		if (renderType == null) {
			return;
		}

		int packedLight = boostLight ? FULL_BRIGHT : renderPassInfo.packedLight();
		int packedOverlay = renderPassInfo.packedOverlay();
		int renderColor = renderPassInfo.renderColor();

		renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderType, (pose, buffer) -> {
			PoseStack poseStack = renderPassInfo.poseStack();

			poseStack.pushPose();
			// GeckoLib poses the stack on the root bone (same state the built-in
			// CustomBoneTextureGeoLayer assumes); undo its pivot offset and walk.
			poseStack.last().set(pose);
			rootBone.translateAwayFromPivotPoint(poseStack);
			renderBone(rootName, rootBone, new LinkedHashSet<>(), poseStack, buffer, packedLight, packedOverlay, renderColor, renderState);
			poseStack.popPose();
		});
	}

	private void renderBone(
		String rootName,
		GeoBone bone,
		Set<String> ancestorNames,
		PoseStack poseStack,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		int renderColor,
		LivingEntityRenderState renderState
	) {
		Set<String> chain = new LinkedHashSet<>(ancestorNames);

		if (isBoneVisibleInOverride(rootName, bone.name(), chain, renderState) && bone instanceof CuboidGeoBone cuboidBone) {
			for (GeoCube cube : cuboidBone.cubes) {
				poseStack.pushPose();
				cube.render(poseStack, buffer, packedLight, packedOverlay, renderColor);
				poseStack.popPose();
			}
		}

		chain.add(bone.name());

		for (GeoBone child : bone.children()) {
			poseStack.pushPose();
			RenderUtil.prepMatrixForBone(poseStack, child);
			renderBone(rootName, child, chain, poseStack, buffer, packedLight, packedOverlay, renderColor, renderState);
			poseStack.popPose();
		}
	}

	/// immediate helpers for subclass main-pass updaters ----------------------

	protected static boolean hasAncestorNamed(GeoBone bone, String name) {
		for (GeoBone current = bone; current != null; current = current.parent()) {
			if (name.equals(current.name())) {
				return true;
			}
		}

		return false;
	}

	protected static void skipBoneTree(BoneSnapshots snapshots, String boneName, boolean ownCubes, boolean children) {
		snapshots.get(boneName).ifPresent(snapshot -> {
			if (ownCubes) {
				snapshot.skipRender(true);
			}

			snapshot.skipChildrenRender(children);
		});
	}

	protected static void hideAll(BoneSnapshots snapshots, Iterable<String> names, boolean ownCubes, boolean children) {
		for (String name : names) {
			skipBoneTree(snapshots, name, ownCubes, children);
		}
	}
}

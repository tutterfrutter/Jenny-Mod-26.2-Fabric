package com.trolmastercard.sexmod.port.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ManglelieEntity extends PathfinderMob implements GeoEntity {
	private static final EntityDataAccessor<Integer> ACTION_STATE =
		SynchedEntityData.defineId(ManglelieEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FOLLOWING =
		SynchedEntityData.defineId(ManglelieEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> OWNER_UUID =
		SynchedEntityData.defineId(ManglelieEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> GALATH_UUID =
		SynchedEntityData.defineId(ManglelieEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);

	private static final EntityDataAccessor<Integer> WALK_STATE =
		SynchedEntityData.defineId(ManglelieEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> WALK_SPEED =
		SynchedEntityData.defineId(ManglelieEntity.class, EntityDataSerializers.FLOAT);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.manglelie.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.manglelie.walk");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.manglelie.running");
	private static final RawAnimation SCARED_RUN_ANIMATION =
		RawAnimation.begin().thenLoop("animation.manglelie.scared_run");

	@Nullable
	private UUID ownerUuid;
	@Nullable
	private UUID galathUuid;
	private int actionTicks;
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

	public ManglelieEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createManglelieAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 80.0)
			.add(Attributes.MOVEMENT_SPEED, 0.65)
			.add(Attributes.ATTACK_DAMAGE, 6.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(WALK_STATE, 0);
		builder.define(WALK_SPEED, 0.0f);
		builder.define(ACTION_STATE, ActionState.NONE.ordinal());
		builder.define(FOLLOWING, true);
		builder.define(OWNER_UUID, Optional.<EntityReference<LivingEntity>>empty());
		builder.define(GALATH_UUID, Optional.<EntityReference<LivingEntity>>empty());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new ActionLockGoal());
		goalSelector.addGoal(2, new FollowGalathGoal());
		goalSelector.addGoal(4, new RandomStrollGoal(this, 0.65) {
			@Override
			public boolean canUse() {
				return isFollowing() && getActionState() == ActionState.NONE && super.canUse();
			}
		});
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 7.0f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public void tick() {
		super.tick();

		if (!level().isClientSide()) {
			updateServerWalkState();
		}

		if (level().isClientSide()) {
			return;
		}

		updateServerWalkState();

		if (galathUuid == null && tickCount % 40 == 0) {
			GalathEntity galath = findNearestGalath();

			if (galath != null) {
				setGalath(galath);
				Player owner = galath.getOwner();

				if (owner != null) {
					setOwnerUuid(owner.getUUID());
				}
			}
		}

		if (actionTicks > 0 && --actionTicks <= 0) {
			if (getActionState() == ActionState.THREESOME_CUM) {
				// Galath releases us when the synchronized scene ends.
				actionTicks = 20;
			} else if (getActionState() == ActionState.THREESOME_SLOW
				|| getActionState() == ActionState.THREESOME_FAST) {
				/*
				 * These are looping scene states. Do not fall back to NONE
				 * after the initial synchronization window, otherwise the
				 * combined rig returns to its bind pose.
				 */
				actionTicks = 40;
			} else {
				setAction(ActionState.NONE, 0);
			}
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		GalathEntity galath = getGalath();

		if (ownerUuid == null && galath != null && galath.getOwner() != null) {
			setOwnerUuid(galath.getOwner().getUUID());
		}

		if (ownerUuid == null) {
			setOwnerUuid(player.getUUID());
			player.sendOverlayMessage(Component.translatable("message.sexmod.manglelie.bonded"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!ownerUuid.equals(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.sexmod.manglelie.not_owner"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (getActionState() != ActionState.NONE) {
			player.sendOverlayMessage(Component.translatable("message.sexmod.manglelie.busy"));
			return InteractionResult.SUCCESS_SERVER;
		}

		setFollowing(!isFollowing());
		player.sendOverlayMessage(
			Component.translatable(isFollowing()
				? "message.sexmod.manglelie.following"
				: "message.sexmod.manglelie.staying")
		);
		return InteractionResult.SUCCESS_SERVER;
	}

	public void beginThreesome(GalathEntity galath, ServerPlayer player) {
		setGalath(galath);
		setOwnerUuid(player.getUUID());
		/*
		 * The original scene is driven by Manglelie's combined
		 * Galath/Manglelie/player rig. Keeping this entity invisible made
		 * Galath render that rig with the wrong animation file, leaving the
		 * partner and player branches in their bind poses.
		 */
		setInvisible(false);
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		needsSync = true;
		setAction(ActionState.THREESOME_SLOW, 40);
		getNavigation().stop();
	}

	public void setThreesomeFast(boolean fast) {
		setAction(fast ? ActionState.THREESOME_FAST : ActionState.THREESOME_SLOW, 40);
	}

	public void finishThreesome() {
		setAction(ActionState.THREESOME_CUM, 70);
	}

	public void endThreesome() {
		setInvisible(false);
		setNoGravity(false);
		setDeltaMovement(Vec3.ZERO);
		needsSync = true;
		setAction(ActionState.NONE, 0);
	}

	private void setAction(ActionState action, int durationTicks) {
		entityData.set(ACTION_STATE, action.ordinal());
		actionTicks = durationTicks;
	}

	public ActionState getActionState() {
		return ActionState.byOrdinal(entityData.get(ACTION_STATE));
	}

	public boolean isFollowing() {
		return entityData.get(FOLLOWING);
	}

	public boolean usesCombinedSceneModel() {
		return getActionState().isScene();
	}

	private void setFollowing(boolean following) {
		entityData.set(FOLLOWING, following);
	}

	private void setOwnerUuid(UUID uuid) {
		ownerUuid = uuid;
		entityData.set(OWNER_UUID, Optional.of(EntityReference.<LivingEntity>of(uuid)));
	}

	private void setGalath(GalathEntity galath) {
		galathUuid = galath.getUUID();
		entityData.set(GALATH_UUID, Optional.of(EntityReference.<LivingEntity>of(galathUuid)));
	}

	@Nullable
	public GalathEntity getGalath() {
		UUID uuid;

		if (level().isClientSide()) {
			EntityReference<LivingEntity> reference = entityData.get(GALATH_UUID).orElse(null);
			uuid = reference == null ? null : reference.getUUID();
		} else {
			uuid = galathUuid;
		}

		if (uuid == null) {
			return null;
		}

		List<GalathEntity> galaths = level().getEntities(
			EntityTypeTest.forClass(GalathEntity.class),
			getBoundingBox().inflate(64.0),
			entity -> uuid.equals(entity.getUUID())
		);
		return galaths.isEmpty() ? null : galaths.get(0);
	}

	@Nullable
	private GalathEntity findNearestGalath() {
		return level().getEntities(
				EntityTypeTest.forClass(GalathEntity.class),
				getBoundingBox().inflate(48.0),
				GalathEntity::isAlive
			)
			.stream()
			.min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b)))
			.orElse(null);
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("ManglelieFollowing", isFollowing());

		if (ownerUuid != null) output.store("ManglelieOwner", UUIDUtil.CODEC, ownerUuid);
		if (galathUuid != null) output.store("ManglelieGalath", UUIDUtil.CODEC, galathUuid);
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setFollowing(!input.contains("ManglelieFollowing") || input.getBooleanOr("ManglelieFollowing", true));

		if (input.contains("ManglelieOwner")) setOwnerUuid(input.read("ManglelieOwner", UUIDUtil.CODEC).orElseThrow());
		if (input.contains("ManglelieGalath")) {
			galathUuid = input.read("ManglelieGalath", UUIDUtil.CODEC).orElseThrow();
			entityData.set(GALATH_UUID, Optional.of(EntityReference.<LivingEntity>of(galathUuid)));
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (getActionState().isScene()) {
			return false;
		}

		setAction(ActionState.SCARED_RUN, 45);
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}


	/// Server-side movement measurement: position deltas are only reliable where
	/// every tick is authoritative, so walking state is measured here and shared
	/// through tracked data (the client-side motion channel is dead for remote mobs).
	private void updateServerWalkState() {
		double dx = getX() - xo;
		double dy = getY() - yo;
		double dz = getZ() - zo;
		float blocksPerTick = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
		int state;

		if (blocksPerTick > 0.15f) {
			state = 3;
		} else if (blocksPerTick > 0.075f) {
			state = 2;
		} else if (blocksPerTick > 0.01f) {
			state = 1;
		} else {
			state = 0;
		}

		entityData.set(WALK_STATE, state);

		// Share the server-side measured movement speed (blocks per second,
		// smoothed) so the client controller can sync the stride to the ground.
		float blocksPerSecond = blocksPerTick * 20f;
		float previous = entityData.get(WALK_SPEED);
		float smoothed = blocksPerSecond > 0.1f
			? previous * 0.6f + blocksPerSecond * 0.4f
			: previous * 0.2f;

		if (state == 0 && smoothed < 0.25f) {
			smoothed = 0.0f;
		}

		entityData.set(WALK_SPEED, smoothed);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		AnimationController<ManglelieEntity> controller =
			new AnimationController<>("movement", 4, state -> {
			ActionState action = getActionState();

			if (action != ActionState.NONE) {
				return state.setAndContinue(action.animation);
			}

			int walkState = entityData.get(WALK_STATE);
			double walkSpeed = entityData.get(WALK_SPEED);

			if (walkState != 0) {
				state.setControllerSpeed((float)net.minecraft.util.Mth.clamp(
					walkSpeed / 2.4,
					0.4f,
					3.5f
				));
			}

			return switch (walkState) {
				case 3 -> state.setAndContinue(RUN);
				case 1 -> state.setAndContinue(WALK);
				default -> state.setAndContinue(IDLE);
			};
		});

		/*
		 * The original animation file uses sound keyframes as scene/particle
		 * markers (cs0, sexui, pound, and similar). Registering the handler is
		 * still required by GeckoLib even when those markers are coordinated
		 * by Galath's scene controller.
		 */
		controller.setSoundKeyframeHandler(event -> {
		});
		controllers.add(controller);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animationCache;
	}

	private final class ActionLockGoal extends Goal {
		private ActionLockGoal() {
			setFlags(EnumSet.allOf(Goal.Flag.class));
		}

		@Override
		public boolean canUse() {
			return getActionState() != ActionState.NONE;
		}

		@Override
		public boolean canContinueToUse() {
			return getActionState() != ActionState.NONE;
		}
	}

	private final class FollowGalathGoal extends Goal {
		@Nullable
		private GalathEntity galath;

		@Override
		public boolean canUse() {
			galath = getGalath();
			return isFollowing()
				&& getActionState() == ActionState.NONE
				&& galath != null
				&& distanceToSqr(galath) > 6.25;
		}

		@Override
		public boolean canContinueToUse() {
			return isFollowing()
				&& getActionState() == ActionState.NONE
				&& galath != null
				&& galath.isAlive()
				&& distanceToSqr(galath) > 3.0;
		}

		@Override
		public void tick() {
			if (galath == null) {
				return;
			}

			if (distanceToSqr(galath) > 625.0) {
				snapTo(galath.getX(), galath.getY(), galath.getZ(), getYRot(), getXRot());
			} else {
				getNavigation().moveTo(galath, 1.05);
			}
		}
	}

	public enum ActionState {
		NONE(IDLE),
		SCARED_RUN(SCARED_RUN_ANIMATION),
		SIT_ON_GALATH(RawAnimation.begin().thenLoop("animation.manglelie.sit_on_galath")),
		HUG(RawAnimation.begin().thenLoop("animation.shared.double_holding_soft")),
		THREESOME_SLOW(RawAnimation.begin().thenLoop("animation.shared.double_holding_slow")),
		THREESOME_FAST(RawAnimation.begin().thenLoop("animation.shared.double_holding_hard")),
		THREESOME_CUM(
			RawAnimation.begin().thenPlayAndHold("animation.shared.double_holding_cum")
		);

		private final RawAnimation animation;

		ActionState(RawAnimation animation) {
			this.animation = animation;
		}

		private static ActionState byOrdinal(int ordinal) {
			ActionState[] values = values();
			return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
		}

		private boolean isScene() {
			return this == THREESOME_SLOW || this == THREESOME_FAST || this == THREESOME_CUM;
		}
	}
}

package com.trolmastercard.sexmod.port.entity;

import com.trolmastercard.sexmod.port.network.GalathPayloads;
import com.trolmastercard.sexmod.port.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GalathEntity extends PathfinderMob implements GeoEntity {
	private static final Map<UUID, GalathEntity> ACTIVE_SCENES = new HashMap<>();

	private static final EntityDataAccessor<Integer> NPC_MODE =
		SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ACTION_STATE =
		SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ACTION_TICKS =
		SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SCENE_AROUSAL =
		SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> OWNER_UUID =
		SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> SCENE_PLAYER_UUID =
		SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);

	private static final EntityDataAccessor<Integer> WALK_STATE =
		SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> WALK_SPEED =
		SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.FLOAT);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.galath.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.galath.walk");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.galath.running");
	private static final RawAnimation THREESOME_LICK =
		RawAnimation.begin().thenLoop("animation.galath.pussy_licking");
	private static final RawAnimation THREESOME_BACK =
		RawAnimation.begin().thenPlayAndHold("animation.galath.pussy_licking_back");

	@Nullable
	private UUID ownerUuid;
	private BlockPos homePos;
	@Nullable
	private UUID scenePlayerUuid;
	@Nullable
	private Vec3 sceneReturnPos;
	private float sceneReturnYaw;
	private float sceneReturnPitch;
	private boolean scenePlayerWasInvisible;
	private boolean scenePlayerHadNoGravity;
	private boolean scenePlayerWasInvulnerable;
	private boolean scenePlayerWasNoClip;
	private double scenePlayerX;
	private double scenePlayerY;
	private double scenePlayerZ;
	private float scenePlayerYaw;
	private float scenePlayerPitch;
	private int sceneLoopTick;
	private boolean sceneFastHeld;
	private int lastOwnerAttackedTime;
	@Nullable
	private ManglelieEntity sceneManglelie;
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

	public GalathEntity(EntityType<? extends PathfinderMob> type, Level world) {
		super(type, world);
		homePos = blockPosition();
	}

	public static AttributeSupplier.Builder createGalathAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 110.0)
			.add(Attributes.MOVEMENT_SPEED, 0.6)
			.add(Attributes.ATTACK_DAMAGE, 12.0)
			.add(Attributes.ARMOR, 8.0)
			.add(Attributes.FOLLOW_RANGE, 50.0)
			.add(Attributes.FLYING_SPEED, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(WALK_STATE, 0);
		builder.define(WALK_SPEED, 0.0f);
		builder.define(NPC_MODE, NpcMode.WANDER.ordinal());
		builder.define(ACTION_STATE, ActionState.NONE.ordinal());
		builder.define(ACTION_TICKS, 0);
		builder.define(SCENE_AROUSAL, 0.0f);
		builder.define(OWNER_UUID, Optional.<EntityReference<LivingEntity>>empty());
		builder.define(SCENE_PLAYER_UUID, Optional.<EntityReference<LivingEntity>>empty());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new ActionLockGoal());
		goalSelector.addGoal(2, new GalathMeleeAttackGoal());
		goalSelector.addGoal(3, new FollowOwnerGoal());
		goalSelector.addGoal(5, new RandomStrollGoal(this, 0.65) {
			@Override
			public boolean canUse() {
				return getNpcMode() == NpcMode.WANDER && super.canUse();
			}
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new DefendOwnerGoal());
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

		ActionState action = getActionState();

		if (action == ActionState.RIDE) {
			tickRide();
		}

		if (action.isScene()) {
			maintainScenePlayer();
			tickSceneArousal(action);

			if (action == ActionState.THREESOME_SLOW || action == ActionState.THREESOME_FAST) {
				maintainManglelie();
			}
		}

		int ticks = getActionTicks();

		if (ticks > 0) {
			entityData.set(ACTION_TICKS, ticks - 1);

			if (ticks <= 1) {
				finishAction(action);
			}
		}

		if (getNpcMode() == NpcMode.STAY && action == ActionState.NONE) {
			getNavigation().stop();
			setDeltaMovement(new Vec3(0.0, getDeltaMovement().y, 0.0));
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (ownerUuid == null) {
			setOwnerUuid(player.getUUID());
			homePos = blockPosition();
			setPersistenceRequired();
			startAction(ActionState.SUMMON);
			playGalathSound("dialog");
			player.sendOverlayMessage(Component.translatable("message.sexmod.galath.bonded"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!ownerUuid.equals(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.sexmod.galath.not_owner"));
			playGalathSound("hmph");
			return InteractionResult.SUCCESS_SERVER;
		}

		if (getActionState() != ActionState.NONE) {
			player.sendOverlayMessage(Component.translatable("message.sexmod.galath.busy"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, new GalathPayloads.OpenMenu(getId()));
			playGalathSound("huh");
		}

		return InteractionResult.SUCCESS_SERVER;
	}

	public void handleMenuAction(ServerPlayer player, @Nullable MenuAction action) {
		if (action == null
			|| ownerUuid == null
			|| !ownerUuid.equals(player.getUUID())
			|| getActionState() != ActionState.NONE) {
			return;
		}

		switch (action) {
			case FOLLOW -> {
				setNpcMode(NpcMode.FOLLOW);
				player.sendOverlayMessage(Component.translatable("message.sexmod.galath.following"));
			}
			case STAY -> {
				setNpcMode(NpcMode.STAY);
				getNavigation().stop();
				player.sendOverlayMessage(Component.translatable("message.sexmod.galath.staying"));
			}
			case SET_HOME -> {
				homePos = blockPosition();
				player.sendOverlayMessage(Component.translatable("message.sexmod.galath.home_set"));
			}
			case COWGIRL -> beginScene(player, SceneType.COWGIRL);
			case ANAL -> beginScene(player, SceneType.ANAL);
			case THREESOME -> beginScene(player, SceneType.THREESOME);
			case RIDE -> beginRide(player);
		}
	}

	private void beginRide(ServerPlayer player) {
		setNpcMode(NpcMode.STAY);
		startAction(ActionState.RIDE);
		player.startRiding(this, true, true);
	}

	private void tickRide() {
		if (!(getFirstPassenger() instanceof Player rider)) {
			setNoGravity(false);
			return;
		}

		setNoGravity(true);
		setYRot(rider.getYRot());
		setYBodyRot(rider.getYRot());
		setYHeadRot(rider.getYRot());
		float forward = rider.zza;
		float sideways = rider.xxa;
		double yaw = Math.toRadians(rider.getYRot());
		double speed = forward < 0.0f ? 0.18 : 0.32;
		double velocityX = (-Math.sin(yaw) * forward + Math.cos(yaw) * sideways * 0.65) * speed;
		double velocityZ = (Math.cos(yaw) * forward + Math.sin(yaw) * sideways * 0.65) * speed;
		double velocityY = rider.yya * 0.24;
		setDeltaMovement(new Vec3(velocityX, velocityY, velocityZ));
		needsSync = true;
	}

	private boolean beginScene(ServerPlayer player, SceneType type) {
		if (player.level() != level() || !player.isAlive()) {
			return false;
		}

		if (type == SceneType.THREESOME) {
			sceneManglelie = findManglelie();

			if (sceneManglelie == null) {
				player.sendOverlayMessage(Component.translatable("message.sexmod.galath.no_manglelie"));
				return false;
			}
		}

		GalathEntity oldScene = ACTIVE_SCENES.get(player.getUUID());

		if (oldScene != null && oldScene != this) {
			oldScene.endScene();
		}

		scenePlayerUuid = player.getUUID();
		entityData.set(SCENE_PLAYER_UUID, Optional.of(EntityReference.<LivingEntity>of(scenePlayerUuid)));
		entityData.set(SCENE_AROUSAL, 0.0f);
		sceneLoopTick = 0;
		sceneFastHeld = false;
		sceneReturnPos = player.position();
		sceneReturnYaw = player.getYRot();
		sceneReturnPitch = player.getXRot();
		scenePlayerWasInvisible = player.isInvisible();
		scenePlayerHadNoGravity = player.isNoGravity();
		scenePlayerWasInvulnerable = player.isInvulnerable();
		scenePlayerWasNoClip = player.noPhysics;

		float facing = player.getYRot() + 180.0f;
		setYRot(facing);
		setYBodyRot(facing);
		setYHeadRot(facing);
		getNavigation().stop();

		double radians = Math.toRadians(facing);
		double distance = type == SceneType.ANAL
			? 0.72
			: type == SceneType.COWGIRL ? 1.27 : 0.82;
		scenePlayerX = getX() - Math.sin(radians) * distance;
		// Cowgirl uses a lower first-person anchor than the other scenes.
		// This is the authoritative player/camera position, so the offset
		// remains effective even before the animated Head2 matrix is ready.
		scenePlayerY = type == SceneType.COWGIRL ? getY() - 1.18 : getY();
		scenePlayerZ = getZ() + Math.cos(radians) * distance;

		if (type == SceneType.ANAL) {
			// Match the original off-centre view: move the camera a little
			// toward Galath's right side instead of leaving it inside the
			// centre line of the animated player rig.
			double sideOffset = 0.28;
			scenePlayerX += Math.cos(radians) * sideOffset;
			scenePlayerZ += Math.sin(radians) * sideOffset;
		}

		scenePlayerYaw = type == SceneType.COWGIRL ? facing + 180.0f : facing;
		if (type == SceneType.ANAL) {
			scenePlayerYaw -= 18.0f;
		}
		scenePlayerPitch = type == SceneType.COWGIRL ? 18.0f : 48.0f;

		ACTIVE_SCENES.put(player.getUUID(), this);
		player.setNoGravity(true);
		player.setInvulnerable(true);
		player.setInvisible(true);
		player.noPhysics = true;
		player.setDeltaMovement(Vec3.ZERO);
		placeScenePlayer(player, true);
		ServerPlayNetworking.send(player, new GalathPayloads.OpenSceneControls(getId()));

		if (type == SceneType.THREESOME) {
			sceneManglelie.beginThreesome(this, player);
			startAction(ActionState.THREESOME_SLOW);
		} else if (type == SceneType.ANAL) {
			startAction(ActionState.ANAL_SLOW);
		} else {
			startAction(ActionState.COWGIRL_INTRO);
		}

		return true;
	}

	private void tickSceneArousal(ActionState action) {
		if (!action.isControllableLoop()) {
			return;
		}

		sceneLoopTick++;
		float increment = action.isFastLoop() ? 0.0085f : 0.004f;
		entityData.set(SCENE_AROUSAL, Math.min(1.0f, getSceneArousal() + increment));
	}

	private void finishAction(ActionState action) {
		switch (action) {
			case SUMMON, ATTACK_0, ATTACK_1, ATTACK_2 -> resetAction();
			case COWGIRL_INTRO -> startAction(ActionState.COWGIRL_SLOW);
			case COWGIRL_SLOW, COWGIRL_FAST, ANAL_SLOW, ANAL_FAST,
				THREESOME_SLOW, THREESOME_FAST -> startAction(action);
			case COWGIRL_CUM, ANAL_CUM, THREESOME_CUM -> endScene();
			case RIDE -> {
				if (getPassengers().isEmpty()) {
					resetAction();
					setNpcMode(NpcMode.FOLLOW);
				} else {
					startAction(ActionState.RIDE);
				}
			}
			default -> resetAction();
		}
	}

	public void handleSceneCommand(ServerPlayer player, @Nullable SceneCommand command) {
		if (command == null
			|| scenePlayerUuid == null
			|| !scenePlayerUuid.equals(player.getUUID())
			|| !getActionState().isScene()) {
			return;
		}

		ActionState action = getActionState();

		switch (command) {
			case SLOW -> {
				sceneFastHeld = false;
				if (action == ActionState.COWGIRL_FAST) startAction(ActionState.COWGIRL_SLOW);
				if (action == ActionState.ANAL_FAST) startAction(ActionState.ANAL_SLOW);
				if (action == ActionState.THREESOME_FAST) {
					startAction(ActionState.THREESOME_SLOW);
					if (sceneManglelie != null) sceneManglelie.setThreesomeFast(false);
				}
			}
			case FAST -> {
				sceneFastHeld = true;
				if (action == ActionState.COWGIRL_SLOW) startAction(ActionState.COWGIRL_FAST);
				if (action == ActionState.ANAL_SLOW) startAction(ActionState.ANAL_FAST);
				if (action == ActionState.THREESOME_SLOW) {
					startAction(ActionState.THREESOME_FAST);
					if (sceneManglelie != null) sceneManglelie.setThreesomeFast(true);
				}
			}
			case FINISH -> {
				if (getSceneArousal() < 0.999f) {
					return;
				}

				if (action == ActionState.COWGIRL_SLOW || action == ActionState.COWGIRL_FAST) {
					startAction(ActionState.COWGIRL_CUM);
				} else if (action == ActionState.ANAL_SLOW || action == ActionState.ANAL_FAST) {
					startAction(ActionState.ANAL_CUM);
				} else if (action == ActionState.THREESOME_SLOW || action == ActionState.THREESOME_FAST) {
					startAction(ActionState.THREESOME_CUM);
					if (sceneManglelie != null) sceneManglelie.finishThreesome();
				}
				playGalathSound("orgasm");
			}
			case EXIT -> endScene();
		}
	}

	private void maintainScenePlayer() {
		ServerPlayer player = getSceneServerPlayer();

		if (player == null || !player.isAlive() || player.level() != level()) {
			endScene();
			return;
		}

		player.setDeltaMovement(Vec3.ZERO);
		player.noPhysics = true;

		if (player.distanceToSqr(scenePlayerX, scenePlayerY, scenePlayerZ) > 0.0025) {
			placeScenePlayer(player, false);
		}
	}

	private void placeScenePlayer(ServerPlayer player, boolean rotate) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		player.teleportTo(
			serverLevel,
			scenePlayerX,
			scenePlayerY,
			scenePlayerZ,
			Set.of(),
			rotate ? scenePlayerYaw : player.getYRot(),
			rotate ? scenePlayerPitch : player.getXRot(),
			true
		);
	}

	private void maintainManglelie() {
		if (sceneManglelie == null || !sceneManglelie.isAlive()) {
			endScene();
			return;
		}

		sceneManglelie.setPos(getX(), getY(), getZ());
		/*
		 * body2 and steve already carry a 180-degree rotation in the
		 * combined geometry. Applying another entity-level half turn moves
		 * that entire rig to the opposite side of Galath on modern
		 * GeckoLib. Keep the entity bases aligned and let the authored root
		 * rotations establish the three-person pose.
		 */
		float manglelieYaw = getYRot();
		sceneManglelie.setYRot(manglelieYaw);
		sceneManglelie.setYBodyRot(manglelieYaw);
		sceneManglelie.setYHeadRot(manglelieYaw);
		sceneManglelie.setDeltaMovement(Vec3.ZERO);
		sceneManglelie.needsSync = true;
	}

	private void endScene() {
		ServerPlayer player = getSceneServerPlayer();
		entityData.set(SCENE_AROUSAL, 0.0f);

		if (player != null) {
			ServerPlayNetworking.send(player, new GalathPayloads.CloseSceneControls(getId()));
			player.setInvisible(scenePlayerWasInvisible);
			player.setNoGravity(scenePlayerHadNoGravity);
			player.setInvulnerable(scenePlayerWasInvulnerable);
			player.noPhysics = scenePlayerWasNoClip;
			player.setDeltaMovement(Vec3.ZERO);

			if (sceneReturnPos != null && level() instanceof ServerLevel serverLevel) {
				player.teleportTo(
					serverLevel,
					sceneReturnPos.x,
					sceneReturnPos.y,
					sceneReturnPos.z,
					Set.of(),
					sceneReturnYaw,
					sceneReturnPitch,
					true
				);
			}
		}

		if (sceneManglelie != null) {
			sceneManglelie.endThreesome();
			sceneManglelie = null;
		}

		if (scenePlayerUuid != null) {
			ACTIVE_SCENES.remove(scenePlayerUuid, this);
		}

		scenePlayerUuid = null;
		entityData.set(SCENE_PLAYER_UUID, Optional.empty());
		sceneReturnPos = null;
		resetAction();
	}

	public static void endSceneForDisconnect(ServerPlayer player) {
		GalathEntity galath = ACTIVE_SCENES.get(player.getUUID());

		if (galath != null) {
			galath.endScene();
		}
	}

	@Nullable
	private ServerPlayer getSceneServerPlayer() {
		if (scenePlayerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		return serverLevel.getServer().getPlayerList().getPlayer(scenePlayerUuid);
	}

	@Nullable
	private ManglelieEntity findManglelie() {
		AABB box = getBoundingBox().inflate(24.0);
		return level().getEntities(
				EntityTypeTest.forClass(ManglelieEntity.class),
				box,
				ManglelieEntity::isAlive
			)
			.stream()
			.min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b)))
			.orElse(null);
	}

	private void startAction(ActionState action) {
		entityData.set(ACTION_STATE, action.ordinal());
		entityData.set(ACTION_TICKS, action.durationTicks);
		getNavigation().stop();
	}

	private void resetAction() {
		entityData.set(ACTION_STATE, ActionState.NONE.ordinal());
		entityData.set(ACTION_TICKS, 0);
	}

	private void playGalathSound(String category) {
		int count = ModSounds.galathCount(category);

		if (count > 0) {
			playSound(ModSounds.galath(category, random.nextInt(count)), 0.9f, 1.0f);
		}
	}

	@Nullable
	public Player getOwner() {
		UUID uuid;

		if (level().isClientSide()) {
			EntityReference<LivingEntity> reference = entityData.get(OWNER_UUID).orElse(null);
			uuid = reference == null ? null : reference.getUUID();
		} else {
			uuid = ownerUuid;
		}

		return uuid == null ? null : level().getPlayerByUUID(uuid);
	}

	@Nullable
	public Player getScenePlayer() {
		UUID uuid;

		if (level().isClientSide()) {
			EntityReference<LivingEntity> reference = entityData.get(SCENE_PLAYER_UUID).orElse(null);
			uuid = reference == null ? null : reference.getUUID();
		} else {
			uuid = scenePlayerUuid;
		}

		return uuid == null ? null : level().getPlayerByUUID(uuid);
	}

	public float getSceneArousal() {
		return entityData.get(SCENE_AROUSAL);
	}

	public ActionState getActionState() {
		return ActionState.byOrdinal(entityData.get(ACTION_STATE));
	}

	public int getActionTicks() {
		return entityData.get(ACTION_TICKS);
	}

	public NpcMode getNpcMode() {
		return NpcMode.byOrdinal(entityData.get(NPC_MODE));
	}

	private void setNpcMode(NpcMode mode) {
		entityData.set(NPC_MODE, mode.ordinal());
	}

	private void setOwnerUuid(UUID uuid) {
		ownerUuid = uuid;
		entityData.set(OWNER_UUID, Optional.of(EntityReference.<LivingEntity>of(uuid)));
	}

	public boolean usesCombinedManglelieModel() {
		return getActionState() == ActionState.THREESOME_SLOW
			|| getActionState() == ActionState.THREESOME_FAST
			|| getActionState() == ActionState.THREESOME_CUM;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("GalathMode", getNpcMode().ordinal());
		output.putInt("GalathHomeX", homePos.getX());
		output.putInt("GalathHomeY", homePos.getY());
		output.putInt("GalathHomeZ", homePos.getZ());

		if (ownerUuid != null) {
			output.store("GalathOwner", UUIDUtil.CODEC, ownerUuid);
		}
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setNpcMode(NpcMode.byOrdinal(input.getIntOr("GalathMode", 0)));
		homePos = input.getInt("GalathHomeX").isPresent()
			? new BlockPos(input.getIntOr("GalathHomeX", 0), input.getIntOr("GalathHomeY", 0), input.getIntOr("GalathHomeZ", 0))
			: blockPosition();

		if (input.contains("GalathOwner")) {
			setOwnerUuid(input.read("GalathOwner", UUIDUtil.CODEC).orElseThrow());
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (getActionState().isScene()) {
			return false;
		}

		return super.hurtServer(level, source, amount);
	}

	@Override
	public void die(DamageSource damageSource) {
		if (!level().isClientSide() && scenePlayerUuid != null) {
			endScene();
		}

		super.die(damageSource);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.galath("breathing", random.nextInt(ModSounds.galathCount("breathing")));
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.galath("ahh", random.nextInt(ModSounds.galathCount("ahh")));
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.galath("aaa", random.nextInt(ModSounds.galathCount("aaa")));
	}

	@Override
	public int getAmbientSoundInterval() {
		return 180;
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
		AnimationController<GalathEntity> controller = new AnimationController<>("movement", 4, state -> {
			ActionState action = getActionState();

			/*
			 * pussy_licking_forward is an authored transition, not the fast
			 * loop. Re-selecting it whenever Shift changes state snaps Galath
			 * to its first keyframe. Keep one continuous loop and alter only
			 * the playback speed.
			 */
			if (action == ActionState.THREESOME_SLOW
				|| action == ActionState.THREESOME_FAST) {
				state.setControllerSpeed(
					action == ActionState.THREESOME_FAST ? 1.65f : 1.0f
				);
				return state.setAndContinue(THREESOME_LICK);
			}

			if (action == ActionState.THREESOME_CUM) {
				return state.setAndContinue(THREESOME_BACK);
			}

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
		controller.setSoundKeyframeHandler(event -> {
			String marker = event.keyframeData().getSound().toLowerCase();

			if (marker.contains("orgasm") || marker.contains("cum")) playGalathSound("orgasm");
			else if (marker.contains("giggle")) playGalathSound("giggle");
			else if (marker.contains("moan")) playGalathSound("moan");
			else if (marker.contains("charge")) playGalathSound(
				marker.contains("strong") ? "strongcharge" : "lightcharge"
			);
			else if (marker.contains("breath")) playGalathSound("breathing");
		});
		controllers.add(controller);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animationCache;
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction positionUpdater) {
		super.positionRider(passenger, positionUpdater);

		if (passenger instanceof Player player) {
			setYRot(player.getYRot());
			setYBodyRot(player.getYRot());
			setYHeadRot(player.getYRot());
		}
	}

	private final class ActionLockGoal extends Goal {
		private ActionLockGoal() {
			setFlags(EnumSet.allOf(Goal.Flag.class));
		}

		@Override
		public boolean canUse() {
			return getActionState() != ActionState.NONE && getActionState() != ActionState.RIDE;
		}

		@Override
		public boolean canContinueToUse() {
			return getActionState() != ActionState.NONE && getActionState() != ActionState.RIDE;
		}
	}

	private final class GalathMeleeAttackGoal extends MeleeAttackGoal {
		private GalathMeleeAttackGoal() {
			super(GalathEntity.this, 1.15, true);
		}

		@Override
		public boolean canUse() {
			return getActionState() == ActionState.NONE && super.canUse();
		}

		@Override
		protected void checkAndPerformAttack(LivingEntity target) {
			if (getActionState() == ActionState.NONE && target.isAttackable()) {
				startAction(switch (random.nextInt(3)) {
					case 1 -> ActionState.ATTACK_1;
					case 2 -> ActionState.ATTACK_2;
					default -> ActionState.ATTACK_0;
				});
			}

			super.checkAndPerformAttack(target);
		}
	}

	private final class DefendOwnerGoal extends Goal {
		@Nullable
		private LivingEntity attacker;

		private DefendOwnerGoal() {
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			Player owner = getOwner();

			if (owner == null || owner.getLastHurtByMob() == null) {
				return false;
			}

			int attackedTime = owner.getLastHurtByMobTimestamp();

			if (attackedTime == lastOwnerAttackedTime) {
				return false;
			}

			attacker = owner.getLastHurtByMob();
			lastOwnerAttackedTime = attackedTime;
			return attacker != null && GalathEntity.this.canAttack(attacker);
		}

		@Override
		public void start() {
			setTarget(attacker);
		}
	}

	private final class FollowOwnerGoal extends Goal {
		@Override
		public boolean canUse() {
			Player owner = getOwner();
			return getNpcMode() == NpcMode.FOLLOW
				&& getActionState() == ActionState.NONE
				&& owner != null
				&& distanceToSqr(owner) > 9.0;
		}

		@Override
		public boolean canContinueToUse() {
			Player owner = getOwner();
			return getNpcMode() == NpcMode.FOLLOW
				&& getActionState() == ActionState.NONE
				&& owner != null
				&& distanceToSqr(owner) > 4.0;
		}

		@Override
		public void tick() {
			Player owner = getOwner();

			if (owner == null) {
				return;
			}

			if (distanceToSqr(owner) > 576.0) {
				snapTo(owner.getX(), owner.getY(), owner.getZ(), getYRot(), getXRot());
			} else {
				getNavigation().moveTo(owner, 1.1);
			}
		}
	}

	private enum SceneType {
		COWGIRL,
		ANAL,
		THREESOME
	}

	public enum NpcMode {
		WANDER,
		FOLLOW,
		STAY;

		private static NpcMode byOrdinal(int ordinal) {
			NpcMode[] values = values();
			return ordinal >= 0 && ordinal < values.length ? values[ordinal] : WANDER;
		}
	}

	public enum MenuAction {
		COWGIRL,
		ANAL,
		THREESOME,
		RIDE,
		FOLLOW,
		STAY,
		SET_HOME;

		@Nullable
		public static MenuAction byId(int id) {
			MenuAction[] values = values();
			return id >= 0 && id < values.length ? values[id] : null;
		}
	}

	public enum SceneCommand {
		SLOW,
		FAST,
		FINISH,
		EXIT;

		@Nullable
		public static SceneCommand byId(int id) {
			SceneCommand[] values = values();
			return id >= 0 && id < values.length ? values[id] : null;
		}
	}

	public enum ActionState {
		NONE(0, IDLE),
		SUMMON(55, RawAnimation.begin().thenPlay("animation.galath.summon")),
		ATTACK_0(18, RawAnimation.begin().thenPlay("animation.galath.attack0")),
		ATTACK_1(20, RawAnimation.begin().thenPlay("animation.galath.attack1")),
		ATTACK_2(22, RawAnimation.begin().thenPlay("animation.galath.attack2")),
		COWGIRL_INTRO(115, RawAnimation.begin().thenPlay("animation.galath.rape_intro")),
		COWGIRL_SLOW(40, RawAnimation.begin().thenLoop("animation.galath.rape1")),
		COWGIRL_FAST(40, RawAnimation.begin().thenLoop("animation.galath.rape2")),
		COWGIRL_CUM(115, RawAnimation.begin().thenPlay("animation.galath.rape_cum")),
		ANAL_SLOW(40, RawAnimation.begin().thenLoop("animation.galath.corrupt_slow")),
		ANAL_FAST(30, RawAnimation.begin().thenLoop("animation.galath.corrupt_hard")),
		ANAL_CUM(90, RawAnimation.begin().thenPlay("animation.galath.corrupt_cum")),
		THREESOME_SLOW(40, RawAnimation.begin().thenLoop("animation.galath.pussy_licking")),
		THREESOME_FAST(30, RawAnimation.begin().thenLoop("animation.galath.pussy_licking_forward")),
		THREESOME_CUM(80, RawAnimation.begin().thenPlay("animation.galath.pussy_licking_back")),
		RIDE(40, RawAnimation.begin().thenLoop("animation.galath.controlled_flight"));

		private final int durationTicks;
		private final RawAnimation animation;

		ActionState(int durationTicks, RawAnimation animation) {
			this.durationTicks = durationTicks;
			this.animation = animation;
		}

		private static ActionState byOrdinal(int ordinal) {
			ActionState[] values = values();
			return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
		}

		public boolean isScene() {
			return this == COWGIRL_INTRO
				|| this == COWGIRL_SLOW
				|| this == COWGIRL_FAST
				|| this == COWGIRL_CUM
				|| this == ANAL_SLOW
				|| this == ANAL_FAST
				|| this == ANAL_CUM
				|| this == THREESOME_SLOW
				|| this == THREESOME_FAST
				|| this == THREESOME_CUM;
		}

		public boolean isControllableLoop() {
			return this == COWGIRL_SLOW
				|| this == COWGIRL_FAST
				|| this == ANAL_SLOW
				|| this == ANAL_FAST
				|| this == THREESOME_SLOW
				|| this == THREESOME_FAST;
		}

		public boolean isFastLoop() {
			return this == COWGIRL_FAST || this == ANAL_FAST || this == THREESOME_FAST;
		}
	}
}

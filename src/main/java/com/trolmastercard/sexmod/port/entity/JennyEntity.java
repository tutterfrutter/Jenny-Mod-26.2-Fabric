package com.trolmastercard.sexmod.port.entity;

import com.trolmastercard.sexmod.port.network.JennyPayloads;
import com.trolmastercard.sexmod.port.network.JennyNetworking;
import com.trolmastercard.sexmod.port.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.UUIDUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class JennyEntity extends PathfinderMob implements GeoEntity {
	private static final Map<UUID, JennyEntity> ACTIVE_SCENES = new HashMap<>();

	private static final EntityDataAccessor<Integer> NPC_MODE =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ACTION_STATE =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ACTION_TICKS =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SCENE_AROUSAL =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> DRESSED =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> OWNER_UUID =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> SCENE_PLAYER_UUID =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
	private static final EntityDataAccessor<ItemStack> OFFERED_ITEM =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Integer> WALK_STATE =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> WALK_SPEED =
		SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.FLOAT);


	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.jenny.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.jenny.walk");
	private static final RawAnimation FAST_WALK = RawAnimation.begin().thenLoop("animation.jenny.fastwalk");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.jenny.run");
	private static final RawAnimation WAVE = RawAnimation.begin().thenPlay("animation.jenny.wave");
	private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.jenny.sit");
	private static final RawAnimation PAYMENT = RawAnimation.begin().thenPlay("animation.jenny.payment");
	private static final RawAnimation THROW_PEARL = RawAnimation.begin().thenPlay("animation.jenny.throwpearl");
	private static final RawAnimation ATTACK_0 = RawAnimation.begin().thenPlay("animation.jenny.attack0");
	private static final RawAnimation ATTACK_1 = RawAnimation.begin().thenPlay("animation.jenny.attack1");
	private static final RawAnimation ATTACK_2 = RawAnimation.begin().thenPlay("animation.jenny.attack2");
	private static final RawAnimation STRIP = RawAnimation.begin().thenPlay("animation.jenny.strip");
	private static final RawAnimation BLOWJOB_INTRO =
		RawAnimation.begin().thenPlay("animation.jenny.blowjobintro");
	private static final RawAnimation BLOWJOB_SUCK =
		RawAnimation.begin().thenLoop("animation.jenny.blowjobsuck");
	private static final RawAnimation BLOWJOB_THRUST =
		RawAnimation.begin().thenLoop("animation.jenny.blowjobthrust");
	private static final RawAnimation BLOWJOB_CUM =
		RawAnimation.begin().thenPlay("animation.jenny.blowjobcum");
	private static final RawAnimation PAIZURI_START =
		RawAnimation.begin().thenPlay("animation.jenny.paizuri_start");
	private static final RawAnimation PAIZURI_SLOW =
		RawAnimation.begin().thenLoop("animation.jenny.paizuri_slow");
	private static final RawAnimation PAIZURI_FAST =
		RawAnimation.begin().thenLoop("animation.jenny.paizuri_fast");
	private static final RawAnimation PAIZURI_CUM =
		RawAnimation.begin().thenPlay("animation.jenny.paizuri_cum");
	private static final RawAnimation DOGGY_GO_BED =
		RawAnimation.begin().thenPlay("animation.jenny.doggygoonbed");
	private static final RawAnimation DOGGY_START =
		RawAnimation.begin().thenPlay("animation.jenny.doggystart");
	private static final RawAnimation DOGGY_SLOW =
		RawAnimation.begin().thenLoop("animation.jenny.doggyslow");
	private static final RawAnimation DOGGY_FAST =
		RawAnimation.begin().thenLoop("animation.jenny.doggyfast_soft");
	private static final RawAnimation DOGGY_CUM =
		RawAnimation.begin().thenPlay("animation.jenny.doggycum");

	@Nullable
	private UUID ownerUuid;
	private BlockPos homePos;
	private float pendingPearlYaw;
	private float pendingPearlPitch;
	private int lastOwnerAttackedTime;
	private Service pendingService = Service.NONE;
	@Nullable
	private UUID pendingScenePlayerUuid;
	@Nullable
	private UUID paymentPlayerUuid;
	private double paymentLockX;
	private double paymentLockY;
	private double paymentLockZ;
	private float paymentLockYaw;
	private float paymentLockPitch;
	@Nullable
	private Vec3 paymentReturnPos;
	private float paymentReturnYaw;
	private float paymentReturnPitch;
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
	private double sceneCameraBaseX;
	private double sceneCameraBaseY;
	private double sceneCameraBaseZ;
	private float sceneCameraBaseYaw;
	private float scenePlayerYaw;
	private float scenePlayerPitch;
	private static final int SCENE_TRANSITION_DURATION = 24;
	private int sceneTransitionTicks;
	private int sceneLoopTick;
	private boolean sceneFastHeld;
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

	public JennyEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		homePos = blockPosition();
	}

	public static AttributeSupplier.Builder createJennyAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.5)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 30.0)
			.add(Attributes.TEMPT_RANGE, 10.0);
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
		builder.define(DRESSED, true);
		builder.define(OWNER_UUID, Optional.<EntityReference<LivingEntity>>empty());
		builder.define(SCENE_PLAYER_UUID, Optional.<EntityReference<LivingEntity>>empty());
		builder.define(OFFERED_ITEM, ItemStack.EMPTY);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new ActionLockGoal());
		goalSelector.addGoal(1, new StayGoal());
		goalSelector.addGoal(2, new JennyMeleeAttackGoal());
		goalSelector.addGoal(2, new TemptGoal(
			this,
			0.4,
			Ingredient.of(Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.ENDER_PEARL),
			false
		));
		goalSelector.addGoal(3, new DoorInteractGoal(this) {
			private int closeCountdown;

			@Override
			public boolean canUse() {
				return getNpcMode() != NpcMode.STAY && super.canUse();
			}

			@Override
			public void start() {
				super.start();
				closeCountdown = 10;
				setOpen(true);
			}

			@Override
			public boolean canContinueToUse() {
				return closeCountdown > 0;
			}

			@Override
			public void tick() {
				super.tick();
				closeCountdown--;
			}

			@Override
			public void stop() {
				setOpen(false);
				super.stop();
			}
		});
		goalSelector.addGoal(4, new FollowOwnerGoal());
		goalSelector.addGoal(4, new ReturnHomeGoal());
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f, 1.0f));
		goalSelector.addGoal(5, new ModeWanderGoal());
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));

		targetSelector.addGoal(1, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				return getLastHurtByMob() != getOwner() && super.canUse();
			}
		});
		targetSelector.addGoal(2, new DefendOwnerGoal());
	}

	@Override
	public void tick() {
		super.tick();

		if (!level().isClientSide()) {
			updateServerWalkState();
		}

		if (!level().isClientSide() && getActionTicks() > 0) {
			int remainingTicks = getActionTicks();
			ActionState currentAction = getActionState();
			int elapsedTicks = currentAction.durationTicks - remainingTicks;

			if (currentAction.isPartnerScene()) {
				maintainScenePlayer();
			}

			if (currentAction == ActionState.PAYMENT && !maintainPaymentPlayer()) {
				return;
			}

			handleOriginalTimedEvents(currentAction, elapsedTicks);

			if (currentAction == ActionState.THROW_PEARL
				&& remainingTicks == ActionState.THROW_PEARL.durationTicks - 19) {
				throwPendingPearl();
			}

			if (currentAction == ActionState.STRIP
				&& remainingTicks == ActionState.STRIP.durationTicks - 60) {
				setDressed(!isDressed());
			}

			if (currentAction.isControllableLoop()) {
				tickSceneArousal(currentAction);

				if (currentAction.isFastLoop()) {
					entityData.set(ACTION_TICKS, remainingTicks - 1);

					if (remainingTicks <= 1) {
						if (sceneFastHeld) {
							entityData.set(ACTION_TICKS, currentAction.durationTicks);
						} else {
							startAction(currentAction.slowVariant());
						}
					}
				}

				return;
			}

			entityData.set(ACTION_TICKS, remainingTicks - 1);

			if (remainingTicks <= 1) {
				finishAction(currentAction);
			}
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
			player.sendOverlayMessage(Component.translatable("message.sexmod.jenny.bonded"));
			if (player instanceof ServerPlayer serverPlayer) {
				sendJennyDialogueTo(serverPlayer, "jenny.dialogue.hihi");
			}
			waveAndGreet();
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!ownerUuid.equals(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.sexmod.jenny.not_owner"));
			playJennySound("huh");
			return InteractionResult.SUCCESS_SERVER;
		}

		if (getActionState() != ActionState.NONE) {
			if (player instanceof ServerPlayer serverPlayer) {
				sendJennyDialogueTo(serverPlayer, "jenny.dialogue.busy");
			}
			return InteractionResult.SUCCESS_SERVER;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, new JennyPayloads.OpenMenu(getId()));
		}

		return InteractionResult.SUCCESS_SERVER;
	}

	public void handleMenuAction(ServerPlayer player, MenuAction action) {
		if (action == null
			|| ownerUuid == null
			|| !ownerUuid.equals(player.getUUID())
			|| getActionState() != ActionState.NONE) {
			return;
		}

		switch (action) {
			case FOLLOW -> {
				setNpcMode(NpcMode.FOLLOW);
				sendJennyDialogueTo(player, "jenny.dialogue.followme", player.getName().getString());
				playJennySound("happyoh");
			}
			case STOP_FOLLOWING -> {
				setNpcMode(NpcMode.WANDER);
				sendJennyDialogueTo(player, "jenny.dialogue.bye");
				playJennySound("huh");
			}
			case GO_HOME -> {
				setNpcMode(NpcMode.WANDER);
				Vec3 home = Vec3.atBottomCenterOf(homePos);
				getNavigation().moveTo(home.x, home.y, home.z, 0.35);
				sendJennyDialogueTo(player, "jenny.dialogue.bye2");
			}
			case SET_HOME -> {
				homePos = blockPosition();
				player.sendOverlayMessage(Component.translatable("message.sexmod.jenny.home_set"));
				playJennySound("happyoh");
			}
			case BLOWJOB -> beginPaidService(player, Service.BLOWJOB, Items.EMERALD, 3);
			case BOOBJOB -> beginPaidService(player, Service.BOOBJOB, Items.ENDER_PEARL, 2);
			case DOGGY -> beginPaidService(player, Service.DOGGY, Items.DIAMOND, 2);
			case STRIP_OR_DRESS -> {
				if (isDressed()) {
					beginPaidService(player, Service.STRIP, Items.GOLD_INGOT, 1);
				} else {
					pendingService = Service.NONE;
					startAction(ActionState.STRIP);
				}
			}
		}
	}

	private void beginPaidService(ServerPlayer player, Service service, Item paymentItem, int count) {
		if (!takeItems(player, paymentItem, count)) {
			player.sendOverlayMessage(
				Component.translatable(
					"message.sexmod.jenny.missing_payment",
					count,
					new ItemStack(paymentItem).getItemName()
				)
			);
			playJennySound("hmph");
			return;
		}

		pendingService = service;
		pendingScenePlayerUuid = service.isPartnerScene() ? player.getUUID() : null;
		beginPaymentPlayerLock(player);
		entityData.set(OFFERED_ITEM, new ItemStack(paymentItem, count));
		startAction(ActionState.PAYMENT);
	}

	private void beginPaymentPlayerLock(ServerPlayer player) {
		paymentPlayerUuid = player.getUUID();
		paymentReturnPos = player.position();
		paymentReturnYaw = player.getYRot();
		paymentReturnPitch = player.getXRot();
		double outwardX = player.getX() - getX();
		double outwardZ = player.getZ() - getZ();
		double outwardLength = Math.sqrt(outwardX * outwardX + outwardZ * outwardZ);

		if (outwardLength < 0.001) {
			double yawRadians = Math.toRadians(player.getYRot());
			outwardX = Math.sin(yawRadians);
			outwardZ = -Math.cos(yawRadians);
		} else {
			outwardX /= outwardLength;
			outwardZ /= outwardLength;
		}

		// the strip/payment poses lean Jenny toward the payer; 1m placed the
		// first-person camera inside her chest
		double paymentDistance = 1.8;
		double paymentX = getX() + outwardX * paymentDistance;
		double paymentY = getY();
		double paymentZ = getZ() + outwardZ * paymentDistance;
		double lookX = getX() - paymentX;
		double lookZ = getZ() - paymentZ;
		float paymentYaw = (float)Math.toDegrees(Math.atan2(-lookX, lookZ));
		float jennyYaw = paymentYaw + 180.0f;
		// the payment anim leans her over the payer; pitching the camera down
		// buried the view inside her chest. A level, slightly-up look matches
		// the original face-to-face framing.
		float paymentPitch = -5.0f;

		setYRot(jennyYaw);
		setYBodyRot(jennyYaw);
		setYHeadRot(jennyYaw);
		getNavigation().stop();

		if (level() instanceof ServerLevel serverLevel) {
			player.teleportTo(
				serverLevel,
				paymentX,
				paymentY,
				paymentZ,
				Set.of(),
				paymentYaw,
				paymentPitch,
				true
			);
		}

		paymentLockX = paymentX;
		paymentLockY = paymentY;
		paymentLockZ = paymentZ;
		paymentLockYaw = paymentYaw;
		paymentLockPitch = paymentPitch;
	}

	private boolean maintainPaymentPlayer() {
		ServerPlayer player = getPaymentServerPlayer();

		if (player == null || !player.isAlive() || player.level() != level()) {
			clearPaymentPlayerLock();
			pendingService = Service.NONE;
			pendingScenePlayerUuid = null;
			resetAction();
			return false;
		}

		// keep the payer pinned in front of Jenny for the payment animation;
		// zeroing velocity alone does not stop walk packets on 26.2
		if (level() instanceof ServerLevel serverLevel) {
			player.teleportTo(
				serverLevel,
				paymentLockX,
				paymentLockY,
				paymentLockZ,
				Set.of(),
				paymentLockYaw,
				paymentLockPitch,
				false
			);
		}

		player.setDeltaMovement(Vec3.ZERO);
		return true;
	}

	@Nullable
	private ServerPlayer getPaymentServerPlayer() {
		if (paymentPlayerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		return serverLevel.getServer().getPlayerList().getPlayer(paymentPlayerUuid);
	}

	private void clearPaymentPlayerLock() {
		paymentPlayerUuid = null;
		paymentReturnPos = null;
	}

	private boolean takeItems(ServerPlayer player, Item item, int count) {
		if (player.getAbilities().instabuild) {
			return true;
		}

		if (player.getInventory().countItem(item) < count) {
			return false;
		}

		int remaining = count;

		for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);

			if (!stack.is(item)) {
				continue;
			}

			int removed = Math.min(stack.getCount(), remaining);
			stack.shrink(removed);
			remaining -= removed;
		}

		player.getInventory().setChanged();
		return remaining == 0;
	}

	private void waveAndGreet() {
		startAction(ActionState.WAVE);
		playJennySound(random.nextBoolean() ? "giggle" : "happyoh");
	}

	private void startAction(ActionState action) {
		if (getActionState() != action) {
			sceneLoopTick = 0;
		}

		entityData.set(ACTION_STATE, action.ordinal());
		entityData.set(ACTION_TICKS, action.durationTicks);
		getNavigation().stop();
	}

	private void tickSceneArousal(ActionState action) {
		sceneLoopTick++;
		int phase = sceneLoopTick % action.loopPeriodTicks();
		float increment = switch (action) {
			case BLOWJOB_SUCK -> phase == 13 || phase == 73 ? 0.02f : 0.0f;
			case BLOWJOB_THRUST -> phase == 6 ? 0.04f : 0.0f;
			case PAIZURI_SLOW -> phase == 18 ? 0.02f : 0.0f;
			case PAIZURI_FAST -> phase == 2 ? 0.04f : 0.0f;
			case DOGGY_SLOW -> phase == 9 ? 0.00666f : 0.0f;
			case DOGGY_FAST -> phase == 4 ? 0.02f : 0.0f;
			default -> 0.0f;
		};

		if (increment > 0.0f) {
			entityData.set(SCENE_AROUSAL, Math.min(1.0f, getSceneArousal() + increment));
		}
	}

	private void handleOriginalTimedEvents(ActionState action, int elapsedTicks) {
		switch (action) {
			case PAYMENT -> {
				if (elapsedTicks == 18) {
					sendJennyDialogue("jenny.dialogue.huh");
					playJennySound("huh", 1);
				} else if (elapsedTicks == 42) {
					sendPlayerDialogue(switch (pendingService) {
						case STRIP -> "jenny.dialogue.showBobsandveganapls";
						case BLOWJOB -> "jenny.dialogue.giveblowjob";
						case DOGGY -> "jenny.dialogue.givesex";
						case BOOBJOB -> "jenny.dialogue.givebooba";
						default -> null;
					});
					playSound(ModSounds.misc("plob", 0), 0.5f, 1.0f);
				} else if (elapsedTicks == 83) {
					sendJennyDialogue("jenny.dialogue.hehe");
					playJennySound("giggle");
				} else if (elapsedTicks == 164) {
					entityData.set(OFFERED_ITEM, ItemStack.EMPTY);
				}
			}
			case BLOWJOB_INTRO -> {
				switch (elapsedTicks) {
					case 82 -> {
						sendJennyDialogue("jenny.dialogue.blowjobtext1");
						playJennySound("mmm", 8);
					}
					case 124 -> {
						sendJennyDialogue("jenny.dialogue.blowjobtext2");
						playJennySound("lightbreathing", 8);
					}
					case 162 -> {
						sendJennyDialogue("jenny.dialogue.blowjobtext3");
						playJennySound("aftersessionmoan", 0);
					}
					case 262 -> {
						sendJennyDialogue("jenny.dialogue.blowjobtext4");
						playJennySound("hmph", 1);
					}
					case 302 -> {
						sendJennyDialogue("jenny.dialogue.blowjobtext5");
						playJennySound("lightbreathing", 8);
					}
					case 467 -> {
						sendJennyDialogue("jenny.dialogue.blowjobtext6");
						playJennySound("giggle", 4);
					}
					case 510 -> {
						sendPlayerDialogue("jenny.dialogue.blowjobtext7");
						playSound(ModSounds.misc("plob", 0), 0.5f, 1.0f);
					}
					case 538 -> {
						sendJennyDialogue("jenny.dialogue.blowjobtext8");
						playJennySound("giggle", 2);
					}
					case 722 -> applyOriginalCameraOffset(-0.65, -0.8, -0.25, 60.0f, -3.0f);
					default -> {
					}
				}
			}
			case PAIZURI_START -> {
				if (elapsedTicks == 67) {
					applyOriginalCameraOffset(-0.7, -0.6, 0.2, 60.0f, -3.0f);
				}
			}
			case DOGGY_GO_BED -> {
				if (elapsedTicks == 50) {
					sendJennyDialogue("jenny.dialogue.doggytext1");
					playJennySound("lightbreathing", 9);
				} else if (elapsedTicks == 83) {
					sendJennyDialogue("jenny.dialogue.doggytext2");
					playJennySound("giggle", 0);
				}
			}
			default -> {
			}
		}
	}

	private void sendJennyDialogue(@Nullable String translationKey) {
		sendDialogue(Component.literal("<Jenny> "), translationKey);
	}

	private void sendPlayerDialogue(@Nullable String translationKey) {
		ServerPlayer player = getDialoguePlayer();

		if (player != null) {
			sendDialogue(Component.literal("<" + player.getName().getString() + "> "), translationKey);
		}
	}

	private void sendDialogue(Component prefix, @Nullable String translationKey) {
		if (translationKey == null) {
			return;
		}

		ServerPlayer player = getDialoguePlayer();

		if (player != null) {
			player.sendSystemMessage(prefix.copy().append(Component.translatable(translationKey)));
		}
	}

	private void sendJennyDialogueTo(ServerPlayer player, String translationKey, Object... args) {
		player.sendSystemMessage(
			Component.literal("<Jenny> ").append(Component.translatable(translationKey, args))
		);
	}

	@Nullable
	private ServerPlayer getDialoguePlayer() {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		UUID uuid = scenePlayerUuid != null ? scenePlayerUuid
			: paymentPlayerUuid != null ? paymentPlayerUuid
			: pendingScenePlayerUuid != null ? pendingScenePlayerUuid
			: ownerUuid;
		return uuid == null ? null : serverLevel.getServer().getPlayerList().getPlayer(uuid);
	}

	private void finishAction(ActionState action) {
		switch (action) {
			case PAYMENT -> startPendingService();
			case BLOWJOB_INTRO -> startAction(ActionState.BLOWJOB_SUCK);
			case PAIZURI_START -> startAction(ActionState.PAIZURI_SLOW);
			case DOGGY_GO_BED -> startAction(ActionState.DOGGY_START);
			case DOGGY_START -> startAction(ActionState.DOGGY_SLOW);
			case BLOWJOB_CUM, PAIZURI_CUM, DOGGY_CUM -> endPartnerScene();
			default -> {
				entityData.set(ACTION_STATE, ActionState.NONE.ordinal());
				entityData.set(ACTION_TICKS, 0);
			}
		}
	}

	private void startPendingService() {
		Service service = pendingService;
		pendingService = Service.NONE;

		if (service.isPartnerScene() && !beginPartnerScene(service)) {
			clearPaymentPlayerLock();
			resetAction();
			return;
		}

		clearPaymentPlayerLock();
		entityData.set(OFFERED_ITEM, ItemStack.EMPTY);

		switch (service) {
			case BLOWJOB -> startAction(ActionState.BLOWJOB_INTRO);
			case BOOBJOB -> startAction(ActionState.PAIZURI_START);
			case DOGGY -> startAction(ActionState.DOGGY_GO_BED);
			case STRIP -> startAction(ActionState.STRIP);
			default -> resetAction();
		}
	}

	private boolean beginPartnerScene(Service service) {
		if (!(level() instanceof ServerLevel serverLevel) || pendingScenePlayerUuid == null) {
			pendingScenePlayerUuid = null;
			return false;
		}

		ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(pendingScenePlayerUuid);
		pendingScenePlayerUuid = null;

		if (player == null || player.level() != serverLevel || !player.isAlive()) {
			return false;
		}

		JennyEntity previousScene = ACTIVE_SCENES.get(player.getUUID());

		if (previousScene != null && previousScene != this) {
			previousScene.endPartnerScene();
		}

		scenePlayerUuid = player.getUUID();
		entityData.set(SCENE_PLAYER_UUID, Optional.of(EntityReference.<LivingEntity>of(scenePlayerUuid)));
		entityData.set(SCENE_AROUSAL, 0.0f);
		sceneLoopTick = 0;
		sceneFastHeld = false;
		boolean hasPaymentReturn = paymentPlayerUuid != null
			&& paymentPlayerUuid.equals(player.getUUID())
			&& paymentReturnPos != null;
		sceneReturnPos = hasPaymentReturn ? paymentReturnPos : player.position();
		sceneReturnYaw = hasPaymentReturn ? paymentReturnYaw : player.getYRot();
		sceneReturnPitch = hasPaymentReturn ? paymentReturnPitch : player.getXRot();
		scenePlayerWasInvisible = player.isInvisible();
		scenePlayerHadNoGravity = player.isNoGravity();
		scenePlayerWasInvulnerable = player.isInvulnerable();
		scenePlayerWasNoClip = player.noPhysics;

		float jennyYaw = player.getYRot() + 180.0f;
		setYRot(jennyYaw);
		setYBodyRot(jennyYaw);
		setYHeadRot(jennyYaw);

		double radians = Math.toRadians(jennyYaw);

		if (service == Service.DOGGY) {
			// Keep the doggy camera inside the animated partner rig: slightly
			// ahead of its center, without letting it pass through the body.
			// The player and animated partner rig stay on one stable anchor.
			// User camera distance is applied client-side during rendering.
			double distance = JennyNetworking.DOGGY_SCENE_PLAYER_ANCHOR;
			scenePlayerX = getX() - Math.sin(radians) * distance;
			scenePlayerY = getY();
			scenePlayerZ = getZ() + Math.cos(radians) * distance;
			scenePlayerYaw = jennyYaw;
			scenePlayerPitch = 30.0f;
		} else {
			// Stand the payer exactly where the animated player rig renders:
			// the model's "steve" branch sits 1m in jenny-local -Z (her front),
			// so the camera ends up inside the rig (culled) looking at her.
			double distance = 1.0;
			scenePlayerX = getX() - Math.sin(radians) * distance;
			scenePlayerY = getY();
			scenePlayerZ = getZ() + Math.cos(radians) * distance;
			scenePlayerYaw = jennyYaw + 180.0f;
			scenePlayerPitch = 8.0f;
		}

		sceneCameraBaseX = scenePlayerX;
		sceneCameraBaseY = scenePlayerY;
		sceneCameraBaseZ = scenePlayerZ;
		sceneCameraBaseYaw = scenePlayerYaw;
		ACTIVE_SCENES.put(player.getUUID(), this);
		player.setNoGravity(true);
		player.setInvulnerable(true);
		player.setInvisible(true);
		player.noPhysics = true;
		player.setDeltaMovement(Vec3.ZERO);

		if (service == Service.DOGGY) {
			// Doggy deliberately starts immediately, without the kiss-style
			// camera glide used by the other paid scenes.
			sceneTransitionTicks = 0;
			placeScenePlayer(player, true);
		} else {
			sceneTransitionTicks = SCENE_TRANSITION_DURATION;
			ServerPlayNetworking.send(player, new JennyPayloads.CameraTransition(
				scenePlayerX,
				scenePlayerY + player.getEyeHeight(player.getPose()),
				scenePlayerZ,
				scenePlayerYaw,
				scenePlayerPitch,
				SCENE_TRANSITION_DURATION
			));
		}

		ServerPlayNetworking.send(player, new JennyPayloads.OpenSceneControls(getId()));
		return true;
	}

	private void maintainScenePlayer() {
		ServerPlayer player = getSceneServerPlayer();

		if (player == null || !player.isAlive() || player.level() != level()) {
			endPartnerScene();
			return;
		}

		player.setDeltaMovement(Vec3.ZERO);
		player.noPhysics = true;

		if (sceneTransitionTicks > 0) {
			sceneTransitionTicks--;

			if (sceneTransitionTicks <= 0) {
				placeScenePlayer(player, true);
			}

			return;
		}

		if (player.distanceToSqr(scenePlayerX, scenePlayerY, scenePlayerZ) > 0.01) {
			placeScenePlayer(player, false);
		}
	}

	private void placeScenePlayer(ServerPlayer player, boolean applyInitialRotation) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		player.teleportTo(
			serverLevel,
			scenePlayerX,
			scenePlayerY,
			scenePlayerZ,
			Set.of(),
			applyInitialRotation ? scenePlayerYaw : player.getYRot(),
			applyInitialRotation ? scenePlayerPitch : player.getXRot(),
			true
		);
	}

	public void handleSceneCommand(ServerPlayer player, @Nullable SceneCommand command) {
		if (command == null
			|| scenePlayerUuid == null
			|| !scenePlayerUuid.equals(player.getUUID())
			|| !getActionState().isPartnerScene()) {
			return;
		}

		ActionState action = getActionState();

		switch (command) {
			case SLOW -> {
				sceneFastHeld = false;
			}
			case FAST -> {
				sceneFastHeld = true;

				if (action == ActionState.BLOWJOB_SUCK) {
					startAction(ActionState.BLOWJOB_THRUST);
				} else if (action == ActionState.PAIZURI_SLOW) {
					startAction(ActionState.PAIZURI_FAST);
				} else if (action == ActionState.DOGGY_SLOW) {
					startAction(ActionState.DOGGY_FAST);
				}
			}
			case FINISH -> {
				if (getSceneArousal() < 1.0f) {
					return;
				}

				sceneFastHeld = false;

				if (action == ActionState.BLOWJOB_SUCK || action == ActionState.BLOWJOB_THRUST) {
					scenePlayerX = sceneCameraBaseX;
					scenePlayerY = sceneCameraBaseY;
					scenePlayerZ = sceneCameraBaseZ;
					scenePlayerYaw = sceneCameraBaseYaw;
					scenePlayerPitch = 70.0f;
					placeScenePlayer(player, true);
					startAction(ActionState.BLOWJOB_CUM);
				} else if (action == ActionState.PAIZURI_SLOW || action == ActionState.PAIZURI_FAST) {
					startAction(ActionState.PAIZURI_CUM);
				} else if (action == ActionState.DOGGY_SLOW || action == ActionState.DOGGY_FAST) {
					startAction(ActionState.DOGGY_CUM);
				}
			}
			case EXIT -> endPartnerScene();
		}
	}

	private void applyOriginalCameraOffset(
		double lateral,
		double vertical,
		double forward,
		float yawOffset,
		float pitch
	) {
		ServerPlayer player = getSceneServerPlayer();

		if (player == null) {
			return;
		}

		// the authored per-keyframe camera offsets ARE the original mod's first-person framing;
		// push them to the payer as smooth camera transitions
		if (level() instanceof ServerLevel serverLevel) {
			float targetYaw = sceneCameraBaseYaw + yawOffset;
			ServerPlayNetworking.send(player, new JennyPayloads.CameraTransition(
				scenePlayerX,
				scenePlayerY + player.getEyeHeight(player.getPose()),
				scenePlayerZ,
				targetYaw,
				pitch,
				6
			));
		}

		double radians = Math.toRadians(sceneCameraBaseYaw);
		double sideRadians = Math.toRadians(sceneCameraBaseYaw + 90.0f);
		scenePlayerX = sceneCameraBaseX
			- Math.sin(sideRadians) * lateral
			- Math.sin(radians) * forward;
		scenePlayerY = sceneCameraBaseY + vertical;
		scenePlayerZ = sceneCameraBaseZ
			+ Math.cos(sideRadians) * lateral
			+ Math.cos(radians) * forward;
		scenePlayerYaw = sceneCameraBaseYaw + yawOffset;
		scenePlayerPitch = pitch;
		placeScenePlayer(player, true);
	}

	private void endPartnerScene() {
		ServerPlayer player = getSceneServerPlayer();
		com.trolmastercard.sexmod.port.JennyPort.LOGGER.info(
			"[sexmod-sceneend] endPartnerScene action={} scenePlayer={} alive={}",
			getActionState(),
			player != null ? player.getName().getString() : "null",
			player != null && player.isAlive()
		);
		sceneTransitionTicks = 0;
		sceneLoopTick = 0;
		sceneFastHeld = false;
		entityData.set(SCENE_AROUSAL, 0.0f);

		if (player != null) {
			ServerPlayNetworking.send(player, new JennyPayloads.CloseSceneControls(getId()));
			restoreScenePlayer(player);
		}

		if (scenePlayerUuid != null) {
			ACTIVE_SCENES.remove(scenePlayerUuid, this);
		}

		scenePlayerUuid = null;
		entityData.set(SCENE_PLAYER_UUID, Optional.<EntityReference<LivingEntity>>empty());
		sceneReturnPos = null;
		resetAction();
	}

	private void restoreScenePlayer(ServerPlayer player) {
		com.trolmastercard.sexmod.port.JennyPort.LOGGER.info(
			"[sexmod-sceneend] restore: returnPos={} teleport={}",
			sceneReturnPos,
			sceneReturnPos != null && player.isAlive() && level() instanceof ServerLevel
		);
		player.setInvisible(scenePlayerWasInvisible);
		player.setNoGravity(scenePlayerHadNoGravity);
		player.setInvulnerable(scenePlayerWasInvulnerable);
		player.noPhysics = scenePlayerWasNoClip;
		player.setDeltaMovement(Vec3.ZERO);

		if (sceneReturnPos != null
			&& player.isAlive()
			&& level() instanceof ServerLevel serverLevel
			&& player.level() == serverLevel) {
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

	@Nullable
	private ServerPlayer getSceneServerPlayer() {
		if (scenePlayerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		return serverLevel.getServer().getPlayerList().getPlayer(scenePlayerUuid);
	}

	public static void endSceneForDisconnect(ServerPlayer player) {
		JennyEntity jenny = ACTIVE_SCENES.get(player.getUUID());

		if (jenny != null) {
			jenny.endPartnerScene();
		}
	}

	private void resetAction() {
		entityData.set(ACTION_STATE, ActionState.NONE.ordinal());
		entityData.set(ACTION_TICKS, 0);
		entityData.set(OFFERED_ITEM, ItemStack.EMPTY);
		clearPaymentPlayerLock();
	}

	private void throwPendingPearl() {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		ItemStack pearlStack = new ItemStack(Items.ENDER_PEARL);
		ThrownEnderpearl pearl = new ThrownEnderpearl(serverLevel, this, pearlStack);
		pearl.setPos(getX(), getEyeY() - 0.1, getZ());
		pearl.shootFromRotation(this, pendingPearlPitch, pendingPearlYaw, 0.0f, 1.4f, 1.0f);
		serverLevel.addFreshEntity(pearl);
		playSound(SoundEvents.ENDER_PEARL_THROW, 1.0f, 0.95f + random.nextFloat() * 0.1f);
	}

	private void playJennySound(String category) {
		int count = ModSounds.count(category);

		if (count > 0) {
			playJennySound(category, random.nextInt(count));
		}
	}

	private void playJennySound(String category, int index) {
		int count = ModSounds.count(category);

		if (count > 0) {
			playSound(ModSounds.jenny(category, Math.floorMod(index, count)), 0.85f, 1.0f);
		}
	}

	private void playMiscSound(String category, float volume) {
		int count = ModSounds.miscCount(category);

		if (count > 0) {
			playSound(ModSounds.misc(category, random.nextInt(count)), volume, 1.0f);
		}
	}

	/// The camera is authored through the boyCam bone while these actions run;
	/// the client renderer feeds the bone's world position into the camera.
	public boolean isCameraControlled() {
		return getActionState() == ActionState.PAYMENT
			|| getActionState().isPartnerScene()
			|| getActionState() == ActionState.STRIP;
	}

	public boolean isOwnedBy(net.minecraft.world.entity.Entity player) {
		return level().isClientSide()
			? entityData.get(OWNER_UUID).map(r -> player.getUUID().equals(r.getUUID())).orElse(false)
			: player.getUUID().equals(ownerUuid);
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

	public ItemStack getOfferedItem() {
		return entityData.get(OFFERED_ITEM);
	}

	public boolean shouldRenderOfferedItem() {
		if (getActionState() != ActionState.PAYMENT || getOfferedItem().isEmpty()) {
			return false;
		}

		int elapsed = ActionState.PAYMENT.durationTicks - getActionTicks();
		return elapsed >= 2 && elapsed < 164;
	}

	private void setOwnerUuid(UUID uuid) {
		ownerUuid = uuid;
		entityData.set(OWNER_UUID, Optional.of(EntityReference.<LivingEntity>of(uuid)));
	}

	public NpcMode getNpcMode() {
		int ordinal = entityData.get(NPC_MODE);
		NpcMode[] values = NpcMode.values();
		return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NpcMode.WANDER;
	}

	public void setNpcMode(NpcMode mode) {
		entityData.set(NPC_MODE, mode.ordinal());
	}

	public int getActionTicks() {
		return entityData.get(ACTION_TICKS);
	}

	public ActionState getActionState() {
		return ActionState.byOrdinal(entityData.get(ACTION_STATE));
	}

	public boolean isDressed() {
		return entityData.get(DRESSED);
	}

	private void setDressed(boolean dressed) {
		entityData.set(DRESSED, dressed);
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("JennyNpcMode", getNpcMode().ordinal());
		output.putBoolean("JennyDressed", isDressed());
		output.putInt("JennyHomeX", homePos.getX());
		output.putInt("JennyHomeY", homePos.getY());
		output.putInt("JennyHomeZ", homePos.getZ());

		if (ownerUuid != null) {
			output.store("JennyOwner", UUIDUtil.CODEC, ownerUuid);
		}
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setNpcMode(NpcMode.byOrdinal(input.getIntOr("JennyNpcMode", 0)));
		setDressed(!input.contains("JennyDressed") || input.getBooleanOr("JennyDressed", true));

		if (input.getInt("JennyHomeX").isPresent()) {
			homePos = new BlockPos(
				input.getIntOr("JennyHomeX", 0),
				input.getIntOr("JennyHomeY", 0),
				input.getIntOr("JennyHomeZ", 0)
			);
		} else {
			homePos = blockPosition();
		}

		if (input.contains("JennyOwner")) {
			setOwnerUuid(input.read("JennyOwner", UUIDUtil.CODEC).orElseThrow());
		}
	}

	@Override
	public void die(DamageSource damageSource) {
		if (!level().isClientSide() && scenePlayerUuid != null) {
			endPartnerScene();
		}

		super.die(damageSource);
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		if (!level().isClientSide() && scenePlayerUuid != null) {
			endPartnerScene();
		}

		super.onRemoval(reason);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		ActionState action = getActionState();

		if (action == ActionState.PAYMENT || action.isPartnerScene()) {
			return false;
		}

		return super.hurtServer(level, source, amount);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.jenny("lightbreathing", random.nextInt(ModSounds.count("lightbreathing")));
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.jenny("hmph", random.nextInt(ModSounds.count("hmph")));
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.jenny("sadoh", random.nextInt(ModSounds.count("sadoh")));
	}

	@Override
	public int getAmbientSoundInterval() {
		return 220;
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
		AnimationController<JennyEntity> movementController = new AnimationController<>("movement", 4, state -> {
			if (getActionState() != ActionState.NONE) {
				return state.setAndContinue(getActionState().animation);
			}

			if (getNpcMode() == NpcMode.STAY) {
				return state.setAndContinue(SIT);
			}

			int walkState = entityData.get(WALK_STATE);
			double walkSpeed = entityData.get(WALK_SPEED);

			// GeckoLib 5 does not auto-sync animation speed to movement like v3/v4
			// did. Scale the controller with the measured blocks/second so the
			// stride matches the ground the entity covers (sliding fix).
			state.setControllerSpeed((float)net.minecraft.util.Mth.clamp(
				walkSpeed / 2.2,
				walkState == 0 ? 1.0f : 0.4f,
				3.5f
			));

			return switch (walkState) {
				case 3 -> state.setAndContinue(RUN);
				case 2 -> state.setAndContinue(FAST_WALK);
				case 1 -> state.setAndContinue(WALK);
				default -> state.setAndContinue(IDLE);
			};
		});

		movementController.setSoundKeyframeHandler(event -> {
			String marker = event.keyframeData().getSound();

			switch (marker) {
				case "idle" -> playJennySound(random.nextInt(4) == 0 ? "sigh" : "lightbreathing");
				case "paymentMSG4" -> playMiscSound("plob", 0.25f);
				case "paymentMSG1", "paymentMSG2", "paymentMSG3",
					"bjiMSG1", "bjiMSG2", "bjiMSG3", "bjiMSG4", "bjiMSG5",
					"bjiMSG6", "bjiMSG7", "bjiMSG8", "bjiMSG9", "bjiMSG10",
					"doggyGoOnBedMSG2", "doggyGoOnBedMSG3", "boobjob_camera" -> {
					// These are fired on the authoritative server tick so every
					// nearby player receives one, and only one, copy.
				}
				case "bjiMSG11", "bjiMSG12" -> {
					if (random.nextInt(5) == 0) {
						playJennySound("bjmoan");
					}
					playJennySound("lipsound");
				}
				case "bjtMSG1" -> {
					playJennySound("mmm");
					playJennySound("lipsound");
				}
				case "bjcMSG1" -> playJennySound("bjmoan", 1);
				case "bjcMSG2" -> playJennySound("bjmoan", 7);
				case "bjcMSG3" -> playJennySound("aftersessionmoan", 1);
				case "bjcMSG4" -> playJennySound("lightbreathing", 0);
				case "bjcMSG5" -> playJennySound("lightbreathing", 1);
				case "bjcMSG6" -> playJennySound("lightbreathing", 2);
				case "bjcMSG7" -> playJennySound("lightbreathing", 3);
				case "stripMSG1" -> playJennySound("huh");
				case "becomeNude" -> playJennySound("sigh");
				case "doggyGoOnBedMSG1" -> playMiscSound("bedrustle", 1.0f);
				case "doggyGoOnBedMSG4" -> playMiscSound("slap", 0.75f);
				case "doggystartMSG1" -> playMiscSound("slap", 0.75f);
				case "doggystartMSG3" -> playSound(ModSounds.misc("touch", 0), 1.0f, 1.0f);
				case "doggystartMSG4" -> playSound(ModSounds.misc("touch", 1), 1.0f, 1.0f);
				case "doggystartMSG5" -> {
					playSound(ModSounds.misc("bedrustle", 1), 0.5f, 1.0f);
					playJennySound("mmm", 1);
				}
				case "doggyslowMSG1" -> {
					playMiscSound("smallinserts", 1.0f);
					playJennySound("mmm", 1);
				}
				case "doggyslowMSG2" -> {
					playMiscSound("pounding", 0.33f);
					playJennySound("moan");
				}
				case "doggyfastMSG1" -> {
					playMiscSound("pounding", 0.75f);

					if (random.nextBoolean()) {
						playJennySound("ahh");
					} else {
						playJennySound(random.nextBoolean() ? "moan" : "heavybreathing");
					}
				}
				case "doggycumMSG1" -> {
					playSound(ModSounds.misc("cuminflation", 0), 2.0f, 1.0f);
					playMiscSound("pounding", 2.0f);
					playJennySound("moan");
				}
				case "doggycumMSG2" -> playJennySound("heavybreathing", 4);
				case "doggycumMSG3" -> playJennySound("heavybreathing", 5);
				case "doggycumMSG4" -> playJennySound("heavybreathing", 6);
				case "doggycumMSG5" -> playJennySound("heavybreathing", 7);
				case "paizuriFastMSG1" -> {
					playMiscSound("pounding", 1.0f);
					playJennySound(random.nextBoolean() ? "mmm" : "ahh");
				}
				case "paizuriSlowMSG1", "paizuriStartMSG1" -> playMiscSound("pounding", 1.0f);
				case "attackSound" ->
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.95f + random.nextFloat() * 0.1f);
				default -> {
				}
			}
		});

		controllers.add(movementController);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animationCache;
	}

	public enum NpcMode {
		WANDER("mode.sexmod.jenny.wander"),
		FOLLOW("mode.sexmod.jenny.follow"),
		STAY("mode.sexmod.jenny.stay");

		private final String translationKey;

		NpcMode(String translationKey) {
			this.translationKey = translationKey;
		}

		private NpcMode next() {
			return values()[(ordinal() + 1) % values().length];
		}

		private static NpcMode byOrdinal(int ordinal) {
			NpcMode[] values = values();
			return ordinal >= 0 && ordinal < values.length ? values[ordinal] : WANDER;
		}
	}

	public enum ActionState {
		NONE(0, IDLE),
		WAVE(72, JennyEntity.WAVE),
		PAYMENT(187, JennyEntity.PAYMENT),
		THROW_PEARL(92, JennyEntity.THROW_PEARL),
		ATTACK_0(8, JennyEntity.ATTACK_0),
		ATTACK_1(11, JennyEntity.ATTACK_1),
		ATTACK_2(11, JennyEntity.ATTACK_2),
		STRIP(101, JennyEntity.STRIP),
		BLOWJOB_INTRO(986, JennyEntity.BLOWJOB_INTRO),
		BLOWJOB_SUCK(160, JennyEntity.BLOWJOB_SUCK),
		BLOWJOB_THRUST(35, JennyEntity.BLOWJOB_THRUST),
		BLOWJOB_CUM(236, JennyEntity.BLOWJOB_CUM),
		PAIZURI_START(174, JennyEntity.PAIZURI_START),
		PAIZURI_SLOW(160, JennyEntity.PAIZURI_SLOW),
		PAIZURI_FAST(22, JennyEntity.PAIZURI_FAST),
		PAIZURI_CUM(230, JennyEntity.PAIZURI_CUM),
		DOGGY_GO_BED(131, JennyEntity.DOGGY_GO_BED),
		DOGGY_START(199, JennyEntity.DOGGY_START),
		DOGGY_SLOW(160, JennyEntity.DOGGY_SLOW),
		DOGGY_FAST(22, JennyEntity.DOGGY_FAST),
		DOGGY_CUM(134, JennyEntity.DOGGY_CUM);

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

		public boolean isPartnerScene() {
			return switch (this) {
				case BLOWJOB_INTRO, BLOWJOB_SUCK, BLOWJOB_THRUST, BLOWJOB_CUM,
					PAIZURI_START, PAIZURI_SLOW, PAIZURI_FAST, PAIZURI_CUM,
					DOGGY_GO_BED, DOGGY_START, DOGGY_SLOW, DOGGY_FAST, DOGGY_CUM -> true;
				default -> false;
			};
		}

		public boolean isDoggyScene() {
			return switch (this) {
				case DOGGY_GO_BED, DOGGY_START, DOGGY_SLOW, DOGGY_FAST, DOGGY_CUM -> true;
				default -> false;
			};
		}

		public boolean isControllableLoop() {
			return switch (this) {
				case BLOWJOB_SUCK, BLOWJOB_THRUST,
					PAIZURI_SLOW, PAIZURI_FAST,
					DOGGY_SLOW, DOGGY_FAST -> true;
				default -> false;
			};
		}

		public boolean isSlowLoop() {
			return this == BLOWJOB_SUCK || this == PAIZURI_SLOW || this == DOGGY_SLOW;
		}

		public boolean isFastLoop() {
			return this == BLOWJOB_THRUST || this == PAIZURI_FAST || this == DOGGY_FAST;
		}

		private int loopPeriodTicks() {
			return switch (this) {
				case BLOWJOB_SUCK -> 121;
				case BLOWJOB_THRUST -> 35;
				case PAIZURI_SLOW -> 68;
				case PAIZURI_FAST, DOGGY_FAST -> 22;
				case DOGGY_SLOW -> 46;
				default -> 1;
			};
		}

		private ActionState slowVariant() {
			return switch (this) {
				case BLOWJOB_THRUST -> BLOWJOB_SUCK;
				case PAIZURI_FAST -> PAIZURI_SLOW;
				case DOGGY_FAST -> DOGGY_SLOW;
				default -> this;
			};
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

	public enum MenuAction {
		FOLLOW,
		STOP_FOLLOWING,
		GO_HOME,
		SET_HOME,
		BLOWJOB,
		BOOBJOB,
		DOGGY,
		STRIP_OR_DRESS;

		@Nullable
		public static MenuAction byId(int id) {
			MenuAction[] values = values();
			return id >= 0 && id < values.length ? values[id] : null;
		}
	}

	private enum Service {
		NONE,
		BLOWJOB,
		BOOBJOB,
		DOGGY,
		STRIP;

		private boolean isPartnerScene() {
			return this == BLOWJOB || this == BOOBJOB || this == DOGGY;
		}
	}

	private final class ActionLockGoal extends Goal {
		private ActionLockGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return getActionState() != ActionState.NONE;
		}

		@Override
		public boolean canContinueToUse() {
			return getActionState() != ActionState.NONE;
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			getNavigation().stop();
		}
	}

	private final class JennyMeleeAttackGoal extends MeleeAttackGoal {
		private JennyMeleeAttackGoal() {
			super(JennyEntity.this, 0.5, true);
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
			return attacker.isAlive() && attacker != JennyEntity.this;
		}

		@Override
		public void start() {
			Player owner = getOwner();

			if (owner != null && attacker != null) {
				setTarget(attacker);
				lastOwnerAttackedTime = owner.getLastHurtByMobTimestamp();
			}
		}

		@Override
		public boolean canContinueToUse() {
			return attacker != null
				&& attacker.isAlive()
				&& getTarget() == attacker
				&& distanceToSqr(attacker) < 1024.0;
		}

		@Override
		public void stop() {
			attacker = null;
		}
	}

	private final class StayGoal extends Goal {
		private StayGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return getNpcMode() == NpcMode.STAY;
		}

		@Override
		public boolean canContinueToUse() {
			return getNpcMode() == NpcMode.STAY;
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			getNavigation().stop();
		}
	}

	private final class FollowOwnerGoal extends Goal {
		@Nullable
		private Player owner;

		private FollowOwnerGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			owner = getOwner();
			return getNpcMode() == NpcMode.FOLLOW
				&& owner != null
				&& !owner.isSpectator()
				&& distanceToSqr(owner) > 9.0;
		}

		@Override
		public boolean canContinueToUse() {
			return getNpcMode() == NpcMode.FOLLOW
				&& owner != null
				&& owner.isAlive()
				&& distanceToSqr(owner) > 4.0;
		}

		@Override
		public void stop() {
			owner = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (owner == null) {
				return;
			}

			getLookControl().setLookAt(owner, 10.0f, getMaxHeadXRot());

			if (distanceToSqr(owner) > 1024.0 && owner instanceof ServerPlayer) {
				snapTo(owner.getX(), owner.getY(), owner.getZ(), getYRot(), getXRot());
				getNavigation().stop();
			} else {
				// Matches the original Jenny tempt/follow navigation speed.
				getNavigation().moveTo(owner, 0.4);
			}
		}
	}

	private final class ReturnHomeGoal extends Goal {
		private ReturnHomeGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return getNpcMode() == NpcMode.WANDER
				&& homePos != null
				&& blockPosition().distSqr(homePos) > 256.0;
		}

		@Override
		public boolean canContinueToUse() {
			return getNpcMode() == NpcMode.WANDER
				&& !getNavigation().isDone()
				&& blockPosition().distSqr(homePos) > 16.0;
		}

		@Override
		public void start() {
			Vec3 target = Vec3.atBottomCenterOf(homePos);
			getNavigation().moveTo(target.x, target.y, target.z, 0.55);
		}
	}

	private final class ModeWanderGoal extends RandomStrollGoal {
		private ModeWanderGoal() {
			super(JennyEntity.this, 0.35);
		}

		@Override
		public boolean canUse() {
			return getNpcMode() == NpcMode.WANDER && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return getNpcMode() == NpcMode.WANDER && super.canContinueToUse();
		}
	}
}

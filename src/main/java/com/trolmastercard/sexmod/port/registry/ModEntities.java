package com.trolmastercard.sexmod.port.registry;

import com.trolmastercard.sexmod.port.JennyPort;
import com.trolmastercard.sexmod.port.entity.GalathEntity;
import com.trolmastercard.sexmod.port.entity.JennyEntity;
import com.trolmastercard.sexmod.port.entity.ManglelieEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class ModEntities {
	private static final ResourceKey<EntityType<?>> JENNY_KEY =
		ResourceKey.create(Registries.ENTITY_TYPE, JennyPort.id("jenny"));
	private static final ResourceKey<EntityType<?>> GALATH_KEY =
		ResourceKey.create(Registries.ENTITY_TYPE, JennyPort.id("galath"));
	private static final ResourceKey<EntityType<?>> MANGLELIE_KEY =
		ResourceKey.create(Registries.ENTITY_TYPE, JennyPort.id("manglelie"));

	public static final EntityType<JennyEntity> JENNY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		JENNY_KEY,
		EntityType.Builder.of(JennyEntity::new, MobCategory.CREATURE)
			.sized(0.6f, 1.95f)
			.eyeHeight(1.75f)
			.clientTrackingRange(10)
			.build(JENNY_KEY)
	);
	public static final EntityType<GalathEntity> GALATH = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		GALATH_KEY,
		EntityType.Builder.of(GalathEntity::new, MobCategory.CREATURE)
			.sized(0.75f, 2.25f)
			.eyeHeight(2.0f)
			.clientTrackingRange(12)
			.build(GALATH_KEY)
	);
	public static final EntityType<ManglelieEntity> MANGLELIE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MANGLELIE_KEY,
		EntityType.Builder.of(ManglelieEntity::new, MobCategory.CREATURE)
			.sized(0.58f, 1.72f)
			.eyeHeight(1.52f)
			.clientTrackingRange(10)
			.build(MANGLELIE_KEY)
	);

	private ModEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(JENNY, JennyEntity.createJennyAttributes());
		FabricDefaultAttributeRegistry.register(GALATH, GalathEntity.createGalathAttributes());
		FabricDefaultAttributeRegistry.register(MANGLELIE, ManglelieEntity.createManglelieAttributes());
	}
}

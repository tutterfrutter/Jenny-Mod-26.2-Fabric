package com.trolmastercard.sexmod.port.registry;

import com.trolmastercard.sexmod.port.JennyPort;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	private static final ResourceKey<Item> JENNY_SPAWN_EGG_KEY =
		ResourceKey.create(Registries.ITEM, JennyPort.id("jenny_spawn_egg"));
	private static final ResourceKey<Item> GALATH_SPAWN_EGG_KEY =
		ResourceKey.create(Registries.ITEM, JennyPort.id("galath_spawn_egg"));
	private static final ResourceKey<Item> MANGLELIE_SPAWN_EGG_KEY =
		ResourceKey.create(Registries.ITEM, JennyPort.id("manglelie_spawn_egg"));

	public static final Item JENNY_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		JENNY_SPAWN_EGG_KEY,
		new SpawnEggItem(
			new Item.Properties()
				.setId(JENNY_SPAWN_EGG_KEY)
				.spawnEgg(ModEntities.JENNY)
		)
	);
	public static final Item GALATH_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		GALATH_SPAWN_EGG_KEY,
		new SpawnEggItem(
			new Item.Properties()
				.setId(GALATH_SPAWN_EGG_KEY)
				.spawnEgg(ModEntities.GALATH)
		)
	);
	public static final Item MANGLELIE_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		MANGLELIE_SPAWN_EGG_KEY,
		new SpawnEggItem(
			new Item.Properties()
				.setId(MANGLELIE_SPAWN_EGG_KEY)
				.spawnEgg(ModEntities.MANGLELIE)
		)
	);

	public static final CreativeModeTab JENNY_GROUP = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, JennyPort.id("jenny")),
		FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(JENNY_SPAWN_EGG))
			.title(Component.translatable("itemGroup.sexmod.jenny"))
			.displayItems((parameters, output) -> {
				output.accept(JENNY_SPAWN_EGG);
				output.accept(GALATH_SPAWN_EGG);
				output.accept(MANGLELIE_SPAWN_EGG);
			})
			.build()
	);

	private ModItems() {
	}

	public static void initialize() {
		// Loading this class performs the registry work.
	}
}

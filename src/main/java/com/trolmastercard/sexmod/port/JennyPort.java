package com.trolmastercard.sexmod.port;

import com.trolmastercard.sexmod.port.registry.ModEntities;
import com.trolmastercard.sexmod.port.registry.ModItems;
import com.trolmastercard.sexmod.port.registry.ModSounds;
import com.trolmastercard.sexmod.port.network.JennyNetworking;
import com.trolmastercard.sexmod.port.network.GalathNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JennyPort implements ModInitializer {
	public static final String RESOURCE_NAMESPACE = "sexmod";
	public static final Logger LOGGER = LoggerFactory.getLogger("sexmod_jenny_port");

	@Override
	public void onInitialize() {
		ModSounds.initialize();
		JennyNetworking.initialize();
		GalathNetworking.initialize();
		ModEntities.initialize();
		ModItems.initialize();
		LOGGER.info("Initialized the Jenny, Galath and Manglelie Fabric port");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(RESOURCE_NAMESPACE, path);
	}
}

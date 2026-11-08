package com.trolmastercard.sexmod.port.client.render;

import com.trolmastercard.sexmod.port.JennyPort;
import com.trolmastercard.sexmod.port.entity.ManglelieEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public final class ManglelieModel extends GeoModel<ManglelieEntity> {
	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		// model swap for the combined three-person scene rig
		return JennyPort.id(renderState.getGeckolibData(SceneRenderTickets.PARTNER_RIG_ACTIVE)
			? "galath/galath_con_mang"
			: "manglelie/manglelie");
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return JennyPort.id("textures/entity/manglelie/manglelie.png");
	}

	@Override
	public Identifier getAnimationResource(ManglelieEntity animatable) {
		return JennyPort.id("manglelie/manglelie");
	}
}

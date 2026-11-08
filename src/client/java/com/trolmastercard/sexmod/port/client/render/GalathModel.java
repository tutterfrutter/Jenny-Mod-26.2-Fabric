package com.trolmastercard.sexmod.port.client.render;

import com.trolmastercard.sexmod.port.JennyPort;
import com.trolmastercard.sexmod.port.entity.GalathEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public final class GalathModel extends GeoModel<GalathEntity> {
	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return JennyPort.id("galath/galath");
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return JennyPort.id("textures/entity/galath/galath.png");
	}

	@Override
	public Identifier getAnimationResource(GalathEntity animatable) {
		return JennyPort.id("galath/galath");
	}
}

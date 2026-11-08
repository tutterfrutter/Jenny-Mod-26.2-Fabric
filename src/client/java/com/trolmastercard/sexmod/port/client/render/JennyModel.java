package com.trolmastercard.sexmod.port.client.render;

import com.trolmastercard.sexmod.port.JennyPort;
import com.trolmastercard.sexmod.port.entity.JennyEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public final class JennyModel extends GeoModel<JennyEntity> {
	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		boolean dressed = renderState.getGeckolibData(SceneRenderTickets.DRESSED);
		return JennyPort.id(dressed
			? "jenny/jennydressed"
			: "jenny/jennynude");
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return JennyPort.id("textures/entity/jenny/jenny.png");
	}

	@Override
	public Identifier getAnimationResource(JennyEntity animatable) {
		return JennyPort.id("jenny/jenny");
	}
}

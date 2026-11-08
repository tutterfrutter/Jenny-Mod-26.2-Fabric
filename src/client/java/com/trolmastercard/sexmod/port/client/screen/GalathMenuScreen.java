package com.trolmastercard.sexmod.port.client.screen;

import com.trolmastercard.sexmod.port.entity.GalathEntity;
import com.trolmastercard.sexmod.port.network.GalathPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class GalathMenuScreen extends Screen {
	private final int entityId;

	public GalathMenuScreen(int entityId) {
		super(Component.translatable("screen.sexmod.galath.title"));
		this.entityId = entityId;
	}

	@Override
	protected void init() {
		int actionX = width / 2 - 155;
		int actionY = height - 100;
		addAction(actionX, actionY, "screen.sexmod.galath.cowgirl", GalathEntity.MenuAction.COWGIRL);
		addAction(actionX + 160, actionY, "screen.sexmod.galath.anal", GalathEntity.MenuAction.ANAL);
		addAction(actionX, actionY + 25, "screen.sexmod.galath.threesome", GalathEntity.MenuAction.THREESOME);
		addAction(actionX + 160, actionY + 25, "screen.sexmod.galath.ride", GalathEntity.MenuAction.RIDE);

		int commandX = 24;
		int commandY = 55;
		addAction(commandX, commandY, "screen.sexmod.galath.follow", GalathEntity.MenuAction.FOLLOW);
		addAction(commandX, commandY + 25, "screen.sexmod.galath.stay", GalathEntity.MenuAction.STAY);
		addAction(commandX, commandY + 50, "screen.sexmod.galath.set_home", GalathEntity.MenuAction.SET_HOME);
	}

	private void addAction(int x, int y, String key, GalathEntity.MenuAction action) {
		addRenderableWidget(new LegacyButton(
			x,
			y,
			150,
			20,
			Component.translatable(key),
			() -> {
				ClientPlayNetworking.send(new GalathPayloads.SelectAction(entityId, action.ordinal()));
				onClose();
			}
		));
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	private static final class LegacyButton extends AbstractWidget {
		private final Runnable pressAction;

		private LegacyButton(int x, int y, int width, int height, Component message, Runnable pressAction) {
			super(x, y, width, height, message);
			this.pressAction = pressAction;
		}

		@Override
		public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
			pressAction.run();
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			int x = getX();
			int y = getY();
			int right = x + getWidth();
			int bottom = y + getHeight();
			int face = isHovered() ? 0xFF777777 : 0xFF555555;
			graphics.fill(x, y, right, bottom, 0xFF000000);
			graphics.fill(x + 1, y + 1, right - 1, bottom - 1, face);
			graphics.fill(x + 1, y + 1, right - 1, y + 2, 0xFFA0A0A0);
			graphics.fill(x + 1, y + 1, x + 2, bottom - 1, 0xFFA0A0A0);
			graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, 0xFF202020);
			graphics.fill(right - 2, y + 1, right - 1, bottom - 1, 0xFF202020);
			var font = Minecraft.getInstance().font;
			int color = isHovered() ? 0xFFFFFFA0 : 0xFFFFFFFF;
			graphics.text(
				font,
				getMessage(),
				x + (getWidth() - font.width(getMessage())) / 2,
				y + 6,
				color,
				true
			);
		}

		@Override
		protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}
	}
}

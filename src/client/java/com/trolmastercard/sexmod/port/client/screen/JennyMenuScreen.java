package com.trolmastercard.sexmod.port.client.screen;

import com.trolmastercard.sexmod.port.client.hud.JennyPaymentLock;
import com.trolmastercard.sexmod.port.entity.JennyEntity;
import com.trolmastercard.sexmod.port.network.JennyPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class JennyMenuScreen extends Screen {
	private final int entityId;
	private final List<PriceDisplay> prices = new ArrayList<>();

	public JennyMenuScreen(int entityId) {
		super(Component.translatable("screen.sexmod.jenny.title"));
		this.entityId = entityId;
	}

	@Override
	protected void init() {
		prices.clear();

		int serviceX = width - 120;
		int firstServiceY = height - 160;

		addService(serviceX, firstServiceY, "screen.sexmod.jenny.blowjob",
			JennyEntity.MenuAction.BLOWJOB, new ItemStack(Items.EMERALD), 3);
		addService(serviceX, firstServiceY + 30, "screen.sexmod.jenny.boobjob",
			JennyEntity.MenuAction.BOOBJOB, new ItemStack(Items.ENDER_PEARL), 2);
		addService(serviceX, firstServiceY + 60, "screen.sexmod.jenny.doggy",
			JennyEntity.MenuAction.DOGGY, new ItemStack(Items.DIAMOND), 2);

		if (isJennyDressed()) {
			addService(serviceX, firstServiceY + 90, "screen.sexmod.jenny.strip",
				JennyEntity.MenuAction.STRIP_OR_DRESS, new ItemStack(Items.GOLD_INGOT), 1);
		} else {
			addService(serviceX, firstServiceY + 90, "screen.sexmod.jenny.dress",
				JennyEntity.MenuAction.STRIP_OR_DRESS, ItemStack.EMPTY, 0);
		}

		int commandX = 36;
		int commandY = 70;
		addCommand(commandX, commandY, "screen.sexmod.jenny.follow", JennyEntity.MenuAction.FOLLOW);
		addCommand(commandX, commandY + 30, "screen.sexmod.jenny.stop_following",
			JennyEntity.MenuAction.STOP_FOLLOWING);
		addCommand(commandX, commandY + 60, "screen.sexmod.jenny.go_home", JennyEntity.MenuAction.GO_HOME);
		addCommand(commandX, commandY + 90, "screen.sexmod.jenny.set_home", JennyEntity.MenuAction.SET_HOME);
	}

	private boolean isJennyDressed() {
		if (minecraft != null && minecraft.level != null) {
			Entity entity = minecraft.level.getEntity(entityId);
			return !(entity instanceof JennyEntity jenny) || jenny.isDressed();
		}

		return true;
	}

	private void addService(
		int x,
		int y,
		String translationKey,
		JennyEntity.MenuAction action,
		ItemStack payment,
		int count
	) {
		addRenderableWidget(new LegacyButton(x, y, 100, 20, Component.translatable(translationKey), () -> select(action)));

		if (!payment.isEmpty() && count > 0) {
			prices.add(new PriceDisplay(x - 29, y + 2, payment, count));
		}
	}

	private void addCommand(int x, int y, String translationKey, JennyEntity.MenuAction action) {
		addRenderableWidget(new LegacyButton(
			x,
			y,
			112,
			20,
			Component.translatable(translationKey),
			() -> select(action)
		));
	}

	private void select(JennyEntity.MenuAction action) {
		boolean startsPayment = action == JennyEntity.MenuAction.BLOWJOB
			|| action == JennyEntity.MenuAction.BOOBJOB
			|| action == JennyEntity.MenuAction.DOGGY
			|| (action == JennyEntity.MenuAction.STRIP_OR_DRESS && isJennyDressed());

		if (startsPayment) {
			JennyPaymentLock.begin(entityId);
		}

		ClientPlayNetworking.send(new JennyPayloads.SelectAction(entityId, action.ordinal()));
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		for (PriceDisplay price : prices) {
			graphics.item(price.stack, price.x + 13, price.y);
			graphics.fill(price.x - 2, price.y + 3, price.x + 13, price.y + 14, 0xB0000000);
			graphics.text(font, price.count + "x", price.x, price.y + 4, 0xFFFFFFFF, true);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	private record PriceDisplay(int x, int y, ItemStack stack, int count) {
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

			// Recreate the beveled 1.12.2 GuiButton rather than using the
			// modern nine-sliced widget texture.
			graphics.fill(x, y, right, bottom, 0xFF000000);
			graphics.fill(x + 1, y + 1, right - 1, bottom - 1, face);
			graphics.fill(x + 1, y + 1, right - 1, y + 2, 0xFFA0A0A0);
			graphics.fill(x + 1, y + 1, x + 2, bottom - 1, 0xFFA0A0A0);
			graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, 0xFF202020);
			graphics.fill(right - 2, y + 1, right - 1, bottom - 1, 0xFF202020);

			int textColor = isHovered() ? 0xFFFFFFA0 : 0xFFFFFFFF;
			var font = Minecraft.getInstance().font;
			int textX = x + (getWidth() - font.width(getMessage())) / 2;
			int textY = y + (getHeight() - 8) / 2;
			graphics.text(font, getMessage(), textX, textY, textColor, true);
		}

		@Override
		protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}
	}
}

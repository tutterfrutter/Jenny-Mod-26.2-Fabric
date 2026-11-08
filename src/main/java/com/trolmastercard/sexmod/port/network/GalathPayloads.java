package com.trolmastercard.sexmod.port.network;

import com.trolmastercard.sexmod.port.JennyPort;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class GalathPayloads {
	private GalathPayloads() {
	}

	public record OpenMenu(int entityId) implements CustomPacketPayload {
		public static final Type<OpenMenu> TYPE = new Type<>(JennyPort.id("open_galath_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenu> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenMenu::entityId, OpenMenu::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SelectAction(int entityId, int actionId) implements CustomPacketPayload {
		public static final Type<SelectAction> TYPE = new Type<>(JennyPort.id("select_galath_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectAction> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			SelectAction::entityId,
			ByteBufCodecs.VAR_INT,
			SelectAction::actionId,
			SelectAction::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record OpenSceneControls(int entityId) implements CustomPacketPayload {
		public static final Type<OpenSceneControls> TYPE = new Type<>(JennyPort.id("open_galath_scene_controls"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenSceneControls> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenSceneControls::entityId, OpenSceneControls::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CloseSceneControls(int entityId) implements CustomPacketPayload {
		public static final Type<CloseSceneControls> TYPE = new Type<>(JennyPort.id("close_galath_scene_controls"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CloseSceneControls> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, CloseSceneControls::entityId, CloseSceneControls::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SceneCommand(int entityId, int commandId) implements CustomPacketPayload {
		public static final Type<SceneCommand> TYPE = new Type<>(JennyPort.id("galath_scene_command"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SceneCommand> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			SceneCommand::entityId,
			ByteBufCodecs.VAR_INT,
			SceneCommand::commandId,
			SceneCommand::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}

package com.trolmastercard.sexmod.port.network;

import com.trolmastercard.sexmod.port.JennyPort;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class JennyPayloads {
	private JennyPayloads() {
	}

	public record OpenMenu(int entityId) implements CustomPacketPayload {
		public static final Type<OpenMenu> TYPE = new Type<>(JennyPort.id("open_jenny_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenu> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenMenu::entityId, OpenMenu::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SelectAction(int entityId, int actionId) implements CustomPacketPayload {
		public static final Type<SelectAction> TYPE = new Type<>(JennyPort.id("select_jenny_action"));
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
		public static final Type<OpenSceneControls> TYPE =
			new Type<>(JennyPort.id("open_jenny_scene_controls"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenSceneControls> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenSceneControls::entityId, OpenSceneControls::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CloseSceneControls(int entityId) implements CustomPacketPayload {
		public static final Type<CloseSceneControls> TYPE =
			new Type<>(JennyPort.id("close_jenny_scene_controls"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CloseSceneControls> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, CloseSceneControls::entityId, CloseSceneControls::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CameraTransition(
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		int durationTicks
	) implements CustomPacketPayload {
		public static final Type<CameraTransition> TYPE =
			new Type<>(JennyPort.id("jenny_camera_transition"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CameraTransition> CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE,
			CameraTransition::x,
			ByteBufCodecs.DOUBLE,
			CameraTransition::y,
			ByteBufCodecs.DOUBLE,
			CameraTransition::z,
			ByteBufCodecs.FLOAT,
			CameraTransition::yaw,
			ByteBufCodecs.FLOAT,
			CameraTransition::pitch,
			ByteBufCodecs.VAR_INT,
			CameraTransition::durationTicks,
			CameraTransition::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SceneCommand(int entityId, int commandId) implements CustomPacketPayload {
		public static final Type<SceneCommand> TYPE = new Type<>(JennyPort.id("jenny_scene_command"));
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

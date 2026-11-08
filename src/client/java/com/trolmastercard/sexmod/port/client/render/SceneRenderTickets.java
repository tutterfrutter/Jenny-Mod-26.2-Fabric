package com.trolmastercard.sexmod.port.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.google.common.reflect.TypeToken;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Per-frame scene data captured into GeckoLib render states by the scene
 * renderers, and read back by the models and bone layers.
 */
final class SceneRenderTickets {
	/// the first-person rig tree ("steve"-rooted) should render at all
	public static final DataTicket<Boolean> PARTNER_RIG_ACTIVE =
		DataTicket.create("sexmod:partner_rig_active", Boolean.class);
	/// hide the rig's own Head2 (first-person camera inside the head otherwise)
	public static final DataTicket<Boolean> RIG_HEAD_HIDDEN =
		DataTicket.create("sexmod:rig_head_hidden", Boolean.class);
	/// replacement texture for the partner subtree; empty = keep default
	public static final DataTicket<Optional<Identifier>> PARTNER_SKIN =
		DataTicket.create("sexmod:partner_skin", new TypeToken<Optional<Identifier>>() {});
	/// skin model of the scene player (true = slim/Alex arms)
	public static final DataTicket<Boolean> PARTNER_SLIM =
		DataTicket.create("sexmod:partner_slim", Boolean.class);
	/// payment-only visibility filtering active (Jenny's PAYMENT animation)
	public static final DataTicket<Boolean> PAYMENT_FILTER =
		DataTicket.create("sexmod:payment_filter", Boolean.class);
	/// the "body" reference branch of Manglelie's combined model is skipped entirely
	public static final DataTicket<Boolean> SKIP_BODY_BRANCH =
		DataTicket.create("sexmod:skip_body_branch", Boolean.class);
	/// Galath anal scenes render the rig translucent
	public static final DataTicket<Boolean> RIG_TRANSLUCENT =
		DataTicket.create("sexmod:rig_translucent", Boolean.class);
	/// Jenny-specific: the dressed/nude model switch
	public static final DataTicket<Boolean> DRESSED =
		DataTicket.create("sexmod:dressed", Boolean.class);
	/// Galath-specific: hides the wings unless riding
	public static final DataTicket<Boolean> SHOW_WINGS =
		DataTicket.create("sexmod:show_wings", Boolean.class);
	/// Galath-specific: three-person scene uses Manglelie's combined rig
	public static final DataTicket<Boolean> COMBINED_MANGLELIE =
		DataTicket.create("sexmod:combined_manglelie", Boolean.class);
	/// offering the payment item on the itemRenderer bone this pass
	public static final DataTicket<Boolean> OFFERED_ITEM_VISIBLE =
		DataTicket.create("sexmod:offered_item_visible", Boolean.class);
	/// id of the entity a bone listeners callback belongs to
	public static final DataTicket<Integer> SCENE_ENTITY_ID =
		DataTicket.create("sexmod:scene_entity_id", Integer.class);
	/// whether the current action is part of the doggy family
	public static final DataTicket<Boolean> SCENE_IS_DOGGY =
		DataTicket.create("sexmod:scene_is_doggy", Boolean.class);
	/// Manglelie-specific: which scene player's skin owns the rig
	public static final DataTicket<Boolean> GALATH_SCENE_PLAYER_ACTIVE =
		DataTicket.create("sexmod:galath_scene_player_active", Boolean.class);

	private SceneRenderTickets() {
	}
}

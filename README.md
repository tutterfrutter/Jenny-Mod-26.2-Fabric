# Fapcraft: Jenny Fabric port

> **DISCLAIMER:** This repo is **not 100% finished** — it's a hobby port made
> by boredom and purely for the lol. Expect half-working cameras, a
> freeze after scenes I haven't fully squashed yet, and general jank. Do
> not sue me. It builds, it loads, the girls walk — that's a start.

---

Minecraft 26.2 Fabric port containing:

- the `sexmod:jenny`, `sexmod:galath` and `sexmod:manglelie` entities;
- spawn eggs for all three;
- the original dressed and nude Jenny geometry assets, plus the Galath,
  Manglelie and combined threesome geometries;
- the original Jenny texture and animation file, plus Galath/Manglelie ones;
- idle, walk, fast-walk, run, and greeting animation selection;
- all 106 original Jenny sound clips, the Galath voice lines, and the shared
  scene sound library;
- the original base AI parameters: swimming, wandering, looking at nearby
  players, following owners and (Jenny) following emeralds, diamonds, gold
  ingots or ender pearls;
- owner bonding and the persistent `wander`, `follow`, and `stay` NPC modes;
- Jenny's paid interaction scenes (blowjob, boobjob, doggy, strip/dress) with
  the original payment animation, arousal meter and fast/slow/final controls;
- Galath's cowgirl, anal, threesome and ride scenes, including the combined
  three-person rig with Manglelie;
- Manglelie's follow/stay behaviour, scared-run reaction and combined-rig
  animations;
- owner defense with melee attacks;
- original renderer bone-visibility rules (hidden camera/armor/bones,
  first-person head hiding, per-bone texture overrides for the partner rigs).

## Requirements

- Minecraft 26.2
- Java 25 (the game itself requires it; the build needs it too)
- Fabric Loader 0.19.3 or newer
- Fabric API 0.161.0+26.2 or newer
- GeckoLib Fabric 5.5.5 for Minecraft 26.2

## Build

`gradle.properties` already pins the 26.2 toolchain (Loom 1.19.4 requires
Java 25 for Gradle itself). If your system Java is older, point Gradle at a
JDK 25 install, for example:

```bash
JAVA_HOME=$HOME/.jdks/jdk-25.x ./gradlew build
```

On Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-25"
.\gradlew.bat build
```

The mod JAR is written to `build/libs/`.

## Test

Install the mod together with Fabric API and GeckoLib. In Creative mode,
open the `Fapcraft: Jenny` item group and use one of the three spawn eggs.
They can also be obtained with:

```text
/give @s sexmod:jenny_spawn_egg
/give @s sexmod:galath_spawn_egg
/give @s sexmod:manglelie_spawn_egg
```

Right-click Jenny to bond her to the first player and greet her. Sneak +
right-click to cycle between wandering near home, following her owner, and
waiting in place. The owner, home position, and selected mode are saved with
the entity.

After bonding, right-click Jenny again to open her action menu. The restored
original Jenny prices from Fapcraft 1.12.2 are:

- Blowjob: 3 emeralds
- Boobjob: 2 ender pearls
- Doggy: 2 diamonds
- Strip: 1 gold ingot
- Dress up: free

The selected payment is consumed only after a menu choice, then the original
payment animation transitions into the selected action chain. Clothing state
switches during the original strip animation and is saved with the entity.
The same menu also provides follow, stop following, go home, and set new
home. If her owner is attacked, Jenny targets the attacker and uses one of
her three original melee animations.

Galath is bonded the same way (right-click), and her menu contains cowgirl,
anal, threesome (needs a nearby Manglelie) and ride actions plus
follow/stay/set-home. During a paid scene the original arousal meter appears
in the upper-left corner; hold Shift for the fast animation cycle and press
Space once the meter is full to start the finish animation.

## Port notes (1.21.4 -> 26.2)

- 26.2 ships with official (Mojang) class names natively, so all port
  sources use them directly (`net.minecraft.world.item.Item`,
  `net.minecraft.resources.Identifier`, ...). Fabric Loom performs no
  remapping for this version.
- Entity data is saved through `ValueInput`/`ValueOutput` (`addAdditionalSaveData`
  / `readAdditionalSaveData`); UUIDs use the vanilla `UUIDUtil` codec.
- Owner/scene tracking uses `EntityReference` trackers
  (`EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE`).
- GeckoLib moved from `software.bernie.geckolib` to `com.geckolib`. The
  GeckoLib 5 render pipeline is submission-based: the scene renderers capture
  per-frame state (`addRenderData`), hide/re-route bones via bone updaters,
  and re-render partner subtrees with replaced textures in per-bone render
  tasks. The 1.21.4 render hooks (`renderRecursively`,
  `createVerticesOfQuad`, `boneRenderOverride`, `DynamicGeoEntityRenderer`)
  no longer exist.
- The camera is overridden from a `Camera` mixin after the vanilla camera
  aligns with the player (26.2 made the camera setters private).
  `ItemInHandRenderer` hands are hidden from `submitHandsWithItems` instead
  of the old immediate-mode hooks.
- The HUD registers a Fabric `HudElement`; screens override
  `extractRenderState(GuiGraphicsExtractor, ...)` and their buttons extend
  `AbstractWidget` with `extractWidgetRenderState`.

## Known gaps

- The payment item rendering draws a single stack (the 1.21.4 renderer drew
  up to three offset copies).
- Runtime behaviour should be verified in game: mbox the HUD, camera
  transitions and all scene animations were code-reviewed against 26.2 APIs
  but not play-tested in this environment.

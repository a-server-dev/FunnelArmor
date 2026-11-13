# FunnelArmor

Paper 26.3 / Java 25 / Gradle

A prototype Minecraft plugin for four floating sci-fi funnel drones around a player, controlled by a custom leather horse armor item.

## Current prototype (v0.3.2)
- Four selectable ItemDisplay drone types. Each separately obtained controller displays one orbiting drone.
- Right-click fires a cyan particle beam towards the view target, with 2 HP damage.
- Custom Blockbench .bbmodel drones, custom item resource pack, and a /funnel pack command.
- The client needs to accept the resource pack; remote clients need a reachable pack distribution URL.
- This is a prototype; build/runtime verification and deployment configuration are ongoing.

## Development
Open the complete IntelliJ Gradle project (v0.3.0) and use JDK 25. The project files should be committed here separately.

## Historical experiment (reference only)
Before FunnelArmor, a separate IronGolemModelStarter experiment used BetterModel 3.x to apply ferroseed/ferrothorn Blockbench models to only command-spawned iron golems. Tests showed that the model could load, but its initial pose was incorrect until the idle animation `iddleferrothorn` was played. There was also an experiment to replace a witch model using a YSM model, but conversion from the encrypted .ysm format was not completed.

This historical BetterModel work **is not part of FunnelArmor's active source or dependency set**, and should only be revisited if we later want custom-modeled mobs.

## Repository status
The v0.3.1 Java source, all five Blockbench model source files, resource-pack JSON/texture sources and the bundled `src/main/resources/funnel-pack.zip` have been committed. The Gradle Wrapper JAR and launch scripts are not currently committed, so use an installed Gradle 9.1+ via IntelliJ until a valid wrapper is added. Gradle and in-game runtime have not been tested here.

## Individual variants and pack fix (v0.3.2)
- `/funnel give 1`, `/funnel give 2`, `/funnel give 3`, `/funnel give 4` supply one distinct funnel controller each; `/funnel give all` supplies all four.
- Holding one controller displays only its associated floating drone; beam damage remains 2 HP.
- The controller model remains hidden in hand. Each drone uses a unique PDC variant value.
- Fixed missing textures caused by 3D block models referencing item-atlas textures. The resource pack now places its PNG at `assets/funnel/textures/block/funnel.png` with a `funnel:block/funnel` reference.
- The updated resource pack is bundled into `src/main/resources/funnel-pack.zip`. Clients must accept the new pack and reload resources (F3+T). For troubleshooting, inspect `.minecraft/logs/latest.log` after loading.

## Four distinct drone designs (v0.4.0)

The four separately obtainable `.bbmodel` source models were redesigned with different silhouettes:

| Item | Look | Cubes |
| --- | --- | ---: |
| `/funnel give 1` | Tall articulated white-and-navy fin with long parallel panels and a forward emitter | 31 |
| `/funnel give 2` | Compact heavy red armored pod with central cannon and rear thruster | 24 |
| `/funnel give 3` | Wide pearl-white winged petal/butterfly unit with swept wing tips | 22 |
| `/funnel give 4` | Dark-blue X-shaped interceptor with cyan wing edges and long gun | 22 |

All four have unique in-inventory custom models. The hand transforms remain hidden, and the floating ItemDisplay uses the `fixed` transform. These are **original science-fiction designs informed by recognizable funnel silhouettes, not exact copies**.

Gradle produces the embedded resource pack from `resourcepack/` during each build. Use the JAR from a successful Actions run. Reconnect to the server and accept the updated pack (or install the newly generated pack manually). Real in-game rendering still requires visual verification.

## v0.5.0: multi-drone formations
Each item remains separate. `/funnel give 1` deploys **6** tall fin funnels, `/funnel give 2` deploys **6** heavy red funnels, `/funnel give 3` deploys **12** white petal bits (two rings of 6), and `/funnel give 4` deploys **3** white shield funnels with red glowing frames. These are homage-like original designs, not exact copies. The display scale defaults to **1.45** in `drones.model-scale`, with orbit radius **1.65**; adjust the config on an existing server manually. All visible drones emit particle beams on right-click, but a successful hit still deals **2 HP total** (not 2 HP per drone).\n
## v0.6.0: five selectable formations

All four funnel types support these independent, per-item layouts:

| Mode | Japanese | Behavior |
| --- | --- | --- |
| `orbit` | 周回 | Circle the player; 12 bits use two staggered rings |
| `wings` | 左右展開 | Escort the player's left and right flanks |
| `rear` | 背面待機 | Fan formation behind the player |
| `front` | 前方防御 | Arc in front, especially suitable for three shield drones |
| `spiral` | 螺旋 | Rotating three-dimensional helix |

Hold the controller, then **sneak + right-click** to cycle styles (normal right-click still shoots). Use `/funnel formation <orbit|wings|rear|front|spiral|next>` for a named style, and `/funnel formation` to display current selection. The mode is stored in item metadata and survives reconnects. The action bar and item lore reflect the selection. Only the server plugin needs updating; the models and resource pack did not change.

JUnit tests cover formation geometry and all drone counts.

## v0.7.0: Unicorn shield guard (offhand only)

Place the **variant 4 Unicorn-style shield funnel controller** in the offhand and right-click to toggle a forward-facing three-shield defensive barrier. Only variant 4 has this function.

- Toggle with offhand right-click; `/funnel shield` is an alternative if the client uses the main-hand item instead of issuing an offhand interaction.
- Three shield ItemDisplays form a front wall about 1.85 blocks ahead of the player, following the player's yaw and position.
- Intercepts inbound Bukkit Projectile entities whose travel path crosses the front shield field (about 3.4 blocks wide and 0.2–2.65 blocks above the feet). Cancels incoming projectile-hit damage as a fallback.
- Uses a one-tick projectile check and removes intercepted projectiles with particles/sound. It does not consume the shield controller, damage friendly/self-fired projectiles, or protect against melee attacks and area/explosion damage.
- Automatically deactivates on changing world, death, leaving, or removing the shield controller from the offhand. The other three funnel variants cannot activate the shield.
- The normal five deployment patterns and main-hand funnel shooting remain available, but variant 4's normal orbiting displays are suppressed when its offhand guard is active for the same player.

The shield uses the **existing** variant 4 model: only the JAR needs updating. Verify in-game with arrows/tridents and check for conflicts with other projectile-control plugins.

## v0.7.1 — Unicorn shield model v5

The fourth (Unicorn-style) funnel has been replaced by the user's edited
`models/funnel_drone_4.bbmodel` (137 cubes). The model uses a dedicated
256x256 texture (`assets/funnel/textures/item/funnel_drone_4.png`) and
grouped UV regions for editable coloring.

The corresponding game model `assets/funnel/models/item/funnel_drone_4.json`
is exported from the Blockbench model, with coordinates and rotations preserved
and UV coordinates converted from pixels (0–256) to Minecraft's 0–16 units.
The resource-pack build includes it automatically, including both item and
block atlases.

Existing v0.6.0 formations and v0.7.0 offhand shield interception are unchanged.
Replacing the JAR updates the embedded resource pack; no separate GitHub
resource-pack upload is required.

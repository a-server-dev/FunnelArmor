# FunnelArmor

Paper 26.3 / Java 25 / Gradle

A prototype Minecraft plugin for four floating sci-fi funnel drones around a player, controlled by a custom leather horse armor item.

## Current prototype (v0.3.0)
- Four independent ItemDisplay drones orbit and follow the player.
- Right-click fires cyan particle beams towards the view target, with 2 HP damage.
- Custom Blockbench .bbmodel drones, custom item resource pack, and a /funnel pack command.
- The client needs to accept the resource pack; remote clients need a reachable pack distribution URL.
- This is a prototype; build/runtime verification and deployment configuration are ongoing.

## Development
Open the complete IntelliJ Gradle project (v0.3.0) and use JDK 25. The project files should be committed here separately.

## Historical experiment (reference only)
Before FunnelArmor, a separate IronGolemModelStarter experiment used BetterModel 3.x to apply ferroseed/ferrothorn Blockbench models to only command-spawned iron golems. Tests showed that the model could load, but its initial pose was incorrect until the idle animation `iddleferrothorn` was played. There was also an experiment to replace a witch model using a YSM model, but conversion from the encrypted .ysm format was not completed.

This historical BetterModel work **is not part of FunnelArmor's active source or dependency set**, and should only be revisited if we later want custom-modeled mobs.

## Repository status
The GitHub repository is initialized. Full project synchronization has not yet been completed; do not treat this README alone as a buildable Gradle project.

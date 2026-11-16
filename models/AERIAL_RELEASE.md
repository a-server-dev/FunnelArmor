# Aerial GUND-bit model releases (approved, 2026-11-16)

All source models in this folder were created for the FunnelArmor project. No community-provided BBModel was used.

## Final model files

| File | Role | Approved source | Meshes |
| --- | --- | --- | ---: |
| `models/aerial_beam_rifle.bbmodel` | Unmounted/base beam rifle | v0.7 accepted base, extracted without changing its 158 meshes | 158 |
| `models/aerial_escutcheon.bbmodel` | Escutcheon shield, eleven GUND-bit parts | `gund_bits_shield_v03_tight(2).bbmodel` | 74 |
| `models/aerial_gunbit_rifle.bbmodel` | Rifle with all eleven GUND bits and rifle-only customized armor | v1.4 angular muzzle/shortened plier-tip final | 270 |

## Behavior specification (not implemented by this model-only commit)

- Normal GUND-bit mode: eleven independent units fly irregularly. At random ticks, detect nearby hostile mobs and auto-fire.
- Escutcheon: offhand right-click deploys a shield that protects against melee and projectiles. Shield has durability and a cooldown after breaking.
- GUND-bit rifle: main-hand sneak + right-click assembles the rifle and fires five stronger shots. Shooting all five, or canceling the mode, starts a cooldown.

These gameplay features require separate Paper-plugin implementation, model animation, asset export/pack conversion, and in-game validation. The source `.bbmodel` files are not automatically usable by Minecraft as-is. This commit does not modify existing Fin Funnel, Qubeley, or Unicorn mechanics or their resource-pack assets.

## Notes

- The base rifle and Escutcheon are frozen as approved.
- Rifle-only bit geometry may differ from the other two forms, as approved for silhouette accuracy.
- No player-specific machine names or local file paths are embedded in these models.
- Blockbench and in-game runtime validation have not been performed in this commit.
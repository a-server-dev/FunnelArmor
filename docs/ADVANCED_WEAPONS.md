# Advanced weapon reference and first implementation (v0.33.0)

Original models authored for vanilla Minecraft resource packs; no external model assets imported.

- Epyon EW: BANDAI SPIRITS MG product https://www.bandaispirits.co.jp/products/search/detail.php?grp_id=5325&prd_id=4543112703798000 and official RG build article https://www.gundam-base.net/staffblog/45475.html . Shield with red/navy armor, fourteen articulated links and tapered terminal wedge.
- Heavyarms Custom EW: official MG feature https://p-bandai.jp/hobby/special-1000006405/ . Two twin receivers, four independently spinning six-barrel groups.
- Freedom: official MS armament https://gundam-official.com/news/i/special-series/ms-pb/special-series_ms-pb_20111104_678p and MG deployment https://www.gundam-base.net/staffblog/17066.html . Ten articulated blue/navy wing elements, two Balaena shoulder cannons, two Xiphias waist railguns, Lupus rifle.

Commands: /funnel give epyon|heavyarms|freedom [player]. All included in give all; GN distribution remains suspended. Models use existing heavy_weapon_palette. Server-only logic uses bounded ray tracing and ItemDisplays; no block destruction. Damage defaults/caps: Epyon 1.5 HP per target per swing; Heavyarms .5 HP per target per four-gun volley; Freedom 1.5 HP per target per burst. Controls are described in item lore. Initial shapes and attachment poses need in-game verification.

## v0.34.0 refinement
Freedom now has separate CLOSED / HIMAT / FULL_BURST states. Left click toggles stowed/HiMAT (or stows full burst); sneak-left switches HiMAT/full burst; sneak-left while closed first opens HiMAT. Right click fires the rifle in closed/HiMAT; only fully deployed full burst enables five guns and sneak-right multi-lock. Broad flat wing panels replace the detached edge spikes. Epyon shield is rotated 180 degrees, and the rod starts at the actual bottom socket. Existing item IDs, damage and cooldowns are unchanged.

## v0.35.0
Freedom: narrower angular wing armor with integrated diagonal tips; mode and combat unchanged. Heavyarms: vertical twin receivers, scale 1.18, hold right mouse to fire/release to stop using native active-item state, no left-click stop, 15 volleys/s at 20 TPS (1/1/2 tick cadence); heat per volley reduced to 2 to retain the previous overheat time. Existing held weapons migrate automatically; damage, range and effects remain unchanged.

## v0.36.0
Heavyarms only: all four hit rays and 15 volleys/s retained, but one alternating muzzle shows a sparse tracer every other volley (4-block start, 6-block spacing). No muzzle flash or smoke; reduced casing particles. Freedom and Epyon effects are unchanged.

## v0.37.0
Epyon is one gift: green beam sword held by default; left click sword attack (1.5 HP cap), sneak-right equips/stows a real unbreakable vanilla shield with custom model; right click swings the heat rod only while equipped. The rod retracts after each attack. Original offhand ItemStack is serialized in player PDC; restores on stow, weapon change, logout/reload/world change and handles death according to keepInventory. Temporary shield cannot be transferred. Previous Epyon items migrate on hold. The roster remains 14.

## v0.38.0
Freedom rebuilt against the approved front-view design: ten elongated angular dark/bright-blue armor binders, individually layered hinge positions, approximately 5.5–6 block deployed span, compact stow, lower backpack with linked wing roots, larger and higher shoulder cannon deployment, forward hip guns and hand-height rifle. Muzzle offsets use the new display scales. Mode controls, firing behavior, damage and all other weapons remain unchanged. Geometry and projection tested; in-game visual verification still required.

## v0.39.0
Epyon shield: reverse the first- and third-person held-model Y rotations by 180 degrees for both hands, preserving all geometry, offsets, scale, and GUI/ground/fixed transforms. No combat or inventory changes. Blockbench and resource-pack display transforms stay identical.

## v0.40.0
Freedom uses five separately sculpted wing profiles per side (upper sword, upper-middle armored binder, horizontal main blade, lower tapered wing, short auxiliary wing), not repeated scaled geometry. Existing mode, weapon and deployment layout retained, with lower-wing scale compensation for shorter profiles. Heavyarms restores 50% original tracer density (2.30 block spacing vs 1.15), small dust muzzle flash, original smoke and casing count/lifetime; impact sparks remain disabled. Combat and all other weapons unchanged.

## v0.41.0
Action bars are limited to cooldown and charging states (including charge-ready status). Firing, deployment, storage, mode switches, shield durability, ammunition/fire announcements and other routine notices are silent. Heavyarms displays heat only during overheat cooldown. Critical errors and offhand escrow warnings move to chat. Weapon behavior, sounds, particles, and models remain unchanged.

## v0.42.0
Twin Buster Rifle dual and combined shots default to 1.5 HP per target and are capped at 1.5 even with older configs; the legacy dual default 1.25 is treated as 1.5. Beam Saber uses DIAMOND_SWORD and Heat Hawk uses DIAMOND_AXE with their existing item model, PDC, unbreakability, damage, timings, and right-click behavior. Existing tagged feather weapons migrate in the main hand, preserving metadata. Neutral attack attributes and cancellation of extra vanilla sweep damage retain the former combat behavior.

## v0.43.0
Config is authoritative for weapon damage, with no hard-coded maximum. The first upgrade backs up the existing config and copies the listed damage defaults from the bundled config once, marking damage-config-revision: 1. Subsequent reloads/restarts preserve user edits. Twin Buster dual .5 / combined 1.5, Satellite 3 per target per volley, Aerial .5 for both modes. Tagged Beam Saber, Heat Hawk and Barbatos Mace have normal durability; existing held items migrate, keeping metadata. Barbatos uses MACE and retains custom attacks. Heavyarms bypasses existing target hurt cooldown only for its own volleys (configurable), still deduplicating the four muzzles per victim. No model changes.

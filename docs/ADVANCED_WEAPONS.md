# Advanced weapon reference and first implementation (v0.33.0)

Original models authored for vanilla Minecraft resource packs; no external model assets imported.

- Epyon EW: BANDAI SPIRITS MG product https://www.bandaispirits.co.jp/products/search/detail.php?grp_id=5325&prd_id=4543112703798000 and official RG build article https://www.gundam-base.net/staffblog/45475.html . Shield with red/navy armor, fourteen articulated links and tapered terminal wedge.
- Heavyarms Custom EW: official MG feature https://p-bandai.jp/hobby/special-1000006405/ . Two twin receivers, four independently spinning six-barrel groups.
- Freedom: official MS armament https://gundam-official.com/news/i/special-series/ms-pb/special-series_ms-pb_20111104_678p and MG deployment https://www.gundam-base.net/staffblog/17066.html . Ten articulated blue/navy wing elements, two Balaena shoulder cannons, two Xiphias waist railguns, Lupus rifle.

Commands: /funnel give epyon|heavyarms|freedom [player]. All included in give all; GN distribution remains suspended. Models use existing heavy_weapon_palette. Server-only logic uses bounded ray tracing and ItemDisplays; no block destruction. Damage defaults/caps: Epyon 1.5 HP per target per swing; Heavyarms .5 HP per target per four-gun volley; Freedom 1.5 HP per target per burst. Controls are described in item lore. Initial shapes and attachment poses need in-game verification.

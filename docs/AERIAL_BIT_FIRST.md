# Bit-first Escutcheon: lower the equipped shield hand pose v0.8.37

Each of the 11 detached GUND-bit parts remains independently authored. Reference 1 (the user-authored model) guides appearance; reference 3 (Gunpla assembly and loose parts) guides structure. The assembled preview translates those exact 11 geometries, including their rotation pivots.

0.8.37 lowers ONLY native equipped Aerial Escutcheon poses after the v0.8.36 in-game screenshot. Both third-person hands change translation Y from 7 to 5 (idle) and 9 to 7 (guard); first-person hands change Y from 3 to 2 in both states. X/Z, rotations, scales, all 493 geometry cuboids, eleven standalone bits, rear grip, fixed/GUI preview poses, colors, rifle and game behavior remain exactly as v0.8.36. Shield item identifiers and blocking-state selector stay unchanged.

| Bit | Part | Joined offset x,y,z | Cuboids |
| --- | --- | --- | --- |
| 01 | 上部青コア | 0, 9.3, 0.8 | 45 |
| 02 | 左上白ブレード | -1.99, 9.51, 0.12 | 20 |
| 03 | 右上白ブレード | 1.99, 9.51, 0.12 | 20 |
| 04 | 中央白ノーズ | 0, 5.18, 1 | 54 |
| 05 | 左肩青＋黄ベント | -1.82, 7.12, 1.06 | 22 |
| 06 | 右肩青＋黄ベント | 1.82, 7.12, 1.06 | 22 |
| 07 | 左下白外装 | -2.59, -6.98, 0.13 | 97 |
| 08 | 右下白外装 | 2.59, -6.98, 0.13 | 97 |
| 09 | 中央白V字装甲 | 0, 1.02, 0.58 | 65 |
| 10 | 左グレー内装 | -0.72, -6.12, -0.3 | 23 |
| 11 | 右グレー内装 | 0.72, -6.12, -0.3 | 23 |

Total 493 cuboids (11 standalone bits plus 5 rear-only handle parts); shield width 7.21462 vs height 23.07 (height/width ratio 3.19767).

Single source of truth: `tools/generate_aerial_bit_first.mjs`; generates 11 individual models, the joined shield, and the same shield geometry with a blocking hand pose (13 JSON model files). Regenerate with `node tools/generate_aerial_bit_first.mjs` or check with `--check`.

Live Aerial flying bits and native held shield now reference the generated bit-first models. OP previews remain available. The gunbit rifle and original `.bbmodel` sources are unchanged. The five-cuboid rear grip belongs to the shield only. No BetterModel needed.

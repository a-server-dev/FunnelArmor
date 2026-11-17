# Bit-first Escutcheon: larger front-facing native shield pose v0.8.36

Each of the 11 detached GUND-bit parts remains independently authored. Reference 1 (the user-authored model) guides appearance; reference 3 (Gunpla assembly and loose parts) guides structure. The assembled preview translates those exact 11 geometries, including their rotation pivots.

0.8.36 adjusts only the equipped Aerial shield hand display poses after the first 0.8.35 in-game screenshot: the plate now faces more forward (X 75 to 65 degrees; left/right Y 180 to plus/minus 165 degrees), with third-person normal size 1.215 to 1.40 and blocking 1.35 to 1.56. First-person scales also increase modestly. All 493 cuboids, eleven bit models, blue lateral faces, rear grip, fixed GUI and preview poses, rifle, and original BBModel sources remain identical. The item model identifier and shield blocking-state selector remain unchanged.

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

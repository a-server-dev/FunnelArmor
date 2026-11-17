# Bit-first Escutcheon preview: placement-only alignment v0.8.32 (not yet approved)

Each of the 11 detached GUND-bit parts remains independently authored. Reference 1 (the user-authored model) guides appearance; reference 3 (Gunpla assembly and loose parts) guides structure. The assembled preview translates those exact 11 geometries, including their rotation pivots.

0.8.32 is strictly a position-only adjustment based on the user-approved 0.8.31 screenshot. Independent geometry, rotations, the 5-piece rear grip, outer dimensions and actual equipment are unchanged. In the assembly, bits 10/11 move outward, up and backward to form two visibly distinct grey blades; blue shoulders 05/06 move slightly inward and down; central white nose 04 moves slightly up/back, and white V crest 09 slightly down/forward. Every standalone bit JSON remains byte-identical to 0.8.31. Preview-only until the next Minecraft screenshot.

| Bit | Part | Joined offset x,y,z | Cuboids |
| --- | --- | --- | --- |
| 01 | 上部青コア | 0, 9.3, 0.8 | 45 |
| 02 | 左上白ブレード | -1.99, 9.51, 0.12 | 20 |
| 03 | 右上白ブレード | 1.99, 9.51, 0.12 | 20 |
| 04 | 中央白ノーズ | 0, 5.18, 1 | 54 |
| 05 | 左肩青＋黄ベント | -1.88, 7.2, 1.06 | 22 |
| 06 | 右肩青＋黄ベント | 1.88, 7.2, 1.06 | 22 |
| 07 | 左下白外装 | -2.67, -7.12, 0.13 | 97 |
| 08 | 右下白外装 | 2.67, -7.12, 0.13 | 97 |
| 09 | 中央白V字装甲 | 0, 1.02, 0.58 | 65 |
| 10 | 左グレー内装 | -0.72, -6.12, -0.3 | 23 |
| 11 | 右グレー内装 | 0.72, -6.12, -0.3 | 23 |

Total 493 cuboids (11 standalone bits plus 5 rear-only handle parts); shield width 7.37462 vs height 23.21 (height/width ratio 3.14728).

Single source of truth: `tools/generate_aerial_bit_first.mjs` which regenerates all 12 vanilla JSON geometry files with `node tools/generate_aerial_bit_first.mjs` or checks with `--check`.

Previews only: `/funnel aerial preview <1..11|shield|clear>` (OP). Existing normal aerial bits, native equipped shield, rifle and original `.bbmodel` files remain unchanged. No BetterModel required. The dark non-bit rear frame in the picture is not counted among the 11 bits.

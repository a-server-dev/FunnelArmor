# Bit-first Escutcheon preview: angled shoulders and tighter inner armor (not yet approved)

Each of the 11 detached GUND-bit parts remains independently authored. Reference 1 (the user-authored model) guides appearance; reference 3 (Gunpla assembly and loose parts) guides structure. The assembled preview translates those exact 11 geometries, including their rotation pivots.

0.8.30 addresses the supplied in-game screenshot: shoulders 05/06 lean outward by mirrored 22.5-degree vanilla Z rotations and carry framed yellow vents; nose 04 and crest 09 have finer, pointed profiles with a blue V beneath the nose; inner bits 10/11 converge toward a narrow center seam. Upper bits 01/02/03, white outer bits 07/08 (including their shoulder fairings), the five rear grip elements, production equipment, and approved BBModels remain unchanged. Preview-only, pending in-game screenshot validation.

| Bit | Part | Joined offset x,y,z | Cuboids |
| --- | --- | --- | --- |
| 01 | 上部青コア | 0, 9.3, 0.8 | 45 |
| 02 | 左上白ブレード | -1.99, 9.51, 0.12 | 20 |
| 03 | 右上白ブレード | 1.99, 9.51, 0.12 | 20 |
| 04 | 中央白ノーズ | 0, 5.1, 1.02 | 54 |
| 05 | 左肩青＋黄ベント | -1.96, 7.34, 1.06 | 22 |
| 06 | 右肩青＋黄ベント | 1.96, 7.34, 1.06 | 22 |
| 07 | 左下白外装 | -2.67, -7.12, 0.13 | 76 |
| 08 | 右下白外装 | 2.67, -7.12, 0.13 | 76 |
| 09 | 中央白V字装甲 | 0, 1.12, 0.56 | 65 |
| 10 | 左グレー内装 | -0.52, -6.23, -0.24 | 23 |
| 11 | 右グレー内装 | 0.52, -6.23, -0.24 | 23 |

Total 451 cuboids (11 standalone bits plus 5 rear-only handle parts); shield width 7.37462 vs height 23.21 (height/width ratio 3.14728).

Single source of truth: `tools/generate_aerial_bit_first.mjs` which regenerates all 12 vanilla JSON geometry files with `node tools/generate_aerial_bit_first.mjs` or checks with `--check`.

Previews only: `/funnel aerial preview <1..11|shield|clear>` (OP). Existing normal aerial bits, native equipped shield, rifle and original `.bbmodel` files remain unchanged. No BetterModel required. The dark non-bit rear frame in the picture is not counted among the 11 bits.

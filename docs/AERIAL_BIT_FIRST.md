# Bit-first Escutcheon preview: compact front panels (not yet approved)

Each of the 11 detached GUND-bit parts is separately authored from the right-hand part layout in the user's photo. The assembled preview contains only those exact 11 geometries translated into the arrangement shown on the LEFT side of the photo.

0.8.24 extends the INSIDE edges of the two lower white GUND parts toward the grey central elements without widening the outer silhouette. It lifts the grey elements closer to the central V part, slims the shoulders, and replaces many horizontal face bands with long continuous white plates. This is still preview only.

| Bit | Part | Joined offset x,y,z | Cuboids |
| --- | --- | --- | --- |
| 01 | 上部青コア | 0, 9.3, 0.8 | 45 |
| 02 | 左上白ブレード | -1.99, 9.51, 0.12 | 20 |
| 03 | 右上白ブレード | 1.99, 9.51, 0.12 | 20 |
| 04 | 中央白ノーズ | 0, 5.1, 1.02 | 16 |
| 05 | 左肩青＋黄ベント | -2.08, 7.23, 1.06 | 16 |
| 06 | 右肩青＋黄ベント | 2.08, 7.23, 1.06 | 16 |
| 07 | 左下白外装 | -2.67, -7.12, 0.13 | 69 |
| 08 | 右下白外装 | 2.67, -7.12, 0.13 | 69 |
| 09 | 中央白V字装甲 | 0, 1.12, 0.56 | 16 |
| 10 | 左グレー内装 | -0.91, -6.23, -0.24 | 23 |
| 11 | 右グレー内装 | 0.91, -6.23, -0.24 | 23 |

Total 333 cuboids; shield width 7.37462 vs height 23.21 (height/width ratio 3.14728).

Single source of truth: `tools/generate_aerial_bit_first.mjs` which regenerates all 12 vanilla JSON geometry files with `node tools/generate_aerial_bit_first.mjs` or checks with `--check`.

Previews only: `/funnel aerial preview <1..11|shield|clear>` (OP). Existing normal aerial bits, native equipped shield, rifle and original `.bbmodel` files remain unchanged. No BetterModel required. The dark non-bit rear frame in the picture is not counted among the 11 bits.

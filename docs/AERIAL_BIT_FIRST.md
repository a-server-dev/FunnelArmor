# Bit-first Escutcheon preview: angular restoration (not yet approved)

Each of the 11 detached GUND-bit parts is separately authored from the right-hand part layout in the user's photo. The assembled preview contains only those exact 11 geometries translated into the arrangement shown on the LEFT side of the photo.

0.8.23 restores the narrow angular white panels of 0.8.21. The rounded bulges from 0.8.22 are removed. The exposed three blue-core apertures, thin upper fin tips, longer pointed nose, compact gold vents and blue edge trim remain. The grey inner pieces are slightly wider and shorter, set behind the white shells. Redundant white panel overlays have been removed.

| Bit | Part | Joined offset x,y,z | Cuboids |
| --- | --- | --- | --- |
| 01 | 上部青コア | 0, 9.3, 0.8 | 53 |
| 02 | 左上白ブレード | -1.99, 9.51, 0.12 | 30 |
| 03 | 右上白ブレード | 1.99, 9.51, 0.12 | 30 |
| 04 | 中央白ノーズ | 0, 5.1, 1.02 | 23 |
| 05 | 左肩青＋黄ベント | -2.19, 7.13, 1.06 | 20 |
| 06 | 右肩青＋黄ベント | 2.19, 7.13, 1.06 | 20 |
| 07 | 左下白外装 | -2.67, -7.12, 0.13 | 67 |
| 08 | 右下白外装 | 2.67, -7.12, 0.13 | 67 |
| 09 | 中央白V字装甲 | 0, 1.42, 0.47 | 22 |
| 10 | 左グレー内装 | -0.94, -6.71, -0.28 | 23 |
| 11 | 右グレー内装 | 0.94, -6.71, -0.28 | 23 |

Total 378 cuboids; shield width 7.37462 vs height 23.21 (height/width ratio 3.14728).

Single source of truth: `tools/generate_aerial_bit_first.mjs` which regenerates all 12 vanilla JSON geometry files with `node tools/generate_aerial_bit_first.mjs` or checks with `--check`.

Previews only: `/funnel aerial preview <1..11|shield|clear>` (OP). Existing normal aerial bits, native equipped shield, rifle and original `.bbmodel` files remain unchanged. No BetterModel required. The dark non-bit rear frame in the picture is not counted among the 11 bits.

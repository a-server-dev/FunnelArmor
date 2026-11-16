# Bit-first Escutcheon preview: shoulder seam bridging (not yet approved)

Each of the 11 detached GUND-bit parts is separately authored from the right-hand part layout in the user's photo. The assembled preview contains only those exact 11 geometries translated into the arrangement shown on the LEFT side of the photo.

0.8.27 connects lower white outer bits 07/08 to the blue shoulders 05/06 from behind. Each white outer bit now has a short stepped white collar and narrow blue edge connector at its top; these reach into the blue shoulder in frontal projection, closing the sky gap without widening the shield or touching the blue/white central nose. The shield-only five-piece charcoal rear hand grip from 0.8.26 remains exactly the same. No production equipment or approved BBModel changes.

| Bit | Part | Joined offset x,y,z | Cuboids |
| --- | --- | --- | --- |
| 01 | 上部青コア | 0, 9.3, 0.8 | 45 |
| 02 | 左上白ブレード | -1.99, 9.51, 0.12 | 20 |
| 03 | 右上白ブレード | 1.99, 9.51, 0.12 | 20 |
| 04 | 中央白ノーズ | 0, 5.1, 1.02 | 21 |
| 05 | 左肩青＋黄ベント | -2.08, 7.23, 1.06 | 23 |
| 06 | 右肩青＋黄ベント | 2.08, 7.23, 1.06 | 23 |
| 07 | 左下白外装 | -2.67, -7.12, 0.13 | 73 |
| 08 | 右下白外装 | 2.67, -7.12, 0.13 | 73 |
| 09 | 中央白V字装甲 | 0, 1.12, 0.56 | 16 |
| 10 | 左グレー内装 | -0.91, -6.23, -0.24 | 23 |
| 11 | 右グレー内装 | 0.91, -6.23, -0.24 | 23 |

Total 365 cuboids (11 standalone bits plus 5 rear-only handle parts); shield width 7.37462 vs height 23.21 (height/width ratio 3.14728).

Single source of truth: `tools/generate_aerial_bit_first.mjs` which regenerates all 12 vanilla JSON geometry files with `node tools/generate_aerial_bit_first.mjs` or checks with `--check`.

Previews only: `/funnel aerial preview <1..11|shield|clear>` (OP). Existing normal aerial bits, native equipped shield, rifle and original `.bbmodel` files remain unchanged. No BetterModel required. The dark non-bit rear frame in the picture is not counted among the 11 bits.

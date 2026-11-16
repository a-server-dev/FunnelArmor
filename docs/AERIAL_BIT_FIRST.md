# Bit-first Escutcheon preview, v4 primary front reference (not yet approved)

Each of the 11 detached GUND-bit parts is separately authored from the right-hand part layout in the user's photo. The assembled preview contains only those exact 11 geometries translated into the arrangement shown on the LEFT side of the photo.

This 0.8.22 iteration targets the SECOND image (stylized frontal joined shield) as the primary silhouette. The THIRD physical photo defines the 11 separate component shapes. Compared with 0.8.21, the white upper fins are narrower and longer; the blue core's three sockets move in front of its surfaces; the white central nose is longer and sharper; gold shoulder vents become compact and slanted; the side armor is broadened inward with blue exterior edge; the grey center blades are inset and gain blue/cyan channels. Redundant per-slice raised face plates were removed to reduce zebra striping.

| Bit | Part | Joined offset x,y,z | Cuboids |
| --- | --- | --- | --- |
| 01 | 上部青コア | 0, 9.28, 1 | 43 |
| 02 | 左上白ブレード | -2.07, 9.68, 0.25 | 17 |
| 03 | 右上白ブレード | 2.07, 9.68, 0.25 | 17 |
| 04 | 中央白ノーズ | 0, 4.62, 1.4 | 16 |
| 05 | 左肩青＋黄ベント | -2.27, 7.17, 1.04 | 15 |
| 06 | 右肩青＋黄ベント | 2.27, 7.17, 1.04 | 15 |
| 07 | 左下白外装 | -3.41, -8.32, 0.25 | 78 |
| 08 | 右下白外装 | 3.41, -8.32, 0.25 | 78 |
| 09 | 中央白V字装甲 | 0, 0.85, 1 | 14 |
| 10 | 左グレー内装 | -1.24, -8.2, 0.08 | 22 |
| 11 | 右グレー内装 | 1.24, -8.2, 0.08 | 22 |

Total 337 cuboids; shield width 9.85724 vs height 24.75 (height/width ratio 2.51084).

Single source of truth: `tools/generate_aerial_bit_first.mjs` which regenerates all 12 vanilla JSON geometry files with `node tools/generate_aerial_bit_first.mjs` or checks with `--check`.

Previews only: `/funnel aerial preview <1..11|shield|clear>` (OP). Existing normal aerial bits, native equipped shield, rifle and original `.bbmodel` files remain unchanged. No BetterModel required. The dark non-bit rear frame in the picture is not counted among the 11 bits.

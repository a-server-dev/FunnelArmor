# Bit-first Escutcheon preview, v3 revised fit (not yet approved)

Each of the 11 detached GUND-bit parts is separately authored from the right-hand part layout in the user's photo. The assembled preview contains only those exact 11 geometries translated into the arrangement shown on the LEFT side of the photo.

The first (0.8.20) draft looked like an exploded shield because part placements copied the spacing of the loose pieces. In 0.8.21, each join transform is tightened, the upper core/side-armor/nose/crest and white/grey lower rails are brought into contact, the white palette is corrected, and new layered bevels, blue outlines, gold vent and nozzle-ring details are included.

| Bit | Part | Joined offset x,y,z | Cuboids |
| --- | --- | --- | --- |
| 01 | 上部青コア | 0, 9.45, 0.56 | 70 |
| 02 | 左上白ブレード | -1.92, 9.52, 0.12 | 50 |
| 03 | 右上白ブレード | 1.92, 9.52, 0.12 | 50 |
| 04 | 中央白ノーズ | 0, 6.13, 0.84 | 32 |
| 05 | 左肩青＋黄ベント | -2.17, 7.2, 0.92 | 33 |
| 06 | 右肩青＋黄ベント | 2.17, 7.2, 0.92 | 33 |
| 07 | 左下白外装 | -2.5, -7.1, 0.04 | 189 |
| 08 | 右下白外装 | 2.5, -7.1, 0.04 | 189 |
| 09 | 中央白V字装甲 | 0, 1.69, 0.47 | 38 |
| 10 | 左グレー内装 | -1, -7.02, 0.04 | 75 |
| 11 | 右グレー内装 | 1, -7.02, 0.04 | 75 |

Total 834 cuboids; shield width 7.144 vs height 22.6 (height/width ratio 3.16349).

Single source of truth: `tools/generate_aerial_bit_first.mjs` which regenerates all 12 vanilla JSON geometry files with `node tools/generate_aerial_bit_first.mjs` or checks with `--check`.

Previews only: `/funnel aerial preview <1..11|shield|clear>` (OP). Existing normal aerial bits, native equipped shield, rifle and original `.bbmodel` files remain unchanged. No BetterModel required. The dark non-bit rear frame in the picture is not counted among the 11 bits.

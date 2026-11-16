# Aerial bit-first preview (not yet approved)

The eleven GUND bits are independently authored from the user's physical disassembled shield reference. The joined shield is generated **only by translating the exact same eleven bit geometries**, without sourcing geometry from the old assembled shield.

| Bit | Reference part | Joined X,Y,Z in pixels |
| --- | --- | --- |
| 01 | 上部青コア | 0, 8.1, 0.4 |
| 02 | 左上白ロングブレード | -2.4, 9.13, 0.1 |
| 03 | 右上白ロングブレード | 2.4, 9.13, 0.1 |
| 04 | 中央白ノーズ | 0, 3.98, 0.46 |
| 05 | 左肩青・黄色ベント | -3.69, 2.36, 0.75 |
| 06 | 右肩青・黄色ベント | 3.69, 2.36, 0.75 |
| 07 | 左下白ロングレール | -4.3, -13.3, 0 |
| 08 | 右下白ロングレール | 4.3, -13.3, 0 |
| 09 | 中央白V字アーマー | 0, -3.1, 0.84 |
| 10 | 左グレー内側ブレード | -1.43, -12.6, 0.06 |
| 11 | 右グレー内側ブレード | 1.43, -12.6, 0.06 |

The separate charcoal-colored frame at the far right of the reference photo is a mounting frame, not one of the eleven detached GUND-bit parts. This preview does not include it.

**Source of truth**: `tools/generate_aerial_bit_first.mjs`; `node tools/generate_aerial_bit_first.mjs` regenerates all models, and `--check` verifies the committed pack. All colors come from the existing `aerial_escutcheon.png` palette. No `.bbmodel` is altered or uploaded.

Use `/funnel aerial preview 1` through `11` (OP-only) to view one piece or `/funnel aerial preview shield` to see all eleven joined. `/funnel aerial preview clear` removes the temporary nonpersistent display. Production GUND bits and shield remain unchanged until approved.

The preview is a first-pass, vanilla cuboid approximation intended for iterative inspection of each piece's taper, layering, accent colors and 11-to-1 fit.

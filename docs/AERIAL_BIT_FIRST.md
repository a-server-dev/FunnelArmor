# Escutcheon V3 import (0.29.0)
The supplied aerial_v3_escutcheon archive replaces the active V2-named models. The live shield item still uses the native using_item selector and the flying bits retain existing model keys. No client mod or new items required.

491 assembled cubes = 486 detached bit cubes plus 5 rear handle cubes. The exact imported normal shield is canonical; tools/generate_aerial_bit_first.mjs reconstructs detached models with the archive's offsets and decimal rounding, and the blocking pose. Use node tools/generate_aerial_bit_first.mjs --check.

Normal third-person Y=2, scale=1.40; blocking Y=5, scale=1.56. First-person normal Y=1, blocking Y=2. The supplied transforms are preserved. The rifle is unchanged.

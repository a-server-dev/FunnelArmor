# FunnelArmor

Paper 26.3 / Java 25 / Gradle

A prototype Minecraft plugin for four floating sci-fi funnel drones around a player, controlled by a custom leather horse armor item.

## Current prototype (v0.3.2)
- Four selectable ItemDisplay drone types. Each separately obtained controller displays one orbiting drone.
- Right-click fires a cyan particle beam towards the view target, with 2 HP damage.
- Custom Blockbench .bbmodel drones, custom item resource pack, and a /funnel pack command.
- The client needs to accept the resource pack; remote clients need a reachable pack distribution URL.
- This is a prototype; build/runtime verification and deployment configuration are ongoing.

## Development
Open the complete IntelliJ Gradle project (v0.3.0) and use JDK 25. The project files should be committed here separately.

## Historical experiment (reference only)
Before FunnelArmor, a separate IronGolemModelStarter experiment used BetterModel 3.x to apply ferroseed/ferrothorn Blockbench models to only command-spawned iron golems. Tests showed that the model could load, but its initial pose was incorrect until the idle animation `iddleferrothorn` was played. There was also an experiment to replace a witch model using a YSM model, but conversion from the encrypted .ysm format was not completed.

This historical BetterModel work **is not part of FunnelArmor's active source or dependency set**, and should only be revisited if we later want custom-modeled mobs.

## Repository status
The v0.3.1 Java source, all five Blockbench model source files, resource-pack JSON/texture sources and the bundled `src/main/resources/funnel-pack.zip` have been committed. The Gradle Wrapper JAR and launch scripts are not currently committed, so use an installed Gradle 9.1+ via IntelliJ until a valid wrapper is added. Gradle and in-game runtime have not been tested here.

## Individual variants and pack fix (v0.3.2)
- `/funnel give 1`, `/funnel give 2`, `/funnel give 3`, `/funnel give 4` supply one distinct funnel controller each; `/funnel give all` supplies all four.
- Holding one controller displays only its associated floating drone; beam damage remains 2 HP.
- The controller model remains hidden in hand. Each drone uses a unique PDC variant value.
- Fixed missing textures caused by 3D block models referencing item-atlas textures. The resource pack now places its PNG at `assets/funnel/textures/block/funnel.png` with a `funnel:block/funnel` reference.
- The updated resource pack is bundled into `src/main/resources/funnel-pack.zip`. Clients must accept the new pack and reload resources (F3+T). For troubleshooting, inspect `.minecraft/logs/latest.log` after loading.

## Four distinct drone designs (v0.4.0)

The four separately obtainable `.bbmodel` source models were redesigned with different silhouettes:

| Item | Look | Cubes |
| --- | --- | ---: |
| `/funnel give 1` | Tall articulated white-and-navy fin with long parallel panels and a forward emitter | 31 |
| `/funnel give 2` | Compact heavy red armored pod with central cannon and rear thruster | 24 |
| `/funnel give 3` | Wide pearl-white winged petal/butterfly unit with swept wing tips | 22 |
| `/funnel give 4` | Dark-blue X-shaped interceptor with cyan wing edges and long gun | 22 |

All four have unique in-inventory custom models. The hand transforms remain hidden, and the floating ItemDisplay uses the `fixed` transform. These are **original science-fiction designs informed by recognizable funnel silhouettes, not exact copies**.

Gradle produces the embedded resource pack from `resourcepack/` during each build. Use the JAR from a successful Actions run. Reconnect to the server and accept the updated pack (or install the newly generated pack manually). Real in-game rendering still requires visual verification.

## v0.5.0: multi-drone formations
Each item remains separate. `/funnel give 1` deploys **6** tall fin funnels, `/funnel give 2` deploys **6** heavy red funnels, `/funnel give 3` deploys **12** white petal bits (two rings of 6), and `/funnel give 4` deploys **3** white shield funnels with red glowing frames. These are homage-like original designs, not exact copies. The display scale defaults to **1.45** in `drones.model-scale`, with orbit radius **1.65**; adjust the config on an existing server manually. All visible drones emit particle beams on right-click, but a successful hit still deals **2 HP total** (not 2 HP per drone).\n
## v0.6.0: five selectable formations

All four funnel types support these independent, per-item layouts:

| Mode | Japanese | Behavior |
| --- | --- | --- |
| `orbit` | 周回 | Circle the player; 12 bits use two staggered rings |
| `wings` | 左右展開 | Escort the player's left and right flanks |
| `rear` | 背面待機 | Fan formation behind the player |
| `front` | 前方防御 | Arc in front, especially suitable for three shield drones |
| `spiral` | 螺旋 | Rotating three-dimensional helix |

Hold the controller, then **sneak + right-click** to cycle styles (normal right-click still shoots). Use `/funnel formation <orbit|wings|rear|front|spiral|next>` for a named style, and `/funnel formation` to display current selection. The mode is stored in item metadata and survives reconnects. The action bar and item lore reflect the selection. Only the server plugin needs updating; the models and resource pack did not change.

JUnit tests cover formation geometry and all drone counts.

## v0.7.0: Unicorn shield guard (offhand only)

Place the **variant 4 Unicorn-style shield funnel controller** in the offhand and right-click to toggle a forward-facing three-shield defensive barrier. Only variant 4 has this function.

- Toggle with offhand right-click; `/funnel shield` is an alternative if the client uses the main-hand item instead of issuing an offhand interaction.
- Three shield ItemDisplays form a front wall about 1.85 blocks ahead of the player, following the player's yaw and position.
- Intercepts inbound Bukkit Projectile entities whose travel path crosses the front shield field (about 3.4 blocks wide and 0.2–2.65 blocks above the feet). Cancels incoming projectile-hit damage as a fallback.
- Uses a one-tick projectile check and removes intercepted projectiles with particles/sound. It does not consume the shield controller, damage friendly/self-fired projectiles, or protect against melee attacks and area/explosion damage.
- Automatically deactivates on changing world, death, leaving, or removing the shield controller from the offhand. The other three funnel variants cannot activate the shield.
- The normal five deployment patterns and main-hand funnel shooting remain available, but variant 4's normal orbiting displays are suppressed when its offhand guard is active for the same player.

The shield uses the **existing** variant 4 model: only the JAR needs updating. Verify in-game with arrows/tridents and check for conflicts with other projectile-control plugins.

## v0.7.1 — Unicorn shield model v5

The fourth (Unicorn-style) funnel has been replaced by the user's edited
`models/funnel_drone_4.bbmodel` (137 cubes). The model uses a dedicated
256x256 texture (`assets/funnel/textures/item/funnel_drone_4.png`) and
grouped UV regions for editable coloring.

The corresponding game model `assets/funnel/models/item/funnel_drone_4.json`
is exported from the Blockbench model, with coordinates and rotations preserved
and UV coordinates converted from pixels (0–256) to Minecraft's 0–16 units.
The resource-pack build includes it automatically, including both item and
block atlases.

Existing v0.6.0 formations and v0.7.0 offhand shield interception are unchanged.
Replacing the JAR updates the embedded resource pack; no separate GitHub
resource-pack upload is required.

## v0.7.2 — Shield facing and animated actions

- **Offhand Unicorn guard** now faces the opposite way from v0.7.1, fixing the reversed front/back face. Set `shield.facing-yaw-offset: 180` to switch it back for custom models.
- Right-click with the Unicorn controller in the offhand: the three shields glide from near the shoulders into a front defense wall over `shield.deploy-ticks: 12` (ease-in/out), turning to face front as they move.
- Right-click again: the shields return to the shoulders and disappear over `shield.retract-ticks: 12`. If you switch modes mid-animation they smoothly reverse.
- Projectile protection activates after the wall finishes deploying, and is immediately disabled when retraction starts. Defense is not active during the initial deployment, making the visual and gameplay states consistent.
- Normal **main-hand right-click shooting** now gives deployed drones a brief forward firing-pulse motion (8 ticks, up to `drones.shot-recoil-blocks`). The beam damage and cooldown are unchanged.
- The five formations, all model files and the embedded pack remain otherwise unchanged.

## v0.7.3 — Aim the muzzle and arrange a 3-petal shield

Uses the existing v5 Unicorn model and `ItemDisplay` rotations. No BetterModel,
external animation plugin, changed bbmodel or resource-pack update is required.

- **Main-hand right-click (variant 4 only)**: while firing, each visible
  shield rotates its full model so the two lower gun muzzles (model -Y) aim
  at the exact ray-trace target (entity/block or range end). The rotation
  ramps up and returns to idle over 8 ticks, alongside the existing recoil.
  Other funnel models are unaffected; damage/cooldown remain unchanged.
- **Offhand shield defense**: three ItemDisplays glide together into a
  **three-petal rosette**. The bottom shield and the upper-left / upper-right
  shields overlap at the center. The upper shields roll -120/+120 degrees
  around the model face (local Z); their Z depths are slightly staggered to
  prevent texture flicker. The protective hit-plane and 12-tick transitions
  are unchanged.
- Pure geometry/JUnit tests cover the completed flower arrangement,
  progressive animation, and barrel-aim rotations toward targets at
  multiple player yaw angles.
- A whole-model rotation is used for aiming: moving the twin barrels alone
  would require separating the model into individually controlled displays.

## v0.7.4 — Instant Unicorn muzzle snap and vertical spiral tower

- **Unicorn main-hand firing:** twin barrels now turn to the ray-trace target
  **immediately on right-click**, instead of easing into the aim over several
  ticks. Aim is held through tick 3, and eases back during ticks 4–8. The return
  completion time remains 8 ticks; existing 8-tick firing recoil is unchanged.
  An immediate `ItemDisplay` update with zero interpolation ensures the initial
  snap does not wait for the normal drone update loop.
- **Spiral formation:** redesigned from a mostly flat orbit to a visibly
  **vertical double-helix tower**. Drones form two opposite rotating strands,
  with 3.6 blocks of height separation. Variant 4 has 2 tiers, variants 1/2
  have 3 tiers, and variant 3 has 6 tiers. The `spiral` selection remains
  available in the same formation cycle.
- Other formations, offhand three-petal shield defense, model textures, and
  damage/cooldown values are unchanged.

## v0.7.5 — Cloudflare R2 automatic resource-pack publishing

GitHub Actions creates the JAR and the **exact same** resource-pack ZIP from
`resourcepack/`. After a successful build on `main`, it can upload the ZIP
to your Cloudflare R2 bucket without opening a port on the Paper/Velocity PCs.

### Cloudflare one-time setup

1. In Cloudflare R2, create a bucket (e.g. `funnelarmor-packs`).
2. Under bucket **Settings**, enable public access using a **Custom Domain**
   (recommended for regular usage). You can use the automatically assigned
   **r2.dev Public Development URL** for testing only; it is rate-limited.
3. Under **R2 > Manage API Tokens**, create an S3-compatible API token
   restricted to that bucket with **Object Read & Write** permission.
   Copy its Access Key ID and Secret Access Key; never paste them into Git
   or your Paper `config.yml`.
4. In this GitHub repository, go to **Settings > Secrets and variables > Actions**.
   Add **Repository variables** `R2_ACCOUNT_ID` (Cloudflare account ID),
   `R2_BUCKET` (bucket name), and optionally `R2_PUBLIC_BASE_URL`
   (the public HTTPS URL, **not** the S3 API endpoint).
   Add **Repository secrets** `R2_ACCESS_KEY_ID` and
   `R2_SECRET_ACCESS_KEY`.
5. Run the GitHub Actions workflow on `main` (or make any commit).
   The `publish-r2` job runs **only when** the R2 account ID and bucket
   variables are present and the build succeeded. The job will stop with
   a clear error if its credentials are missing.
6. On your Paper PC, update `plugins/FunnelArmor/config.yml`:

   ```yaml
   pack:
     send-on-join: true
     require: false
     external-base-url: "https://packs.example.com"  # YOUR public R2 domain
     external-url: ""
     embedded-http:
       enabled: false
   ```

   Edit these keys in your existing config; keep the remaining settings
   unchanged. Use your actual public R2 base URL, without a ZIP filename.
   Replace the Paper JAR with the **matching JAR from the same Actions run**
   that successfully published to R2, then restart Paper.

### Behavior and diagnostics

- The uploaded filename is `funnel-pack-<ZIP-SHA1>.zip`; the plugin
  computes that exact name from the SHA-1 of its embedded ZIP.
  Example result: `https://packs.example.com/funnel-pack-<SHA1>.zip`.
  Each release has an immutable URL, preventing stale CDN files and mismatched
  JAR/packs. Do not delete older pack objects while old JARs are in use.
- Test from an external client: `/funnel status` shows the URL, hash and
  size. `/funnel pack` requests the pack again. Check Paper console for
  `SUCCESSFULLY_LOADED` or `FAILED_DOWNLOAD`.
- If `publish-r2` says **skipped**, set the GitHub Actions repository
  variables. If it fails on upload, check the R2 S3 API credentials and
  bucket-scoped permissions. If the upload succeeds but loading fails, check
  that the **public** domain is enabled and the Paper config uses it.
- Do not put your private S3 API endpoint
  (`https://<ACCOUNT_ID>.r2.cloudflarestorage.com`) in `external-base-url`.
  That endpoint requires authentication and will not serve a public pack.
- `pack.external-url` remains an exact URL override. When either external
  URL or external base URL is present, the embedded HTTP listener is disabled,
  so **no new port forwarding** is needed.
- Publishing is intentionally skipped for pull requests and remains inactive
  until your own Cloudflare account is configured.

## v0.7.6 — Spiral model facing fix

The tall double-helix path and 3.6-block height stay the same. Correct
the model-facing yaw from `angle + 90°` to `angle - 90°`: the previous
tangent formula accidentally pointed the funnels **inward** toward the
helix center, showing their backs/edges. They now face **outward**.

This does not change the direction of travel / helix winding, the other
four formation modes, the main-hand aiming animation, or the offhand
three-petal defense. No resource-pack or Blockbench edits are needed.
A geometry test verifies the outward orientation at several ticks
across all four models.

## v0.7.7 — GitHub Releases distribution, replacing R2

**No credit card, Cloudflare account, R2 API token or Paper port forwarding
is required** for this distribution method.

GitHub Actions builds the Java JAR, tests and bundled ZIP together. For each
successful `main` build **in a public repository**, the `publish-release`
job uses the built-in `GITHUB_TOKEN` (`contents: write`) to publish the
identical ZIP as a GitHub Release asset:

```text
https://github.com/a-server-dev/FunnelArmor/releases/download/pack-<SHA1>/funnel-pack-<SHA1>.zip
```

The `<SHA1>` portion is the lowercase 40-character SHA-1 checksum of the
**bundled ZIP**, not the JAR. Every URL is immutable and old pack releases can
be reused without overwrites. The workflow confirms anonymous public ZIP
download and verifies its SHA-1 against the generated ZIP.

The plugin offers this link automatically on join, without changing model,
combat, beam, or formation behavior. Set `pack.github-releases.enabled: true`
and `pack.github-releases.repository: "a-server-dev/FunnelArmor"` (defaults
also apply to older config files). The existing `pack.external-url` and
`pack.external-base-url` may override it. Its embedded HTTP listener is
disabled when GitHub Releases is active. `/funnel pack` sends it again.

### Important: current repository privacy

**a-server-dev/FunnelArmor is currently PRIVATE.** GitHub Release assets
inside private repositories are **not anonymously accessible** to Minecraft
clients. Accordingly, `publish-release` is deliberately skipped while
this repository is private. Its JAR and ZIP build artifacts remain available
to authorized repository users, but are **NOT playable public pack URLs**.
The GitHub workflow does not alter visibility. To activate public delivery:

1. **Either** intentionally change this repository to PUBLIC **after auditing
   all files and historical commits for sensitive information**; then
   re-run the `Build and publish resource pack` workflow on `main`.
2. **Or**, to keep the source private, use a separate PUBLIC GitHub repository
   dedicated to pack releases, and configure an authorized cross-repository
   write token / GitHub App credential for its release publishing. That
   cannot be done with this source repository's `GITHUB_TOKEN` alone.

Once a public release has been successfully uploaded and externally verified,
install **the matching JAR from the same successful workflow** and restart
Paper. For existing server configs, leave any previous R2 override URL
fields blank. `/funnel status` shows the actual SHA-1 and URL.

GitHub Release asset downloads use HTTPS redirects. Although the workflow
checks that a public unauthenticated client can download the ZIP and verify
its SHA-1, actual Minecraft client compatibility should still be verified.

## v0.7.8 — Public GitHub Releases and quiet runtime messages

- Once the repository is public, a successful GitHub Actions build on main automatically uploads the exact resource-pack ZIP to a publicly accessible GitHub Release.
- Commented out repeated in-game formation-switch and shield deploy/return action-bar and chat notices.
- The player can still explicitly query a mode with /funnel formation. Errors remain visible.
- Routine pack-offer, success and server-log messages are commented out. Download failures remain visible and direct users to /funnel status.
- No changes to models, gameplay, flight paths or resource-pack bytes.

## v0.7.9 — Direct GitHub raw resource-pack delivery

The GitHub Releases ZIP was confirmed available and checksum-valid from GitHub Actions,
but some Minecraft clients can fail while following its short-lived signed CDN redirects.
The exact client failure reason requires inspection of the player's latest.log.

To remove that potential failure point, a separate publish-direct Actions job now
commits the identical ZIP to the public pack-assets branch at an immutable filename
funnel-pack-<ZIP_SHA1>.zip. The job checks HTTP 200, ZERO redirects and downloaded
SHA-1 byte equality. It uses only GITHUB_TOKEN and does not need a credit card or
port forwarding. The GitHub Releases workflow continues as a fallback/archive.

By default (including preexisting config.yml), the plugin downloads from:
https://raw.githubusercontent.com/a-server-dev/FunnelArmor/pack-assets/funnel-pack-<SHA1>.zip

Set pack.github-raw.enabled: false to return to the GitHub Releases URL.
Explicit pack.external-url and pack.external-base-url overrides take precedence.
The server must use the matching JAR after publish-direct succeeds. If the client
still reports FAILED_DOWNLOAD, open .minecraft/logs/latest.log for the HTTP or TLS
exception; the server can only see the status, not the client's exact error.

## v0.7.10 — Manual-only pack delivery and command tab completion

- Login never automatically calls the Minecraft resource-pack prompt.
- Players only receive the resource pack when `/funnel pack` is executed
  explicitly; admins may send it to an online user with `/funnel pack <player>`.
- `pack.send-on-join` is set to `false` in the bundled config and
  is no longer used by the code, even if an existing config still says true.
- `/funnel <TAB>`: displays usable subcommands with permission filtering.
- `/funnel formation <TAB>`: orbit, wings, rear, front, spiral, next.
- `/funnel give <TAB>`: 1, 2, 3, 4, all (admin only); a third argument
  suggests online player names.
- `/funnel pack <TAB>`: online players (admin only), otherwise no target.
- Existing commands, combat behavior, resource pack bytes, models, and
  published GitHub ZIP URLs are unchanged.

## v0.7.13 — Uniform fin orientation + glass BlockDisplay barrier

- Correct all variant-1 (ν fin funnel) ItemDisplay yaw by +90 degrees, including stored, orbit, wings, rear, front, spiral. Variants 2/3/4 are unchanged.
- All six variant-1 offhand barrier displays now share the same model yaw (no alternating 180-degree pairs).
- New `fin-funnel.model-yaw-offset: 90` configuration, read dynamically, defaults to 90 for existing installations.
- During active offhand barrier mode spawn **five** thin WHITE_STAINED_GLASS BlockDisplays: front, back, left, right, and overhead. These are visual-only entities, not real blocks and do not alter world terrain, player movement, or other block entities. No floor to avoid obstructing movement.
- The glass bounds match `FinBarrierGeometry`: horizontal +/-1.48 blocks, vertical 0.28–3.14 blocks relative to player feet. The glass follows the player's rotation and position, and cleans up on retract, death, quit, dimension change, or plugin disable; orphan cleanup includes BlockDisplay.
- Configure `fin-barrier.glass-enabled` and `fin-barrier.glass-thickness` (default 0.035, visual thickness).
- Existing projectile and hitscan beam interception and 0.5 HP beam damage remain unchanged. Resource pack bytes are unchanged.

## v0.7.14 — fin-funnel orientation and immediate muzzle aim

- Model's two different reference axes are now handled independently: the
  unfolded U-shaped model fires toward local -X; the folded rod runs along local
  +Y. Previously, a single +90-degree yaw adjustment was applied to both.
- Fin funnel stored and stationary formations use forward-facing yaw. ORBIT and
  SPIRAL face radially outward; barrier corners face inward, and the remaining
  two folded units stay behind the player.
- Folded rods lie horizontally thanks to a model-local Z rotation, configurable
  using fin-funnel.folded-roll-degrees (default 90 degrees).
- Six shooting fin funnels snap their -X muzzles to the actual ray trace hit,
  hold for 3 ticks, then ease back to the formation heading by tick 8 (the same
  timing as the existing variant-4 Unicorn barrels). Stored funnels cannot fire.
- fin-funnel.yaw-adjustment-degrees (default 0) applies a user-adjustable
  horizontal correction if a custom model uses a different local axis.
  The legacy model-yaw-offset setting is ignored; this avoids loading a stale
  +90 correction from previous plugin configs.
- Beam damage cap, manual /funnel pack, 4-unit barrier collision and the other
  three variants are unchanged.

## v0.7.21 — Qubeley-inspired long conical bit (variant 3)

- Rebuilt `models/funnel_drone_3.bbmodel` from its 22-cube placeholder as a
  long, tapered purple/magenta bit with a flared bell-shaped tail, tapered
  needle tip, 16-sided rounded cross-section, reflective streaks and a dark
  recessed tail cap.
- The editable Blockbench source and the vanilla-compatible JSON export use
  the same 448 cubes and a dedicated 64×64 texture; both are tracked.
- Added `resourcepack/assets/funnel/textures/item/funnel_drone_3.png` and
  registered it in the item/block atlases. Reapply `/funnel pack` after
  installing the matching JAR and restarting Paper.
- Only variant 3 visuals changed; other models, drone behavior, damage,
  and the ν-fin barrier configuration are unchanged.
- This is Minecraft-model geometry, not a smooth polygon mesh. The curve
  is approximated by narrow sixteen-sided plates and is more demanding
  to render than the old 22-cube placeholder.

## v0.7.23 — Qubeley final bit, offhand target pursuit and shield durability

- Updated the editable variant-3 bbmodel to the user-approved 598-cube model; exported matching JSON and embedded texture.
- Variant 3 keeps the same five formation modes with twelve mainhand bits. Offhand right click locks the nearest live Mob within 24 blocks (or stops when the same target is chosen again); twelve additional visual bits circle the locked target in two rings and fire staggered half-HP beams every eight ticks. Stops on target death, distance, offhand removal, death, world transfer or logout. No player-targeting.
- ν fin barrier and Unicorn shield now absorb at most five attacks or 20 HP of incoming damage, whichever comes first. Each blocked projectile and beam counts once; direct attacks absorbed according to the existing coverage. Shield disengages immediately and displays retract.
- Vanilla model/pack compatibility is checked by CI; the matching new resource pack must be applied with /funnel pack.

## v0.7.24 — Qubeley visual scale

- Variant 3 Qubeley bits in **all mainhand formations and the 12-bit offhand mob-pursuit animation** display at 25% of their previous width, height and length.
- The existing user-approved `funnel_drone_3.bbmodel` and resource-pack geometry are unchanged. No separate pack update is required.
- Change `qubeley.model-scale-multiplier` (default `0.25`) in `plugins/FunnelArmor/config.yml` to adjust just variant 3. Other variants keep the existing `drones.model-scale` value.
- Reload configuration with `/funnel reload`. Existing active offhand attacks may need restarting to apply a changed multiplier; mainhand formations refresh automatically.

## v0.7.25 — Qubeley target-facing bits and organic attack motion

- Corrected the backward-facing Qubeley offhand bit orientation using target-facing yaw without the previous 180-degree reversal and target-relative up/down pitch; the firing bit snaps to a fresh aim on every shot.
- The offhand twelve-bit attack now uses independent smooth seeded trajectories, varying the distance and height around the selected mob; the selected target and 0.5-HP staggered beam logic remain unchanged.
- Removed **spiral** for variant 3 from right-click formation cycling, `/funnel formation` and tab-completion; previously saved spiral settings fall back to orbit. Other models retain spiral.
- Model geometry, texture and the quarter-size scaling from v0.7.24 are unchanged; updating the resource pack is unnecessary for this version.


## v0.7.26 — Aerial WINGS and launch-to-target movement

- Variant 3's twelve WINGS bit positions are now loosely scattered at staggered heights around the player's shoulders and above their head, with separate forwards/backwards offsets and slight hover movement. Other variants' WINGS formations are unchanged.
- Offhand activation snapshots the player's location. All twelve Qubeley bits spawn around the player at distinct shoulder/back launch points, then fly to their orbiting mob positions on curved, smoothly eased flight paths with short per-bit departure delays. The last unit finishes deployment after roughly 35 ticks (1.75 seconds).
- Repeated 0.5 HP beams start only after all twelve bits have arrived; existing target selection, tracking, gun aiming, and organic orbital motion remain in place. Models and resource-pack contents are unchanged.


## v0.8.0 — Aerial gameplay (initial runtime implementation)

The approved models under \`models/aerial_*.bbmodel\` are **not** vanilla
Minecraft JSON item models. Aerial combat is now implemented in an isolated
\`AerialCombat\` module; the temporary client item models are explicitly
placeholders until a compatible BBModel free-mesh renderer/export pipeline is
integrated. **Do not treat the temporary asset geometry as the approved final
appearance.**

- OP: \`/funnel aerial give [player]\`. Players: \`/funnel aerial status\`.
- Main hand: 11 independently animated displays with irregular motion,
  random-tick hostile-Mob detection and 0.5-HP automatic beam shots.
- Offhand + right-click: Escutcheon mode with an assembly transition.
  Protects from frontal physical/projectile damage. Breaks after 5 hits
  or 20 HP absorbed; default 200-tick cooldown. Manual dismissal is allowed.
- Main hand + sneak + right-click: toggle GUND-bit Rifle assembly.
  Each ordinary right-click fires one stronger 1.5-HP beam, 5 shots total.
  Empty magazine or manual cancellation starts a 200-tick cooldown.
- Both-hand controller conflicts give priority to the offhand shield,
  so one click cannot activate two modes simultaneously.
- Death, disconnect, world change and plugin disable remove active displays.
- Existing four funnel variants and their mechanics remain separate.

Settings: \`aerial.*\` in config.yml. A vanilla-Paper preview represents
assembly with 11 custom ItemDisplays and a temporary assembled display.
**Approved 3D mesh rendering, exact part-to-part transformation, and in-game
visual verification remain outstanding.** The BBModels are preserved, not
modified or claimed to be usable as vanilla models.

### Aerial approximated source-derived item models

The resource pack additionally includes **11 different Aerial bit visual models**, a shield model, and a rifle model, all derived from their approved BBModel sources. Each free-mesh part is approximated by a bounding cuboid, retaining the source element's sampled palette material. The approved BBModels remain untouched. This is a **low-fidelity preview**, not lossless free-mesh rendering. Unchanged source meshes still require a compatible runtime/exporter for full detail.

### Aerial flight and orientation calibration

Aerial's eleven bit display positions use `AerialFlightMath` (world-space
player-relative offsets). Switching between autonomous flight, shield formation,
and rifle formation interpolates over `aerial.assembly-ticks` rather than
teleporting the bits directly. The selected firing bit briefly aims at its target.
The assembled rifle beam begins near the rendered muzzle instead of the display
center. Orientation and emitter position are configurable through
`aerial.rifle-yaw-offset-degrees`,
`aerial.shield-yaw-offset-degrees`,
`aerial.bit-yaw-offset-degrees`,
`aerial.rifle-muzzle-forward-offset` and `aerial.rifle-muzzle-up-offset`.
Unit tests cover formations and interpolation; **the actual orientation,
visual scale and bit-to-cube mesh approximation still require a live Paper
server visual test.**

### v0.8.2 Controller input fix and compatibility diagnostics

- Restores right-click-air handling for Aerial equipment even when Paper
  initially marks a no-vanilla-action click as cancelled. This is required
  for LEATHER_HORSE_ARMOR controllers.
- Sneak+right-click in the main hand now selects rifle transformation even
  when both hands hold Aerial controllers. In rifle mode, normal main-hand
  right-click fires. Offhand right-click toggles the shield in other modes.
- Other four funnel variants keep their existing interaction logic.
- OP-only `/funnel diagnose` displays both held item models, the
  persistent-data keys identifying controllers, the recognized old-funnel
  variant, the last observed player resource-pack event, and the server pack
  hash. This is intended to distinguish missing item tags from client
  resource-pack loading problems.
- Previously approved fin / heavy / Qubeley / Unicorn asset files remain
  unchanged by this update. If the old visuals differ, check `/funnel
  diagnose`, `/funnel status`, `/funnel pack`, and client latest.log
  for pack errors. An in-game rendering check is still needed.

### v0.8.3 — Fin barrier retraction and legacy inventory icon framing

- Fix variant 1 (Fin Funnel) offhand right-click **retraction**: the
  guard membership is now actually removed on its second toggle.
  This permits `deploy → retract → deploy`, and preserves the
  existing glass and shield retraction animation. A new unit test verifies
  toggles and independence between players.
- `/funnel barrier` shares the same corrected toggle implementation.
- Reframe **inventory GUI only** for the original heavy (variant 2),
  Qubeley (variant 3), and Unicorn shield (variant 4) weapon item icons.
  Variant 3's long Z-axis model previously extended beyond the normal
  icon frame; it now uses a side-facing view with lower GUI scale.
  Variants 2/4 have separately tuned GUI scaling and centering.
- The **elements, texture references, in-world ItemDisplay fixed and hand
  transforms**, and all four original combat implementations are unchanged.
  The fin icon is unchanged. Unit tests assert all four original model
  definitions remain packaged with their existing world-scale transforms.
- GUI framing is a code-side correction: final appearance still requires
  a client test with the new pack. After upgrading the JAR and restarting,
  send the refreshed pack using `/funnel pack` and reload client resources.


### v0.8.4 — Official inventory-only sprites (old prototype retired from giving)

The four current controller icons are independent 2D transparent textures:
fin, Qubeley, shield, and Aerial GUND-bit. Inventory controllers now point
to \`funnel_icon_fin\`, \`funnel_icon_qubeley\`, \`funnel_icon_shield\`, or
\`funnel_icon_gund\`. World \`ItemDisplay\` drones retain the approved
\`funnel_drone_*\`, \`aerial_bit_*\`, shield, and rifle 3D item models.

Every 40 ticks, online player storage and offhand controller item-model
components are migrated to the new official icons **without altering PDC,
formation settings, item names, or abilities**. Old prototype variant 2 is
no longer offered by \`/funnel give\`, but previously obtained variant 2
items are not deleted or broken.

The new \`/funnel give all\` gives the **four current** types (Fin,
Qubeley, Shield, GUND). Individual legacy options are 1, 3, and 4;
\`/funnel aerial give\` remains available. The four icon JSON models
are flat sprites whose first-/third-person hand transforms are hidden,
without affecting hovering 3D displays.

If previously issued icons still appear unchanged, verify the player's
actual ItemModel component with \`/funnel diagnose\`, and the SHA1 and
load status of the newly published resource pack.


### v0.8.5 — Hand-equipped Aerial shield and beam rifle; restored 3D inventory icons

- Restore 3D references for the three approved legacy controllers:
  Fin \`funnel_drone_1\`, Qubeley \`funnel_drone_3\`, Unicorn shield
  \`funnel_drone_4\`. The Aerial controller now uses the 3D
  \`aerial_bit_01\` model. Previously issued inventory items are migrated
  by the existing PDC-preserving icon update; the discarded 2D icons are
  not needed for gameplay.
- The 11 Aerial bit meshes now use **per-piece ItemDisplay scaling**
  calculated from their individual dimensions. The intended longest side
  is approximately 0.8 world blocks per bit. The server can tune the
  multiplier at \`aerial.bit-size-multiplier\`.
- Offhand normal right-click with a GUND controller transforms that same
  held item into a **real vanilla SHIELD** with the approved
  source-derived Escutcheon model. **After transformation, hold
  right-click to raise/guard** (normal vanilla shield-use pose).
  **Sneak + right-click with offhand shield retracts** it. The shield
  still has 5-hit / 20-HP durability and cooldown, and the custom
  interception now requires native blocking pose.
- Main-hand sneak + right-click transforms the same GUND controller into
  a **real vanilla BOW** using the source-derived GUND-bit Rifle model.
  It is visible as a held main-hand weapon. Right-click shoots a
  1.5-HP beam, max 5 shots; sneak + right-click reverts the weapon.
  Vanilla arrow shooting is suppressed for the GUND-tagged BOW.
- The floating duplicate full-size shield/rifle ItemDisplays are removed.
  The separated bits converge and then hide when a held item takes over.
- On disconnect, world change, death, restart, or switching away from
  transformed equipment, temporary SHIELD/BOW items are restored to
  the original GUND controller. No unrelated inventory items are cleared.
- The BBModels in \`models/\` remain untouched, and the in-game JSON
  representations are still cuboid approximations. This feature must be
  validated with a Paper server and client before calling the visual
  alignment final.


### v0.8.6 — Restore approved GUND bit silhouettes in normal flight

The previous Aerial normal-flight exporter converted each source free mesh
into **one huge axis-aligned bounding cuboid**, which erased its tapered
edges, nose, stepped plates, and gaps. This made normal GUND bits look
different from the eleven approved parts inside
\`models/aerial_escutcheon.bbmodel\`.

Re-export all 11 normal-flight JSON item models from those **unchanged**
approved 74 source meshes by taking repeated polygon cross-sections along
each part's main longitudinal Y-axis and assembling short cuboids. This
preserves taper and stepped armor much more faithfully than one cuboid per
mesh. All 11 pieces remain distinct, with their own colors and proportions.

Only \`resourcepack/assets/funnel/models/item/aerial_bit_01..11.json\`
and tests are changed. The large per-bit in-world scale from v0.8.5,
shield, rifle, animations, and approved .bbmodel files are untouched.

This is still a cuboid-based approximation: native Minecraft item JSON
does not preserve arbitrary free-mesh polygon faces from Blockbench. A
lossless free-mesh pipeline would be required for pixel-perfect fidelity.


### v0.8.7 — Escutcheon replaces the visual model of the equipped Minecraft shield

The GUND Escutcheon is still a **real \`Material.SHIELD\` item** in
the player's offhand, with the same Aerial PDC and custom 5-hit/20-damage
durability rules. The vanilla blocking mechanic, hand-raising animation
and collision/blocking logic are preserved.

The item model definition \`funnel:items/aerial_escutcheon.json\` now
switches based on the vanilla \`minecraft:using_item\` condition:
- not guarding: \`funnel:item/aerial_escutcheon\`
- guarding: \`funnel:item/aerial_escutcheon_blocking\`

Both use the same approved Escutcheon 74-part source-derived Minecraft model;
only display transforms differ. Pose and positioning are based on Minecraft's
normal/offhand blocking shield transforms, with a larger (1.9x normal,
2.15x blocking, in third person) scale. Previously this was 1.18x, so
the shield appeared much too small. First-person left-hand transforms
have also been increased to 2.05x/2.25x.

This *only* changes the appearance of the Aerial-tagged shield; it does not
replace \`assets/minecraft/items/shield.json\` or alter ordinary shield
items, the 11 independently flying GUND bits, the rifle, or the approved
BBModel file. Confirm the visual angle/occlusion in first and third person
on a Paper 26.3 client; automated tests cannot assess the final pose.


### v0.8.8 — Re-export the approved Escutcheon source instead of 74 large boxes

The user's supplied \`gund_bits_shield_v03_tight(4).bbmodel\` was
compared to \`models/aerial_escutcheon.bbmodel\`. All 74 source meshes,
their vertices, face definitions, 11 groups and outliner agree exactly.
The discrepancy was the resource-pack model exporter, **not a wrong
source model**.

Reconstructed the shield's packed geometry from the same 74 source meshes
by intersecting each mesh with closely spaced longitudinal slices and
building 677 narrow cuboids. This retains the original tapered rails,
split silhouettes and the central V-shaped armor significantly better
than the previous one-bounding-box-per-mesh export. Both holding and
blocking use this same mesh-derived geometry. The hand model has been
rotated away from the earlier side-on orientation to expose its broad
front plate. First-/third-person holding and blocking offsets have also
been adjusted.

The approved BBModel, normal flight bits, shield durability and
transformation rules are unchanged. **This is still a cuboid approximation,
not lossless polygon rendering**, and in-game orientation and material
appearance need a client check.

The GUND-bit rifle remains based on the approved rifle mesh but is now
larger in the hand: 0.57 → 0.96 first person and 0.60 → 0.98 third person.
Rifle geometry, firing damage, ammunition and cooldown are unchanged.


## v0.8.14: Experimental BetterModel Escutcheon (OFF by default)

- The **original** `models/aerial_escutcheon.bbmodel` is bundled in the JAR and, when the experiment is enabled, copied once to `plugins/BetterModel/models/funnel_aerial_escutcheon.bbmodel` if absent.
- The existing shield mechanics (blocking, HP, input, cooldowns, inventory restoration) remain unchanged. Only the shield appearance is optionally rendered as a BetterModel dummy model near the offhand.
- **Try it:** install BetterModel 3.x; start the server once with `aerial.bettermodel.enabled: true` in `plugins/FunnelArmor/config.yml`; have BetterModel reload its model files or restart after the file is installed, and use the updated BetterModel resource pack alongside FunnelArmor's pack. For existing configs, add the boolean explicitly.
- If BetterModel is unavailable or has not loaded `funnel_aerial_escutcheon`, FunnelArmor retains the old resource-pack shield model rather than hiding it.
- **Instant rollback via configuration:** set `aerial.bettermodel.enabled: false` and restart Paper; this restores the existing resource-pack shield rendering. No Git rollback or file removal required. The copy in BetterModel's models folder is left untouched.
- This is a **runtime visual experiment**, not yet visually verified. The original BBModel has Blockbench free meshes. BetterModel 3.x and Minecraft 26.1+ are required for mesh rendering; actual model ingestion and client pack synchronization must be tested in game.

## v0.8.18 — BetterModel Escutcheon display size and lighting diagnostics

- Optional `aerial.bettermodel.model-scale` defaults to `2.0` and applies a clamped 0.1–8.0 scale only to FunnelArmor's BetterModel dummy tracker. This does not change the approved 74-mesh `.bbmodel`, vanilla Escutcheon rendering, combat behavior, or other drones.
- `aerial.bettermodel.full-bright` defaults to `true`: the dummy tracker requests block and sky brightness 15/15. This is diagnostic; if the mesh remains black, inspect BetterModel's generated client pack and UV/texture mapping. The original texture file remains unchanged.
- A per-deployment server log reports scale, full-bright and viewer count. An unregistered model now logs a warning instead of silently skipping its overlay.
- Existing config files without these two new keys use the same defaults; `aerial.bettermodel.enabled` remains opt-in (`false` in the shipped config) and `hide-vanilla-shield` remains `false`.
- **Test with the offhand GUND-bit controller on the server.** A standalone `/bettermodel spawn funnel_aerial_escutcheon` creates a tracker outside FunnelArmor and therefore does not inherit FunnelArmor's size or lighting settings.
- Re-equip/redeploy the shield after changing the visual settings (and restart Paper when switching BetterModel integration on/off). Keep the vanilla shield visible until the overlay's shape, colors, placement and pack loading are verified in game.

## v0.8.19 — Return exclusively to the standard Minecraft item-model pipeline

- **BetterModel Escutcheon experiment ended.** FunnelArmor no longer uses or depends on BetterModel, spawns a dummy-model overlay, or copies model sources into other plugin folders.
- The **existing vanilla-compatible resource-pack Escutcheon JSON** (idle and blocking poses) is the only equipped shield appearance. The prior hand transforms, 681 fine cuboids, normal blocking mechanics, 5-hit / 20-HP durability, rifle, and other funnel types stay unchanged.
- The obsolete invisible overlay item definitions are removed from the pack, and the obsolete optional BetterModel configuration is removed from the shipped config. Any obsolete `aerial.bettermodel.*` keys in a server's existing config are now ignored and may be deleted.
- `models/aerial_escutcheon.bbmodel` and other approved Blockbench sources are **unchanged**. The historical experimental `models/funnel_aerial_escutcheon_bettermodel.bbmodel` remains in source control only as an unused reference; it is no longer bundled into the JAR. Future visual corrections should edit only the vanilla-compatible resource-pack JSON (or a user-approved model source, if explicitly authorized).
- After updating the JAR, restart Paper, then reapply FunnelArmor's resource pack via `/funnel pack`. BetterModel's generated pack and experimental spawned husk are no longer required by FunnelArmor. If BetterModel is used elsewhere on the server, it may remain installed.

## v0.8.20 — Bit-first Aerial visual preview

A new, opt-in preview based on the user's disassembled physical GUND-bit reference photo is generated from 11 separately authored piece recipes, **not** by splitting an assembled model. See `docs/AERIAL_BIT_FIRST.md` and `tools/generate_aerial_bit_first.mjs`.

OP commands: `/funnel aerial preview <1..11|shield|clear>`. They display one nonpersistent ItemDisplay without changing any live equipment, combat, approved BBModel, existing 11 flying bit models, or the current native shield/rifle visuals. This allows iterative review piece-by-piece before adopting the assembled geometry.

The assembled test shield contains precisely the union of the 11 authored bit geometries (229 cuboid elements); it does not contain the detached dark mounting frame. The existing Aerial palette is reused. In-game visual checks and piece-by-piece approval are still required.

## v0.8.21 — Joined shield rebuilt after reference-photo comparison

The v0.8.20 preview was visibly incorrect: pieces were separated as if still laid out on the table, the outline was too wide, the upper white fins and lower white/grey rails were disconnected, and the yellow/blue/front-face details were too crude.

The **same eleven independent bit recipes** now use compact *joined* positions based on the left-hand assembled shield in the user-provided image, while the detached right-hand parts remain the references for the individual bit contours. The preview now has 834 cuboid surfaces and an overall height/width of about 3.16. The large white panels use the brighter reference white texture tile, and the bits gain narrower/tapered edges, colored bevels, vent detail, and small blue nozzle rings. Tests guard the joined outline and keep the 11-part union invariant.

This is still an unapproved **preview**, not a replacement for the existing shield, rifle, in-flight bits, or any BBModel source. The existing `/funnel aerial preview 1..11|shield|clear` commands still work. User acceptance requires a new Minecraft screenshot.

## v0.8.22 — Visual reference alignment for the bit-first preview

The **second user reference image** (front-on stylized 3D assembled shield) takes precedence for the joined silhouette; the third photo defines the eleven separate components. We reauthor the 11 bit recipes (rather than editing an existing assembled shield), sharpen the upper white fins, expose three blue-core apertures, elongate the white center nose, compact the blue shoulder/gold vents, widen the lower white armor into panels with blue outside trim, and inset the grey inner blades with cyan lines. Redundant face layers causing horizontal striping are removed. This remains a **preview only**; the actual equipped shield, original bit models, rifle, and approved BBModels are unchanged.

## v0.8.23 — Sharp-shell Escutcheon preview

The 0.8.22 test showed the widened lower white shells becoming bulbous and rounded. This version restores the angular 0.8.21 proportions and narrow edge, retains the three raised core apertures, thin upper fins, pointed longer nose, gold vents, inset grey rails and blue outline, and removes overlaid white face strips that created zebra-like shading. It retains the 11 separate bit recipes and their derived shield assembly. This remains **preview only**; existing production shield, rifle, flying bits, combat code and approved BBModels are unchanged. Requires in-game visual review.

## v0.8.24 — Narrow internal gaps and reduce banding

The 0.8.23 in-game screenshot showed large voids between the white outer plates, exposed charcoal blades, and central crest. In the new preview, the **inner** edge of both white side armor bits extends toward the center without moving their sharp outside edges outward (avoids the bulbous 0.8.22 shape). The inner charcoal elements rise closer to the central V crest and the blue shoulders become smaller and closer together. Long uninterrupted white front panels conceal horizontal row seams. All 11 parts remain independent; the assembled preview is their exact union. The live shield, autonomous flying bits, rifle, battle behavior and original approved BBModels have not been changed. Minecraft in-game visual approval is still pending.

## v0.8.25 — White nose and blue shoulder detail pass

The **0.8.24 assembled preview silhouette is preserved**. Only standalone GUND part 04 (central white nose) and mirrored 05/06 (blue shoulders with gold vents) were updated: the nose now has a sharper six-station pointed front outline, two long side facets and a restrained white vertical ridge; shoulder bits have tapered, layered blue armor and a compact diagonal gold cartridge. The other eight generated part model JSON files are byte-identical to 0.8.24, and no 11-part placements moved. This is still isolated to the OP preview; existing armed shield, original BBModels, rifle, automatic bit visuals and combat logic remain unchanged.

## v0.8.28 — Taper the blue-to-white shoulder join (preview only)

The 0.8.27 reference screenshot showed an overly rectangular connection. Both blue shoulder bits 05/06 are now narrower and taper at their lower edge, moved 0.12 model pixels toward center and 0.11 up. The white shell connector on bits 07/08 is replaced with a three-stage diagonal tucked joint and a reduced cobalt seam. Only bits 05/06/07/08 and the assembled preview shield change; the rear grip, other seven bit models, the approved BBModel files, real equipped shield/rifle, and combat logic are unchanged. In-game visual approval remains necessary.

## v0.8.29 — Blend the white shell into the blue shoulders (preview only)

Following the 0.8.28 in-game screenshot, three narrow, diagonally stepped *inboard* fairings are added to white bits 07/08 just below and behind blue shoulders 05/06. This reduces the triangular see-through gap while preserving the shield's total width, length and rear grip. The other nine standalone bit geometries, central nose, accepted BBModels, equipped production shield/rifle and all combat mechanics stay untouched. Preview geometry still needs in-game acceptance.


## v0.8.30 — Reference-guided angled shoulders and tighter inner armor (preview only)

The user-authored model is the appearance reference; the Gunpla photo explains the eleven independent parts. The supplied in-game screenshot showed upright block-like shoulders, blunt central plates, and an open lower center. This pass angles shoulders 05/06 by mirrored vanilla 22.5-degree Z rotations, replaces their yellow stair strips with recessed rectangular vent frames, sharpens nose 04 and crest 09 with a blue V under the nose, and brings tapered inner bits 10/11 toward a narrow center seam.

Core/upper bits 01–03, outer white bits 07/08, all five rear grip elements, approved BBModel sources and production equipment are unchanged. Rotated standalone bits use rendered bounds for centering; assembly translates both geometry and rotation pivots. Use `/funnel aerial preview <1..11|shield|clear>` and `/funnel pack` to inspect. Minecraft appearance still needs user validation; this does not replace the production shield.


## v0.8.31 — Stable preview lighting and pointed outer armor

The 0.8.30 in-game screenshot showed dark white armor and blunt lower ends. OP-only `/funnel aerial preview` displays now override block and sky light to 15/15; combat displays continue to use world lighting. White outer bits 07/08 taper to narrow lower tips, with finer slices limited to their ends, and include three slim inner lap joints behind the nose/crest connection. The joints reduce open space while remaining part of each independent bit.

Other nine bit models, overall width and height, approved BBModels, production equipment and the five rear grip elements are preserved. Recreate an existing preview after installing the JAR and requesting `/funnel pack`; lighting and appearance still need Minecraft validation.

## v0.8.32 — Assembly placements only

Based on the 0.8.31 Minecraft screenshot, **only the join positions** in `tools/generate_aerial_bit_first.mjs` have changed. Grey independent bits 10/11 shift from X=±0.52 to ±0.72, move 0.11 up and 0.06 rearward, changing their central seam from 0.04 to 0.44 model pixels. Blue shoulders 05/06 shift inward 0.08 and down 0.14. The central nose 04 moves up 0.08 and rearward 0.02; the white crest 09 moves down 0.10 and forward 0.02.

The eleven standalone models (including all profiles, rotation angles and colors) are byte-for-byte unchanged, as are the 5 rear hand-grip cubes and the shield's outside bounds. No production models, BBModel source, game mechanics or equipped shield were changed. Preview only: `/funnel aerial preview shield`. Visual confirmation in Minecraft is pending.

## v0.8.33 — Placement-only white outer armor adjustment

In the 0.8.32 Minecraft screenshot, grey bits 10/11 are visibly separate as intended, but white side armor 07/08 leaves small daylight triangles beneath the slanted blue shoulders. The assembled positions of white armor 07/08 are moved 0.08 inward and 0.14 upward, keeping the standalone bits unchanged. This is exclusively an assembly **translation**: all 11 independently generated parts, their rotations, faces, colors, the five-piece rear grip and other nine placement transforms are preserved, as are production models and approved BBModels. The overall assembled width is slightly narrower. In-game visual approval still pending.

## v0.8.34 — Placement-only blue shoulder adjustment

After the 0.8.33 in-game screenshot, adjust only the **joined positions** of shoulder GUND bits 05 and 06: each is shifted 0.06 model pixels inward and 0.08 down, keeping the ±22.5° rotation. All eleven standalone bit models, the white outer plates, central nose and crest, the now-separated gray inner blades, and the shield-only rear grip remain identical to 0.8.33. The prototype remains separate from the production equipped Escutcheon; no approved BBModel is edited. Screenshot validation is pending.

## v0.8.35 — Production Aerial now uses bit-first models

The eleven live flying Aerial GUND bits use `aerial_v2_bit_01`–`11` with scales normalized to roughly 0.8 block in height. The Aerial controller and its legacy-inventory icon migration now use the bit-first model for bit 01. Aerial shields retain the existing `funnel:aerial_escutcheon` **item identifier** (preserving previously issued items) but its native `using_item` condition now points to the joined bit-first shield or an identical-geometry blocking model with a distinct raised-hand display transform. Gameplay mechanics and rifle are unchanged; legacy assets remain available for rollback.

The white lower outer fins (bits 07/08) now expose cobalt on their outer side faces (west/east respectively), while front, back and other surfaces remain original colors. All geometry/placements (493 shield cuboids including 5 rear-grip cuboids) and approved original `.bbmodel` files remain unchanged. Test flying size, actual shield holding/guarding, side colors and reissued items in Minecraft.

## v0.8.36 — More front-facing and slightly larger equipped Escutcheon

Based on the real 0.8.35 third-person screenshot, only the held shield's normal and guarding `display` transforms were adjusted, both left/right hands and first/third-person. The plate is rotated less steeply backward (X 75° → 65°) and its yaw is turned slightly inward so the front is more visible (left Y 180° → 165°, mirrored right Y -165°). Third-person scale grows from 1.215 to 1.40 normally (~15%) and 1.35 to 1.56 when guarding (~16%). First-person scale grows from 1.21 to 1.34 normally and from 1.335 to 1.49 when guarding.

**No bit geometry, face colors, joining placements, rear handle, flight displays, rifle, GUI/fixed preview pose, item identifiers, or production mechanics were changed.** The normal shield and blocking model still contain the same 493 elements; visual orientation and scale need a fresh in-game check.

## v0.8.37 — Lower the held Escutcheon, pose-only

The real v0.8.36 equipped shield screenshot showed the hand-held model a little too high. Keep all angles and scales exactly as v0.8.36, and adjust only the native item-model **translation Y** in both hands: third-person idle **7 → 5**, third-person guarding **9 → 7**, first-person idle and guarding **3 → 2**. The same positions apply to left/right hands. No geometry, texture, model ID, game behavior, 11 bit models, rear grip, inventory GUI, fixed preview or rifle changes. Test the real shield from the side and front after updating the resource pack.

## v0.8.38 — Escutcheon pose/color, barrier cooldowns, Qubeley timed pursuit

The held Escutcheon is lowered again, without changing item display rotation, scale, bit geometry or rear grip: third-person idle Y 5→3, guarding 7→5; first-person both 2→1. Escutcheon texture colors are gently muted without remapping UVs.

ν fin and Unicorn shield-funnel barriers now acquire independent 200-tick (10-second) cooldowns **when broken** at 5 hits or 20 HP absorbed. Manual stowing is free. Cooldowns remain active across item swaps, death and reconnects in the same server session.

Qubeley variant 3 offhand pursuit requires **sneak + right click** and targets hostile mobs within **8 blocks**. Pursuit runs at most 100 ticks (5 seconds), then 12 bits fly back toward the player for 10 ticks and cooldown lasts 200 ticks (10 seconds) after return. Early cancellation or target loss also triggers return and cooldown. The ordinary non-sneaking offhand click does not activate pursuit.

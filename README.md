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

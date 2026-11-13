# Build and automated tests

The GitHub Actions workflow builds the plugin and runs JUnit 5 tests on every push to `main`, pull request, or manual run.

## Environment
- Paper API 26.3 build 218 stable
- JDK 25
- Gradle 9.2.1
- JUnit Jupiter 5.11.4

## What the tests check
1. The embedded `funnel-pack.zip` can be read and includes four drones, their item definitions, the controller, and the shared texture.
2. Model JSONs have the correct `#0` texture references (protects against the v0.3.0 missing-texture bug).
3. Item definitions reference models actually present in the pack.

## What they do **not** check
- Live Paper server startup, chunk lifecycle, rendering, right-click attack behavior, or multiplayer handling.
- Whether the client actually downloads a pack from the configured URL.

Those require a test Paper server and Minecraft client. A green CI result does not mean the full in-game behavior has been verified.

## Local
In IntelliJ use Gradle 9.2.1 (not old Gradle 8.8), Java 25, then `gradle clean build`. Check the Actions tab for the workflow run and download the resulting JAR from the successful run's artifacts.

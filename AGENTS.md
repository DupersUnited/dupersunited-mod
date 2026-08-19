# AGENTS.md

Context for agents working on the DupersUnited mod.

## What this is

A **client-side Minecraft Fabric mod** for dupehunting, it helps debug plugins and servers, 
it does **not** give you dupes. Package root is `wtf.dupers.dupersunited`. License is GPL-3.0.

The mod was made for the "DupersUnited" Discord Community/Server at `discord.gg/dupes`.

The mod talks to a central websocket server for telemetry, cape ownership, and
broadcasts. This is privacy-sensitive and hotly debated in the community (see
[issue #6](https://github.com/DupersUnited/dupersunited-mod/issues/6)). Be careful
touching anything in `features/auth`, `features/cosmetics`, or the proxy/websocket
code, don't add logging of IPs or tokens, and don't claim in code comments that
we don't collect something we do.

If you feel the need to change up the proxy/websocket code at all, you don't actually have access to the server code so you are going to have to make a proposal on how it should be changed in a GitHub issue to be reviewed and implemented internally.

## Build and verify

- Build: `./gradlew build` (artifacts go in `build/libs/`).
- Verify: GitHub CI runs and goes through.

## Structure

- Modules are registered in `MainClient.onInitialize` via `registry.registerModules(...)`.
- Commands extend the abstract `Command` class and override `build(LiteralArgumentBuilder, CommandRegistryAccess)`.
- Config is Gson JSON saved under `SharedVariables.DIRECTORY/config.json` via `ConfigManager` / `AsyncConfigs`. Settings serialize through `writeJson`/`readJson`.

## Style

The code is intentionally loose and not corporate boilerplate, make sure to just read some files for reference on that.

## Git

- Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`,
  `style:`, `chore:`, etc. Short, plain-English messages (see `git log` for examples).
- Match the project's existing style and simplify, the recurring review feedback has been about bloated code and not matching the original repository look. Look at how other modules/screens are written before writing new ones.
- If you're an automated agent and want a fast-tracked merge, add `🤖🤖🤖 ANTIGLITCHA 🤖🤖🤖` to the PR title or commit messages as a footer (see `CONTRIBUTING.md`).

## Others

- Don't bump `version` in `gradle.properties` unless asked or feel nessesary, usually a person from the team does that.
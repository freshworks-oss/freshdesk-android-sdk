# AI Integration Kit

Resources for using AI coding assistants to integrate or migrate Freshworks mobile SDKs on Android.

## Contents

| File | Purpose |
|------|---------|
| [SKILL.md](./SKILL.md) | Agent instructions for migrating **Freshchat Android SDK** → **Freshdesk Android SDK** |
| [MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md](./MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md) | Full migration reference (API mapping, before/after code, checklist) |

Read **SKILL.md** first when prompting an agent. It defines the workflow; the migration document is the detailed source of truth.

## How to use with your AI tool

Copy or reference these files from your app repository. You do not need to copy the entire kit—at minimum, give the agent **SKILL.md** and **MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md** (same directory).

### Cursor

1. Copy this folder into your project, or keep it nearby and `@`-mention both files in chat.
2. **Optional — project skill:** copy **both** `SKILL.md` and `MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md` into the same skill folder (relative links in `SKILL.md` require the migration doc alongside it):

   ```text
   .cursor/skills/freshchat-to-freshdesk-android-migration/
   ├── SKILL.md
   └── MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md
   ```

   Cursor loads skills from `.cursor/skills/` when configured.
3. Example prompt: *“Follow AI-Integration-kit/SKILL.md and migrate our Freshchat integration to the Freshdesk Android SDK.”*

### Claude Code / Claude Projects

1. Add **both** `SKILL.md` and `MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md` to project knowledge, or place them together in one folder (for example `docs/AI-Integration-kit/`) so `SKILL.md` relative links to the migration doc keep working.
2. Example prompt: *“Use docs/AI-Integration-kit/SKILL.md as your procedure and MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md as the reference. Audit our app and migrate off Freshchat.”*

### Other assistants (Copilot, Windsurf, etc.)

- **System / custom instructions:** place **SKILL.md** and **MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md** in the same directory in your repo (for example `docs/AI-Integration-kit/`), point instructions at `SKILL.md`, and ensure the agent can read the migration file via the same relative path (`./MIGRATION_FRESHCHAT_TO_FRESHDESK_ANDROID.md`). Do not copy `SKILL.md` alone—it links to the migration doc.
- **Chat:** attach both markdown files to the conversation and ask the agent to follow **SKILL.md** step by step.

## Scope

- **Platform:** Android only
- **Mapping versions:** Freshchat Android `6.5.10` → Freshdesk Android `2.2.4` (see migration doc Appendix C; use the latest Freshdesk artifact version on Maven Central when editing Gradle files)

## Freshdesk SDK after migration

For net-new APIs and integration patterns beyond migration, use the [Freshdesk Android SDK README](https://github.com/freshworks/freshdesk-android-sdk/blob/main/README.md) in the public repository.

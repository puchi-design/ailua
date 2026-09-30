# AILUA P5 Release Candidate Report

Date: 2026-09-30 (Asia/Singapore)

## 1. Baseline

- Repository: `H:/ailua`
- Starting branch: `local-pass3c-ai-runtime`
- Starting SHA: `9891023174ea9fd4847605eee91fa65cbb13359b`
- P4 baseline: 373 unit tests, debug build and Android 10 installation passed.
- Existing untracked `.idea/` and `gradle/gradle-daemon-jvm.properties` were left alone.

## 2. Final source

- Final implementation SHA, before this report commit: `777904c`.
- Commits after the baseline:
  - `04c3376 feat(onboarding): guide provider setup and first session`
  - `fe415ff feat(group-chat): persist real multi-character conversations`
  - `6f78488 fix(release): add settings privacy controls and branded alpha package`
  - `3ab21f7 fix(first-session): restore character and theme after restart`
  - `777904c fix(character): activate imported card on preview`
- This report is committed separately; `git log -1` identifies its documentation commit.
- All work remains local on the stated branch. No GitHub push was made.

## 3. Test and build results

- `:app:testDebugUnitTest :app:assembleDebug --offline`: **PASS**.
- 383 tests in 73 suites; 0 failures; 0 errors. Ten tests were added without removing the P4 suite.
- `git diff --check`: PASS.
- Test coverage includes provider validation and error copy; first-session state, memory policy and continuation; group speaker selection, persistence after database restart, regeneration, relationship event isolation and per-character memory; settings and Reality preferences.
- The fake provider/JDBC tests establish runtime behavior under controlled inputs. They do not prove that a third-party AI service works on the device.
- APK: `H:/ailua/dist/AILUA-0.5.0-alpha-debug.apk` (27,097,311 bytes).
- SHA-256: `4B701EBC4C1A289B97A4184E8C6F59BB48DCEACCD6077557B002D3FF91075D0F`.

## 4. Physical device

- Xiaomi M2007J22C, Android 10, ADB serial `pnhykzpzzxq8lbs8`.
- Installed the final APK with `adb install -r`; installation succeeded and existing app data was retained. No `pm clear` or Settings reset was run.
- Verified installed `versionCode=2` and `versionName=0.5.0-alpha` and a successful launcher start after force-stop.
- A temporary small display setting of 990 × 2200 at 440 dpi was used for Home and private Chat checks; original 1080 × 2340 size was restored.
- Offline check temporarily disabled Wi-Fi, force-stopped and relaunched the app, opened Home and Living, and restored Wi-Fi. Mobile data was already off.

## 5. P5A — Provider and onboarding

- Four short welcome steps: introduction, official service/BYOK/skip, role choice or import, then direct entry to the first private chat.
- Official service is visibly marked “即将开放” and cannot be selected. No account or server integration was invented.
- BYOK uses the existing OpenAI-compatible provider runtime. Presets cover OpenAI, DeepSeek, OpenRouter and a compatible custom endpoint; native Gemini/Claude protocols are not claimed.
- URL/model/key validation, a bounded minimal connection request, readable errors and a disabled button during testing are implemented. The existing secret store remains the key storage path; errors do not display a key.
- On-device no-provider flow passed: four key taps from launch to the first chat; a suggested prompt opens the provider sheet without crashing. Invalid configuration behavior is covered by unit tests, but wrong-key behavior was not exercised against a real endpoint.
- **ENVIRONMENT_BLOCKED:** no working provider credentials were present on the device. Connection success, streaming, regeneration and cancellation against a real service were not verified.

## 6. P5B — First 10 minutes

- Home gives a short invitation to chat; private Chat offers three first-message suggestions. After a successful first reply, local flags stop repeating introductory guidance.
- The first explicit `我叫…`/`叫我…` name statement can create at most one first-session memory through the existing memory repository. Small talk does not become long-term memory. Other preference/fact extraction is still governed by the existing extractor.
- A successful first reply appends a character-specific `LifeEvent` to the existing world ledger; Living can show the continuation, and completion requires a reply, that event and a Living visit. This continuation is emitted immediately at reply time, rather than after 5–20 virtual minutes.
- Role and theme selection persist across process restart; previewing an imported card activates that character.
- The full first-successful-reply → Home → Living → cross-app moment → second-open journey was **not demonstrated on the physical device**, because there is no working provider. The new policies and persistence are covered by tests. Existing P4 world and projection behavior remains in its regression suite.

## 7. P5C — real group chat

- Replaced the scripted demo screen with persistent group turns in the existing `ChatRepository`, `ChatSession` and `ChatVariant` model. Group identity and participants use a canonical group session key; no second database was added.
- `ChatGenerationRuntime` handles group sends and regeneration with the existing provider. A deterministic planner selects one speaker normally and at most two for a message naming both. Speaker IDs are validated against participants; prompts use bounded context and each speaker's own memory and relationships.
- The UI shows speaker identity, bubbles, timestamps, a generation indicator, missing-provider errors and retry/regenerate. User and actual speaker interactions feed existing LifeEvent/relationship paths; memories remain scoped to the responding character.
- On device the empty group state, keyboard placement and missing-provider error passed. The one/two-speaker reply path, persistence after database restart, regeneration and isolation passed in fake-provider/JDBC tests. Real group replies on device are **ENVIRONMENT_BLOCKED**.

## 8. Release polish and packaging

- Added user-facing Settings and Privacy screens, theme persistence, separate battery/screen Reality controls, existing Usage/Health controls and permission status. Developer world-time controls appear after seven version taps.
- Privacy text explains local storage and that selected context is sent to the chosen remote AI provider for generation. It does not claim that no data leaves the device.
- Local reset has a confirmation dialog; it was deliberately not executed on the test device to preserve existing data. Fine-grained chat/world clearing and full data export are not implemented.
- Removed static fake badges from the app library. Replaced template legacy launcher/round icons with branded assets and retained branded adaptive icons. App label is AILUA.
- Version is `0.5.0-alpha`, code 2. The existing application ID `com.aistudio.ailua.osnv` was retained so in-place installation preserves user data. Formal package ownership and final ID require a P0 decision before broad distribution.
- This is a **debug-signed** APK; no release signing or store publication was performed.

## 9. Screens and routes exercised on device

- Opened and navigated back: Home, Messages, private Chat, group Chat, Contacts, Moments, Living, Diary, Check Phone, Relations, Memories, Mailbox, Calls, Gallery, World Map, Character Creator, World Book, Theater, Settings, Privacy, Reality and the AI connection sheet.
- Home and private Chat were also checked at a temporary small display size. The group composer and send control remained visible above the keyboard at normal size.
- No-provider, no Usage access and no Health Connect conditions did not prevent Home/Living and the checked routes from opening. Calls entered the virtual call UI and returned without a fatal error.
- This was a route/navigation smoke pass, not exhaustive visual, accessibility, scrolling, dark-mode or every empty/error state validation. Character import format round-trips and long-session stability were not exercised on the device.

## 10. Logcat

- Final APK install and launcher smoke after clearing logcat: **0** matching `FATAL EXCEPTION`, `AndroidRuntime: E`, app ANR or app process crash lines.
- Earlier route and offline passes likewise found no `AndroidRuntime:E` entries.
- Build emitted Compose deprecated-icon warnings, Gradle deprecation notices and a SQLite JDBC native-access warning; these are not Android runtime failures. MIUI sometimes printed a `uiautomator` theme-config warning while XML dumps still succeeded.

## 11. BLOCKED

- **ENVIRONMENT_BLOCKED:** no configured usable AI provider/key on the device; cannot prove real connection, first response, streaming, regeneration, cancellation, model failure or the complete first-ten-minute continuity chain.
- **ENVIRONMENT_BLOCKED:** Usage Access is denied and Health Connect is absent, so real records were not validated in this run. Their missing-permission paths remained usable.
- No signing configuration for a distributable release APK was available; only the requested debug APK was produced.

## 12. Known issues

### P0

- Closed-alpha readiness depends on a real provider end-to-end device run covering first reply, Home/Living/cross-app continuation, restart, and group generation. Current evidence cannot establish the core product promise.
- Decide the long-term package ID before broad distribution. Changing it after distribution creates a separate Android app and breaks in-place upgrade.

### P1

- First continuation appears immediately after the reply, rather than 5–20 virtual minutes later.
- Small-screen, dark-theme, keyboard, back navigation and empty/error states have only a targeted smoke pass, not a full route-by-route matrix.
- Local reset exists, but selective chat/world clearing and export do not.
- Notification support is not enabled in this build.

### P2

- Optional `/models` discovery and model recommendations are not implemented; manual model entry works.
- The first-session shortcut extracts an explicit name only; other facts rely on the existing regular memory extractor.
- Group participants are fixed in this first version; there is no group membership editor.

## 13. Release recommendation

**NOT_READY_FOR_CLOSED_ALPHA.** Engineering tests, debug build, no-provider onboarding, route smoke, restart/offline checks and the final device installation pass. A real AI provider was unavailable, so the required first ten minutes and real group responses remain unproven on a physical device. Complete that end-to-end run and settle the package ID before handing the APK to first users.

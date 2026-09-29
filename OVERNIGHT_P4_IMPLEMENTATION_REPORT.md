# AILUA P4 overnight implementation report

## 1. Starting baseline

- Repository: `https://github.com/puchi-design/ailua`
- Branch: `local-pass3c-ai-runtime`
- Starting commit: `a0a14cd8ede3754f12c058b21bb9f4c90f14222a` (`Add autonomous world planning and relationship runtime`)
- Existing local work and phone app data were preserved. The unrelated untracked `.idea/` and `gradle/gradle-daemon-jvm.properties` were not staged.

## 2. Final state and commits

- Final implementation commit: `a9ffb0b` (`fix(product): connect live chat and photo projections`). This report is committed separately after that implementation commit.
- Commits, in order:
  1. `ecabea2` `fix(world): harden autonomous planning and provider fallback`
  2. `396ecc3` `feat(reality): add local optional device signal bridge`
  3. `cf5f560` `feat(companion): integrate bounded reality context and contact cooldown`
  4. `1ae8ee3` `feat(living): persist comments and refine world rhythms`
  5. `a9ffb0b` `fix(product): connect live chat and photo projections`
- All changes are local commits on the existing branch. No remote push or release was performed in this overnight scope.

## 3. Implemented

### World and relationship

- `WorldPlanRuntime` now admits only one planning attempt at a time, keeps an adequate existing future plan, falls back to a deterministic validated plan when AI planning fails, and throttles repeated failures. Fallback searches alternative times and event types when daily caps or collisions reject a candidate.
- The planner retries once without `response_format` only for a compatible 400/422 unsupported-parameter response. Plain output still has to pass local JSON parsing and `WorldPlanValidator`.
- Plan installation encodes and commits persistent state before publishing the new in-memory action list. Failure leaves the previous plan in place.
- Planner context includes bounded real resolved chat turns, alongside existing world, memory, relationship, and event context. Planner instructions emphasize independent character lives and optional metadata.
- Relationship event deduplication history is bounded to 256 IDs; ordinary character activity does not raise user affinity. Existing character-to-character relationship paths remain active.
- Validator adds daily diary and moment caps, per-character spacing, contact cooldown, and basic late-night behavior checks. Planned contact and proactive messages share recent-contact checks.

### Reality Bridge

- Added a local, nullable `RealitySnapshot` and `RealityRepository` for battery, charging, screen interaction, optional UsageStats summary, and optional Health Connect steps/sleep reads.
- Battery and screen are available without special permission. Usage access and Health Connect are opt-in, with on-demand permission/settings entry points and unknown values when records or access are absent.
- Snapshot, UsageStats, and Health reads are cached at different intervals; there is no permanent service, wake lock, or high-frequency polling.
- `RealityContextPolicy` turns only meaningful low-sensitivity signals into bounded summaries. Raw package names and health records are not sent as prompt context. Chat, planner, and proactive paths use these summaries conditionally.
- Added a Reality Bridge page in the App Library with local persistent switches and graceful unsupported/denied states.

### Living product and cross-app continuity

- Home hero and Life Bento, Chat header, Contacts, and Relations use existing world/presence/relationship projections rather than a fabricated online count.
- Conversation list reads latest completed SQLDelight chat turns and real presence instead of seeded private conversation text.
- Moment comments persist as `LifeEvent`s linked to the source post and flow into relationship continuity. The comment composer was adjusted for landscape use.
- Runtime `PHOTO` facts now project to both Moments and Gallery; seed assets are deduplicated by life-event ID. Gallery import already records user activity through `GalleryRepository`.
- Diary and Moments remain fact-based projections. Check Phone accepts only explicit event metadata; custom-character fallback does not use Mira's private seed data.
- World Time developer sheet scrolls in landscape so its action buttons remain reachable.

## 4. Architecture

The new Reality repository and context policy read Android signals on demand, then feed small optional blocks into the existing `PromptAssembler`, `WorldActionPlanner`, and `ProactiveMessageEngine`. They do not form another event ledger or scheduler. `GalleryProjection` maps existing `LifeEventType.PHOTO` facts to the Gallery UI. Moment comments are recorded through the existing user-activity/event/relationship path. Chat remains on the existing SQLDelight `ChatRepository`; no second chat runtime or database was introduced.

## 5. Tests and build

- `:app:testDebugUnitTest`: **PASS**, 373 tests across 70 suites; 0 failures, 0 errors, 0 skipped.
- `:app:assembleDebug`: **PASS**. Final debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- `git diff --check`: **PASS**. Git reported line-ending conversion notices only.
- Focused regression tests cover planner compatibility retry and server failure behavior, valid fallback, plan JSON round-trip, relationship bounded deduplication, reality-context filtering, contact cooldown, and cross-app projections. The atomic planning gate and SharedPreferences write failure path do not have direct concurrent/fault-injection tests; their coverage remains partial.

## 6. Physical device

- Serial: `pnhykzpzzxq8lbs8`; model: `M2007J22C`; Android: `10`.
- Installed with `adb install -r` and launched `com.aistudio.ailua.osnv/com.example.MainActivity` without clearing app data.
- No fatal `AndroidRuntime` entry appeared in the inspected logcat after the final install and exercise.

## 7. Runtime scenarios

| Scenario | Result | Observation |
| --- | --- | --- |
| No-provider plan generation | **PASS** | A validated fallback produced three future actions; the existing world continued without an AI provider. |
| AI dynamic plan and structured-format retry on phone | **BLOCKED** | No usable provider is configured on this device. Compatibility behavior passed a unit test. |
| Advance time and cross midnight | **PASS** | Scheduled THOUGHT, MEAL, and PHOTO actions fired; the clock advanced from `9月25日` to `9月26日`. |
| Plan/event restart persistence | **PASS** | Force-stop and relaunch retained world clock, future plan, and executed events. |
| PHOTO across apps | **PASS** | The same runtime PHOTO appeared as `诺亚拍下窗边光影` in Moments and Gallery. |
| Home and conversation presence | **PASS** | Home displayed `小弥整理手记`; the conversation list displayed real presence and `尚无对话` instead of seeded private messages or a fake online count. |
| Moment comment and relationship continuity | **PASS** | Test comment `P4QA` persisted after restart, linked to its post and updated the relationship's recent interaction. This test artifact remains on the device. |
| Battery and screen state | **PASS** | Reality page displayed a real battery percentage/charging state and interactive screen state. |
| UsageStats without access | **PASS** | Page showed required access/unknown minutes and remained functional. Actual usage summary read is **BLOCKED** until user grants special access. |
| Health Connect unavailable | **PASS** for graceful fallback | The page showed unavailable/unknown and the app remained functional. Real steps/sleep record reads are **BLOCKED** because the provider is absent on this Android 10 phone. |
| Chat streaming and memory pipeline on phone | **BLOCKED** | The chat route opens and the conversation repository loads, but no provider was configured to verify streamed replies or downstream extraction. |
| Diary and Check Phone runtime metadata on phone | **PARTIAL** | Projection unit tests passed; the exercised fallback plan did not generate a DIARY or metadata-bearing action for on-device inspection. |
| Character-to-character relationship on phone | **PARTIAL** | Existing reducer/projector paths and unit tests were exercised; no matching device event was generated in this run. |
| Long-run soak and duplicate planner detection | **PARTIAL** | Short foreground exercise and logcat inspection showed no fatal error or obvious loop; a prolonged provider-backed soak was unavailable. |

## 8. Logcat

The inspected `AndroidRuntime:E` logcat stream contained no fatal exception after install, launch, world progression, app navigation, restart, and Reality page checks. No startup crash required repair. A landscape button clipping issue in the World Time sheet and comment composer was found through UI automation and fixed before the final APK. This is a short exercise, not a multi-hour soak.

## 9. Permission state

- Usage access: **not granted** (`GET_USAGE_STATS` default AppOps mode). The app does not auto-grant it.
- Health Connect: provider package not installed on the test phone; no health permissions granted.
- Notification: not requested or granted by this work. Local proactive notifications were outside the required phases and were not implemented.
- Battery and screen checks require no special permission.

## 10. Known issues and limits

- AI planning, real chat streaming, and runtime memory extraction still need a configured provider for device verification. Deterministic world fallback works without one.
- The existing `GroupChatScreen` remains a prototype with seeded dialogue and canned in-memory replies. It does not use the private-chat persistence/runtime; its list entry is labeled as a group-chat entry, so this should be clearly productized or removed before treating all chat as live continuity.
- Moment likes remain local UI state; the requested comment path is persistent.
- Android 24/25 Health Connect unsupported handling is guarded in code and compile-tested, but no Android 24/25 device was tested.
- The device cannot validate real UsageStats or Health data without the relevant permission/provider. No permission was forced for QA.

## 11. Recommended next pass

1. Configure a test AI provider and run dynamic planner, streamed chat, memory extraction, and duplicate-request soak on device.
2. Replace or clearly isolate the seeded Group Chat prototype before presenting it as a persistent live conversation.
3. Test opt-in UsageStats and Health Connect reads on a device with permissions/provider available.
4. Add focused concurrency and persistence fault-injection tests around planning and installation.
5. Exercise a metadata-bearing DIARY/Check Phone plan and character-to-character interaction on device.

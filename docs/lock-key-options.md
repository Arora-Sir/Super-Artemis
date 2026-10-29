# Lock key: findings and options

This page records why the Lock key does not lock a Windows PC, what was proven on the owner's hardware, and the options for fixing it. It asks for a decision at the end.

## What was proven

1. **The Tab sends the right keys.** A debug-only `KEYDBG` log in `NvConnection.sendKeyboardInput` showed Win down, L down, L up, Win up, with the Windows modifier set, for both the Lock button and the on-screen keyboard.
2. **Windows ignores injected Win+L.** On the host PC, a local script sent the same four events with `keybd_event`, which is the same kind of injection that Sunshine and Apollo use. Nothing locked. A real keyboard locks the PC every time.
3. **A hook sees the injected keys.** An AutoHotkey v2 script with the hotkey `#l` caught the injected Win+L, and the same script with `DllCall("user32\LockWorkStation")` locked the PC when the Lock key was pressed on the Tab.
4. **The web agrees.** Chromium Remote Desktop documents that Win+L cannot be overridden and works from a physical keyboard only, and the common fix on the AutoHotkey and SikuliX forums is to call `LockWorkStation` directly.

So the problem is on the Windows side and no change in the key timing or the modifier flags on the Tab can fix it.

## Options

| # | Option | Needs on the PC | Works for | Effort in the app | Notes |
| :-- | :-- | :-- | :-- | :-- | :-- |
| A | **Apollo server command.** Add a server command named Lock with `rundll32.exe user32.dll,LockWorkStation` in the Apollo web UI. The Server commands entry of the in-stream menu already runs it. | Apollo only | Apollo hosts | None | Nothing to install. The owner already uses `server_cmd` for a screensaver command. Runs in the user session when "Run as administrator" is off. |
| B | **Smart Lock key.** The Lock key and button run a server command named Lock when the host offers one, and fall back to Win+L otherwise. | Option A on the PC | Apollo hosts | Small, about one class and a test | Makes option A feel like a normal Lock key. Needs a name match rule, for example a command whose name is "Lock" in any case. |
| C | **AutoHotkey script on the PC.** `#l::DllCall("user32\LockWorkStation")`. | AutoHotkey v2 and a startup entry | Any host, including Sunshine | Done: the Settings row "Lock key does nothing?" and `scripts/host/SuperArtemisLock.ahk` | Catches a real keyboard too, which is harmless. One more program running. |
| D | **Lock when the stream ends.** An Apollo prep command (Do and Undo) whose Undo step runs `LockWorkStation`. | Apollo or Sunshine app entry | Both | None | Locks the PC when the session closes. Good for privacy, not a Lock key. Locks even when the stream drops by accident, which some users will dislike. |
| E | **Windows Dynamic lock or a Bluetooth lock.** | A paired phone | Windows | None | Locks when the phone walks away, which is unrelated to the stream. |
| F | **Ctrl+Alt+Del then Lock.** | Nothing | Nobody | None | Windows also blocks the secure attention sequence from injected input, so it fails the same way. |

## On the owner's PC

The owner already runs a fleet of AutoHotkey v2 scripts started by `StartupScript.ahk`. The Win+L hotkey is not a separate file there. It lives in `BasicTasks.ahk` next to the other power hotkeys (`Win+X+X` sleeps the laptop), as `$#l::LockPc()` with a small `LockPc()` helper, and it is listed in the header of that file, which feeds Hotkey Help. `#!l` (Win+Alt+L, the skills vault toggle) is a different hotkey and is unaffected. The standalone `scripts/host/SuperArtemisLock.ahk` in this repository stays as the generic script for other users.

## What shipped (2026-10-29)

The three layer plan: the app explains the problem (the help row and the one time hint), an Apollo server command named Lock runs from the Lock key with nothing to install (option B, built in `hosthelp/LockKeyAction.java`), and the help dialog lists both options as numbered steps with a Copy button each, and an AutoHotkey line is the answer for any other host (option C).

## Recommendation

1. Keep option C as the generic path for users on plain Sunshine, since it is already built.
2. Document option A as the first choice for Apollo users, because it needs nothing installed. It is one line in `SETUP.md` and one sentence in the help dialog.
3. Build option B only if the owner wants the Lock key itself to work on Apollo with no AutoHotkey. It is a small change.
4. Offer option D as an extra tip, never as a default.

## Decisions for the owner

1. Should the Lock help dialog lead with option A for Apollo hosts and show the AutoHotkey script second?
2. Should I build option B, the smart Lock key that uses a server command named Lock?
3. Should the Lock key stay Win+L when no server command exists, as it is today?

## Sources

- [Chromium Remote Desktop host, desktop process](https://gitlab.collabora.com/web/chromium/-/blob/83eff387b6535ea4d74ed993e0dbc7cf719a71c5/remoting/host/desktop_process.h) for Win+L working only from a physical keyboard.
- [Windows: lock screen not working using keyboard shortcut, workaround](https://answers.launchpad.net/sikuli/+question/688683) for calling `rundll32.exe user32.dll,LockWorkStation`.
- [AutoHotkey forum thread on locking the screen](https://www.autohotkey.com/boards/viewtopic.php?p=301135) for `DllCall("LockWorkStation")`.
- [Apollo README](https://github.com/ClassicOldSong/Apollo) for per-client permissions and client connection commands.
- [Sunshine app examples](https://docs.lizardbyte.dev/projects/sunshine/latest/md_docs_2app__examples.html) for commands and Do and Undo prep commands.

# Highmark SSH - Phase 3: Multi-Tab Sessions + Integrated SFTP

## Overview

Add tabbed SSH sessions (each tab connects to any server independently) and an SFTP file browser panel integrated alongside the terminal.

---

## Phase 3A: Multi-Tab Sessions

### Core change: Replace companion object with SessionManager

**New: `app/src/main/java/com/questterm/session/SessionManager.kt`** (Hilt singleton)
- `_tabs: MutableStateFlow<List<TabSession>>` — all open sessions
- `_activeTabId: MutableStateFlow<String?>` — currently visible tab
- `activeTab: StateFlow<TabSession?>` — derived from tabs + activeTabId
- Methods: `addTab()`, `removeTab()`, `switchTab()`

**New: `app/src/main/java/com/questterm/session/TabSession.kt`**
```kotlin
data class TabSession(
    val id: String,           // UUID
    val label: String,        // "user@host"
    val sshSession: SshSession,
    val terminalSession: SshTerminalSession,
    val viewClient: QuestTermViewClient,
)
```

### UI: Single-screen layout with tab bar

Replace the two-route navigation (QuickConnect ↔ Terminal) with a single `TerminalHostScreen` that has a tab bar on top and shows the quick connect form as a dialog.

```
+----------------------------------------------+
| [root@server1] [dev@server2]  [+]            |  ← TabBar (Compose)
+----------------------------------------------+
|                                               |
|         Terminal Content (AndroidView)        |  ← TerminalView
|                                               |
+----------------------------------------------+
```

**New: `app/src/main/java/com/questterm/ui/screens/TerminalHostScreen.kt`**
- Composes TabBar + TerminalContent + QuickConnectDialog
- Auto-shows QuickConnect dialog when no tabs exist
- Injects TerminalViewModel and QuickConnectViewModel

**New: `app/src/main/java/com/questterm/ui/components/TabBar.kt`**
- `ScrollableTabRow` with tab label + close button per tab
- "+" button opens QuickConnect dialog

**New: `app/src/main/java/com/questterm/ui/components/TerminalContent.kt`**
- Extracted from current TerminalScreen.kt
- `AndroidView` wrapping TerminalView
- `update` lambda handles tab switching: re-attaches viewClient and session

### Modified files

| File | Change |
|------|--------|
| `TerminalViewModel.kt` | Remove companion object, inject SessionManager |
| `QuickConnectViewModel.kt` | Inject SessionManager, `addTab()` instead of static assignment |
| `QuickConnectScreen.kt` | Refactor into `QuickConnectDialog` (dialog wrapper) |
| `Screen.kt` | Replace QuickConnect+Terminal routes with single `TerminalHost` |
| `NavGraph.kt` | Single route: `TerminalHost` |
| `SshSession.kt` | Expose `connection` as public getter (needed for SFTP) |

**Delete: `TerminalScreen.kt`** (replaced by TerminalHostScreen + TerminalContent)

### Tab switching mechanics

When switching tabs, the `update` lambda on `AndroidView`:
1. Detaches old tab's `QuestTermViewClient` from the `TerminalView`
2. Attaches new tab's `QuestTermViewClient` via `viewClient.attachView(view)`
3. Calls `view.attachSession(newSession)` which sets the emulator and triggers redraw

Each `QuestTermViewClient` holds a nullable `terminalView` ref — only the active tab's client should have a non-null ref.

### Build order (each step compiles)

1. Create `TabSession` + `SessionManager` (new files, no dependents yet)
2. Expose `SshSession.connection` (trivial getter)
3. Create `TabBar` composable (standalone)
4. Create `TerminalContent` composable (extracted from TerminalScreen)
5. Rework `TerminalViewModel` to use SessionManager
6. Rework `QuickConnectViewModel` to use SessionManager
7. Create `TerminalHostScreen`
8. Convert `QuickConnectScreen` → dialog
9. Update navigation (Screen.kt + NavGraph.kt)
10. Delete `TerminalScreen.kt`
11. Verify `MainActivity` scroll handling (should work unchanged — `findTerminalView` still finds the single TerminalView)

---

## Phase 3B: Integrated SFTP

ConnectBot sshlib already includes `SFTPv3Client`. It can share the same `Connection` as the shell.

### New files

**`app/src/main/java/com/questterm/ssh/SftpClient.kt`** — Kotlin coroutine wrapper:
- `open()` / `close()` — create/destroy `SFTPv3Client` from `SshSession.connection`
- `listDirectory(path)` → `List<SftpEntry>` — sorted, no `.`/`..`
- `downloadFile(remotePath, localFile, onProgress)`
- `uploadFile(localFile, remotePath, onProgress)`
- `mkdir()`, `delete()`, `rmdir()`, `rename()`, `canonicalPath()`

**`app/src/main/java/com/questterm/ssh/SftpEntry.kt`** — data class:
- name, size, modifiedTime, isDirectory, isSymlink, permissions

**`app/src/main/java/com/questterm/ui/screens/SftpViewModel.kt`**:
- currentPath, entries, isLoading, error, transferProgress, isPaneOpen
- `openSftp()`, `navigateTo()`, `navigateUp()`, `togglePane()`
- Resets when tab switches

**`app/src/main/java/com/questterm/ui/components/SftpPane.kt`**:
- Path breadcrumb bar with up-button
- `LazyColumn` of file entries (icon, name, size, date)
- Tap directory → navigate into it
- File context actions (download, delete)
- Upload button + transfer progress bar
- 48dp min row height for Quest controller ray-casting

### Integration into TerminalHostScreen

```
+----------------------------------------------+
| [Tab1] [Tab2]  [+]              [SFTP toggle]|
+----------------------------------------------+
|                            |                  |
|    Terminal (weight 1f)    | SFTP Pane (~400dp)|
|                            |                  |
+----------------------------------------------+
```

SFTP pane slides in/out with `AnimatedVisibility`. Uses `Row` with `weight(1f)` for terminal + fixed width for SFTP.

### Modified files (Phase 3B)

| File | Change |
|------|--------|
| `TabSession.kt` | Add lazy `sftpClient` property |
| `TerminalHostScreen.kt` | Add SFTP pane + toggle |
| `TabBar.kt` | Add SFTP toggle button |

### File picker

- Upload: Android SAF `ActivityResultContracts.OpenDocument`
- Download: Save to `context.getExternalFilesDir("downloads")`

---

## Verification

1. **Tabs**: Open 3+ tabs to different servers, switch between them, verify each shows correct terminal state. Close middle tab, verify adjacent tab activates.
2. **QuickConnect dialog**: Opens on "+" tap, closes on connect, can't dismiss when no tabs exist.
3. **SFTP**: Toggle pane, navigate directories, download a file, upload a file, verify progress bar.
4. **Scroll**: Quest controller thumbstick still scrolls the active terminal tab.
5. **Host keys**: New servers still show fingerprint dialog.
6. **Build**: `./gradlew assembleDebug` succeeds. Install on Quest and test.

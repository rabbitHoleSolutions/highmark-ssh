# Changelog

All notable changes to Highmark SSH will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.4] - 2026-11-14

### Added
- Pop a tab out into its own window with the ↗ button (contributed by @j333c, PR #5, reworked)
  - The session moves without reconnecting; closing a window disconnects only its own sessions
  - A popped-out window closes itself when its last tab is closed
- Unit test suite (JUnit, MockK, coroutines-test) for sessions, profiles and the connect form (contributed by @j333c, PR #5)

### Changed
- Saved connections show every stored credential ("🔑 Key saved" and/or "Password saved"), not just the last-used method
- The key panel says whether it's showing the connection's saved key or a new one
- "Authentication failed" now says what to check (password, or the key in `authorized_keys`)

### Fixed
- The demo connection lost its saved password after the first connect, so later attempts sent a blank password
- Connection errors could be hidden below the fold of the connect form, so a failed connect gave no visible feedback
- Plugging in or unplugging a USB keyboard relaunched the app window, dropping keystrokes until it came back
- Builds without a release keystore no longer fail (contributed by @j333c, PR #5)

## [1.0.3] - 2026-09-22

### Added
- SSH public-key authentication (contributed by @freeagent1384, PR #3)
  - Generates an Ed25519 key on the headset and shows the `ssh-ed25519` line to add to `authorized_keys`
  - Copy or Share (system share sheet) the public key to install it on the server
  - Private key is encrypted at rest with the Android Keystore, like saved passwords
  - A profile keeps its saved key and password independently
- Paste from the clipboard via the text-selection menu (contributed by @freeagent1384, PR #4)

### Fixed
- On-screen keyboard not appearing on current Horizon OS builds, from either the ⌨ button or tapping the terminal
  - The terminal now presents itself as a text field (suggestions off) and requests the keyboard explicitly
- ⌨ button toggling against stale state instead of the keyboard's real visibility

## [1.0.2] - 2026-06-24

### Added
- Adjustable terminal font size with A−/A+ controls in the tab bar
  - Size persists across sessions and applies to all tabs
  - Reflows the terminal grid and notifies the remote shell on change
  - Needed on Quest, where the OS provides no usable pinch-to-zoom for 2D panel apps

## [1.0.0] - 2026-02-07

### Added
- Initial release of Highmark SSH for Meta Quest
- Native SSH client with password authentication
- Full VT-100 terminal emulation via Termux terminal-emulator
- Multi-tab session management
  - Create multiple SSH sessions in separate tabs
  - Switch between tabs with click navigation
  - Close tabs individually
  - New tab button (+) in tab bar
- Connection history management
  - Auto-saves last 8 connections
  - Star/favorite connections for permanent storage
  - Quick reconnect from saved connections
  - Delete unwanted connection history
- Encrypted password storage
  - AES-256-GCM encryption via Android Keystore
  - Optional per-connection password memory
  - "Remember password" checkbox per host
  - Hardware-backed encryption on Quest devices
- SSH host key verification
  - Trust-on-First-Use (TOFU) security model
  - SHA-256 fingerprint display
  - Host key change detection (MITM warning)
  - User confirmation dialog for unknown/changed keys
- Quest-optimized UI
  - 2D panel mode for comfortable VR viewing
  - Landscape-only orientation
  - Edge-to-edge display maximizes terminal space
  - Side-by-side saved connections and connection form layout
- VR controller support
  - Scroll wheel navigation in terminal
  - Smooth scroll accumulation
- Connection shortcuts
  - Parse `username@host:port` format for auto-fill
  - Click saved connections to auto-fill form
  - Port field auto-parses from host string
- Supported Quest devices: Quest 2, Quest Pro, Quest 3, Quest 3S

### Technical Details
- Built with Jetpack Compose (Material 3)
- Hilt dependency injection
- Coroutine-based async operations
- MVVM architecture pattern
- SharedPreferences for lightweight storage
- ConnectBot SSHLib for SSH protocol
- Minimum SDK: Android 10 (API 29)
- Target SDK: Android 14 (API 34)
- Compile SDK: Android 15 (API 35)

### Known Limitations
- SSH key authentication not supported (password only)
- SFTP file transfers not implemented
- No port forwarding support
- Single window/panel only

---

## [Unreleased]

### Planned for v1.1.0
- Additional SSH key types (RSA, ECDSA)
- SFTP file browser integration
- Port forwarding (local and remote)
- Custom color schemes
- Session profile management
- Connection groups/folders

### Future Considerations
- Arranged multi-window VR layouts (beyond pop-out panels)
- Tmux/screen integration
- Command history search
- Gesture-based shortcuts

---

[1.0.0]: https://github.com/rabbitHoleSolutions/highmark-ssh/releases/tag/v1.0.0

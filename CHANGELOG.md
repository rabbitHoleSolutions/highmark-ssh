# Changelog

All notable changes to Highmark SSH will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
- SSH key authentication (ED25519, RSA, ECDSA)
- SFTP file browser integration
- Port forwarding (local and remote)
- Custom color schemes
- Font size adjustment
- Session profile management
- Connection groups/folders

### Future Considerations
- Multi-window VR layout (floating terminals)
- Tmux/screen integration
- Command history search
- Gesture-based shortcuts
- Clipboard integration

---

[1.0.0]: https://github.com/rabbitHoleSolutions/highmark-ssh/releases/tag/v1.0.0

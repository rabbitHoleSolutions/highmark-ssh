# Highmark SSH

A native SSH terminal emulator for Meta Quest headsets, bringing full-featured remote server access to VR.

## Features

### Core Functionality

- **Native SSH Client** - Direct SSH connections to remote servers
- **Full Terminal Emulation** - VT-100 compatible terminal via Termux terminal-emulator
- **Multi-Tab Sessions** - Run multiple SSH sessions simultaneously with tab management
- **Trust-on-First-Use (TOFU)** - Secure host key verification with fingerprint display
- **Connection History** - Auto-saves last 8 connections for quick reconnect
- **Favorite Connections** - Star connections to save them permanently
- **Encrypted Password Storage** - AES-256-GCM encryption via Android Keystore (hardware-backed)

### Quest-Optimized UX

- **2D Panel Mode** - Comfortable flat panel interface in VR
- **Landscape Orientation** - Optimized for Quest's display format
- **Controller Scroll Support** - Native VR controller wheel scrolling
- **Virtual Keyboard** - Quest keyboard integration for text input
- **Edge-to-Edge Display** - Maximizes usable terminal space

### Advanced Features

- **Username@Host Parsing** - Paste `user@host:port` format for quick fill
- **Per-Connection Password Memory** - Choose which passwords to remember
- **Host Key Change Detection** - Warns about potential MITM attacks
- **Session Cleanup** - Proper SSH session termination on disconnect

## Supported Devices

- Meta Quest 2
- Meta Quest Pro
- Meta Quest 3
- Meta Quest 3S

Minimum: Android 10 (API 29)

## Installation

### From Meta Quest Store

_(Coming soon)_

### Sideload (Development)

1. Enable Developer Mode on your Quest headset
2. Download the latest APK from [Releases](../../releases)
3. Install via ADB:
   ```bash
   adb install -r highmark-ssh-v1.0.0.apk
   ```

## Usage

### First Connection

1. Launch Highmark SSH from your Quest app library
2. Enter your SSH server details:
   - **Host**: Server hostname or IP address
   - **Port**: SSH port (default: 22)
   - **Username**: Your SSH username
   - **Password**: Your SSH password
3. Check "Remember password" to save credentials (encrypted)
4. Tap "Connect"
5. If prompted, verify the host key fingerprint
6. Accept the key to connect

### Quick Connect

- **Paste Format**: Type or paste `username@host:port` in the Host field - username and port will auto-fill
- **Saved Connections**: Click any saved connection to auto-fill the form
- **Favorites**: Star frequently-used connections to keep them permanently

### Multi-Tab Sessions

- **New Tab**: Click the "+" button in the tab bar
- **Switch Tabs**: Click on any tab label
- **Close Tab**: Click the "X" icon on a tab
- Tabs persist until closed or app restart

### Scrolling

- Use your VR controller's scroll wheel to navigate terminal history
- Scroll accumulates smoothly for precise control

## Build from Source

### Prerequisites

- Android SDK (API 34+)
- Java 17 or higher
- Gradle 8.x
- Meta Quest Developer Hub (for deployment)

### Build Instructions

```bash
# Clone repository
git clone https://github.com/rabbitHoleSolutions/highmark-ssh.git
cd highmark-ssh

# Set JAVA_HOME (macOS example)
export JAVA_HOME=/path/to/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Install to connected Quest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Project Structure

```
app/src/main/java/com/questterm/
├── data/               # Connection profiles and history storage
├── session/            # Multi-tab session management
├── ssh/                # SSH connection and encryption
├── terminal/           # Terminal emulation (Termux integration)
└── ui/                 # Jetpack Compose UI components
```

## Security

### Credential Storage

- Passwords encrypted with **AES-256-GCM**
- Encryption keys stored in **Android Keystore** (hardware-backed on Quest)
- Automatic IV generation prevents replay attacks
- GCM authentication tag ensures data integrity
- Old plaintext credentials migrated automatically

### SSH Host Verification

- **Trust-on-First-Use (TOFU)** model
- Host keys stored in SharedPreferences with SHA-256 fingerprints
- Warns if host key changes (potential MITM attack)
- User must explicitly accept changed keys

### Network Permissions

- `INTERNET` - Required for SSH connections
- `ACCESS_NETWORK_STATE` - Check network availability
- `ACCESS_WIFI_STATE` - WiFi status detection

**No tracking, no analytics, no external servers.**

## Privacy

Highmark SSH does not:

- Collect any user data
- Send any telemetry or analytics
- Connect to any servers except your SSH hosts
- Require a Highmark SSH account

All data stays on your device.

## Known Limitations

- SSH key authentication not yet supported (password only)
- SFTP file transfers not yet implemented
- No port forwarding support
- Single window/panel only (no multi-window VR layout)

## Troubleshooting

### Connection Issues

- **"Connection refused"**: Verify SSH server is running and port is correct
- **"Host key changed"**: Your server's SSH key changed - verify it's legitimate before accepting
- **"Authentication failed"**: Check username/password are correct

### Display Issues

- **Terminal too small**: App uses landscape mode - ensure Quest is in correct orientation
- **Text not visible**: Check Quest brightness settings

### Performance Issues

- **Laggy input**: Close unused tabs to free resources
- **Scroll stuttering**: Try smaller scroll gestures

## Dependencies

- [Termux Terminal Emulator](https://github.com/termux/termux-app) (GPL v3) - VT-100 terminal emulation
- [ConnectBot SSHLib](https://github.com/connectbot/sshlib) (Apache 2.0) - SSH protocol implementation
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) - Dependency injection
- [Gson](https://github.com/google/gson) (Apache 2.0) - JSON serialization

## License

This project uses a dual-license approach:

- **Custom Code** (Highmark SSH-specific): MIT License
- **Termux Terminal Emulator Integration**: GPL v3

See [LICENSE](LICENSE) for full details. When using or modifying this project, please respect both licenses.

## Contributing

Contributions welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Roadmap

### Planned Features (Phase 3B+)

- SSH key authentication (ED25519, RSA)
- SFTP file browser and transfers
- Local port forwarding
- Custom color schemes
- Font size adjustment
- Saved session profiles with custom labels

### Ideas for Future

- Multi-window VR layout (floating terminals)
- Gesture-based shortcuts
- Command history search
- Tmux integration
- Connection groups/folders

## Support

- **Issues**: [GitHub Issues](../../issues)
- **Discussions**: [GitHub Discussions](../../discussions)

## Credits

Created by [@jasonburrows](https://github.com/jasonburrows) for the Meta Quest platform.

Built with ❤️ for the VR developer community.

## Hi Mark!

---

**Version**: 1.0.0
**Last Updated**: February 2026

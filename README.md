# Highmark SSH

A native SSH terminal emulator for Meta Quest headsets, bringing full-featured remote server access to VR.

## Features

### Core Functionality

- **Native SSH Client** - Direct SSH connections to remote servers
- **Full Terminal Emulation** - VT-100 compatible terminal via Termux terminal-emulator
- **Multi-Tab Sessions** - Run multiple SSH sessions simultaneously with tab management
- **Pop-Out Windows** - Move any tab into its own panel without dropping the connection
- **Trust-on-First-Use (TOFU)** - Secure host key verification with fingerprint display
- **Connection History** - Auto-saves last 8 connections for quick reconnect
- **Favorite Connections** - Star connections to save them permanently
- **Encrypted Credential Storage** - Saved passwords and SSH keys encrypted with AES-256-GCM via Android Keystore (hardware-backed)

### Quest-Optimized UX

- **2D Panel Mode** - Comfortable flat panel interface in VR
- **Landscape Orientation** - Optimized for Quest's display format
- **Controller Scroll Support** - Native VR controller wheel scrolling
- **Virtual Keyboard** - Quest keyboard integration for text input
- **Physical Keyboards** - Plug in a USB keyboard and type straight into the terminal
- **Adjustable Font Size** - A−/A+ controls in the tab bar
- **Edge-to-Edge Display** - Maximizes usable terminal space

### Advanced Features

- **Ed25519 Public Key Authentication** - On-device Ed25519 key pair generation and public key display for passwordless auth
- **Username@Host Parsing** - Paste `user@host:port` format for quick fill
- **Clipboard Paste** - Paste into the terminal from the text-selection menu
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
2. Build the APK from source (see [Build from Source](#build-from-source))
3. Install via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Usage

### First Connection

1. Launch Highmark SSH from your Quest app library
2. Enter your SSH server details:
   - **Host**: Server hostname or IP address
   - **Port**: SSH port (default: 22)
   - **Username**: Your SSH username
   - **Password** or **Key**: choose how to log in
3. For a password, check "Remember password" to save it (encrypted)
4. Tap "Connect"
5. If prompted, verify the host key fingerprint
6. Accept the key to connect

### Key Authentication

1. Fill in the host, port and username, then choose **Key**
2. A new Ed25519 key is generated for that connection. Use **Copy** or **Share** to send the public key line to your computer
3. Add that line to `~/.ssh/authorized_keys` on the server
4. Tap "Connect"

Each saved connection keeps its own key, separately from any saved password. The key panel says whether it's showing the connection's saved key or a new one, and **Regen** replaces the saved key (you'll need to install the new one on the server).

### Quick Connect

- **Paste Format**: Type or paste `username@host:port` in the Host field - username and port will auto-fill
- **Saved Connections**: Click any saved connection to auto-fill the form. Each shows "🔑 Key saved" and/or "Password saved" for what it has stored
- **Favorites**: Star frequently-used connections to keep them permanently

### Multi-Tab Sessions

- **New Tab**: Click the "+" button in the tab bar
- **Switch Tabs**: Click on any tab label
- **Close Tab**: Click the "X" icon on a tab
- **Pop Out Tab**: With two or more tabs open, click the "↗" icon to move a tab into its own window. The session stays connected; closing that window disconnects it
- Tabs stay open until you close them or close their window

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

# Run unit tests
./gradlew testDebugUnitTest

# Build release APK (signed when keystore.properties is present, unsigned otherwise)
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

- Passwords and generated Ed25519 private keys encrypted with **AES-256-GCM**
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

- SFTP file transfers not yet implemented
- No port forwarding support
- Pop-out windows are plain panels; no arranged multi-window VR layout yet

## Troubleshooting

### Connection Issues

- **"Connection refused"**: Verify SSH server is running and port is correct
- **"Host key changed"**: Your server's SSH key changed - verify it's legitimate before accepting
- **"Authentication failed"**: Check username/password, or verify the generated public key is added to `~/.ssh/authorized_keys` on your server

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
- **Names, logos, icons and artwork**: not open source — all rights reserved

The code is yours to build on. The branding is not: "Rabbit Hole Solutions" and its logo are trademarks with a registration application pending, "Highmark SSH" and its logo are unregistered trademarks, and the icon, logo and artwork files in this repository are excluded from the MIT grant. They ship here so the project builds, not as a licence to reuse them.

See [LICENSE](LICENSE) for full details. When using or modifying this project, please respect both licenses.

## Contributing

Contributions welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Roadmap

### Planned Features (Phase 3B+)

- Additional SSH key formats (RSA, ECDSA)
- SFTP file browser and transfers
- Local port forwarding
- Custom color schemes
- Saved session profiles with custom labels

### Ideas for Future

- Arranged multi-window VR layouts (beyond pop-out panels)
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

**Version**: 1.0.4
**Last Updated**: November 2026

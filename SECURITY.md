# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

### How to Report

If you discover a security vulnerability in Highmark SSH, please report it privately:

**GitHub Security Advisories**:
- Go to the [Security tab](../../security)
- Click "Report a vulnerability"
- Fill out the form with detailed description, steps to reproduce, and potential impact

### What to Include

A good security report includes:
- **Description** of the vulnerability
- **Steps to reproduce** the issue
- **Potential impact** (what could an attacker do?)
- **Affected versions** (if known)
- **Proposed fix** (if you have one)
- **Screenshots or logs** (if applicable)

### Response Timeline

- **Acknowledgment**: Within 48 hours
- **Initial assessment**: Within 7 days
- **Fix timeline**: Depends on severity
  - **Critical**: Hotfix within 7 days
  - **High**: Patch within 30 days
  - **Medium/Low**: Included in next release

### Disclosure Policy

- We follow **coordinated disclosure** principles
- We will credit security researchers (unless they prefer anonymity)
- Public disclosure only after patch is available
- We'll coordinate disclosure timing with you

## Security Measures

### Credential Protection

Highmark SSH protects your SSH credentials using:

1. **AES-256-GCM Encryption**
   - Industry-standard authenticated encryption
   - 256-bit keys for maximum security
   - Galois/Counter Mode for integrity verification

2. **Android Keystore**
   - Encryption keys stored in Android Keystore
   - Hardware-backed security on Quest devices
   - Keys never leave secure hardware
   - Automatic key generation and management

3. **No Plaintext Storage**
   - Passwords never stored in plain text
   - Old plaintext credentials migrated automatically
   - Encrypted blobs stored in SharedPreferences

### SSH Security

1. **Trust-on-First-Use (TOFU)**
   - First connection saves host key fingerprint
   - Subsequent connections verify against saved key
   - Warns if host key changes (potential MITM attack)

2. **Host Key Verification**
   - SHA-256 fingerprints displayed to user
   - User must explicitly accept new/changed keys
   - Known hosts stored locally (per-device)

3. **No Credential Transmission**
   - Passwords sent only via SSH protocol encryption
   - No third-party servers involved
   - Direct connection to your SSH server

### Network Security

- **Direct connections only** - No proxy or relay servers
- **No analytics or tracking** - Zero telemetry data collected
- **No cloud services** - All data stays on device
- **Standard SSH protocol** - RFC 4253 compliant

### Permissions

Highmark SSH requests minimal permissions:
- `INTERNET` - Required for SSH connections
- `ACCESS_NETWORK_STATE` - Check network availability
- `ACCESS_WIFI_STATE` - WiFi status detection

**We do NOT request:**
- Camera, microphone, or location access
- Storage permissions (unless SFTP feature added)
- Contact or calendar access
- Any unnecessary permissions

## Known Security Considerations

### Current Limitations

1. **Password-Only Authentication**
   - SSH keys not yet supported (planned for v1.1.0)
   - Passwords are less secure than key-based auth
   - Consider using strong, unique passwords

2. **No Multi-Factor Authentication**
   - MFA happens at SSH server level
   - Highmark SSH supports keyboard-interactive if your server uses it

3. **Local Storage**
   - Encrypted credentials stored on device
   - If device is rooted, encryption may be bypassable
   - Non-rooted Quest devices have strong protection

### Best Practices for Users

1. **Use Strong Passwords**
   - Unique passwords for each SSH server
   - Consider using a password manager
   - Enable "Remember password" only for trusted servers

2. **Verify Host Keys**
   - Check fingerprints match your server's actual key
   - If key changes unexpectedly, investigate before accepting
   - Don't ignore MITM warnings

3. **Keep Highmark SSH Updated**
   - Install updates promptly
   - Check CHANGELOG for security fixes
   - Enable automatic updates if available

4. **Secure Your Quest Device**
   - Use a strong unlock pattern/PIN
   - Don't share your Quest with untrusted users
   - Logout from Highmark SSH sessions when done

5. **Network Security**
   - Use secure WiFi networks when possible
   - Avoid public WiFi for sensitive SSH sessions
   - Consider VPN for additional protection

## Security Roadmap

### Planned Security Enhancements (v1.1.0+)

- SSH key authentication (ED25519, RSA, ECDSA)
- SSH agent support
- Biometric authentication for app access
- Session timeout/auto-lock
- Secure clipboard handling

## Third-Party Dependencies

Highmark SSH relies on these security-relevant libraries:

| Dependency | Purpose | Security Notes |
|------------|---------|----------------|
| ConnectBot SSHLib | SSH protocol | Battle-tested, actively maintained |
| Termux Terminal Emulator | Terminal rendering | Widely used, GPL v3 licensed |
| Android Keystore | Credential encryption | OS-level security, hardware-backed |
| Gson | JSON serialization | Does not handle sensitive data |

We monitor security advisories for all dependencies and update promptly when vulnerabilities are disclosed.

## Security Contact

**For security inquiries (non-vulnerability)**:
- Open a [GitHub Discussion](../../discussions) in the Security category

**For vulnerability reports**:
- Use GitHub Security Advisories (see above)

---

**Last Updated**: February 2026

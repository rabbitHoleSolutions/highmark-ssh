# Contributing to Highmark SSH

Thank you for your interest in contributing to Highmark SSH! This document provides guidelines and instructions for contributing.

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community
- Show empathy towards other contributors

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

**Good bug reports include:**
- Clear, descriptive title
- Steps to reproduce the issue
- Expected vs. actual behavior
- Quest device model and software version
- Screenshots or screen recordings if applicable
- Relevant logs (use `adb logcat`)

### Suggesting Features

Feature requests are welcome! Please:
- Check if the feature has already been requested
- Provide clear use cases and examples
- Explain why this feature would benefit Quest users
- Consider VR-specific ergonomics and usability

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Follow the code style** (see below)
3. **Write clear commit messages** describing what and why
4. **Test on a physical Quest device** before submitting
5. **Update documentation** if you change functionality
6. **Reference related issues** in your PR description

## Development Setup

### Prerequisites
- **Android Studio** (latest stable)
- **Meta Quest Developer Hub** (for deployment)
- **Physical Quest device** (Quest 2, Pro, 3, or 3S)
- **JDK 17+** (Java 17 or higher)
- **Android SDK** (API 34+)

### Initial Setup
```bash
# Clone your fork
git clone https://github.com/<your-username>/highmark-ssh.git
cd highmark-ssh

# Configure JAVA_HOME
export JAVA_HOME=/path/to/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Build and install debug APK
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Development Workflow
```bash
# Create a feature branch
git checkout -b feature/your-feature-name

# Make your changes
# Test thoroughly on Quest

# Build and test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Commit with clear messages
git commit -m "Add: Brief description of your change"

# Push to your fork
git push origin feature/your-feature-name

# Open a Pull Request on GitHub
```

## Code Style

### Kotlin Guidelines
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **4 spaces** for indentation (not tabs)
- **Maximum line length**: 120 characters
- Use **meaningful variable names** (avoid abbreviations)
- Prefer **immutability** (`val` over `var`)
- Use **trailing commas** in multi-line lists

### Architecture Patterns
- **MVVM** for UI layer (ViewModel + Compose)
- **Repository pattern** for data access
- **Dependency injection** via Hilt
- **Coroutines** for async operations (avoid threads)
- **StateFlow** for reactive state management

### Compose UI Guidelines
- Use **Material 3** components
- Keep composables **small and focused**
- Extract reusable UI into separate composables
- Use `remember` and `rememberSaveable` appropriately
- Prefer **stateless composables** when possible

### File Organization
```
app/src/main/java/com/questterm/
├── data/          # Data models and storage
├── session/       # Session management
├── ssh/           # SSH connection logic
├── terminal/      # Terminal emulation
└── ui/            # Compose UI components
    ├── components/    # Reusable UI components
    ├── screens/       # Full screens/activities
    └── theme/         # Material 3 theme
```

### Naming Conventions
- **Composables**: `PascalCase` (e.g., `QuickConnectDialog`)
- **ViewModels**: `PascalCase` ending in `ViewModel`
- **Data classes**: `PascalCase` (e.g., `ConnectionProfile`)
- **Functions**: `camelCase` (e.g., `loadSavedProfiles`)
- **Private fields**: `camelCase` with `_` prefix for backing properties (e.g., `_uiState`)

## Testing

### Manual Testing Checklist
Before submitting a PR, test on a physical Quest device:

- [ ] **Connection flow**: Create new SSH connection
- [ ] **Host key verification**: Accept/reject unknown hosts
- [ ] **Multi-tab**: Open, switch, close multiple tabs
- [ ] **Saved connections**: Save, favorite, delete connections
- [ ] **Password memory**: Test with "Remember password" on/off
- [ ] **Terminal interaction**: Type commands, see output
- [ ] **Scrolling**: Use VR controller to scroll terminal
- [ ] **Error handling**: Test with invalid credentials
- [ ] **Back navigation**: Test Quest toolbar back button behavior
- [ ] **App lifecycle**: Pause/resume, background/foreground

### Unit Testing
- Write tests for business logic (ViewModels, repositories)
- Use JUnit 4 for unit tests
- Mock dependencies with Mockito or MockK

```kotlin
// Example test structure
class QuickConnectViewModelTest {
    @Test
    fun `selectProfile updates uiState with profile data`() {
        // Arrange
        val profile = ConnectionProfile(...)

        // Act
        viewModel.selectProfile(profile)

        // Assert
        assertEquals(profile.host, viewModel.uiState.value.host)
    }
}
```

## Commit Message Format

Use clear, imperative commit messages:

```
Add: Feature description (for new features)
Fix: Bug description (for bug fixes)
Update: Component description (for updates)
Refactor: Code description (for refactoring)
Docs: Documentation change
Test: Test addition/modification
```

**Examples:**
- `Add: SFTP file browser UI`
- `Fix: TabBar disappearing after reconnect`
- `Update: Encrypt passwords with AES-256-GCM`
- `Refactor: Extract connection logic into repository`

## Security Considerations

If you're working on security-sensitive areas:
- **Never hardcode credentials** or API keys
- **Always encrypt sensitive data** (use Android Keystore)
- **Validate all user input** for SSH connections
- **Use secure defaults** (e.g., TOFU for SSH)
- **Test security changes thoroughly**

Report security vulnerabilities privately via GitHub Security Advisories, not public issues.

## Documentation

Update documentation when you:
- Add new features
- Change existing behavior
- Add new dependencies
- Modify build process

**Files to update:**
- `README.md` - User-facing features and setup
- `CHANGELOG.md` - Version history and changes
- `CONTRIBUTING.md` - Developer guidelines (this file)
- Code comments - Explain "why", not "what"

## Questions?

- **General questions**: Open a [GitHub Discussion](../../discussions)
- **Bug reports**: Create a [GitHub Issue](../../issues)
- **Feature requests**: Create a [GitHub Issue](../../issues) with the "enhancement" label

---

Thank you for contributing to Highmark SSH! 🚀

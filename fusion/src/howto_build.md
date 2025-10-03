<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Installing and Building Ion Fusion

This guide covers multiple ways to install Ion Fusion, from building from source to using pre-built distributions.

## Table of Contents

- [Quick Start](#quick-start)
- [Installation Methods](#installation-methods)
- [Building from Source](#building-from-source)
- [Verification](#verification)
- [Troubleshooting](#troubleshooting)
- [What's Next](#whats-next)

## Quick Start

**For most users, building from source is currently the recommended approach:**

```bash
# Clone and build
git clone https://github.com/ion-fusion/fusion-java.git
cd fusion-java
./gradlew release

# Add to PATH
export PATH=$PATH:$PWD/build/install/fusion/bin

# Verify installation
fusion version
```

## Installation Methods

### Method 1: Build from Source (Recommended)

**Prerequisites:**
- Java 8 or later ([Corretto](https://aws.amazon.com/corretto/) recommended)
- [Git](https://git-scm.com/)
- Internet connection (for downloading dependencies)

**Advantages:**
- Always get the latest version
- Can customize the build
- Full control over the installation

**Steps:**

1. **Install Prerequisites:**
   ```bash
   # macOS with Homebrew
   brew install --cask corretto8
   brew install git
   
   # Ubuntu/Debian
   sudo apt-get update
   sudo apt-get install openjdk-8-jdk git
   
   # CentOS/RHEL/Fedora
   sudo yum install java-1.8.0-openjdk-devel git  # CentOS/RHEL
   sudo dnf install java-1.8.0-openjdk-devel git  # Fedora
   ```

2. **Verify Java Installation:**
   ```bash
   java -version
   # Should show Java 8 or later
   ```

3. **Clone and Build:**
   ```bash
   git clone https://github.com/ion-fusion/fusion-java.git
   cd fusion-java
   ./gradlew release
   ```

4. **Install to PATH:**
   ```bash
   # Temporary (current session only)
   export PATH=$PATH:$PWD/build/install/fusion/bin
   
   # Permanent (add to your shell profile)
   echo 'export PATH=$PATH:/path/to/fusion-java/build/install/fusion/bin' >> ~/.bashrc
   # OR for zsh:
   echo 'export PATH=$PATH:/path/to/fusion-java/build/install/fusion/bin' >> ~/.zshrc
   ```

### Method 2: Pre-built Releases *(Coming Soon)*

Pre-built binaries will be available from the [GitHub releases page](https://github.com/ion-fusion/fusion-java/releases) once version 1.0 is released.

**Planned distribution formats:**
- Standalone JAR files
- Platform-specific installers
- Docker containers
- Package manager distributions (Homebrew, apt, etc.)

### Method 3: Docker *(Future)*

Docker images will be available for easy containerized deployment:

```bash
# Planned usage (not yet available)
docker run -it ion-fusion/fusion:latest repl
```

## Building from Source

### Standard Build

The standard build creates a complete SDK with CLI, documentation, and libraries:

```bash
./gradlew release
```

**Build artifacts:**
- `build/install/fusion/bin/fusion` - CLI executable
- `build/install/fusion/lib/` - JAR files for embedding
- `build/install/fusion/docs/fusiondoc/` - Language documentation
- `build/install/fusion/docs/javadoc/` - Java API documentation

### Development Build

For development and testing:

```bash
./gradlew build
```

This creates a faster build without full documentation generation.

### Custom Build Options

**Clean build:**
```bash
./gradlew clean release
```

**Build with specific Java version:**
```bash
export JAVA_HOME=/path/to/java8
./gradlew release
```

**Offline build (if dependencies already cached):**
```bash
./gradlew --offline release
```

**Parallel build (faster on multi-core systems):**
```bash
./gradlew --parallel release
```

### Build Requirements

**System Requirements:**
- **Memory:** At least 2GB RAM (4GB recommended)
- **Disk Space:** At least 1GB free space
- **Network:** Internet connection for downloading dependencies (first build only)

**Java Requirements:**
- **Version:** Java 8 or later (Java 11+ recommended for better performance)
- **Distribution:** Any OpenJDK distribution (Corretto, AdoptOpenJDK, etc.)

### Build Troubleshooting

**Common Issues:**

1. **Out of Memory:**
   ```bash
   export GRADLE_OPTS="-Xmx2g"
   ./gradlew release
   ```

2. **Network Issues:**
   ```bash
   # Try with different repository
   ./gradlew --refresh-dependencies release
   ```

3. **Permission Issues:**
   ```bash
   chmod +x gradlew
   ./gradlew release
   ```

4. **Java Version Issues:**
   ```bash
   # Check Java version
   java -version
   javac -version
   
   # Set specific Java version
   export JAVA_HOME=/path/to/correct/java
   ```

## Verification

### Verify Installation

```bash
# Check CLI is available
fusion help

# Check version information
fusion version

# Test basic functionality
fusion eval '(+ 1 2 3)'
# Should output: 6
```

### Verify Build Artifacts

```bash
# Check CLI executable
ls -la build/install/fusion/bin/fusion

# Check libraries
ls -la build/install/fusion/lib/

# Check documentation
ls -la build/install/fusion/docs/
```

### Run Tests

```bash
# Run all tests
./gradlew test

# Run specific test suite
./gradlew test --tests "*CliTest*"

# Run with coverage
./gradlew test jacocoTestReport
```

## Advanced Installation

### System-wide Installation

**Linux/macOS:**
```bash
# Copy to system location
sudo cp -r build/install/fusion /opt/
sudo ln -s /opt/fusion/bin/fusion /usr/local/bin/fusion

# Or create a package
./gradlew distTar
# Extract tar to desired location
```

**Windows:**
```cmd
# Copy to Program Files
xcopy build\install\fusion "C:\Program Files\Ion-Fusion\" /E /I

# Add to PATH via System Properties or:
setx PATH "%PATH%;C:\Program Files\Ion-Fusion\bin"
```

### IDE Integration

**IntelliJ IDEA:**
1. Import the Gradle project
2. Set Project SDK to Java 8+
3. Enable annotation processing
4. Configure code style (see `doc/draft/style_guide.md`)

**Eclipse:**
1. Import as Gradle project
2. Configure Java Build Path
3. Set up code formatting

**VS Code:**
1. Install Java Extension Pack
2. Open project folder
3. Configure Java runtime

### Custom Repositories

For organizations wanting to customize Fusion:

```bash
# Fork the repository
git clone https://github.com/yourorg/fusion-java.git
cd fusion-java

# Make customizations
# ... edit source files ...

# Build custom version
./gradlew release

# Create custom distribution
./gradlew distTar distZip
```

## Troubleshooting

### Build Failures

**Gradle daemon issues:**
```bash
./gradlew --stop
./gradlew release
```

**Dependency resolution:**
```bash
./gradlew --refresh-dependencies clean release
```

**Disk space:**
```bash
df -h .
./gradlew clean  # Free up space
```

### Runtime Issues

**Java not found:**
```bash
which java
export JAVA_HOME=/path/to/java
export PATH=$JAVA_HOME/bin:$PATH
```

**Permission denied:**
```bash
chmod +x build/install/fusion/bin/fusion
```

**Library conflicts:**
```bash
# Check for conflicting JARs
java -cp "build/install/fusion/lib/*" -version
```

### Getting Help

If you encounter issues:

1. **Check the [troubleshooting guide](cli_troubleshooting.html)**
2. **Search [existing issues](https://github.com/ion-fusion/fusion-java/issues)**
3. **Ask on [GitHub Discussions](https://github.com/orgs/ion-fusion/discussions)**
4. **Join our [Slack workspace](https://join.slack.com/t/ion-fusion/shared_invite/zt-2y0jr8vh2-bZLa66hdyZ3ykHcgOcYkcA)**

## What's Next?

With Ion Fusion installed, you're ready to start coding:

### Learn the CLI
- **[CLI Quick Start](cli_quickstart.html)** - Get productive in minutes
- **[CLI Tutorial](tutorial_cli.html)** - Comprehensive learning guide
- **[CLI Reference](cli_reference.html)** - Complete command documentation

### Explore the Language
- **[Language Reference](fusion.html)** - Complete language documentation
- **[Java API](javadoc/index.html)** - For embedding in applications

### Join the Community
- **[GitHub](https://github.com/ion-fusion/fusion-java)** - Source code and issues
- **[Slack](https://join.slack.com/t/ion-fusion/shared_invite/zt-2y0jr8vh2-bZLa66hdyZ3ykHcgOcYkcA)** - Community chat
- **[Discussions](https://github.com/orgs/ion-fusion/discussions)** - Questions and help

Happy coding with Ion Fusion!

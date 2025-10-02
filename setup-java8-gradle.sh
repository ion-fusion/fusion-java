#!/bin/bash

echo "🔧 Starting Java 8 setup for Gradle..."

# 1. Install SDKMAN if it doesn't exist
if [ -z "$SDKMAN_DIR" ] && [ ! -d "$HOME/.sdkman" ]; then
  echo "📥 Installing SDKMAN..."
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
else
  echo "✅ SDKMAN is already installed."
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# 2. Install Java 8
echo "📦 Installing Java 8 (Amazon Corretto)..."
sdk install java 8.0.392-amzn

# 3. Configure Gradle to use Java 8 with toolchain
mkdir -p ./gradle
GRADLE_PROPS_FILE=./gradle/gradle.properties

echo "📝 Configuring gradle.properties to use Java 8..."
cat > "$GRADLE_PROPS_FILE" <<EOF
org.gradle.java.installations.paths=${HOME}/.sdkman/candidates/java/8.0.392-amzn
EOF

# 4. Show installed Java version
echo "✅ Java installed and configured. Verifying..."
JAVA_PATH="${HOME}/.sdkman/candidates/java/8.0.392-amzn/bin/java"
"$JAVA_PATH" -version

echo "🎉 Done! You can now run your Gradle build."


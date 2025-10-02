<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Ion Fusion CLI Troubleshooting Guide

This guide helps you diagnose and fix common issues when using the Ion Fusion CLI. If you don't find your issue here, please check our [community resources](#getting-help) for additional support.

## Table of Contents

- [Installation Issues](#installation-issues)
- [Command Execution Problems](#command-execution-problems)
- [Syntax and Runtime Errors](#syntax-and-runtime-errors)
- [Module and Import Issues](#module-and-import-issues)
- [Performance Problems](#performance-problems)
- [File and Path Issues](#file-and-path-issues)
- [REPL Issues](#repl-issues)
- [Environment and Configuration](#environment-and-configuration)
- [Getting Help](#getting-help)

## Installation Issues

### Command Not Found

**Problem:**
```bash
$ fusion help
fusion: command not found
```

**Solutions:**

1. **Check if `fusion` is in your PATH:**
   ```bash
   which fusion
   echo $PATH
   ```

2. **If you built from source, add to PATH:**
   ```bash
   export PATH=$PATH:/path/to/fusion-java/build/install/fusion/bin
   # Add to your shell profile (.bashrc, .zshrc, etc.) to make permanent
   ```

3. **Verify the binary exists and is executable:**
   ```bash
   ls -la /path/to/fusion/bin/fusion
   chmod +x /path/to/fusion/bin/fusion  # If not executable
   ```

4. **Try using the full path:**
   ```bash
   /path/to/fusion/bin/fusion help
   ```

### Java Runtime Issues

**Problem:**
```bash
$ fusion help
Error: Could not find or load main class dev.ionfusion.fusion.cli.Cli
```

**Solutions:**

1. **Check Java installation:**
   ```bash
   java -version
   which java
   ```

2. **Ensure Java 8 or later is installed:**
   ```bash
   # Install Amazon Corretto (recommended)
   # macOS with Homebrew:
   brew install --cask corretto8
   
   # Ubuntu/Debian:
   sudo apt-get install openjdk-8-jdk
   ```

3. **Check JAVA_HOME environment variable:**
   ```bash
   echo $JAVA_HOME
   export JAVA_HOME=/path/to/java
   ```

### Build Issues

**Problem:**
```bash
$ ./gradlew release
BUILD FAILED
```

**Solutions:**

1. **Check Java version for building:**
   ```bash
   java -version  # Should be Java 8 or later
   ```

2. **Clean and rebuild:**
   ```bash
   ./gradlew clean
   ./gradlew release
   ```

3. **Check for network issues (if downloading dependencies):**
   ```bash
   ./gradlew --offline release  # Try offline build
   ```

4. **Check disk space:**
   ```bash
   df -h .
   ```

## Command Execution Problems

### General Usage Errors

**Problem:**
```bash
$ fusion
No command given.
Type 'fusion help' for more information.
```

**Solution:**
Always specify a command:
```bash
fusion help           # Show available commands
fusion eval '(+ 1 2)' # Evaluate an expression
fusion repl           # Start interactive mode
```

### Invalid Command Arguments

**Problem:**
```bash
$ fusion eval
Usage: eval EXPRESSIONS
Type 'fusion help eval' for more information.
```

**Solution:**
Provide required arguments:
```bash
fusion eval '(+ 1 2)'              # Correct
fusion help eval                   # Get detailed help
```

### Shell Quoting Issues

**Problem:**
```bash
$ fusion eval (+ 1 2)
bash: syntax error near unexpected token `('
```

**Solutions:**

1. **Use single quotes (recommended):**
   ```bash
   fusion eval '(+ 1 2)'
   ```

2. **Use double quotes with escaping:**
   ```bash
   fusion eval "(+ 1 2)"
   ```

3. **Escape special characters:**
   ```bash
   fusion eval \(+ 1 2\)
   ```

### Command Chaining Problems

**Problem:**
```bash
$ fusion require /fusion/string ; eval '(string_length "test")'
bash: eval: command not found
```

**Solution:**
Quote or escape the semicolon:
```bash
fusion require /fusion/string ';' eval '(string_length "test")'
# OR
fusion require /fusion/string \; eval '(string_length "test")'
```

## Syntax and Runtime Errors

### Unbalanced Parentheses

**Problem:**
```bash
$ fusion eval '(+ 1 2'
Bad syntax: unexpected end of input
```

**Solution:**
Ensure all parentheses are balanced:
```bash
fusion eval '(+ 1 2)'  # Correct
```

### Unbound Identifier

**Problem:**
```bash
$ fusion eval '(string_length "hello")'
Bad syntax: unbound identifier. The symbol 'string_length' has no binding
```

**Solutions:**

1. **Import the required module:**
   ```bash
   fusion require '/fusion/string' ';' eval '(string_length "hello")'
   ```

2. **Check spelling and case sensitivity:**
   ```bash
   fusion eval '(string_append "a" "b")'  # Built-in function
   ```

3. **Use built-in functions when available:**
   ```bash
   fusion eval '(+ 1 2)'  # Math functions are built-in
   ```

### Type Errors

**Problem:**
```bash
$ fusion eval '(+ "hello" 5)'
Argument error: + expects number, got string "hello"
```

**Solution:**
Ensure arguments match expected types:
```bash
fusion eval '(+ 5 3)'                    # Numbers
fusion eval '(string_append "hello" "5")' # Strings
```

### Arity Errors

**Problem:**
```bash
$ fusion eval '(+ 1)'
Arity error: + expects at least 2 arguments, got 1
```

**Solution:**
Provide the correct number of arguments:
```bash
fusion eval '(+ 1 2)'      # Correct
fusion eval '(+ 1 2 3 4)'  # Also correct (variadic)
```

## Module and Import Issues

### Module Not Found

**Problem:**
```bash
$ fusion require '/mymodule'
Module not found: /mymodule
```

**Solutions:**

1. **Check module path spelling:**
   ```bash
   fusion require '/fusion/string'  # Standard library module
   ```

2. **Use repository option for custom modules:**
   ```bash
   fusion --repositories /path/to/modules require '/mymodule'
   ```

3. **List available modules:**
   ```bash
   fusion require '/fusion' ';' repl
   $ (help)  # In REPL, shows available functions
   ```

### Repository Configuration

**Problem:**
```bash
$ fusion --repositories /nonexistent require '/mymodule'
repositories: Directory does not exist: /nonexistent
```

**Solutions:**

1. **Check directory exists:**
   ```bash
   ls -la /path/to/modules
   ```

2. **Use absolute paths:**
   ```bash
   fusion --repositories /absolute/path/to/modules require '/mymodule'
   ```

3. **Use multiple repositories:**
   ```bash
   fusion --repositories /path1:/path2 require '/mymodule'
   ```

## Performance Problems

### Slow Startup

**Problem:**
CLI takes a long time to start.

**Solutions:**

1. **Check available memory:**
   ```bash
   free -h  # Linux
   vm_stat  # macOS
   ```

2. **Increase JVM heap size:**
   ```bash
   export JAVA_OPTS="-Xmx2g"
   fusion eval '(+ 1 2)'
   ```

3. **Use SSD storage if possible**

4. **Check for antivirus interference**

### Memory Issues

**Problem:**
```bash
$ fusion eval '(very-large-computation)'
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**

1. **Increase heap size:**
   ```bash
   export JAVA_OPTS="-Xmx4g"
   fusion eval '(your-expression)'
   ```

2. **Process data in chunks:**
   ```bash
   # Instead of loading all data at once
   fusion eval '(for [(item (in_port))] (process-item item))'
   ```

3. **Use streaming operations:**
   ```bash
   # Stream processing instead of collecting all results
   cat large-file.ion | fusion eval '(for [(item (in_port))] (writeln (process item)))'
   ```

### Slow Data Processing

**Problem:**
Data processing is slower than expected.

**Solutions:**

1. **Use Ion binary format:**
   ```bash
   # Convert to binary for faster processing
   fusion eval '(ionize_to_blob data)' > data.10n
   ```

2. **Optimize algorithms:**
   ```bash
   # Use built-in functions when possible
   fusion require '/fusion/list' ';' eval '(map process-fn data)'
   ```

3. **Profile with coverage tools:**
   ```bash
   fusion --repositories ./modules load --profile script.fusion
   ```

## File and Path Issues

### File Not Found

**Problem:**
```bash
$ fusion load script.fusion
Script is not a readable file: script.fusion
```

**Solutions:**

1. **Check file exists:**
   ```bash
   ls -la script.fusion
   ```

2. **Check file permissions:**
   ```bash
   chmod +r script.fusion
   ```

3. **Use absolute path:**
   ```bash
   fusion load /absolute/path/to/script.fusion
   ```

4. **Check current directory:**
   ```bash
   pwd
   ls *.fusion
   ```

### Permission Denied

**Problem:**
```bash
$ fusion load script.fusion
Permission denied: script.fusion
```

**Solutions:**

1. **Fix file permissions:**
   ```bash
   chmod +r script.fusion
   ```

2. **Check directory permissions:**
   ```bash
   ls -la .
   chmod +rx .  # If directory not readable/executable
   ```

3. **Run with appropriate user:**
   ```bash
   sudo fusion load script.fusion  # If necessary (not recommended)
   ```

## REPL Issues

### REPL Won't Start

**Problem:**
```bash
$ fusion repl
This command cannot be used when stdin or stdout have been redirected.
```

**Solution:**
Don't redirect stdin/stdout when using REPL:
```bash
# Wrong:
echo "test" | fusion repl

# Correct:
fusion repl
```

### REPL Display Issues

**Problem:**
Colors or formatting look wrong in REPL.

**Solutions:**

1. **Check terminal capabilities:**
   ```bash
   echo $TERM
   ```

2. **Use a modern terminal:**
   - Terminal.app (macOS)
   - GNOME Terminal (Linux)
   - Windows Terminal (Windows)

3. **Disable colors if needed:**
   ```bash
   export NO_COLOR=1
   fusion repl
   ```

### REPL History Issues

**Problem:**
Arrow keys don't work for command history.

**Solutions:**

1. **Use a terminal with line editing support**

2. **Install readline support for Java**

3. **Use external tools:**
   ```bash
   rlwrap fusion repl  # If rlwrap is available
   ```

## Environment and Configuration

### Environment Variables

**Common environment variables:**

```bash
# Java options
export JAVA_OPTS="-Xmx2g -XX:+UseG1GC"

# Disable colors
export NO_COLOR=1

# Custom Java home
export JAVA_HOME=/path/to/java

# Additional classpath
export CLASSPATH="/path/to/additional/jars:$CLASSPATH"
```

### Configuration Files

**Check for configuration issues:**

1. **Coverage configuration:**
   ```bash
   cat build/fcov/config.properties
   ```

2. **Repository structure:**
   ```bash
   find /path/to/repositories -name "*.fusion" | head -10
   ```

### Debugging Options

**Enable debugging:**

```bash
# Verbose Java output
export JAVA_OPTS="-verbose:class -verbose:gc"

# Enable assertions
export JAVA_OPTS="-ea"

# Debug class loading
export JAVA_OPTS="-XX:+TraceClassLoading"
```

## Getting Help

### Built-in Help

```bash
fusion help                    # List all commands
fusion help <command>          # Detailed command help
fusion version                 # Version information
```

### REPL Help

```fusion
$ (help)                      # General help
$ (help +)                    # Help for specific function
$ (help "string")             # Search help
```

### Community Resources

- **[GitHub Issues](https://github.com/ion-fusion/fusion-java/issues)** - Bug reports and feature requests
- **[GitHub Discussions](https://github.com/orgs/ion-fusion/discussions)** - Questions and community help
- **[Slack Workspace](https://join.slack.com/t/ion-fusion/shared_invite/zt-2y0jr8vh2-bZLa66hdyZ3ykHcgOcYkcA)** - Real-time chat support

### Reporting Issues

When reporting issues, please include:

1. **Fusion version:**
   ```bash
   fusion version
   ```

2. **Java version:**
   ```bash
   java -version
   ```

3. **Operating system:**
   ```bash
   uname -a  # Linux/macOS
   ver       # Windows
   ```

4. **Complete error message**

5. **Minimal reproduction case**

6. **Steps to reproduce**

### Documentation

- **[CLI Reference](cli_reference.html)** - Complete command reference
- **[CLI Tutorial](tutorial_cli.html)** - Step-by-step learning guide
- **[Language Reference](fusion.html)** - Fusion language documentation
- **[Java API](javadoc/index.html)** - For embedding applications

---

*If your issue isn't covered here, please don't hesitate to reach out to the community for help!*

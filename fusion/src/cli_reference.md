<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Ion Fusion CLI Reference

This is the complete reference for the `fusion` command-line interface. The CLI provides powerful tools for evaluating Fusion code, managing modules, and generating reports.

## Table of Contents

- [Overview](#overview)
- [Global Options](#global-options)
- [Commands](#commands)
  - [eval](#eval) - Evaluate inline expressions
  - [load](#load) - Load and execute scripts
  - [repl](#repl) - Interactive console
  - [require](#require) - Import module bindings
  - [report_coverage](#report_coverage) - Generate coverage reports
  - [help](#help) - Get command help
  - [version](#version) - Show version information
- [Command Chaining](#command-chaining)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

## Overview

The `fusion` CLI follows a command/subcommand pattern where you specify one or more commands to execute in sequence. All commands share the same namespace, allowing you to build complex workflows.

**Basic Syntax:**
```bash
fusion [GLOBAL_OPTIONS] <command> [ARGS] [; <command> [ARGS]] ...
```

**Key Features:**
- Multiple commands can be chained with `;`
- Commands share a common namespace
- Supports both interactive and batch processing
- Built-in help system
- Comprehensive error reporting

## Global Options

Global options affect the entire CLI execution and must appear before any commands.

### `--repositories DIR[:DIR...]`

Specifies directories containing Fusion modules and resources. This option can be used multiple times, and all directories will be searched for modules.

**Examples:**
```bash
fusion --repositories /path/to/modules eval '(require "/mymodule")'
fusion --repositories /usr/local/fusion:/home/user/fusion-libs load script.fusion
```

### `--catalogs CATALOG[:CATALOG...]`

Specifies sources of Ion shared symbol tables for efficient data processing. Catalogs can be files containing serialized symbol tables or directories that will be traversed recursively.

**Examples:**
```bash
fusion --catalogs catalog.ion eval '(read)'
fusion --catalogs /usr/share/ion/catalogs:/home/user/catalogs load data-processor.fusion
```

### `--bootstrapRepository DIR` *(Deprecated)*

**⚠️ DEPRECATED:** This option is deprecated. If used, the directory is treated as the first user repository (equivalent to `--repositories`).

## Commands

### eval

Evaluates Fusion expressions provided as command-line arguments.

**Syntax:** `eval EXPRESSIONS`

**Description:**
Evaluates the given expressions as top-level Fusion forms. If the result of the last expression is not void, it's written to standard output using Ion format.

**Examples:**
```bash
# Simple arithmetic
fusion eval '(+ 1 2 3)'
# Output: 6

# Data construction
fusion eval '{ name: "Alice", age: 30, active: true }'
# Output: {name:"Alice",age:30,active:true}

# Multiple expressions
fusion eval '(define x 10) (define y 20) (+ x y)'
# Output: 30

# Working with lists
fusion eval '[1, 2, 3, (* 2 2), (+ 3 2)]'
# Output: [1,2,3,4,5]
```

**Shell Quoting:**
Be careful with shell quoting and escaping:
```bash
# Good - single quotes protect from shell interpretation
fusion eval '(define message "Hello World") message'

# Good - escaped double quotes
fusion eval "(define message \"Hello World\") message"

# Bad - shell will interpret variables and quotes
fusion eval (define message "Hello $USER") message
```

**Error Handling:**
- Empty expressions will show usage information
- Syntax errors display the problematic location
- Runtime errors show stack traces with context

### load

Loads and evaluates Fusion scripts from files.

**Syntax:** `load FILE`

**Description:**
Reads and evaluates a Fusion script from the specified file. The file must be readable and contain valid Fusion code. If the last expression returns a non-void value, it's written to standard output.

**Examples:**
```bash
# Load a simple script
fusion load hello.fusion

# Load with input redirection
echo "42" | fusion load process-number.fusion

# Load multiple scripts in sequence
fusion load setup.fusion ';' load main.fusion
```

**File Requirements:**
- File must exist and be readable
- File should contain valid Fusion syntax
- File path can be relative or absolute

**Sample Script (`hello.fusion`):**
```fusion
// Simple greeting script
(define (greet name)
  (string_append "Hello, " name "!"))

(greet "World")
```

**Error Handling:**
- File not found or not readable will show clear error message
- Syntax errors in the file will show line numbers and context
- Runtime errors preserve stack trace information

### repl

Starts an interactive Read-Eval-Print Loop for exploring Fusion interactively.

**Syntax:** `repl`

**Description:**
Enters an interactive console where you can type Fusion expressions and see results immediately. The REPL maintains state between expressions, allowing you to define variables and functions that persist throughout the session.

**Features:**
- Colored output (blue for prompts, red for errors)
- Command history (when using a proper terminal)
- Built-in help system
- Graceful error handling

**REPL Commands:**
```fusion
(exit)                    ; Exit the REPL
(help)                    ; Show general help
(help TOPIC)              ; Show help for specific topic
```

**Examples:**
```bash
$ fusion repl

Welcome to Fusion!

Type...
  (exit)            to exit
  (help SOMETHING)  to see documentation; try '(help help)'!

$ (define x 42)
$ (+ x 8)
50
$ (define (factorial n) (if (<= n 1) 1 (* n (factorial (- n 1)))))
$ (factorial 5)
120
$ (exit)
Goodbye!
```

**Limitations:**
- Cannot be used when stdin/stdout are redirected
- Multi-line expressions must be entered on a single line
- No line editing features (depends on terminal capabilities)

### require

Imports bindings from Fusion modules into the current namespace.

**Syntax:** `require MODULE_ID`

**Description:**
Imports all public bindings from the specified module, making them available in the current namespace. This is equivalent to evaluating `(require MODULE_ID)` but more convenient for command-line use.

**Examples:**
```bash
# Import string utilities
fusion require '/fusion/string' ';' eval '(string_length "hello")'
# Output: 5

# Import and use struct operations
fusion require '/fusion/struct' ';' eval '(define s (mutable_struct "a" 1)) (put_m s "b" 2) s'
# Output: {a:1,b:2}

# Chain multiple requires
fusion require '/fusion/list' ';' require '/fusion/string' ';' eval '(map string_length ["a" "bb" "ccc"])'
# Output: [1,2,3]
```

**Module Resolution:**
- Module IDs are absolute paths starting with `/`
- Standard library modules are under `/fusion/`
- User modules depend on repository configuration

**Common Modules:**
- `/fusion/string` - String manipulation functions
- `/fusion/list` - List processing utilities
- `/fusion/struct` - Structure operations
- `/fusion/io` - Input/output operations
- `/fusion/number` - Numeric functions

### report_coverage

Generates HTML coverage reports from Fusion code coverage data.

**Syntax:** `report_coverage COVERAGE_DATA_DIR REPORT_DIR`

**Description:**
Reads code coverage data collected during test runs and generates a comprehensive HTML report showing which lines of code were executed.

**Arguments:**
- `COVERAGE_DATA_DIR` - Directory containing coverage data files
- `REPORT_DIR` - Directory where HTML report will be written (created if it doesn't exist)

**Examples:**
```bash
# Generate coverage report
fusion report_coverage build/coverage-data build/reports/coverage

# Using with build system
./gradlew test
fusion report_coverage build/fcov build/reports/fcoverage
```

**Coverage Data:**
Coverage data is automatically collected when:
- Running tests with coverage enabled
- Using `FusionRuntimeBuilder.setCoverageDataDirectory()`
- Configured via `config.properties` in the data directory

**Report Contents:**
- Overall coverage statistics
- Per-file coverage details
- Line-by-line execution counts
- Uncovered code highlighting

### help

Displays help information for the CLI or specific commands.

**Syntax:** `help [COMMAND ...]`

**Description:**
Shows usage information and documentation. Without arguments, displays a list of all available commands. With command names, shows detailed help for those specific commands.

**Examples:**
```bash
# Show all commands
fusion help

# Show help for specific command
fusion help eval

# Show help for multiple commands
fusion help eval load repl
```

**Help Output:**
- Command syntax and usage
- Description of functionality
- Available options and arguments
- Examples and common patterns
- Related commands and concepts

### version

Displays version and build information.

**Syntax:** `version`

**Description:**
Outputs detailed version information in Ion format, including Fusion version, build details, and dependency versions.

**Example:**
```bash
fusion version
```

**Sample Output:**
```ion
{
  fusion_version: {
    release_label: "0.38a1-SNAPSHOT"
  },
  ion_version: {
    project_version: "1.10.5",
    build_time: 2024-01-15T10:30:00Z
  }
}
```

## Command Chaining

Multiple commands can be executed in sequence by separating them with semicolons (`;`). All commands share the same namespace, so definitions and imports from earlier commands are available to later ones.

**Syntax:**
```bash
fusion command1 args ';' command2 args ';' command3 args
```

**Shell Escaping:**
In most shells, semicolons need to be escaped or quoted:
```bash
# Escaped semicolon
fusion require /fusion/string \; eval '(string_length "test")'

# Quoted semicolon
fusion require /fusion/string ';' eval '(string_length "test")'
```

**Examples:**
```bash
# Setup and execution
fusion require '/fusion/list' ';' eval '(define nums [1 2 3 4 5])' ';' eval '(fold + 0 nums)'

# Load configuration then run script
fusion load config.fusion ';' load main.fusion

# Interactive session with preloaded modules
fusion require '/fusion/string' ';' require '/fusion/list' ';' repl
```

**Namespace Sharing:**
- Variables defined in one command are available in subsequent commands
- Module imports persist across commands
- Error in one command stops execution of remaining commands

## Examples

### Data Processing Pipeline

Process JSON data through multiple transformation steps:

```bash
# Process JSON data with transformations
echo '{"users": [{"name": "Alice", "age": 25}, {"name": "Bob", "age": 30}]}' | \
fusion eval '(define data (read))' ';' \
       eval '(define users (. data "users"))' ';' \
       eval '(map (lambda (user) (. user "name")) users)'
```

### Module Development Workflow

Develop and test a custom module:

```bash
# Load module and test interactively
fusion --repositories ./my-modules require '/myproject/utils' ';' repl

# Run tests with coverage
fusion --repositories ./my-modules load test-suite.fusion ';' \
       report_coverage ./coverage-data ./coverage-report
```

### Batch Processing

Process multiple files with the same script:

```bash
# Process each file in a directory
for file in data/*.ion; do
  echo "Processing $file"
  fusion load process-data.fusion < "$file" > "processed/$(basename "$file")"
done
```

### Configuration and Execution

Load configuration and run application:

```bash
# Production deployment
fusion --repositories /opt/fusion-modules \
       --catalogs /opt/ion-catalogs \
       load config/production.fusion ';' \
       load app/main.fusion
```

## Troubleshooting

### Common Issues

**Command Not Found**
```
fusion: command not found
```
- Ensure `fusion` is in your PATH
- Check installation was successful
- Try using full path to binary

**Module Not Found**
```
Bad syntax: unbound identifier. The symbol 'my_function' has no binding
```
- Check if module was imported with `require`
- Verify module path is correct
- Ensure module repository is configured with `--repositories`

**File Not Readable**
```
Script is not a readable file: script.fusion
```
- Check file exists and has correct permissions
- Verify file path is correct (relative vs absolute)
- Ensure file contains valid Fusion syntax

**Syntax Errors**
```
Bad syntax: unexpected token
```
- Check parentheses are balanced
- Verify string quotes are properly closed
- Review Ion syntax requirements

**Memory Issues**
```
OutOfMemoryError
```
- Increase JVM heap size: `JAVA_OPTS="-Xmx2g" fusion ...`
- Process data in smaller chunks
- Check for infinite loops or recursion

### Performance Tips

**Large Data Processing:**
- Use streaming operations when possible
- Process data in chunks rather than loading everything into memory
- Use efficient operations like struct lookups with `elt` over nested access with `.`

**Module Loading:**
- Organize modules efficiently to minimize loading time
- Use specific imports rather than importing entire modules
- Cache frequently used modules in repositories

**REPL Usage:**
- Define frequently used functions in startup scripts
- Use `load` to bring in utility functions
- Keep REPL sessions focused to avoid memory buildup

### Getting Help

**Built-in Help:**
```bash
fusion help                    # List all commands
fusion help <command>          # Detailed command help
```

**Community Resources:**
- [GitHub Issues](https://github.com/ion-fusion/fusion-java/issues)
- [Discussion Forums](https://github.com/orgs/ion-fusion/discussions)
- [Slack Workspace](https://join.slack.com/t/ion-fusion/shared_invite/zt-2y0jr8vh2-bZLa66hdyZ3ykHcgOcYkcA)

**Documentation:**
- [Language Reference](fusion.html)
- [Java API Documentation](javadoc/index.html)
- [Tutorial](tutorial_cli.html)

---

*For more information about the Ion Fusion language itself, see the [Language Reference](fusion.html).*

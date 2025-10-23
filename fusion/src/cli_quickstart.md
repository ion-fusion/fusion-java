<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Ion Fusion CLI Quick Start

Get up and running with the Ion Fusion CLI in minutes. This guide covers installation, basic usage, and your first Fusion programs.

## Installation

### Option 1: Build from Source (Recommended)

**Prerequisites:**
- Java 8 or later ([Corretto](https://aws.amazon.com/corretto/) recommended)
- Git

**Steps:**
```bash
# Clone the repository
git clone https://github.com/ion-fusion/fusion-java.git
cd fusion-java

# Build the CLI
./gradlew release

# Add to PATH
export PATH=$PATH:$PWD/build/install/fusion/bin

# Verify installation
fusion version
```

### Option 2: Download Pre-built Binary *(Coming Soon)*

Pre-built binaries will be available from the [releases page](https://github.com/ion-fusion/fusion-java/releases) once version 1.0 is released.

## First Steps

### 1. Verify Installation

```bash
fusion help
```

You should see a list of available commands.

### 2. Your First Expression

```bash
fusion eval '(+ 1 2 3)'
```
**Output:** `6`

### 3. Working with Data

```bash
# Create a simple data structure
fusion eval '{ name: "Alice", age: 30, hobbies: ["reading", "coding"] }'
```

**Output:** `{name:"Alice",age:30,hobbies:["reading","coding"]}`

### 4. Interactive Mode

```bash
fusion repl
```

Try these expressions in the REPL:
```fusion
$ (define greeting "Hello, Fusion!")
$ greeting
"Hello, Fusion!"
$ (define (square x) (* x x))
$ (square 5)
25
$ (exit)
```

## Essential Commands

### `eval` - Quick Expressions
Perfect for one-liners and testing:
```bash
fusion eval '(* 7 6)'                    # Math: 42
fusion eval '(string_length "hello")'    # Needs string module
```

### `load` - Run Scripts
Create a file `hello.fusion`:
```fusion
(define (greet name)
  (string_append "Hello, " name "!"))

(greet "World")
```

Run it:
```bash
fusion load hello.fusion
```

### `require` - Import Modules
```bash
# Import string functions, then use them
fusion require '/fusion/string' ';' eval '(string_length "test")'
```

### `repl` - Interactive Development
```bash
fusion repl
```

## Common Patterns

### Data Processing
```bash
# Process Ion data
echo '{"count": 5}' | fusion eval '(define data (read)) (+ (. data "count") 10)'
```

### Module Usage
```bash
# Use list processing functions
fusion require '/fusion/list' ';' eval '(map (lambda (x) (* x 2)) [1 2 3 4])'
```

### Script Development
Create `process.fusion`:
```fusion
// Read input, process it, write output
(define input (read))
(define result (* input 2))
(writeln "Double of " input " is " result)
result
```

Use it:
```bash
echo "21" | fusion load process.fusion
```

## Key Concepts

### Ion Data Format
Fusion uses [Amazon Ion](https://ion-fusion.dev/) as its data format:
```fusion
null                    ; Null value
true false             ; Booleans  
42 3.14                ; Numbers
"hello"                ; Strings
[1, 2, 3]              ; Lists
{name: "Alice"}        ; Structures (like JSON objects)
(+ 1 2)                ; S-expressions (function calls)
```

### Everything is Data
In Fusion, code is data and data is code:
```bash
fusion eval '[1, (+ 2 3), "hello"]'    # Mix data and computation
```

### Functional Programming
Functions are first-class values:
```bash
fusion eval '(define add (lambda (x y) (+ x y))) (add 10 20)'
```

## Next Steps

### Learn More Commands
```bash
fusion help eval       # Detailed help for eval command
fusion help load       # Detailed help for load command
```

### Explore Standard Library
```bash
# See what's available
fusion require '/fusion' ';' repl

# In REPL, try:
$ (help)               # General help
$ (help +)             # Help for specific function
```

### Read the Documentation
- [Complete CLI Reference](cli_reference.html) - All commands and options
- [CLI Tutorial](tutorial_cli.html) - Step-by-step learning
- [Language Reference](fusion.html) - Complete language documentation

### Try Real Examples
```bash
# Data transformation
fusion eval '(map (lambda (x) {value: x, squared: (* x x)}) [1 2 3 4 5])'

# String processing  
fusion require '/fusion/string' ';' eval '(string_split "a,b,c,d" ",")'

# File processing (create test.ion with some data first)
echo '[1, 2, 3, 4, 5]' > test.ion
fusion eval '(define data (with_ion_from_file "test.ion" read)) (fold + 0 data)'
```

## Getting Help

### Built-in Help
```bash
fusion help                    # List all commands
fusion help <command>          # Detailed command help
```

### Community
Visit the [Ion Fusion Community](https://ion-fusion.dev/community/) page for support resources, discussions, and ways to get involved.

### Documentation
- [CLI Reference](cli_reference.html) - Complete command reference
- [Language Tutorial](tutorial_cli.html) - Learn the language
- [Java API](javadoc/index.html) - For embedding in applications

---

**Ready to dive deeper?** Check out the [complete CLI tutorial](tutorial_cli.html) or explore the [language reference](fusion.html).

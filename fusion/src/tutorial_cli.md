<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Tutorial: Exploring the Fusion CLI

This comprehensive tutorial will teach you how to use the Ion Fusion command-line interface effectively. We'll cover all major features with practical examples and real-world use cases.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Basic Expression Evaluation](#basic-expression-evaluation)
- [Working with Data](#working-with-data)
- [Script Execution](#script-execution)
- [Interactive Development](#interactive-development)
- [Module System](#module-system)
- [Command Chaining](#command-chaining)
- [Data Processing Workflows](#data-processing-workflows)
- [Advanced Features](#advanced-features)
- [What's Next](#whats-next)

## Prerequisites

**Installation Options:**

**Option 1: Build from Source (Recommended)**
```bash
git clone https://github.com/ion-fusion/fusion-java.git
cd fusion-java
./gradlew release
export PATH=$PATH:$PWD/build/install/fusion/bin
```

**Option 2: Pre-built SDK** *(Coming Soon)*
- Download the [Ion Fusion SDK][SDK] when available
- Unpack and add `bin` directory to your `PATH`

**Requirements:**
- Java runtime, version 8 or later ([Amazon Corretto][] recommended)
- `java` must be on your shell's `PATH`

**Verify Installation:**
```bash
fusion version
fusion help
```

## Basic Expression Evaluation

The `fusion` CLI has multiple modes of operation. Let's start with direct evaluation of expressions using the `eval` command:

```bash
fusion eval '(+ 1 2)'
```
**Output:** `3`

The result is printed in Amazon Ion format. Let's try more interesting expressions:

```bash
# Working with timestamps
fusion eval '[null, (timestamp_at_day 2025-03-28T02:09-07:00)]'
```
**Output:** `[null, 2025-03-28]`

```bash
# Arithmetic in data structures
fusion eval '{ name: "John Doe", date: 2001-03-27, score: (* 7 3) }'
```
**Output:** `{name:"John Doe", date:2001-03-27, score:21}`

```bash
# String operations
fusion eval '(string_append "Hello, " "World!")'
```
**Output:** `"Hello, World!"`

> **Key Concept:**
> Ion Fusion uses Amazon Ion as its concrete syntax, leveraging Ion's [symbol][] and [S-expression][sexp] types in a Lisp-like style. Fusion source code _is_ Ion data. When a data element isn't an S-expression or symbol, it evaluates to itself!

**More Examples:**
```bash
# Boolean logic
fusion eval '(and true false)'                    # false
fusion eval '(or true false)'                     # true

# List operations
fusion eval '(head [1, 2, 3, 4])'                # 1
fusion eval '(tail [1, 2, 3, 4])'                # [2,3,4]

# Conditional expressions
fusion eval '(if (> 5 3) "yes" "no")'            # "yes"
```

## Working with Data

Fusion excels at data manipulation. Let's explore various data types and operations:

### Numbers and Math
```bash
# Basic arithmetic
fusion eval '(+ 10 20 30)'                       # 60
fusion eval '(* 2 3 4)'                          # 24
fusion eval '(/ 100 4)'                          # 25

# Comparison
fusion eval '(< 5 10)'                           # true
fusion eval '(== 42 42)'                        # true
```

### Strings
```bash
# String length (requires string module)
fusion require '/fusion/string' ';' eval '(string_length "hello")'     # 5

# String concatenation
fusion eval '(string_append "Hello" ", " "World!")'                    # "Hello, World!"
```

### Lists and Collections
```bash
# List creation and access
fusion eval '[1, 2, 3, (+ 2 2), 5]'             # [1,2,3,4,5]
fusion eval '(head [10, 20, 30])'               # 10
fusion eval '(tail [10, 20, 30])'               # [20,30]

# List processing (requires list module)
fusion require '/fusion/list' ';' eval '(map (lambda (x) (* x 2)) [1, 2, 3])'  # [2,4,6]
```

### Structures (Objects)
```bash
# Structure creation
fusion eval '{ name: "Alice", age: 30, city: "Seattle" }'

# Field access
fusion eval '(define person { name: "Bob", age: 25 }) (. person "name")'        # "Bob"

# Nested structures
fusion eval '{ user: { name: "Charlie", profile: { active: true } } }'
```

## Script Execution

The `load` command lets you execute Fusion scripts from files. This is perfect for larger programs and reusable code.

### Your First Script

Create a file called `hello.fusion`:
```fusion
// Simple greeting function
(define (greet name)
  (string_append "Hello, " name "!"))

// Call the function
(greet "World")
```

Run it:
```bash
fusion load hello.fusion
```
**Output:** `"Hello, World!"`

### Processing Input

Scripts can read from standard input. Create `double.fusion`:
```fusion
// Read a number from input and double it
(define input (read))
(define result (* input 2))
(writeln "Input: " input ", Double: " result)
result
```

Use it:
```bash
echo "21" | fusion load double.fusion
```
**Output:** 
```
Input: 21, Double: 42
42
```

### Real-World Example: Git Author Analysis

Let's create a practical script that processes Git log data. Create `authors.fusion`:

```fusion
(define all_names
  '''
A sexp (linked list) containing all the values on the
current input stream.
  '''
  (series_to_sexp (in_port)))

(define (deduplicate s)
  '''
Remove duplicate values from sexp `s`, keeping the _last_
copy of any duplicates.
  '''
  (if (is_empty s) s
    // Decompose the sexp into its head/tail (aka car/cdr).
    (let [(name   (head s)),
          (others (tail s))]
      (if (any (|n| (== n name)) others)
        // The name is in the tail, so ignore this copy.
        (deduplicate others)
        // The name is not in the tail, so keep it and
        // dedup the tail.
        (pair name (deduplicate others))))))

// Print the deduplicated names in chrono order, one per line.
(for [(name (reverse (deduplicate all_names)))]
  (displayln name))
```

Run the script over Git log output:
```bash
git log --pretty=format:'"%an"' | fusion load authors.fusion
```

You'll see deduplicated author names in chronological order of their first commit. This demonstrates Fusion's power for ad-hoc data processing.

## Interactive Development

The `repl` command starts an interactive Read-Eval-Print Loop, perfect for experimentation and learning.

### Starting the REPL
```bash
fusion repl
```

You'll see:
```
Welcome to Fusion!

Type...
  (exit)            to exit
  (help SOMETHING)  to see documentation; try '(help help)'!

$ 
```

### REPL Features

**Define and use variables:**
```fusion
$ (define x 42)
$ (define y 8)
$ (+ x y)
50
```

**Create functions:**
```fusion
$ (define (factorial n)
    (if (<= n 1) 
        1 
        (* n (factorial (- n 1)))))
$ (factorial 5)
120
```

**Get help:**
```fusion
$ (help +)          ; Help for addition function
$ (help help)       ; Help for the help system
```

**Work with data:**
```fusion
$ (define data { users: [{ name: "Alice", age: 30 }, { name: "Bob", age: 25 }] })
$ (. data "users")
[{name:"Alice",age:30},{name:"Bob",age:25}]
```

**Exit the REPL:**
```fusion
$ (exit)
Goodbye!
```

## Module System

Fusion has a rich module system. Use `require` to import functionality:

### Standard Library Modules

**String operations:**
```bash
fusion require '/fusion/string' ';' repl
```
```fusion
$ (string_length "hello world")
11
$ (string_split "a,b,c" ",")
["a","b","c"]
$ (string_upper "hello")
"HELLO"
```

**List processing:**
```bash
fusion require '/fusion/list' ';' repl
```
```fusion
$ (map (lambda (x) (* x x)) [1, 2, 3, 4])
[1,4,9,16]
$ (filter (lambda (x) (> x 5)) [1, 3, 7, 2, 9, 4])
[7,9]
$ (fold + 0 [1, 2, 3, 4, 5])
15
```

**I/O operations:**
```bash
fusion require '/fusion/io' ';' repl
```
```fusion
$ (writeln "Hello, " "World!")
Hello, World!
$ (display "No newline")
No newline
```

### Module Discovery

Explore available modules:
```bash
fusion require '/fusion' ';' repl
```
```fusion
$ (help)           ; General help
$ (help map)       ; Help for map function
$ (help string_length)  ; Help for string functions
```

## Command Chaining

Chain multiple commands with `;` to build complex workflows:

### Basic Chaining
```bash
# Import module, then use it
fusion require '/fusion/string' ';' eval '(string_length "test")'
```

### Multi-step Processing
```bash
# Define data, process it, show result
fusion eval '(define nums [1, 2, 3, 4, 5])' ';' \
       require '/fusion/list' ';' \
       eval '(map (lambda (x) (* x x)) nums)'
```

### Setup and Interactive Session
```bash
# Load modules and start REPL
fusion require '/fusion/string' ';' \
       require '/fusion/list' ';' \
       eval '(define greeting "Welcome to Fusion!")' ';' \
       repl
```

## Data Processing Workflows

### JSON-like Data Processing
```bash
# Process structured data
echo '{"users": [{"name": "Alice", "score": 85}, {"name": "Bob", "score": 92}]}' | \
fusion eval '(define data (read))' ';' \
       eval '(define users (. data "users"))' ';' \
       require '/fusion/list' ';' \
       eval '(map (lambda (user) (. user "score")) users)'
```

### File Processing
```bash
# Create sample data
echo '[1, 2, 3, 4, 5]' > numbers.ion

# Process the file
fusion eval '(define data (with_ion_from_file "numbers.ion" read))' ';' \
       require '/fusion/list' ';' \
       eval '(fold + 0 data)'
```

### Streaming Data
```bash
# Generate data and process it
seq 1 10 | fusion eval '(for [(n (in_port))] (writeln "Number: " n " Square: " (* n n)))'
```

## Advanced Features

### Coverage Reports
```bash
# Run tests with coverage
fusion --repositories ./modules load test-suite.fusion

# Generate coverage report
fusion report_coverage ./coverage-data ./coverage-report
```

### Custom Repositories
```bash
# Use custom module repositories
fusion --repositories /path/to/my/modules:/path/to/shared/modules \
       require '/myproject/utils' ';' \
       load main.fusion
```

### Ion Catalogs
```bash
# Use shared symbol tables for efficient data processing
fusion --catalogs ./catalogs:/usr/share/ion-catalogs \
       load data-processor.fusion < large-dataset.ion
```


## What's Next?

Congratulations! You've learned the fundamentals of the Fusion CLI. Here are your next steps:

### Explore the Documentation
- **[CLI Reference](cli_reference.html)** - Complete command reference with all options
- **[Language Reference](fusion.html)** - Comprehensive guide to Fusion language features
- **[CLI Quick Start](cli_quickstart.html)** - Quick reference for common tasks

### Practice with Examples
- **Data Processing:** Try processing CSV, JSON, or Ion data files
- **Scripting:** Create utility scripts for common tasks
- **Interactive Development:** Use the REPL to explore the standard library

### Advanced Topics
- **Module Development:** Create your own Fusion modules
- **Java Integration:** Embed Fusion in Java applications
- **Performance Optimization:** Learn about efficient data processing patterns

### Get Help
- **Built-in Help:** Use `fusion help` and the REPL's `(help)` system
- **Community:** Join our [Slack workspace](https://join.slack.com/t/ion-fusion/shared_invite/zt-2y0jr8vh2-bZLa66hdyZ3ykHcgOcYkcA)
- **Issues:** Report bugs or request features on [GitHub](https://github.com/ion-fusion/fusion-java/issues)

### Real-World Applications
Consider using Fusion for:
- **Data ETL pipelines**
- **Configuration processing**
- **Log analysis and reporting**
- **API data transformation**
- **Build and deployment scripts**

Happy coding with Ion Fusion!


[Amazon Corretto]: https://aws.amazon.com/corretto
[CONTRIBUTORS]: https://github.com/ion-fusion/fusion-java/blob/main/CONTRIBUTORS.md
[SDK]:          https://github.com/ion-fusion/fusion-java/releases
[sexp]:   https://amazon-ion.github.io/ion-docs/docs/spec.html#sexp
[symbol]: https://amazon-ion.github.io/ion-docs/docs/spec.html#symbol

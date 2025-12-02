
# Compilers Construction
A compiler for an imperative language that translates source code to WebAssembly (WASM).

## Overview
This project implements a complete compiler pipeline from a custom imperative language to WebAssembly. The compiler performs lexical analysis, parsing, semantic analysis, optimization, and finally generates WebAssembly binary code that can be executed in modern web browsers or standalone WASM runtimes.

## Technology Stack
- **Source Language:** Custom imperative language with support for variables, functions, arrays, records, conditionals and loops  
- **Implementation Language:** Java  
- **Parsing Method:** Hand-written parser  
- **Target Platform:** WebAssembly (WASM)    

## Project Structure
```

.
├── analyzer/          # Semantic analysis and optimization
├── ast/               # Abstract Syntax Tree node definitions
├── backup/            # AnalyzerTester and LexerTester
├── parser/            # Lexer and parser implementation
├── runtime/           # WebAssembly runtime and runner
├── tests/             # Test programs 
├── wasm/              # WebAssembly code generator
└── build.sh           # Build script for the entire project

````

## Building the Compiler
To build the entire project, run:

```bash
./build.sh
````

This script compiles all Java source files and places them in the `out/` directory. It also places all generated `.wat` and `.wasm` files from `/tests` in the `output/` directory

## Running Without codegen

### Lexer

To run Lexer that performs tokenization for 1 test:

```bash
javac -d out backup/LexerTester.java
java -cp out AnalyzerTester
java -cp out LexerTester tests/test1.rout
```

### Analyzer

To run Semantic Analyzer with optimizations for 1 test:

```bash
javac -d out backup/AnalyzerTester.java
java -cp out AnalyzerTester
java -cp out AnalyzerTester tests/test_optimization_simple.rout
```

### Lexer + Analyzer + Parser

To run the complete parser with AST output:

```bash
javac -d out parser/*.java ast/*.java
java -cp out parser.Main tests/test1.rout
```


For `test1.rout` this performs:

* Lexical analysis
* Parsing
* Semantic checking (type checking, scope analysis, etc.)
* AST optimizations (constant folding, dead code elimination, etc.)


Or to run all tests at once:

```bash
java -cp out parser.Main
```

### Golden Tests

```bash
javac -encoding UTF-8 -d out backup/GoldenTester.java
java -cp out GoldenTester --update tests/

```

## Complete Compilation to WebAssembly

To compile a source file to WebAssembly:

```bash
# First, compile the entire project
javac -d out analyzer/*.java ast/*.java parser/*.java wasm/*.java

# Then compile a specific program
java -cp out wasm.WasmCompiler tests/test1.rout output/
```

This generates:

* `output/test1.wat` — WebAssembly Text format
* `output/test1.wasm` — Binary WebAssembly module


## Running Generated WebAssembly

### Using the HTML Runtime

Open `runtime/runtime.html` in a web browser, then:

1. Click **"Choose File"** and select your `.wasm` file
2. Click **"Run WASM"**
3. The program output appears in the output area

## Development Team

* Anastasia Kuchumova
* Ivan Nahorny
* Olesia Novoselova


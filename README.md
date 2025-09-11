# GOIDA

Compilator on Java for imperative programming language. Lexer + golden tests setup.

## Build

```bash
cd lexer
javac -encoding UTF-8 Token.java Lexer.java LexerTester.java Main.java
```

## Run the lexer

* **Single file**

  ```bash
  java LexerTester ../tests/test1.rout
  ```
* **Directory (runs all `*.rout` in natural numeric order)**

  ```bash
  java LexerTester ../tests
  ```

## Golden tests (snapshot testing)

Golden files are stored **next to inputs** with the same name + `.gold`.

**Generate (or refresh) goldens for the whole directory:**

  ```bash
javac -encoding UTF-8 Token.java Lexer.java GoldenTester.java
java GoldenTester --update ../tests
# goldens saved to ../tests/golden/*.gold
```


**Run checks against saved goldens:**

  ```bash
java GoldenTester ../tests
```


**Single file (create/update its golden right next to it):**

```bash
java GoldenTester -u ../tests/test1.rout
java GoldenTester ../tests/test1.rout
```
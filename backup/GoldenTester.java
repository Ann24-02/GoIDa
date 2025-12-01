import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import parser.Lexer;
import parser.Token;

// Golden testing system for lexical analysis
public class GoldenTester {
    private static final String FLAG_UPDATE = "--update";
    private static final String FLAG_UPDATE_SHORT = "-u";
    private static final String FLAG_GOLD_DIR = "--golden-dir";
    private static final String FLAG_EXT = "--ext";

    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    // Extract numbers from filenames for natural sorting
    private static int numericKey(Path p) {
        String name = p.getFileName().toString();
        Matcher m = DIGITS.matcher(name);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return Integer.MAX_VALUE;
    }

    // Normalize line endings for consistent comparison
    private static String normalizeEol(String s) {
        return s.replace("\r\n", "\n");
    }

    // Run lexer on source code and format tokens as text
    private static String renderTokens(String source) {
        StringBuilder out = new StringBuilder();
        Lexer lexer = new Lexer(source);
        Token t;
        do {
            t = lexer.nextToken();
            out.append(t.toString()).append('\n');
        } while (t.type != Token.Type.EOF);
        return out.toString();
    }

    // Write string to file
    private static void writeString(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // Test a single source file against its golden reference
    private static void processFile(Path inputFile, Path goldenFile, boolean update, ResultCounter rc) throws IOException {
        if (!Files.isRegularFile(inputFile)) {
            System.err.println("Not a file: " + inputFile.toAbsolutePath());
            rc.skipped++;
            return;
        }

        String src = Files.readString(inputFile, StandardCharsets.UTF_8);
        String actual = normalizeEol(renderTokens(src));

        // Update golden file if requested or missing
        if (update || !Files.exists(goldenFile)) {
            writeString(goldenFile, actual);
            System.out.println("[UPDATED] " + inputFile.getFileName() + " -> " + goldenFile);
            rc.updated++;
            return;
        }

        // Compare with expected output
        String expected = normalizeEol(Files.readString(goldenFile, StandardCharsets.UTF_8));

        if (expected.equals(actual)) {
            System.out.println("[OK] " + inputFile.getFileName());
            rc.passed++;
        } else {
            System.out.println("[FAIL] " + inputFile.getFileName());
            printDiff(expected, actual);
            System.out.println("Expected: " + goldenFile.toAbsolutePath());
            rc.failed++;
        }
    }

    // Test all .rout files in a directory
    private static void processDir(Path dir, Path goldenDir, String goldenExt, boolean update, ResultCounter rc) throws IOException {
        if (!Files.isDirectory(dir)) {
            System.err.println("Not a directory: " + dir.toAbsolutePath());
            rc.skipped++;
            return;
        }
        
        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".rout"))
                    .sorted(
                        Comparator
                            .comparingInt(GoldenTester::numericKey)
                            .thenComparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)
                    )
                    .toList();
        }

        for (Path f : files) {
            String base = stripExt(f.getFileName().toString());
            Path goldenFile = goldenDir.resolve(base + goldenExt);
            processFile(f, goldenFile, update, rc);
        }
    }

    // Remove file extension
    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(0, i) : name;
    }

    // Show difference between expected and actual output
    private static void printDiff(String expected, String actual) {
        String[] e = expected.split("\n", -1);
        String[] a = actual.split("\n", -1);

        int max = Math.max(e.length, a.length);
        int firstDiff = -1;
        
        // Find first differing line
        for (int i = 0; i < max; i++) {
            String el = (i < e.length) ? e[i] : "<EOF>";
            String al = (i < a.length) ? a[i] : "<EOF>";
            if (!Objects.equals(el, al)) {
                firstDiff = i;
                break;
            }
        }
        
        if (firstDiff == -1) {
            System.out.println("Outputs differ but no first-diff found (line-ending issue?).");
            return;
        }

        // Show context around the difference
        int start = Math.max(0, firstDiff - 2);
        int end = Math.min(max - 1, firstDiff + 2);
        System.out.println("---- Diff (context lines " + (start + 1) + "…" + (end + 1) + ") ----");
        
        for (int i = start; i <= end; i++) {
            String el = (i < e.length) ? e[i] : "<EOF>";
            String al = (i < a.length) ? a[i] : "<EOF>";
            String mark = (i == firstDiff) ? ">>" : "  ";
            System.out.printf("%s exp[%d]: %s%n", mark, i + 1, el);
            System.out.printf("%s act[%d]: %s%n", mark, i + 1, al);

            if (i == firstDiff) {
                int caretPos = firstCharDiff(el, al);
                if (caretPos >= 0) {
                    System.out.print("   ");
                    System.out.println(" ".repeat(Math.max(0, ("act[" + (i + 1) + "]: ").length())) + " ".repeat(caretPos) + "^");
                }
            }
        }
        System.out.println("------------------------------");
    }

    // Find position of first differing character
    private static int firstCharDiff(String s1, String s2) {
        int n = Math.min(s1.length(), s2.length());
        for (int i = 0; i < n; i++) {
            if (s1.charAt(i) != s2.charAt(i)) return i;
        }
        return (s1.length() == s2.length()) ? -1 : n;
    }

    // Parse command line arguments
    private record ParsedArgs(boolean update, String goldenDirOpt, String goldenExt, List<String> paths) {}
    private static ParsedArgs parseArgs(String[] args) {
        boolean update = false;
        String goldenDir = null;
        String ext = ".gold";
        List<String> paths = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case FLAG_UPDATE, FLAG_UPDATE_SHORT -> update = true;
                case FLAG_GOLD_DIR -> {
                    if (i + 1 >= args.length) usage("Missing value for --golden-dir");
                    goldenDir = args[++i];
                }
                case FLAG_EXT -> {
                    if (i + 1 >= args.length) usage("Missing value for --ext");
                    ext = args[++i];
                    if (!ext.startsWith(".")) ext = "." + ext;
                }
                default -> paths.add(args[i]);
            }
        }
        
        if (paths.isEmpty()) usage(null);
        return new ParsedArgs(update, goldenDir, ext, paths);
    }

    // Show usage information
    private static void usage(String error) {
        if (error != null) System.err.println("Error: " + error);
        System.err.println("""
                Usage:
                  java GoldenTester [--update|-u] [--golden-dir <dir>] [--ext <.gold>] <file_or_dir>

                Notes:
                  - For directories: golden files go in <dir>/golden by default
                  - Use --golden-dir to override golden file location
                  - Use --update to create/update golden files
                """);
        System.exit(2);
    }

    // Track test results
    private static class ResultCounter {
        int passed = 0, failed = 0, updated = 0, skipped = 0;
    }

    public static void main(String[] args) throws IOException {
        ParsedArgs pa = parseArgs(args);
        ResultCounter rc = new ResultCounter();

        // Process each input path
        for (String raw : pa.paths()) {
            Path p = Paths.get(raw);

            if (Files.isDirectory(p)) {
                Path goldenDir = (pa.goldenDirOpt() != null)
                        ? Paths.get(pa.goldenDirOpt())
                        : p.resolve("golden");
                processDir(p, goldenDir, pa.goldenExt(), pa.update(), rc);
            } else if (Files.isRegularFile(p)) {
                Path goldenFile;
                if (pa.goldenDirOpt() != null) {
                    String base = stripExt(p.getFileName().toString());
                    goldenFile = Paths.get(pa.goldenDirOpt()).resolve(base + pa.goldenExt());
                } else {
                    String base = stripExt(p.getFileName().toString());
                    goldenFile = p.getParent().resolve(base + pa.goldenExt());
                }
                processFile(p, goldenFile, pa.update(), rc);
            } else {
                System.err.println("Does not exist: " + p.toAbsolutePath());
                rc.skipped++;
            }
        }

        // Print summary
        System.out.printf("Summary: %d passed, %d failed, %d updated, %d skipped%n",
                rc.passed, rc.failed, rc.updated, rc.skipped);
        if (rc.failed > 0) System.exit(1);
    }
}
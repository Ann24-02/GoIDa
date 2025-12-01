import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import parser.Lexer;
import parser.Token;

// Simple lexer tester - shows tokens from source files
public class LexerTester {
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    // Extract numbers from filenames for sorting
    private static int numericKey(Path p) {
        String name = p.getFileName().toString();
        Matcher m = DIGITS.matcher(name);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return Integer.MAX_VALUE;
    }

    // Show tokens from a single file
    private static void processFile(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            System.err.println("Not a file: " + file.toAbsolutePath());
            return;
        }
        System.out.println("=== " + file.getFileName() + " ===");
        String source = Files.readString(file);
        Lexer lexer = new Lexer(source);

        Token tok;
        do {
            tok = lexer.nextToken();
            System.out.println(tok);
        } while (tok.type != Token.Type.EOF);

        System.out.println();
    }

    // Process all .rout files in a directory
    private static void processDir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            System.err.println("Not a directory: " + dir.toAbsolutePath());
            return;
        }
        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".rout"))
                    .sorted(
                        Comparator
                            .comparingInt(LexerTester::numericKey)
                            .thenComparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)
                    )
                    .toList();
        }
        for (Path f : files) {
            processFile(f);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java LexerTester <file_or_dir> [more_files_or_dirs...]");
            System.exit(1);
        }

        for (String a : args) {
            Path p = Paths.get(a);
            if (Files.isDirectory(p)) {
                processDir(p);
            } else if (Files.isRegularFile(p)) {
                processFile(p);
            } else {
                System.err.println("Does not exist: " + p.toAbsolutePath());
            }
        }
    }
}
package analyzer;

//  Semantic analysis exception.
//  Stores the line and column of the error.
public class SemanticException extends RuntimeException {
    private final int line;
    private final int column;

    public SemanticException(String message, int line, int column) {
        super(formatMessage(message, line, column));
        this.line = line;
        this.column = column;
    }

    public static String formatMessage(String message, int line, int column) {
        return String.format("[%d:%d] %s", line, column, message);
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
}

package parser;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Lexer {
    private final String input;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private boolean pendingSemicolon = false;
    private int parenDepth = 0;    // ( )
    private int bracketDepth = 0;  // [ ]
    private int braceDepth = 0;    // { }
    private Token.Type lastEmitted = null;

    private static final Map<String, Token.Type> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("routine", Token.Type.ROUTINE);
        KEYWORDS.put("is", Token.Type.IS);
        KEYWORDS.put("end", Token.Type.END);
        KEYWORDS.put("var", Token.Type.VAR);
        KEYWORDS.put("type", Token.Type.TYPE);
        KEYWORDS.put("integer", Token.Type.INTEGER);
        KEYWORDS.put("real", Token.Type.REAL);
        KEYWORDS.put("boolean", Token.Type.BOOLEAN);
        KEYWORDS.put("string", Token.Type.STRING);
        KEYWORDS.put("array", Token.Type.ARRAY);
        KEYWORDS.put("record", Token.Type.RECORD);
        KEYWORDS.put("if", Token.Type.IF);
        KEYWORDS.put("then", Token.Type.THEN);
        KEYWORDS.put("else", Token.Type.ELSE);
        KEYWORDS.put("while", Token.Type.WHILE);
        KEYWORDS.put("loop", Token.Type.LOOP);
        KEYWORDS.put("for", Token.Type.FOR);
        KEYWORDS.put("in", Token.Type.IN);
        KEYWORDS.put("reverse", Token.Type.REVERSE);
        KEYWORDS.put("print", Token.Type.PRINT);
        KEYWORDS.put("and", Token.Type.AND);
        KEYWORDS.put("or", Token.Type.OR);
        KEYWORDS.put("xor", Token.Type.XOR);
        KEYWORDS.put("not", Token.Type.NOT);
        KEYWORDS.put("return", Token.Type.RETURN);
        KEYWORDS.put("ref", Token.Type.REF);
    }

    public Lexer(String input) {
        this.input = input;
    }

    private char peek() {
        return (pos >= input.length()) ? '\0' : input.charAt(pos);
    }

    private char peekNext() {
        int p = pos + 1;
        return (p < input.length()) ? input.charAt(p) : '\0';
    }

    private char advance() {
        char c = peek();
        if (c == '\0') return c;
        pos++;
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (peek() != expected) return false;
        advance();
        return true;
    }

    private void skipSpacesAndComments() {
        while (true) {
            boolean consumed = false;

            while (peek() != '\n' && Character.isWhitespace(peek())) {
                advance();
                consumed = true;
            }

            if (peek() == '-' && peekNext() == '-') {
                advance(); advance(); // съели "--"
                while (peek() != '\n' && peek() != '\0') advance();
                consumed = true;
            }

            if (!consumed) break;
        }
    }

    private boolean lastTokenAllowsSemicolon() {
        if (lastEmitted == null) return false;
        switch (lastEmitted) {
            case IDENTIFIER:
            case INT_LITERAL:
            case REAL_LITERAL:
            case BOOL_LITERAL:
            case STRING_LITERAL:
            case RPAREN:
            case RBRACKET:
            case RBRACE:
            case END:
            case INTEGER:
            case REAL:
            case BOOLEAN:
            case STRING:
                return true;
            default:
                return false;
        }
    }

    public Token nextToken() {
        if (pendingSemicolon) {
            pendingSemicolon = false;
            lastEmitted = Token.Type.SEMICOLON;
            return new Token(Token.Type.SEMICOLON, ";", line, col);
        }

        boolean sawNewline = false;
        while (true) {
            if (peek() == '\n') {
                advance();
                sawNewline = true;
            } else {
                int before = pos;
                skipSpacesAndComments();
                if (pos == before) break;
            }
        }

        if (sawNewline && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0 && lastTokenAllowsSemicolon()) {
            lastEmitted = Token.Type.SEMICOLON;
            return new Token(Token.Type.SEMICOLON, ";", line, col);
        }

        int startCol = col;
        char c = advance();

        // EOF
        if (c == '\0') {
            lastEmitted = Token.Type.EOF;
            return new Token(Token.Type.EOF, "", line, startCol);
        }

        // Identifier / Keyword / Bool literal
        if (Character.isLetter(c) || c == '_') {
            StringBuilder sb = new StringBuilder();
            sb.append(c);
            while (Character.isLetterOrDigit(peek()) || peek() == '_') {
                sb.append(advance());
            }
            String lexeme = sb.toString();
            String low = lexeme.toLowerCase(Locale.ROOT);

            if (low.equals("true") || low.equals("false")) {
                lastEmitted = Token.Type.BOOL_LITERAL;
                return new Token(Token.Type.BOOL_LITERAL, lexeme, line, startCol);
            }

            Token.Type type = KEYWORDS.getOrDefault(low, Token.Type.IDENTIFIER);
            lastEmitted = type;
            return new Token(type, lexeme, line, startCol);
        }

        // Number: INT or REAL (d+ or d+ '.' d+). '1..10' -> INT, RANGE, INT
        if (Character.isDigit(c)) {
            StringBuilder sb = new StringBuilder();
            sb.append(c);
            boolean isReal = false;

            while (Character.isDigit(peek())) sb.append(advance());

            if (peek() == '.' && Character.isDigit(peekNext())) {
                isReal = true;
                sb.append(advance()); // '.'
                while (Character.isDigit(peek())) sb.append(advance());
            }

            Token.Type kind = isReal ? Token.Type.REAL_LITERAL : Token.Type.INT_LITERAL;
            lastEmitted = kind;
            return new Token(kind, sb.toString(), line, startCol);
        }

        // String literal
        if (c == '"') {
            StringBuilder sb = new StringBuilder();
            while (peek() != '"' && peek() != '\0') {
                sb.append(advance());
            }
            if (peek() == '"') advance();
            lastEmitted = Token.Type.STRING_LITERAL;
            return new Token(Token.Type.STRING_LITERAL, sb.toString(), line, startCol);
        }

        // Operators / delimiters
        switch (c) {
            case '+':
                lastEmitted = Token.Type.PLUS;
                return new Token(Token.Type.PLUS, "+", line, startCol);
            case '-':
                // Стрелку '->' не поддерживаем; лишь минус
                lastEmitted = Token.Type.MINUS;
                return new Token(Token.Type.MINUS, "-", line, startCol);
            case '*':
                lastEmitted = Token.Type.MULTIPLY;
                return new Token(Token.Type.MULTIPLY, "*", line, startCol);
            case '/':
                if (match('=')) { // '/=' — not equals
                    lastEmitted = Token.Type.NOT_EQUALS;
                    return new Token(Token.Type.NOT_EQUALS, "/=", line, startCol);
                }
                lastEmitted = Token.Type.DIVIDE;
                return new Token(Token.Type.DIVIDE, "/", line, startCol);
            case '%':
                lastEmitted = Token.Type.MODULO;
                return new Token(Token.Type.MODULO, "%", line, startCol);
            case '=':
                if (match('>')) { // '=>'
                    lastEmitted = Token.Type.FAT_ARROW;
                    return new Token(Token.Type.FAT_ARROW, "=>", line, startCol);
                }
                lastEmitted = Token.Type.EQUALS;
                return new Token(Token.Type.EQUALS, "=", line, startCol);
            case '<':
                if (match('=')) {
                    lastEmitted = Token.Type.LESS_EQUAL;
                    return new Token(Token.Type.LESS_EQUAL, "<=", line, startCol);
                }
                lastEmitted = Token.Type.LESS;
                return new Token(Token.Type.LESS, "<", line, startCol);
            case '>':
                if (match('=')) {
                    lastEmitted = Token.Type.GREATER_EQUAL;
                    return new Token(Token.Type.GREATER_EQUAL, ">=", line, startCol);
                }
                lastEmitted = Token.Type.GREATER;
                return new Token(Token.Type.GREATER, ">", line, startCol);
            case ':':
                if (match('=')) {
                    lastEmitted = Token.Type.ASSIGN;
                    return new Token(Token.Type.ASSIGN, ":=", line, startCol);
                }
                lastEmitted = Token.Type.COLON;
                return new Token(Token.Type.COLON, ":", line, startCol);
            case ';':
                lastEmitted = Token.Type.SEMICOLON;
                return new Token(Token.Type.SEMICOLON, ";", line, startCol);
            case ',':
                lastEmitted = Token.Type.COMMA;
                return new Token(Token.Type.COMMA, ",", line, startCol);
            case '.':
                if (match('.')) {
                    lastEmitted = Token.Type.RANGE;
                    return new Token(Token.Type.RANGE, "..", line, startCol);
                }
                lastEmitted = Token.Type.DOT;
                return new Token(Token.Type.DOT, ".", line, startCol);
            case '(':
                parenDepth++;
                lastEmitted = Token.Type.LPAREN;
                return new Token(Token.Type.LPAREN, "(", line, startCol);
            case ')':
                if (parenDepth > 0) parenDepth--;
                lastEmitted = Token.Type.RPAREN;
                return new Token(Token.Type.RPAREN, ")", line, startCol);
            case '[':
                bracketDepth++;
                lastEmitted = Token.Type.LBRACKET;
                return new Token(Token.Type.LBRACKET, "[", line, startCol);
            case ']':
                if (bracketDepth > 0) bracketDepth--;
                lastEmitted = Token.Type.RBRACKET;
                return new Token(Token.Type.RBRACKET, "]", line, startCol);
            case '{':
                braceDepth++;
                lastEmitted = Token.Type.LBRACE;
                return new Token(Token.Type.LBRACE, "{", line, startCol);
            case '}':
                if (braceDepth > 0) braceDepth--;
                lastEmitted = Token.Type.RBRACE;
                return new Token(Token.Type.RBRACE, "}", line, startCol);
        }

        lastEmitted = Token.Type.ERROR;
        return new Token(Token.Type.ERROR, String.valueOf(c), line, startCol);
    }
}

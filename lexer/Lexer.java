import java.util.*;

public class Lexer {
    private final String input;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

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
        // сюда еще будем ключевые слова докидывать 
    }

    public Lexer(String input) {
        this.input = input;
    }

    private char peek() {
        if (pos >= input.length()) return '\0';
        return input.charAt(pos);
    }

    private char advance() {
        char c = peek();
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

    public Token nextToken() {
        while (Character.isWhitespace(peek())) {
            advance();
        }

        int startCol = col;
        char c = advance();

       
        if (c == '\0') {
            return new Token(Token.Type.EOF, "", line, startCol);
        }

    
        if (Character.isLetter(c) || c == '_') {
            StringBuilder sb = new StringBuilder();
            sb.append(c);
            while (Character.isLetterOrDigit(peek()) || peek() == '_') {
                sb.append(advance());
            }
            String lexeme = sb.toString();
            Token.Type type = KEYWORDS.getOrDefault(lexeme.toLowerCase(), Token.Type.IDENTIFIER);
            return new Token(type, lexeme, line, startCol);
        }

    
        if (Character.isDigit(c)) {
            StringBuilder sb = new StringBuilder();
            sb.append(c);
            boolean isReal = false;
            while (Character.isDigit(peek())) sb.append(advance());
            if (peek() == '.' && Character.isDigit(input.charAt(pos+1))) {
                isReal = true;
                sb.append(advance()); // dot
                while (Character.isDigit(peek())) sb.append(advance());
            }
            return new Token(isReal ? Token.Type.REAL_LITERAL : Token.Type.INT_LITERAL, sb.toString(), line, startCol);
        }

 
        if (c == '"') {
            StringBuilder sb = new StringBuilder();
            while (peek() != '"' && peek() != '\0') {
                sb.append(advance());
            }
            if (peek() == '"') advance();
            return new Token(Token.Type.STRING_LITERAL, sb.toString(), line, startCol);
        }

       
        switch (c) {
            case '+': return new Token(Token.Type.PLUS, "+", line, startCol);
            case '-':
                if (match('>')) return new Token(Token.Type.ARROW, "->", line, startCol);
                return new Token(Token.Type.MINUS, "-", line, startCol);
            case '*': return new Token(Token.Type.MULTIPLY, "*", line, startCol);
            case '/': return new Token(Token.Type.DIVIDE, "/", line, startCol);
            case '%': return new Token(Token.Type.MODULO, "%", line, startCol);
            case '=': return new Token(Token.Type.EQUALS, "=", line, startCol);
            case '!':
                if (match('=')) return new Token(Token.Type.NOT_EQUALS, "!=", line, startCol);
                break;
            case '<':
                if (match('=')) return new Token(Token.Type.LESS_EQUAL, "<=", line, startCol);
                return new Token(Token.Type.LESS, "<", line, startCol);
            case '>':
                if (match('=')) return new Token(Token.Type.GREATER_EQUAL, ">=", line, startCol);
                return new Token(Token.Type.GREATER, ">", line, startCol);
            case ':':
                if (match('=')) return new Token(Token.Type.ASSIGN, ":=", line, startCol);
                return new Token(Token.Type.COLON, ":", line, startCol);
            case ';': return new Token(Token.Type.SEMICOLON, ";", line, startCol);
            case ',': return new Token(Token.Type.COMMA, ",", line, startCol);
            case '.':
                if (match('.')) return new Token(Token.Type.RANGE, "..", line, startCol);
                return new Token(Token.Type.DOT, ".", line, startCol);
            case '(': return new Token(Token.Type.LPAREN, "(", line, startCol);
            case ')': return new Token(Token.Type.RPAREN, ")", line, startCol);
            case '[': return new Token(Token.Type.LBRACKET, "[", line, startCol);
            case ']': return new Token(Token.Type.RBRACKET, "]", line, startCol);
            case '{': return new Token(Token.Type.LBRACE, "{", line, startCol);
            case '}': return new Token(Token.Type.RBRACE, "}", line, startCol);
        }

        return new Token(Token.Type.ERROR, String.valueOf(c), line, startCol);
    }
}

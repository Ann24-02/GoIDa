public class Token {
    public enum Type {
        // Keywords
        ROUTINE, IS, END, VAR, TYPE, INTEGER, REAL, BOOLEAN, 
        ARRAY, RECORD, IF, THEN, ELSE, WHILE, LOOP, FOR, IN, REVERSE, 
        PRINT, AND, OR, XOR, NOT,
        
        // Identifiers
        IDENTIFIER,
        
        // Literals
        INT_LITERAL, REAL_LITERAL, BOOL_LITERAL, STRING_LITERAL,
        
        // Operators
        ASSIGN, PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
        EQUALS, NOT_EQUALS, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
        RANGE, COLON, SEMICOLON, COMMA, DOT, 
        ARROW, FAT_ARROW,
        
        // Brackets
        LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,
        
        // Special
        EOF, ERROR
    }
    
    public final Type type;
    public final String lexeme;
    public final int line;
    public final int column;
    
    public Token(Type type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }
    
    @Override
    public String toString() {
        return String.format("Token{%s, '%s', line=%d, col=%d}", 
                           type, lexeme, line, column);
    }
}
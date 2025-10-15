package parser;

import ast.*;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final Lexer lexer;
    private Token current;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        advance();
    }

    private void advance() {
        current = lexer.nextToken();
    }

    private boolean match(Token.Type type) {
        if (current.type == type) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(Token.Type type) {
        if (current.type != type) {
            throw new RuntimeException("Expected " + type + " but found " + current);
        }
        advance();
    }

    // -------------------- PARSERS --------------------

    public Program parseProgram() {
        List<Declaration> decls = new ArrayList<>();
        while (current.type != Token.Type.EOF) {
            decls.add(parseDeclaration());
        }
        return new Program(decls, 1, 1);
    }

    private Declaration parseDeclaration() {
        switch (current.type) {
            case VAR:
                return parseVariableDeclaration();
            case TYPE:
                return parseTypeDeclaration();
            case ROUTINE:
                return parseRoutineDeclaration();
            default:
                throw new RuntimeException("Unknown declaration at " + current);
        }
    }

    private VariableDeclaration parseVariableDeclaration() {
        int line = current.line, col = current.column;
        expect(Token.Type.VAR);
        String name = current.lexeme;
        expect(Token.Type.IDENTIFIER);
        Type type = null;
        Expression init = null;

        if (match(Token.Type.COLON)) {
            type = parseType();
        }

        if (match(Token.Type.IS)) {
            init = parseExpression();
        }

        expect(Token.Type.SEMICOLON);
        return new VariableDeclaration(name, type, init, line, col);
    }

    private TypeDeclaration parseTypeDeclaration() {
        int line = current.line, col = current.column;
        expect(Token.Type.TYPE);
        String name = current.lexeme;
        expect(Token.Type.IDENTIFIER);
        expect(Token.Type.IS);
        Type aliased = parseType();
        expect(Token.Type.SEMICOLON);
        return new TypeDeclaration(name, aliased, line, col);
    }

    private RoutineDeclaration parseRoutineDeclaration() {
        int line = current.line, col = current.column;
        expect(Token.Type.ROUTINE);
        String name = current.lexeme;
        expect(Token.Type.IDENTIFIER);
        expect(Token.Type.LPAREN);
        List<Parameter> params = new ArrayList<>();
        if (!match(Token.Type.RPAREN)) {
            do {
                params.add(parseParameter());
            } while (match(Token.Type.COMMA));
            expect(Token.Type.RPAREN);
        }
        Type returnType = null;
        if (match(Token.Type.COLON)) {
            returnType = parseType();
        }
        expect(Token.Type.IS);

        Body body = parseBody();
        expect(Token.Type.END);
        return new RoutineDeclaration(name, params, returnType, body, null, line, col);
    }

    private Parameter parseParameter() {
        int line = current.line, col = current.column;
        String name = current.lexeme;
        expect(Token.Type.IDENTIFIER);
        expect(Token.Type.COLON);
        Type type = parseType();
        return new Parameter(name, type, line, col);
    }

    private Body parseBody() {
        List<ASTNode> elements = new ArrayList<>();
        while (current.type != Token.Type.END) {
            if (current.type == Token.Type.VAR || current.type == Token.Type.TYPE || current.type == Token.Type.ROUTINE) {
                elements.add(parseDeclaration());
            } else {
                elements.add(parseStatement());
            }
        }
        return new Body(elements, current.line, current.column);
    }

    private Statement parseStatement() {
        switch (current.type) {
            case IDENTIFIER:
                return parseAssignmentOrCall();
            case PRINT:
                return parsePrint();
            case IF:
                return parseIf();
            case WHILE:
                return parseWhile();
            case FOR:
                return parseFor();
            default:
                throw new RuntimeException("Unknown statement at " + current);
        }
    }

    private Statement parseAssignmentOrCall() {
        int line = current.line, col = current.column;
        String name = current.lexeme;
        expect(Token.Type.IDENTIFIER);

        // Проверка доступа к массиву или полю
        List<ModifiablePrimary.Access> accesses = new ArrayList<>();
        while (match(Token.Type.DOT) || match(Token.Type.LBRACKET)) {
            if (current.type == Token.Type.IDENTIFIER) {
                accesses.add(new ModifiablePrimary.Access(current.lexeme, current.line, current.column));
                advance();
            } else if (current.type == Token.Type.INT_LITERAL) {
                accesses.add(new ModifiablePrimary.Access(new IntegerLiteral(Integer.parseInt(current.lexeme), current.line, current.column), current.line, current.column));
                advance();
            }
            expect(current.type == Token.Type.RBRACKET ? Token.Type.RBRACKET : Token.Type.IDENTIFIER);
        }

        if (match(Token.Type.ASSIGN)) {
            Expression expr = parseExpression();
            expect(Token.Type.SEMICOLON);
            return new Assignment(new ModifiablePrimary(name, accesses, line, col), expr, line, col);
        } else if (match(Token.Type.LPAREN)) {
            List<Expression> args = new ArrayList<>();
            if (!match(Token.Type.RPAREN)) {
                do {
                    args.add(parseExpression());
                } while (match(Token.Type.COMMA));
                expect(Token.Type.RPAREN);
            }
            expect(Token.Type.SEMICOLON);
            return new RoutineCall(name, args, line, col);
        } else {
            throw new RuntimeException("Unexpected token after identifier: " + current);
        }
    }

    private Statement parsePrint() {
        int line = current.line, col = current.column;
        expect(Token.Type.PRINT);
        expect(Token.Type.LPAREN);
        List<Expression> exprs = new ArrayList<>();
        if (!match(Token.Type.RPAREN)) {
            do {
                exprs.add(parseExpression());
            } while (match(Token.Type.COMMA));
            expect(Token.Type.RPAREN);
        }
        expect(Token.Type.SEMICOLON);
        return new PrintStatement(exprs, line, col);
    }

    // ----------------- Управляющие конструкции -----------------

    private Statement parseIf() {
        int line = current.line, col = current.column;
        expect(Token.Type.IF);
        Expression cond = parseExpression();
        expect(Token.Type.THEN);
        Body thenBranch = parseBody();
        Body elseBranch = null;
        if (match(Token.Type.ELSE)) {
            elseBranch = parseBody();
        }
        expect(Token.Type.END);
        return new IfStatement(cond, thenBranch, elseBranch, line, col);
    }

    private Statement parseWhile() {
        int line = current.line, col = current.column;
        expect(Token.Type.WHILE);
        Expression cond = parseExpression();
        expect(Token.Type.LOOP);
        Body body = parseBody();
        expect(Token.Type.END);
        return new WhileLoop(cond, body, line, col);
    }

    private Statement parseFor() {
        int line = current.line, col = current.column;
        expect(Token.Type.FOR);
        String varName = current.lexeme;
        expect(Token.Type.IDENTIFIER);
        expect(Token.Type.IN);
        Expression start = parseExpression();
        expect(Token.Type.RANGE);
        Expression end = parseExpression();
        boolean reverse = match(Token.Type.REVERSE);
        expect(Token.Type.LOOP);
        Body body = parseBody();
        expect(Token.Type.END);
        return new ForLoop(varName, new Range(start, end, line, col), reverse, body, line, col);
    }

    // ----------------- Выражения -----------------

    private Expression parseExpression() {
        // Тут можно добавить поддержку бинарных и унарных выражений
        // Для простоты пока парсим только идентификаторы и литералы
        switch (current.type) {
            case INT_LITERAL:
                int iv = Integer.parseInt(current.lexeme);
                Expression intLit = new IntegerLiteral(iv, current.line, current.column);
                advance();
                return intLit;
            case REAL_LITERAL:
                double rv = Double.parseDouble(current.lexeme);
                Expression realLit = new RealLiteral(rv, current.line, current.column);
                advance();
                return realLit;
            case BOOL_LITERAL:
                boolean bv = Boolean.parseBoolean(current.lexeme);
                Expression boolLit = new BooleanLiteral(bv, current.line, current.column);
                advance();
                return boolLit;
            case STRING_LITERAL:
                Expression strLit = new StringLiteral(current.lexeme, current.line, current.column);
                advance();
                return strLit;
            case IDENTIFIER:
                String name = current.lexeme;
                advance();
                return new Identifier(name, current.line, current.column);
            default:
                throw new RuntimeException("Unexpected token in expression: " + current);
        }
    }

    private Type parseType() {
        switch (current.type) {
            case INTEGER:
            case REAL:
            case BOOLEAN:
            case STRING:
                String tname = current.lexeme.toLowerCase();
                Type prim = new PrimitiveType(tname, current.line, current.column);
                advance();
                return prim;
            case ARRAY:
                advance();
                expect(Token.Type.LBRACKET);
                Expression size = parseExpression();
                expect(Token.Type.RBRACKET);
                Type elementType = parseType();
                return new ArrayType(size, elementType, current.line, current.column);
            case RECORD:
                advance();
                List<VariableDeclaration> fields = new ArrayList<>();
                while (current.type != Token.Type.END) {
                    fields.add(parseVariableDeclaration());
                }
                expect(Token.Type.END);
                return new RecordType(fields, current.line, current.column);
            case IDENTIFIER:
                String uname = current.lexeme;
                advance();
                return new UserType(uname, current.line, current.column);
            default:
                throw new RuntimeException("Unknown type: " + current);
        }
    }
}

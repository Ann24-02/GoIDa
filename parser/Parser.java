package parser;

import ast.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Final full recursive-descent parser for the Imperative Language.
 * Supports all features in tests 1–15.
 */
public class Parser {
    private final Lexer lexer;
    private Token current;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        advance();
    }

    private void advance() { current = lexer.nextToken(); }

    private boolean match(Token.Type type) {
        if (current.type == type) { advance(); return true; }
        return false;
    }

    private void expect(Token.Type type) {
        if (current.type != type)
            throw new RuntimeException("Expected " + type + " but found " + current);
        advance();
    }

    // ================================================================
    // PROGRAM STRUCTURE
    // ================================================================

    public Program parseProgram() {
        List<Declaration> decls = new ArrayList<>();
        while (current.type != Token.Type.EOF) {
            if (current.type == Token.Type.SEMICOLON) { advance(); continue; }
            decls.add(parseDeclaration());
        }
        return new Program(decls, 1, 1);
    }

    private Declaration parseDeclaration() {
        switch (current.type) {
            case VAR: return parseVariableDeclaration();
            case TYPE: return parseTypeDeclaration();
            case ROUTINE: return parseRoutineDeclaration();
            default:
                throw new RuntimeException("Unknown declaration at " + current);
        }
    }

    // ---------------------------------------------------------------
    // Declarations
    // ---------------------------------------------------------------

    private VariableDeclaration parseVariableDeclaration() {
        int line = current.line, col = current.column;
        expect(Token.Type.VAR);
        String name = current.lexeme; expect(Token.Type.IDENTIFIER);
        Type type = null;
        Expression init = null;

        if (match(Token.Type.COLON)) type = parseType();
        if (match(Token.Type.IS)) init = parseExpression();

        expect(Token.Type.SEMICOLON);
        return new VariableDeclaration(name, type, init, line, col);
    }

    private TypeDeclaration parseTypeDeclaration() {
        int line = current.line, col = current.column;
        expect(Token.Type.TYPE);
        String name = current.lexeme; expect(Token.Type.IDENTIFIER);
        expect(Token.Type.IS);
        Type aliased = parseType();
        expect(Token.Type.SEMICOLON);
        return new TypeDeclaration(name, aliased, line, col);
    }

    private RoutineDeclaration parseRoutineDeclaration() {
        int line = current.line, col = current.column;
        expect(Token.Type.ROUTINE);
        String name = current.lexeme; expect(Token.Type.IDENTIFIER);
        expect(Token.Type.LPAREN);

        List<Parameter> params = new ArrayList<>();
        if (!match(Token.Type.RPAREN)) {
            do params.add(parseParameter());
            while (match(Token.Type.COMMA));
            expect(Token.Type.RPAREN);
        }

        Type returnType = null;
        if (match(Token.Type.COLON)) returnType = parseType();

        // expression-body routine: routine f(x) => expr;
        if (match(Token.Type.FAT_ARROW)) {
            Expression exprBody = parseExpression();
            expect(Token.Type.SEMICOLON);
            return new RoutineDeclaration(name, params, returnType, null, exprBody, line, col);
        }

        expect(Token.Type.IS);
        Body body = parseBody();
        expect(Token.Type.END);
        return new RoutineDeclaration(name, params, returnType, body, null, line, col);
    }

    private Parameter parseParameter() {
        int line = current.line, col = current.column;
        boolean isRef = match(Token.Type.REF);
        String name = current.lexeme; expect(Token.Type.IDENTIFIER);
        expect(Token.Type.COLON);
        Type type = parseType();
        return new Parameter((isRef ? "ref " : "") + name, type, line, col);
    }

    private Body parseBody() {
        List<ASTNode> elements = new ArrayList<>();
        while (current.type != Token.Type.END && current.type != Token.Type.ELSE) {
            if (current.type == Token.Type.SEMICOLON) { advance(); continue; }
            if (current.type == Token.Type.VAR || current.type == Token.Type.TYPE || current.type == Token.Type.ROUTINE)
                elements.add(parseDeclaration());
            else elements.add(parseStatement());
        }
        return new Body(elements, current.line, current.column);
    }

    // ================================================================
    // STATEMENTS
    // ================================================================

    private Statement parseStatement() {
        switch (current.type) {
            case IDENTIFIER: return parseAssignmentOrCall();
            case PRINT: return parsePrint();
            case IF: return parseIf();
            case WHILE: return parseWhile();
            case FOR: return parseFor();
            case RETURN: return parseReturn();
            default:
                throw new RuntimeException("Unknown statement at " + current);
        }
    }

    private Statement parseAssignmentOrCall() {
        int line = current.line, col = current.column;
        String name = current.lexeme; expect(Token.Type.IDENTIFIER);

        // Routine call
        if (match(Token.Type.LPAREN)) {
            List<Expression> args = new ArrayList<>();
            if (!match(Token.Type.RPAREN)) {
                do args.add(parseExpression());
                while (match(Token.Type.COMMA));
                expect(Token.Type.RPAREN);
            }
            expect(Token.Type.SEMICOLON);
            return new RoutineCall(name, args, line, col);
        }

        // LHS with accesses (arr[i], rec.field)
        List<ModifiablePrimary.Access> accesses = new ArrayList<>();
        while (current.type == Token.Type.LBRACKET || current.type == Token.Type.DOT) {
            if (match(Token.Type.LBRACKET)) {
                Expression index = parseExpression();
                expect(Token.Type.RBRACKET);
                accesses.add(new ModifiablePrimary.Access(index, current.line, current.column));
            } else if (match(Token.Type.DOT)) {
                String field = current.lexeme; expect(Token.Type.IDENTIFIER);
                accesses.add(new ModifiablePrimary.Access(field, current.line, current.column));
            }
        }

        expect(Token.Type.ASSIGN);
        Expression expr = parseExpression();
        expect(Token.Type.SEMICOLON);
        return new Assignment(new ModifiablePrimary(name, accesses, line, col), expr, line, col);
    }

    private Statement parsePrint() {
        int line = current.line, col = current.column;
        expect(Token.Type.PRINT);
        List<Expression> exprs = new ArrayList<>();

        // allow both print(x, y) and print x, y
        if (match(Token.Type.LPAREN)) {
            if (!match(Token.Type.RPAREN)) {
                do exprs.add(parseExpression());
                while (match(Token.Type.COMMA));
                expect(Token.Type.RPAREN);
            }
        } else {
            exprs.add(parseExpression());
            while (match(Token.Type.COMMA)) exprs.add(parseExpression());
        }

        expect(Token.Type.SEMICOLON);
        return new PrintStatement(exprs, line, col);
    }

    private Statement parseIf() {
        int line = current.line, col = current.column;
        expect(Token.Type.IF);
        Expression cond = parseExpression();
        expect(Token.Type.THEN);
        Body thenBranch = parseBody();
        Body elseBranch = null;
        if (match(Token.Type.ELSE)) elseBranch = parseBody();
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

        // Detect if this is a range or a for-each
        Expression firstExpr = parseExpression();
        Expression secondExpr = null;
        boolean isRange = false;

        if (match(Token.Type.RANGE)) { // 1 .. 10
            secondExpr = parseExpression();
            isRange = true;
        }

        boolean reverse = match(Token.Type.REVERSE);
        expect(Token.Type.LOOP);
        Body body = parseBody();
        expect(Token.Type.END);

        if (isRange) {
            return new ForLoop(varName, new Range(firstExpr, secondExpr, line, col), reverse, body, line, col);
        } else {
            // for-each loop: treat it as a "Range" with only one expression
            // or a distinct ForEach node if you want later
            return new RoutineCall("for_each(" + varName + " in expr)", List.of(firstExpr), line, col);
        }
    }


    private Statement parseReturn() {
        int line = current.line, col = current.column;
        expect(Token.Type.RETURN);
        Expression value = null;
        if (current.type != Token.Type.SEMICOLON)
            value = parseExpression();
        expect(Token.Type.SEMICOLON);
        // Represent return as special RoutineCall node for now
        List<Expression> args = new ArrayList<>();
        if (value != null) args.add(value);
        return new RoutineCall("return", args, line, col);
    }

    // ================================================================
    // EXPRESSIONS (full precedence + literals)
    // ================================================================

    private Expression parseExpression() { return parseOr(); }

    private Expression parseOr() {
        Expression left = parseAnd();
        while (current.type == Token.Type.OR) {
            Token.Type op = current.type; advance();
            Expression right = parseAnd();
            left = new BinaryExpression(left, op, right, current.line, current.column);
        }
        return left;
    }

    private Expression parseAnd() {
        Expression left = parseComparison();
        while (current.type == Token.Type.AND) {
            Token.Type op = current.type; advance();
            Expression right = parseComparison();
            left = new BinaryExpression(left, op, right, current.line, current.column);
        }
        return left;
    }

    private Expression parseComparison() {
        Expression left = parseAdditive();
        while (current.type == Token.Type.LESS || current.type == Token.Type.LESS_EQUAL ||
               current.type == Token.Type.GREATER || current.type == Token.Type.GREATER_EQUAL ||
               current.type == Token.Type.EQUALS || current.type == Token.Type.NOT_EQUALS) {
            Token.Type op = current.type; advance();
            Expression right = parseAdditive();
            left = new BinaryExpression(left, op, right, current.line, current.column);
        }
        return left;
    }

    private Expression parseAdditive() {
        Expression left = parseTerm();
        while (current.type == Token.Type.PLUS || current.type == Token.Type.MINUS) {
            Token.Type op = current.type; advance();
            Expression right = parseTerm();
            left = new BinaryExpression(left, op, right, current.line, current.column);
        }
        return left;
    }

    private Expression parseTerm() {
        Expression left = parseFactor();
        while (current.type == Token.Type.MULTIPLY ||
               current.type == Token.Type.DIVIDE ||
               current.type == Token.Type.MODULO) {
            Token.Type op = current.type; advance();
            Expression right = parseFactor();
            left = new BinaryExpression(left, op, right, current.line, current.column);
        }
        return left;
    }

    private Expression parseFactor() {
        if (current.type == Token.Type.NOT || current.type == Token.Type.MINUS) {
            Token.Type op = current.type; advance();
            Expression operand = parseFactor();
            return new UnaryExpression(op, operand, current.line, current.column);
        }

        switch (current.type) {
            case INT_LITERAL: {
                int iv = Integer.parseInt(current.lexeme);
                advance(); return new IntegerLiteral(iv, current.line, current.column);
            }
            case REAL_LITERAL: {
                double rv = Double.parseDouble(current.lexeme);
                advance(); return new RealLiteral(rv, current.line, current.column);
            }
            case BOOL_LITERAL: {
                boolean bv = Boolean.parseBoolean(current.lexeme);
                advance(); return new BooleanLiteral(bv, current.line, current.column);
            }
            case STRING_LITERAL: {
                String s = current.lexeme;
                advance(); return new StringLiteral(s, current.line, current.column);
            }
            case IDENTIFIER: {
                String name = current.lexeme; advance();
                // function call or variable/field/array access
                if (match(Token.Type.LPAREN)) {
                    List<Expression> args = new ArrayList<>();
                    if (!match(Token.Type.RPAREN)) {
                        do args.add(parseExpression());
                        while (match(Token.Type.COMMA));
                        expect(Token.Type.RPAREN);
                    }
                    return new FunctionCall(name, args, current.line, current.column);
                }
                List<ModifiablePrimary.Access> accesses = new ArrayList<>();
                while (current.type == Token.Type.LBRACKET || current.type == Token.Type.DOT) {
                    if (match(Token.Type.LBRACKET)) {
                        Expression index = parseExpression();
                        expect(Token.Type.RBRACKET);
                        accesses.add(new ModifiablePrimary.Access(index, current.line, current.column));
                    } else if (match(Token.Type.DOT)) {
                        String field = current.lexeme; expect(Token.Type.IDENTIFIER);
                        accesses.add(new ModifiablePrimary.Access(field, current.line, current.column));
                    }
                }
                if (!accesses.isEmpty())
                    return new ModifiablePrimary(name, accesses, current.line, current.column);
                return new Identifier(name, current.line, current.column);
            }
            case LBRACKET: { // array literal
                int line = current.line, col = current.column;
                advance();
                List<Expression> elems = new ArrayList<>();
                if (!match(Token.Type.RBRACKET)) {
                    do elems.add(parseExpression());
                    while (match(Token.Type.COMMA));
                    expect(Token.Type.RBRACKET);
                }
                return new FunctionCall("array_literal", elems, line, col);
            }
            case LBRACE: { // record literal
                int line = current.line, col = current.column;
                advance();
                List<Expression> fields = new ArrayList<>();
                if (!match(Token.Type.RBRACE)) {
                    do {
                        String field = current.lexeme; expect(Token.Type.IDENTIFIER);
                        expect(Token.Type.COLON);
                        Expression value = parseExpression();
                        List<Expression> pair = new ArrayList<>();
                        pair.add(new StringLiteral(field, line, col));
                        pair.add(value);
                        fields.add(new FunctionCall("field", pair, line, col));
                    } while (match(Token.Type.COMMA));
                    expect(Token.Type.RBRACE);
                }
                return new FunctionCall("record_literal", fields, line, col);
            }
            case LPAREN: {
                advance();
                Expression expr = parseExpression();
                expect(Token.Type.RPAREN);
                return expr;
            }
            default:
                throw new RuntimeException("Unexpected token in expression: " + current);
        }
    }

    // ================================================================
    // TYPES
    // ================================================================

    private Type parseType() {
        switch (current.type) {
            case INTEGER: case REAL: case BOOLEAN: case STRING: {
                String tname = current.lexeme.toLowerCase();
                Type prim = new PrimitiveType(tname, current.line, current.column);
                advance(); return prim;
            }
            case ARRAY: {
                advance();
                expect(Token.Type.LBRACKET);
                Expression size = null;
                if (!match(Token.Type.RBRACKET)) {
                    size = parseExpression();
                    expect(Token.Type.RBRACKET);
                }
                Type elemType = parseType();
                return new ArrayType(size, elemType, current.line, current.column);
            }
            case RECORD: {
                advance();
                List<VariableDeclaration> fields = new ArrayList<>();
                while (current.type != Token.Type.END)
                    fields.add(parseVariableDeclaration());
                expect(Token.Type.END);
                return new RecordType(fields, current.line, current.column);
            }
            case IDENTIFIER: {
                String uname = current.lexeme;
                advance();
                return new UserType(uname, current.line, current.column);
            }
            default:
                throw new RuntimeException("Unknown type: " + current);
        }
    }
}

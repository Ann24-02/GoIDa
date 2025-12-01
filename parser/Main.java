package parser;

import analyzer.*;
import ast.*;
import java.io.IOException;
import java.nio.file.*;

// Main compiler entry point
// Parses files, runs semantic checks, and performs optimizations
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            parseAndAnalyze(Path.of(args[0]));
        } else {
            Path testsDir = Path.of("tests");
            if (!Files.exists(testsDir)) {
                System.err.println("No 'tests' directory found. Run from project root.");
                return;
            }

            try (var paths = Files.walk(testsDir)) {
                paths.filter(p -> p.toString().endsWith(".rout"))
                     .sorted()
                     .forEach(Main::safeParseAndAnalyze);
            }
        }
    }

    private static void safeParseAndAnalyze(Path path) {
        try {
            parseAndAnalyze(path);
        } catch (Exception e) {
            System.out.println("X " + path.getFileName() + " failed: " + e.getMessage());
        }
    }

    private static void parseAndAnalyze(Path path) throws IOException {
        System.out.println("== Parsing: " + path.getFileName() + " ==");

        String src = Files.readString(path);
        Lexer lexer = new Lexer(src);
        Parser parser = new Parser(lexer);

        Program program = parser.parseProgram();
        System.out.println(" Parsing successful. AST:");
        printNode(program, 0);

        // Semantic analysis
        System.out.println("\n-- Semantic Analysis --");
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        boolean semanticOk = true;

        try {
            analyzer.analyze(program);
            System.out.println(" Semantic analysis passed");

            if (!analyzer.getWarnings().isEmpty()) {
                System.out.println(" Warnings:");
                for (String warning : analyzer.getWarnings()) {
                    System.out.println("  - " + warning);
                }
            }
        } catch (SemanticException e) {
            semanticOk = false;
            System.out.println(" Semantic error: " + e.getMessage());

            var all = analyzer.getErrors();
            if (all.size() > 1) {
                System.out.println("… plus " + (all.size() - 1) + " more error(s):");
                for (int i = 1; i < all.size(); i++) {
                    System.out.println("  - " + all.get(i).getMessage());
                }
            }
        }

        // Only optimize if semantics passed
        if (!semanticOk) {
            return;
        }

        System.out.println("\n-- Optimization --");
        OptimizationEngine optimizer = new OptimizationEngine();
        Program optimized = optimizer.optimize(program);

        if (optimizer.getOptimizationCount() > 0) {
            System.out.println(" Optimized AST:");
            printNode(optimized, 0);
        } else {
            System.out.println("No optimizations applied");
        }

        System.out.println("\n Processing complete");
    }

    // Pretty print AST structure
    private static void printNode(Object node, int indent) {
        if (node == null) {
            printIndent(indent);
            System.out.println("null");
            return;
        }

        printIndent(indent);
        System.out.println(node.getClass().getSimpleName());
        
        if (node instanceof Program p) {
            for (Declaration d : p.declarations)
                printNode(d, indent + 2);
        } else if (node instanceof Body b) {
            for (ASTNode e : b.elements)
                printNode(e, indent + 2);
        } else if (node instanceof VariableDeclaration v) {
            printIndent(indent + 2);
            System.out.println("name = " + v.name);
            printNode(v.type, indent + 2);
            printNode(v.initializer, indent + 2);
        } else if (node instanceof TypeDeclaration t) {
            printIndent(indent + 2);
            System.out.println("name = " + t.name);
            printNode(t.aliasedType, indent + 2);
        } else if (node instanceof RoutineDeclaration r) {
            printIndent(indent + 2);
            System.out.println("name = " + r.name);
            printIndent(indent + 2);
            System.out.println("params:");
            for (Parameter p : r.parameters)
                printNode(p, indent + 4);
            if (r.returnType != null) {
                printIndent(indent + 2);
                System.out.println("returnType:");
                printNode(r.returnType, indent + 4);
            }
            printIndent(indent + 2);
            System.out.println("body:");
            if (r.body != null) printNode(r.body, indent + 4);
            else if (r.expressionBody != null) printNode(r.expressionBody, indent + 4);
            else {
                printIndent(indent + 4);
                System.out.println("null");
            }
        } else if (node instanceof Parameter p) {
            printIndent(indent + 2);
            System.out.println(p.name + " :");
            printNode(p.type, indent + 4);
        } else if (node instanceof Assignment a) {
            printIndent(indent + 2);
            System.out.println("target:");
            printNode(a.target, indent + 4);
            printIndent(indent + 2);
            System.out.println("value:");
            printNode(a.value, indent + 4);
        } else if (node instanceof RoutineCall c) {
            printIndent(indent + 2);
            System.out.println("call " + c.routineName);
            for (Expression e : c.arguments)
                printNode(e, indent + 4);
        } else if (node instanceof PrintStatement p) {
            printIndent(indent + 2);
            System.out.println("print:");
            for (Expression e : p.expressions)
                printNode(e, indent + 4);
        } else if (node instanceof IfStatement i) {
            printIndent(indent + 2);
            System.out.println("if condition:");
            printNode(i.condition, indent + 4);
            printIndent(indent + 2);
            System.out.println("then:");
            printNode(i.thenBranch, indent + 4);
            if (i.elseBranch != null) {
                printIndent(indent + 2);
                System.out.println("else:");
                printNode(i.elseBranch, indent + 4);
            }
        } else if (node instanceof WhileLoop w) {
            printIndent(indent + 2);
            System.out.println("while condition:");
            printNode(w.condition, indent + 4);
            printIndent(indent + 2);
            System.out.println("body:");
            printNode(w.body, indent + 4);
        } else if (node instanceof ForLoop f) {
            printIndent(indent + 2);
            System.out.println("for " + f.loopVariable + (f.reverse ? " reverse" : "") + " in:");
            printNode(f.range, indent + 4);
            printIndent(indent + 2);
            System.out.println("body:");
            printNode(f.body, indent + 4);
        } else if (node instanceof Range r) {
            printIndent(indent + 2);
            System.out.println("start:");
            printNode(r.start, indent + 4);
            printIndent(indent + 2);
            System.out.println("end:");
            printNode(r.end, indent + 4);
        } else if (node instanceof BinaryExpression b) {
            printIndent(indent + 2);
            System.out.println("op = " + b.operator);
            printNode(b.left, indent + 4);
            printNode(b.right, indent + 4);
        } else if (node instanceof UnaryExpression u) {
            printIndent(indent + 2);
            System.out.println("op = " + u.operator);
            printNode(u.operand, indent + 4);
        } else if (node instanceof Identifier id) {
            printIndent(indent + 2);
            System.out.println("name = " + id.name);
        } else if (node instanceof IntegerLiteral i) {
            printIndent(indent + 2);
            System.out.println("int = " + i.value);
        } else if (node instanceof RealLiteral r) {
            printIndent(indent + 2);
            System.out.println("real = " + r.value);
        } else if (node instanceof BooleanLiteral b) {
            printIndent(indent + 2);
            System.out.println("bool = " + b.value);
        } else if (node instanceof StringLiteral s) {
            printIndent(indent + 2);
            System.out.println("string = \"" + s.value + "\"");
        } else if (node instanceof PrimitiveType t) {
            printIndent(indent + 2);
            System.out.println("primitive = " + t.typeName);
        } else if (node instanceof UserType t) {
            printIndent(indent + 2);
            System.out.println("userType = " + t.typeName);
        } else if (node instanceof ArrayType t) {
            printIndent(indent + 2);
            System.out.println("array of:");
            printNode(t.elementType, indent + 4);
        } else if (node instanceof RecordType r) {
            printIndent(indent + 2);
            System.out.println("record fields:");
            for (VariableDeclaration f : r.fields)
                printNode(f, indent + 4);
        } else if (node instanceof FunctionCall f) {
            printIndent(indent + 2);
            System.out.println("functionName = " + f.functionName);
            for (Expression e : f.arguments)
                printNode(e, indent + 4);
        } else if (node instanceof ModifiablePrimary m) {
            printIndent(indent + 2);
            System.out.println("base = " + m.baseName);
            for (var acc : m.accesses) {
                if (acc.isFieldAccess) {
                    printIndent(indent + 4);
                    System.out.println(".field " + acc.fieldName);
                } else {
                    printIndent(indent + 4);
                    System.out.println("[index]");
                    printNode(acc.index, indent + 6);
                }
            }
        } else {
            printIndent(indent + 2);
            System.out.println("(unhandled node type)");
        }
    }

    private static void printIndent(int n) {
        for (int i = 0; i < n; i++) System.out.print(' ');
    }
}
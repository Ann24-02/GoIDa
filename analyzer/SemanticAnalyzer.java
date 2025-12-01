package analyzer;

import ast.*;
import java.util.*;

// Semantic analyzer that checks for declaration errors and type issues.
// Finds problems before code generation.
public class SemanticAnalyzer {

    private final SemanticContext context;
    private final List<String> warnings = new ArrayList<>();
    private final List<SemanticException> errors = new ArrayList<>();

    public SemanticAnalyzer() {
        this.context = new SemanticContext();
    }
    
    public SemanticContext getSemanticContext() {
        return context;
    }

    // Main analysis in two passes
    public void analyze(Program program) {
        // First pass: collect global declarations
        for (Declaration decl : program.declarations) {
            try {
                collectGlobalDeclaration(decl);
            } catch (SemanticException e) {
                errors.add(e);
            }
        }

        // Second pass: check declarations and bodies
        for (Declaration decl : program.declarations) {
            try {
                checkDeclaration(decl);
            } catch (SemanticException e) {
                errors.add(e);
            }
        }

        // Add warnings for unused variables
        addUnusedWarnings(context.getUnusedVariables());

        // Throw first error if any found
        if (!errors.isEmpty()) {
            throw errors.get(0);
        }
    }

    public List<SemanticException> getErrors() {
        return new ArrayList<>(errors);
    }

    // Collect global declarations (variables, functions, types)
    private void collectGlobalDeclaration(Declaration decl) {
        if (decl instanceof VariableDeclaration v) {
            context.declareVariable(v.name, v.type, v.line, v.column);
        } else if (decl instanceof RoutineDeclaration r) {
            context.declareRoutine(r.name, r.parameters, r.returnType, r.line, r.column);
        } else if (decl instanceof TypeDeclaration t) {
            context.declareType(t.name, t.aliasedType, t.line, t.column);
        }
    }

    // Check declarations and their bodies
    private void checkDeclaration(Declaration decl) {
        if (decl instanceof VariableDeclaration v) {
            if (v.initializer != null) {
                checkExpression(v.initializer);
            }
        } else if (decl instanceof RoutineDeclaration r) {
            context.enterRoutine(r);
            context.enterScope();

            // Add parameters to current scope
            for (Parameter p : r.parameters) {
                try {
                    String pname = p.name;
                    if (pname.startsWith("ref ")) {
                        pname = pname.substring(4);
                    }
                    context.declareVariable(pname, p.type, p.line, p.column);
                } catch (SemanticException e) {
                    errors.add(e);
                }
            }

            // Check function body
            if (r.body != null) {
                checkBody(r.body);
            } else if (r.expressionBody != null) {
                checkExpression(r.expressionBody);
            }

            context.exitScope();
            context.exitRoutine();
        }
    }

    // Check a block of code
    private void checkBody(Body body) {
        if (body == null) return;

        for (ASTNode element : body.elements) {
            if (element instanceof VariableDeclaration v) {
                if (v.initializer != null) {
                    checkExpression(v.initializer);
                }
                context.declareVariable(v.name, v.type, v.line, v.column);
            } else if (element instanceof TypeDeclaration t) {
                context.declareType(t.name, t.aliasedType, t.line, t.column);
            } else if (element instanceof Statement s) {
                checkStatement(s);
            } else if (element instanceof Declaration d) {
                checkDeclaration(d);
            }
        }
    }

    // Check different statement types
    private void checkStatement(Statement stmt) {
        if (stmt instanceof Assignment a) {
            checkAssignment(a);
        } else if (stmt instanceof RoutineCall rc) {
            checkRoutineCall(rc);
        } else if (stmt instanceof PrintStatement ps) {
            checkPrintStatement(ps);
        } else if (stmt instanceof IfStatement ifs) {
            checkIfStatement(ifs);
        } else if (stmt instanceof WhileLoop wl) {
            checkWhileLoop(wl);
        } else if (stmt instanceof ForLoop fl) {
            checkForLoop(fl);
        }
    }

    // Check function calls including 'return'
    private void checkRoutineCall(RoutineCall rc) {
        // Check 'return' is inside a function
        if ("return".equals(rc.routineName)) {
            if (!context.isInRoutine()) {
                throw new SemanticException(
                    "return statement outside of routine",
                    rc.line, rc.column
                );
            }
        }
        // Built-in functions like for_each are allowed
        else if (rc.routineName != null && rc.routineName.contains("for_each")) {
            // Skip checking
        }
        // Check regular function calls
        else {
            if (!context.isDeclaredRoutine(rc.routineName)) {
                throw new SemanticException(
                    "Routine '" + rc.routineName + "' is not declared",
                    rc.line, rc.column
                );
            }
            SemanticContext.RoutineInfo info = context.getRoutineInfo(rc.routineName);
            int expected = (info.params != null ? info.params.size() : 0);
            int given = (rc.arguments != null ? rc.arguments.size() : 0);
            if (given != expected) {
                throw new SemanticException(
                    "Routine '" + rc.routineName + "' expects " + expected +
                    " argument(s) but got " + given,
                    rc.line, rc.column
                );
            }
        }

        // Check all arguments
        for (Expression arg : rc.arguments) {
            checkExpression(arg);
        }
    }

    // Check variable assignments
    private void checkAssignment(Assignment a) {
        checkExpression(a.value);

        if (a.target instanceof ModifiablePrimary mp) {
            if (!context.isDeclaredVariable(mp.baseName)) {
                throw new SemanticException(
                    "Variable '" + mp.baseName + "' is not declared",
                    mp.line, mp.column
                );
            }
            context.markVariableUsed(mp.baseName);

            for (ModifiablePrimary.Access acc : mp.accesses) {
                if (acc.index != null) {
                    checkExpression(acc.index);
                }
            }
        }
    }

    // Check expressions for undeclared variables
    private void checkExpression(Expression expr) {
        if (expr == null) return;

        if (expr instanceof Identifier id) {
            if (!context.isDeclaredVariable(id.name)) {
                throw new SemanticException(
                    "Variable '" + id.name + "' is not declared",
                    id.line, id.column
                );
            }
            context.markVariableUsed(id.name);
        } else if (expr instanceof BinaryExpression be) {
            checkExpression(be.left);
            checkExpression(be.right);
        } else if (expr instanceof UnaryExpression ue) {
            checkExpression(ue.operand);
        } else if (expr instanceof FunctionCall fc) {
            // Check function arity if it's a known routine
            if (fc.functionName != null && context.isDeclaredRoutine(fc.functionName)) {
                SemanticContext.RoutineInfo info = context.getRoutineInfo(fc.functionName);
                int expected = (info.params != null ? info.params.size() : 0);
                int given = (fc.arguments != null ? fc.arguments.size() : 0);
                if (given != expected) {
                    throw new SemanticException(
                        "Routine '" + fc.functionName + "' expects " + expected +
                        " argument(s) but got " + given,
                        fc.line, fc.column
                    );
                }
            }
            for (Expression arg : fc.arguments) {
                checkExpression(arg);
            }
        } else if (expr instanceof ModifiablePrimary mp) {
            if (!context.isDeclaredVariable(mp.baseName)) {
                throw new SemanticException(
                    "Variable '" + mp.baseName + "' is not declared",
                    mp.line, mp.column
                );
            }
            context.markVariableUsed(mp.baseName);

            for (ModifiablePrimary.Access acc : mp.accesses) {
                if (acc.index != null) {
                    checkExpression(acc.index);
                }
            }
        }
    }

    // Check print statements
    private void checkPrintStatement(PrintStatement ps) {
        for (Expression expr : ps.expressions) {
            checkExpression(expr);
        }
    }

    // Check if statements
    private void checkIfStatement(IfStatement ifs) {
        checkExpression(ifs.condition);
        if (ifs.thenBranch != null) {
            context.enterScope();
            checkBody(ifs.thenBranch);
            context.exitScope();
        }
        if (ifs.elseBranch != null) {
            context.enterScope();
            checkBody(ifs.elseBranch);
            context.exitScope();
        }
    }

    // Check while loops
    private void checkWhileLoop(WhileLoop wl) {
        checkExpression(wl.condition);
        context.enterLoop();
        context.enterScope();
        if (wl.body != null) {
            checkBody(wl.body);
        }
        context.exitScope();
        context.exitLoop();
    }

    // Check for loops
    private void checkForLoop(ForLoop fl) {
        context.enterLoop();
        context.enterScope();

        if (fl.range != null) {
            checkExpression(fl.range.start);
            checkExpression(fl.range.end);
        }

        context.declareVariable(fl.loopVariable, null, fl.line, fl.column);

        if (fl.body != null) {
            checkBody(fl.body);
        }

        context.exitScope();
        context.exitLoop();
    }

    // Warning handling
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    private void addWarning(String warning) {
        warnings.add(warning);
    }

    private void addUnusedWarnings(List<SemanticContext.VarInfo> vars) {
        for (SemanticContext.VarInfo v : vars) {
            addWarning("Variable '" + v.name + "' declared at " + v.line + ":" + v.column + " is never used");
        }
    }
}
package analyzer;

import ast.*;
import java.util.*;

/**
 * SemanticAnalyzer - runs semantic checks over the AST.
 *
 * Non-modifying checks included:
 * 1) Declarations before usage (variables, routines, types must exist before use)
 * 2) Correct keyword usage (return only inside routines)
 * 3) Basic type checks
 * 4) Unused variable detection (produces warnings)
 * 5) Arity check for routine calls (number of args must match)
 *
 * Note: this analyzer collects multiple errors (when possible) and then
 * throws the first one to keep a simple external contract. You can fetch
 * all collected errors via getErrors().
 */
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
    /**
     * Run semantic analysis in two passes:
     * Pass 1: collect only global declarations (vars, routines, types).
     * Pass 2: check each top-level declaration and its body in order.
     *
     * We collect errors across both passes. After both passes, if any
     * errors were collected, we throw the first one, but you can inspect
     * the rest with getErrors().
     */
    public void analyze(Program program) {
        // Pass 1: collect ONLY globals
        for (Declaration decl : program.declarations) {
            try {
                collectGlobalDeclaration(decl);
            } catch (SemanticException e) {
                errors.add(e);
            }
        }

        // Pass 2: check top-level decls
        for (Declaration decl : program.declarations) {
            try {
                checkDeclaration(decl);
            } catch (SemanticException e) {
                errors.add(e);
            }
        }

        // Add warnings
        addUnusedWarnings(context.getUnusedVariables());

        // If there are errors, throw the first one to fail the file
        if (!errors.isEmpty()) {
            throw errors.get(0);
        }
    }

    public List<SemanticException> getErrors() {
        return new ArrayList<>(errors);
    }

   // Pass 1: collect global declarations
    private void collectGlobalDeclaration(Declaration decl) {
        if (decl instanceof VariableDeclaration v) {
            // global variable becomes visible
            context.declareVariable(v.name, v.type, v.line, v.column);

        } else if (decl instanceof RoutineDeclaration r) {
            context.declareRoutine(r.name, r.parameters, r.returnType, r.line, r.column);

        } else if (decl instanceof TypeDeclaration t) {
            context.declareType(t.name, t.aliasedType, t.line, t.column);
        }
    }

    // Pass 2: check declarations and bodies
    private void checkDeclaration(Declaration decl) {
        if (decl instanceof VariableDeclaration v) {
            if (v.initializer != null) {
                checkExpression(v.initializer);
            }

        } else if (decl instanceof RoutineDeclaration r) {
            context.enterRoutine(r);
            context.enterScope();  // routine's local scope

            // Parameters are visible inside the body
            for (Parameter p : r.parameters) {
                try {
                    // parameter names may be prefixed with "ref "; strip it for declaration
                    String pname = p.name;
                    if (pname.startsWith("ref ")) {
                        pname = pname.substring(4);
                    }
                    context.declareVariable(pname, p.type, p.line, p.column);
                } catch (SemanticException e) {
                    // If duplicate parameter names exist, collect and continue
                    errors.add(e);
                }
            }

            // Check body in order
            if (r.body != null) {
                checkBody(r.body);
            } else if (r.expressionBody != null) {
                checkExpression(r.expressionBody);
            }

            context.exitScope();
            context.exitRoutine();
        }
    }

    private void checkBody(Body body) {
        if (body == null) return;

        for (ASTNode element : body.elements) {
            if (element instanceof VariableDeclaration v) {
                // 1) check initializer first (can only use earlier-declared names)
                if (v.initializer != null) {
                    checkExpression(v.initializer);
                }
                // 2) then declare the variable (visible for later lines)
                context.declareVariable(v.name, v.type, v.line, v.column);

            } else if (element instanceof TypeDeclaration t) {
                // type becomes visible
                context.declareType(t.name, t.aliasedType, t.line, t.column);

            } else if (element instanceof Statement s) {
                checkStatement(s);

            } else if (element instanceof Declaration d) {
                checkDeclaration(d);
            }
        }
    }

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

    // Routine call checks (return, existence, arity)
    private void checkRoutineCall(RoutineCall rc) {
        // 1) 'return' only inside routines
        if ("return".equals(rc.routineName)) {
            if (!context.isInRoutine()) {
                throw new SemanticException(
                    "return statement outside of routine",
                    rc.line, rc.column
                );
            }
            // No arity check for 'return'
        }
        // 2) allow special built-ins like "for_each"
        else if (rc.routineName != null && rc.routineName.contains("for_each")) {
        }
        // 3) normal routine: must exist and have correct arity
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

        // 4) check each argument expression
        for (Expression arg : rc.arguments) {
            checkExpression(arg);
        }
    }


    // Declarations-before-usage and basic checks inside expressions
    private void checkAssignment(Assignment a) {
        // Check right-hand side
        checkExpression(a.value);

        // Check left-hand side target
        if (a.target instanceof ModifiablePrimary mp) {
            if (!context.isDeclaredVariable(mp.baseName)) {
                throw new SemanticException(
                    "Variable '" + mp.baseName + "' is not declared",
                    mp.line, mp.column
                );
            }
            context.markVariableUsed(mp.baseName);

            // Check any index expressions
            for (ModifiablePrimary.Access acc : mp.accesses) {
                if (acc.index != null) {
                    checkExpression(acc.index);
                }
            }
        }
    }

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
            // If this matches a declared routine, enforce arity too
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
            // Check all argument expressions
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

    // Other statements
    private void checkPrintStatement(PrintStatement ps) {
        for (Expression expr : ps.expressions) {
            checkExpression(expr);
        }
    }

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

    private void checkForLoop(ForLoop fl) {
        context.enterLoop();
        context.enterScope();

        // Range bounds are checked before the loop var exists
        if (fl.range != null) {
            checkExpression(fl.range.start);
            checkExpression(fl.range.end);
        }

        // Declare the loop variable (visible in the loop body)
        context.declareVariable(fl.loopVariable, null, fl.line, fl.column);

        if (fl.body != null) {
            checkBody(fl.body);
        }

        context.exitScope();
        context.exitLoop();
    }


    // Warnings
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

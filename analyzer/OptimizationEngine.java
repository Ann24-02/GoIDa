package analyzer;

import ast.*;
import java.util.*;
import parser.Token;

/**
 * OptimizationEngine - does AST optimizations.
 *
 * Optimizations (modifying):
 * 1. Constant Expression Simplification — e.g., 5 + 3 -> 8, 3 < 5 -> true
 * 2. Dead Code Elimination — remove code after 'return'
 * 3. If Simplification — simplify 'if (true/false)'
 */
public class OptimizationEngine {

    private int optimizationCount = 0;

    public OptimizationEngine() {}

    public Program optimize(Program program) {
        optimizationCount = 0;
        List<Declaration> optimized = new ArrayList<>();

        for (Declaration decl : program.declarations) {
            Declaration opt = optimizeDeclaration(decl);
            if (opt != null) {
                optimized.add(opt);
            }
        }

        System.out.println("  [Optimizer] Applied " + optimizationCount + " optimizations");
        return new Program(optimized, program.line, program.column);
    }

    // Declaration Optimization
    private Declaration optimizeDeclaration(Declaration decl) {
        if (decl instanceof VariableDeclaration v) {
            if (v.initializer != null) {
                Expression opt = optimizeExpression(v.initializer);
                if (opt != v.initializer) {
                    return new VariableDeclaration(v.name, v.type, opt, v.line, v.column);
                }
            }
            return decl;
        }
        else if (decl instanceof RoutineDeclaration r) {
            if (r.body != null) {
                Body optBody = optimizeBody(r.body);
                if (optBody != r.body) {
                    return new RoutineDeclaration(r.name, r.parameters, r.returnType,
                            optBody, r.expressionBody, r.line, r.column);
                }
            } else if (r.expressionBody != null) {
                Expression optExpr = optimizeExpression(r.expressionBody);
                if (optExpr != r.expressionBody) {
                    return new RoutineDeclaration(r.name, r.parameters, r.returnType,
                            r.body, optExpr, r.line, r.column);
                }
            }
            return decl;
        }
        else if (decl instanceof TypeDeclaration t) {
            // Types are not optimized
            return decl;
        }

        return decl;
    }

    // Body Optimization
    private Body optimizeBody(Body body) {
    if (body == null) return null; // как и раньше
    List<ASTNode> elements = body.elements;
    List<ASTNode> optimized = new ArrayList<>();
    boolean changed = false;

    for (int i = 0; i < elements.size(); i++) {
        ASTNode elem = elements.get(i);
        ASTNode optElem;
        if (elem instanceof Declaration d) {
            optElem = optimizeDeclaration(d);
        } else if (elem instanceof Statement s) {
            optElem = optimizeStatement(s);
        } else {
            optElem = elem;
        }

        if (optElem != elem) changed = true;       // заметить замену
        if (optElem != null) optimized.add(optElem);
        else changed = true;                        // узел удалён (например, if(false))

        // DCE: всё после return — недостижимо
        if (optElem instanceof RoutineCall rc && "return".equals(rc.routineName)) {
            int removed = elements.size() - i - 1;
            if (removed > 0) {
                changed = true;
                System.out.println("  [Opt] Dead code elimination: removed " + removed + " statements");
            }
            break;
        }
    }

    if (!changed) return body;                     // ничего не поменялось
    return new Body(optimized, body.line, body.column); // вернуть новый Body
    }

    // Statement Optimization
    private Statement optimizeStatement(Statement stmt) {
        if (stmt instanceof Assignment a) {
            Expression optValue = optimizeExpression(a.value);
            if (optValue != a.value) {
                return new Assignment(a.target, optValue, a.line, a.column);
            }
            return stmt;
        }
        else if (stmt instanceof RoutineCall rc) {
            return stmt; // nothing to do for plain calls
        }
        else if (stmt instanceof PrintStatement ps) {
            List<Expression> optimized = new ArrayList<>();
            boolean changed = false;
            for (Expression expr : ps.expressions) {
                Expression opt = optimizeExpression(expr);
                optimized.add(opt);
                if (opt != expr) changed = true;
            }
            if (changed) {
                return new PrintStatement(optimized, ps.line, ps.column);
            }
            return stmt;
        }
        else if (stmt instanceof IfStatement ifs) {
            return optimizeIfStatement(ifs);
        }
        else if (stmt instanceof WhileLoop wl) {
            Expression optCond = optimizeExpression(wl.condition);
            Body optBody = optimizeBody(wl.body);
            if (optCond != wl.condition || optBody != wl.body) {
                return new WhileLoop(optCond, optBody, wl.line, wl.column);
            }
            return stmt;
        }
        else if (stmt instanceof ForLoop fl) {
            Expression optStart = optimizeExpression(fl.range.start);
            Expression optEnd = optimizeExpression(fl.range.end);
            Body optBody = optimizeBody(fl.body);
            if (optStart != fl.range.start || optEnd != fl.range.end || optBody != fl.body) {
                Range newRange = new Range(optStart, optEnd, fl.range.line, fl.range.column);
                return new ForLoop(fl.loopVariable, newRange, fl.reverse, optBody, fl.line, fl.column);
            }
            return stmt;
        }

        return stmt;
    }

    // If Simplification
    private Statement optimizeIfStatement(IfStatement ifs) {
        Expression optCond = optimizeExpression(ifs.condition);
        Body optThen = optimizeBody(ifs.thenBranch);
        Body optElse = ifs.elseBranch != null ? optimizeBody(ifs.elseBranch) : null;

        // If the condition is a boolean constant, simplify
        if (optCond instanceof BooleanLiteral bl) {
            optimizationCount++;
            if (bl.value) {
                // if (true) keep only 'then'
                System.out.println("  [Opt] If simplification: if(true) - removed else branch");
                return new IfStatement(optCond, optThen, null, ifs.line, ifs.column);
            } else {
                // if (false) keep only 'else' (if it exists), or remove whole if
                if (optElse != null) {
                    System.out.println("  [Opt] If simplification: if(false) - using else branch");
                    return new IfStatement(optCond, optElse, null, ifs.line, ifs.column);
                } else {
                    System.out.println("  [Opt] If simplification: if(false) with no else - removed");
                    // return null to remove the whole 'if' statement
                    return null;
                }
            }
        }

        if (optCond != ifs.condition || optThen != ifs.thenBranch || optElse != ifs.elseBranch) {
            return new IfStatement(optCond, optThen, optElse, ifs.line, ifs.column);
        }

        return ifs;
    }

    // Expression Optimization
    private Expression optimizeExpression(Expression expr) {
        if (expr == null) return null;

        if (expr instanceof BinaryExpression be) {
            return optimizeBinaryExpression(be);
        }
        else if (expr instanceof UnaryExpression ue) {
            return optimizeUnaryExpression(ue);
        }
        else if (expr instanceof FunctionCall fc) {
            List<Expression> optimized = new ArrayList<>();
            boolean changed = false;
            for (Expression arg : fc.arguments) {
                Expression opt = optimizeExpression(arg);
                optimized.add(opt);
                if (opt != arg) changed = true;
            }
            if (changed) {
                return new FunctionCall(fc.functionName, optimized, fc.line, fc.column);
            }
            return fc;
        }
        else if (expr instanceof ModifiablePrimary mp) {
            // Optimize index expressions inside the chain
            List<ModifiablePrimary.Access> optimized = new ArrayList<>();
            boolean changed = false;
            for (ModifiablePrimary.Access acc : mp.accesses) {
                if (acc.index != null) {
                    Expression opt = optimizeExpression(acc.index);
                    if (opt != acc.index) changed = true;
                    optimized.add(new ModifiablePrimary.Access(opt, acc.line, acc.column));
                } else {
                    optimized.add(acc);
                }
            }
            if (changed) {
                return new ModifiablePrimary(mp.baseName, optimized, mp.line, mp.column);
            }
            return mp;
        }

        // Literals and Identifiers are left as is
        return expr;
    }

    // Constant Expression Simplification
    private Expression optimizeBinaryExpression(BinaryExpression be) {
        Expression left = optimizeExpression(be.left);
        Expression right = optimizeExpression(be.right);

        // If both sides are constants, try to compute the result now
        if (isConstant(left) && isConstant(right)) {
            Object leftVal = evaluateConstant(left);
            Object rightVal = evaluateConstant(right);

            if (leftVal != null && rightVal != null) {
                Object result = evaluateBinaryOp(leftVal, rightVal, be.operator);
                if (result != null) {
                    optimizationCount++;

                    if (result instanceof Integer) {
                        System.out.println("  [Opt] Constant simplification: " + be.operator + " result = " + result);
                        return new IntegerLiteral((Integer) result, be.line, be.column);
                    } else if (result instanceof Double) {
                        return new RealLiteral((Double) result, be.line, be.column);
                    } else if (result instanceof Boolean) {
                        return new BooleanLiteral((Boolean) result, be.line, be.column);
                    } else if (result instanceof String) {
                        return new StringLiteral((String) result, be.line, be.column);
                    }
                }
            }
        }

        if (left != be.left || right != be.right) {
            return new BinaryExpression(left, be.operator, right, be.line, be.column);
        }

        return be;
    }

    // Unary optimization
    private Expression optimizeUnaryExpression(UnaryExpression ue) {
        Expression operand = optimizeExpression(ue.operand);

        if (ue.operator == Token.Type.MINUS && operand instanceof UnaryExpression uo) {
            if (uo.operator == Token.Type.MINUS) {
                optimizationCount++;
                System.out.println("  [Opt] Unary simplification: -(-x) -> x");
                return uo.operand;
            }
        }

        // !true -> false, !false -> true
        if (ue.operator == Token.Type.NOT && operand instanceof BooleanLiteral bl) {
            optimizationCount++;
            return new BooleanLiteral(!bl.value, ue.line, ue.column);
        }

        if (operand != ue.operand) {
            return new UnaryExpression(ue.operator, operand, ue.line, ue.column);
        }

        return ue;
    }

    // Helpers for constant evaluation
    private boolean isConstant(Expression expr) {
        return expr instanceof IntegerLiteral ||
               expr instanceof RealLiteral ||
               expr instanceof BooleanLiteral ||
               expr instanceof StringLiteral;
    }

    private Object evaluateConstant(Expression expr) {
        if (expr instanceof IntegerLiteral il) return il.value;
        if (expr instanceof RealLiteral rl) return rl.value;
        if (expr instanceof BooleanLiteral bl) return bl.value;
        if (expr instanceof StringLiteral sl) return sl.value;
        return null;
    }

    private Object evaluateBinaryOp(Object left, Object right, Token.Type op) {
        // Integer ops
        if (left instanceof Integer && right instanceof Integer) {
            int l = (Integer) left;
            int r = (Integer) right;

            return switch (op) {
                case PLUS -> l + r;
                case MINUS -> l - r;
                case MULTIPLY -> l * r;
                case DIVIDE -> r != 0 ? l / r : null;
                case MODULO -> r != 0 ? l % r : null;
                case LESS -> l < r;
                case LESS_EQUAL -> l <= r;
                case GREATER -> l > r;
                case GREATER_EQUAL -> l >= r;
                case EQUALS -> l == r;
                case NOT_EQUALS -> l != r;
                case AND -> l != 0 && r != 0;
                case OR -> l != 0 || r != 0;
                default -> null;
            };
        }

        // Boolean ops
        if (left instanceof Boolean && right instanceof Boolean) {
            boolean l = (Boolean) left;
            boolean r = (Boolean) right;

            return switch (op) {
                case AND -> l && r;
                case OR -> l || r;
                case EQUALS -> l == r;
                case NOT_EQUALS -> l != r;
                default -> null;
            };
        }

        // Double (real) ops, also support mixing int and double
        if ((left instanceof Double || left instanceof Integer) &&
            (right instanceof Double || right instanceof Integer)) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();

            return switch (op) {
                case PLUS -> l + r;
                case MINUS -> l - r;
                case MULTIPLY -> l * r;
                case DIVIDE -> r != 0 ? l / r : null;
                case LESS -> l < r;
                case LESS_EQUAL -> l <= r;
                case GREATER -> l > r;
                case GREATER_EQUAL -> l >= r;
                case EQUALS -> l == r;
                case NOT_EQUALS -> l != r;
                default -> null;
            };
        }

        return null;
    }

    public int getOptimizationCount() {
        return optimizationCount;
    }
}

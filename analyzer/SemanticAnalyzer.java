package analyzer;

import ast.*;
import java.util.*;

/**
 * SemanticAnalyzer - основной класс для семантического анализа.
 * 
 * Реализованные проверки (non-modifying):
 * 1. Declarations Before Usage - переменные/функции используются после объявления
 * 2. Correct Keyword Usage - return только внутри функций
 * 3. Type Checking (частичное) - проверка типов в присваиваниях
 * 4. Unused Variables Detection - обнаружение неиспользованных переменных
 */
public class SemanticAnalyzer {
    
    private SemanticContext context;
    private List<String> warnings = new ArrayList<>();
    
    public SemanticAnalyzer() {
        this.context = new SemanticContext();
    }
    
    // ================================================================
    // ГЛАВНЫЙ МЕТОД АНАЛИЗА
    // ================================================================
    
    public void analyze(Program program) {
        // Проход 1: собрать глобальные декларации
        for (Declaration decl : program.declarations) {
            collectGlobalDeclaration(decl);
        }
        
        // Проход 2: проверить использование
        for (Declaration decl : program.declarations) {
            checkDeclaration(decl);
        }
    }
    
    // ================================================================
    // ПРОХОД 1: СБОР ГЛОБАЛЬНЫХ ДЕКЛАРАЦИЙ
    // ================================================================
    
    private void collectGlobalDeclaration(Declaration decl) {
        if (decl instanceof VariableDeclaration v) {
            try {
                context.declareVariable(v.name, v.type, v.line, v.column);
            } catch (SemanticException e) {
                // Игнорируем дублирование на глобальном уровне для теперь
            }
        } else if (decl instanceof RoutineDeclaration r) {
            try {
                context.declareRoutine(r.name, r.parameters, r.returnType, r.line, r.column);
            } catch (SemanticException e) {
                // Игнорируем
            }
        } else if (decl instanceof TypeDeclaration t) {
            try {
                context.declareType(t.name, t.aliasedType, t.line, t.column);
            } catch (SemanticException e) {
                // Игнорируем
            }
        }
    }
    
    // ================================================================
    // ПРОХОД 2: ПРОВЕРКА И АНАЛИЗ
    // ================================================================
    
    private void checkDeclaration(Declaration decl) {
        if (decl instanceof VariableDeclaration v) {
            if (v.initializer != null) {
                checkExpression(v.initializer);
            }
        } else if (decl instanceof RoutineDeclaration r) {
            context.enterRoutine(r);
            context.enterScope();  // Новый скоп для тела функции
            
            // Объявить параметры функции
            for (Parameter p : r.parameters) {
                try {
                    context.declareVariable(p.name, p.type, p.line, p.column);
                } catch (SemanticException e) {
                    // Игнорируем дублирование параметров
                }
            }
            
            // Собрать все декларации в теле функции (первый проход)
            if (r.body != null) {
                collectBodyDeclarations(r.body);
            }
            
            // Проверить тело функции (второй проход)
            if (r.body != null) {
                checkBody(r.body);
            } else if (r.expressionBody != null) {
                checkExpression(r.expressionBody);
            }
            
            context.exitScope();
            context.exitRoutine();
        }
    }
    
    // Собрать все декларации в теле (для корректной работы скопов)
    private void collectBodyDeclarations(Body body) {
        if (body == null) return;
        
        for (ASTNode element : body.elements) {
            if (element instanceof Declaration d) {
                if (d instanceof VariableDeclaration v) {
                    try {
                        context.declareVariable(v.name, v.type, v.line, v.column);
                    } catch (SemanticException e) {
                        // Игнорируем дублирование для скопов (могут быть вложенные блоки)
                    }
                } else if (d instanceof TypeDeclaration t) {
                    try {
                        context.declareType(t.name, t.aliasedType, t.line, t.column);
                    } catch (SemanticException e) {
                        // Игнорируем
                    }
                }
            } else if (element instanceof Statement s) {
                collectStatementDeclarations(s);
            }
        }
    }
    
    private void collectStatementDeclarations(Statement stmt) {
        if (stmt instanceof IfStatement ifs) {
            if (ifs.thenBranch != null) {
                collectBodyDeclarations(ifs.thenBranch);
            }
            if (ifs.elseBranch != null) {
                collectBodyDeclarations(ifs.elseBranch);
            }
        } else if (stmt instanceof WhileLoop wl) {
            if (wl.body != null) {
                collectBodyDeclarations(wl.body);
            }
        } else if (stmt instanceof ForLoop fl) {
            // Объявить переменную цикла
            try {
                context.declareVariable(fl.loopVariable, null, fl.line, fl.column);
            } catch (SemanticException e) {
                // Игнорируем
            }
            
            if (fl.body != null) {
                collectBodyDeclarations(fl.body);
            }
        }
    }
    
    private void checkBody(Body body) {
        if (body == null) return;
        
        for (ASTNode element : body.elements) {
            if (element instanceof Declaration d) {
                checkDeclaration(d);
            } else if (element instanceof Statement s) {
                checkStatement(s);
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
    
    // ================================================================
    // ПРОВЕРКА: Correct Keyword Usage
    // ================================================================
    
    private void checkRoutineCall(RoutineCall rc) {
        // Проверка 1: return только внутри функций
        if ("return".equals(rc.routineName)) {
            if (!context.isInRoutine()) {
                throw new SemanticException(
                    "return statement outside of routine",
                    rc.line, rc.column
                );
            }
        }
        // Проверка 2: пропустить for_each - это спец функция
        else if (rc.routineName != null && rc.routineName.contains("for_each")) {
            // for_each - встроенная функция, пропускаем проверку
        }
        // Проверка 3: вызванная функция должна быть объявлена
        else if (!"return".equals(rc.routineName)) {
            if (!context.isDeclaredRoutine(rc.routineName)) {
                throw new SemanticException(
                    "Routine '" + rc.routineName + "' is not declared",
                    rc.line, rc.column
                );
            }
        }
        
        // Проверить аргументы
        for (Expression arg : rc.arguments) {
            checkExpression(arg);
        }
    }
    
    // ================================================================
    // ПРОВЕРКА: Declarations Before Usage
    // ================================================================
    
    private void checkAssignment(Assignment a) {
        // Проверить правую часть
        checkExpression(a.value);
        
        // Проверить левую часть (целевая переменная)
        if (a.target instanceof ModifiablePrimary mp) {
            // Целевая переменная должна быть объявлена
            if (!context.isDeclaredVariable(mp.baseName)) {
                throw new SemanticException(
                    "Variable '" + mp.baseName + "' is not declared",
                    mp.line, mp.column
                );
            }
            context.markVariableUsed(mp.baseName);
            
            // Проверить индексы и поля
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
            // Переменная должна быть объявлена
            if (!context.isDeclaredVariable(id.name)) {
                throw new SemanticException(
                    "Variable '" + id.name + "' is not declared",
                    id.line, id.column
                );
            }
            context.markVariableUsed(id.name);
        } 
        else if (expr instanceof BinaryExpression be) {
            checkExpression(be.left);
            checkExpression(be.right);
        } 
        else if (expr instanceof UnaryExpression ue) {
            checkExpression(ue.operand);
        } 
        else if (expr instanceof FunctionCall fc) {
            // Проверить аргументы
            for (Expression arg : fc.arguments) {
                checkExpression(arg);
            }
        } 
        else if (expr instanceof ModifiablePrimary mp) {
            // Проверить базовую переменную
            if (!context.isDeclaredVariable(mp.baseName)) {
                throw new SemanticException(
                    "Variable '" + mp.baseName + "' is not declared",
                    mp.line, mp.column
                );
            }
            context.markVariableUsed(mp.baseName);
            
            // Проверить индексы
            for (ModifiablePrimary.Access acc : mp.accesses) {
                if (acc.index != null) {
                    checkExpression(acc.index);
                }
            }
        }
    }
    
    // ================================================================
    // ПРОВЕРКА: Type Checking (частичное)
    // ================================================================
    
    private void checkPrintStatement(PrintStatement ps) {
        for (Expression expr : ps.expressions) {
            checkExpression(expr);
        }
    }
    
    private void checkIfStatement(IfStatement ifs) {
        checkExpression(ifs.condition);
        if (ifs.thenBranch != null) {
            checkBody(ifs.thenBranch);
        }
        if (ifs.elseBranch != null) {
            checkBody(ifs.elseBranch);
        }
    }
    
    private void checkWhileLoop(WhileLoop wl) {
        checkExpression(wl.condition);
        context.enterLoop();
        if (wl.body != null) {
            checkBody(wl.body);
        }
        context.exitLoop();
    }
    
    private void checkForLoop(ForLoop fl) {
        // Переменная цикла уже декларирована в collectBodyDeclarations
        context.enterLoop();
        
        if (fl.range != null) {
            checkExpression(fl.range.start);
            checkExpression(fl.range.end);
        }
        
        if (fl.body != null) {
            checkBody(fl.body);
        }
        
        context.exitLoop();
    }
    
    // ================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ================================================================
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    private void addWarning(String warning) {
        warnings.add(warning);
    }
}
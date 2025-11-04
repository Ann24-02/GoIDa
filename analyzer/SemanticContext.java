package analyzer;

import ast.*;
import java.util.*;

/**
 * SemanticContext - управляет символьными таблицами и контекстом анализа.
 * Отслеживает декларации переменных, функций, типов и текущий скоп.
 */
public class SemanticContext {
    
    // Информация о переменной
    public static class VarInfo {
        public final String name;
        public final Type type;
        public final int line, column;
        public boolean used = false;  // используется ли
        
        public VarInfo(String name, Type type, int line, int column) {
            this.name = name;
            this.type = type;
            this.line = line;
            this.column = column;
        }
    }
    
    // Информация о функции
    public static class RoutineInfo {
        public final String name;
        public final List<Parameter> params;
        public final Type returnType;
        public final int line, column;
        
        public RoutineInfo(String name, List<Parameter> params, Type returnType, int line, int column) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.line = line;
            this.column = column;
        }
    }
    
    // Стек скопов (для функций и блоков)
    private final Deque<Map<String, VarInfo>> scopes = new ArrayDeque<>();
    
    // Глобальные декларации (функции и типы)
    private final Map<String, RoutineInfo> routines = new HashMap<>();
    private final Map<String, Type> types = new HashMap<>();
    
    // Текущий контекст
    private RoutineDeclaration currentRoutine = null;
    private boolean inLoop = false;
    
    public SemanticContext() {
        // Глобальный скоп
        scopes.push(new HashMap<>());
    }
    
    // ================================================================
    // Управление скопом
    // ================================================================
    
    public void enterScope() {
        scopes.push(new HashMap<>());
    }
    
    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
    }
    
    public void enterRoutine(RoutineDeclaration routine) {
        currentRoutine = routine;
        enterScope();
    }
    
    public void exitRoutine() {
        currentRoutine = null;
        exitScope();
    }
    
    public void enterLoop() {
        inLoop = true;
    }
    
    public void exitLoop() {
        inLoop = false;
    }
    
    public boolean isInLoop() {
        return inLoop;
    }
    
    public boolean isInRoutine() {
        return currentRoutine != null;
    }
    
    public RoutineDeclaration getCurrentRoutine() {
        return currentRoutine;
    }
    
    // ================================================================
    // Декларации переменных
    // ================================================================
    
    public void declareVariable(String name, Type type, int line, int column) {
        Map<String, VarInfo> current = scopes.peek();
        if (current.containsKey(name)) {
            throw new SemanticException(
                "Variable '" + name + "' is already declared in this scope",
                line, column
            );
        }
        current.put(name, new VarInfo(name, type, line, column));
    }
    
    public boolean isDeclaredVariable(String name) {
        // Ищем в текущем скопе и родительских
        for (Map<String, VarInfo> scope : scopes) {
            if (scope.containsKey(name)) {
                return true;
            }
        }
        return false;
    }
    
    public VarInfo getVariableInfo(String name) {
        for (Map<String, VarInfo> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }
    
    public Type getVariableType(String name) {
        VarInfo info = getVariableInfo(name);
        return info != null ? info.type : null;
    }
    
    public void markVariableUsed(String name) {
        VarInfo info = getVariableInfo(name);
        if (info != null) {
            info.used = true;
        }
    }
    
    // ================================================================
    // Декларации функций
    // ================================================================
    
    public void declareRoutine(String name, List<Parameter> params, Type returnType, int line, int column) {
        if (routines.containsKey(name)) {
            throw new SemanticException(
                "Routine '" + name + "' is already declared",
                line, column
            );
        }
        routines.put(name, new RoutineInfo(name, params, returnType, line, column));
    }
    
    public boolean isDeclaredRoutine(String name) {
        return routines.containsKey(name);
    }
    
    public RoutineInfo getRoutineInfo(String name) {
        return routines.get(name);
    }
    
    // ================================================================
    // Декларации типов
    // ================================================================
    
    public void declareType(String name, Type type, int line, int column) {
        if (types.containsKey(name)) {
            throw new SemanticException(
                "Type '" + name + "' is already declared",
                line, column
            );
        }
        types.put(name, type);
    }
    
    public boolean isDeclaredType(String name) {
        return types.containsKey(name);
    }
    
    public Type getType(String name) {
        return types.get(name);
    }
    
    // ================================================================
    // Сбор неиспользованных переменных
    // ================================================================
    
    public List<VarInfo> getUnusedVariables() {
        List<VarInfo> unused = new ArrayList<>();
        Map<String, VarInfo> current = scopes.peek();
        for (VarInfo info : current.values()) {
            if (!info.used) {
                unused.add(info);
            }
        }
        return unused;
    }
}

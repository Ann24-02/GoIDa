package analyzer;

import ast.*;
import java.util.*;

/**
 * SemanticContext - manages symbol tables and analysis context.
 * Tracks declarations of variables, routines, types, and the current scope.
 */
public class SemanticContext {
    
    // Information about a variable
    public static class VarInfo {
        public final String name;
        public final Type type;
        public final int line, column;
        public boolean used = false;  // whether it was used
        
        public VarInfo(String name, Type type, int line, int column) {
            this.name = name;
            this.type = type;
            this.line = line;
            this.column = column;
        }
    }
    
    // Information about a routine (function/procedure)
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
    
    // Stack of scopes (for routines and blocks)
    private final Deque<Map<String, VarInfo>> scopes = new ArrayDeque<>();
    
    // Stack of type scopes to support shadowing of type names
    private final Deque<Map<String, Type>> typeScopes = new ArrayDeque<>();

    // Global declarations (routines)
    private final Map<String, RoutineInfo> routines = new HashMap<>();
    
    // Current context
    private RoutineDeclaration currentRoutine = null;
    private boolean inLoop = false;
    
    public SemanticContext() {
        // Global scope
        scopes.push(new HashMap<>());
        // Global type scope
        typeScopes.push(new HashMap<>());
    }
    
    // Scope management
    public void enterScope() {
        scopes.push(new HashMap<>());
        typeScopes.push(new HashMap<>());
    }
    
    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
        if (typeScopes.size() > 1) {
            typeScopes.pop();
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
    
    
    // Variable management
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
        // Search in the current scope and all parent scopes
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
    
    // Routine declarations
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
    
    // Type declarations
    public void declareType(String name, Type type, int line, int column) {
        Map<String, Type> current = typeScopes.peek();
        if (current.containsKey(name)) {
            throw new SemanticException(
                "Type '" + name + "' is already declared",
                line, column
            );
        }
        current.put(name, type);
    }
    
    public boolean isDeclaredType(String name) {
        for (Map<String, Type> scope : typeScopes) {
            if (scope.containsKey(name)) return true;
        }
        return false;
    }
    
    public Type getType(String name) {
        for (Map<String, Type> scope : typeScopes) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }
    
    // Collect unused variables in the current scope
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

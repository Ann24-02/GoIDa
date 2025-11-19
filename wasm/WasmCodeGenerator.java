package wasm;

import analyzer.SemanticContext;
import ast.*;
import java.util.*;
import java.io.*;

/**
 * WebAssembly Code Generator
 * Converts AST to WebAssembly Text Format (WAT)
 */
public class WasmCodeGenerator {
    private final StringBuilder code;
    private final SemanticContext semanticContext;
    private int tempVarCounter = 0;
    private int stringCounter = 0;
    private final Map<String, Integer> globalVars = new HashMap<>();
    private final Map<String, Integer> localVars = new HashMap<>();
    private final Map<String, String> stringLiterals = new HashMap<>();
    private final List<String> functions = new ArrayList<>();
    private int localVarOffset = 0;
    
    // WASM type mappings
    private static final Map<String, String> TYPE_MAPPING = Map.of(
        "integer", "i32",
        "real", "f64", 
        "boolean", "i32",
        "string", "i32"  // pointer to string in memory
    );
    
    public WasmCodeGenerator(SemanticContext context) {
        this.code = new StringBuilder();
        this.semanticContext = context;
    }
    
    public String generate(Program program) {
        code.setLength(0);
        tempVarCounter = 0;
        stringCounter = 0;
        globalVars.clear();
        localVars.clear();
        stringLiterals.clear();
        functions.clear();
        localVarOffset = 0;
        
        // Generate module header
        code.append("(module\n");
        
        // Import necessary functions from JavaScript
        generateImports();
        
        // Define memory (64KB initial)
        code.append("  (memory $memory 1)\n");
        code.append("  (export \"memory\" (memory $memory))\n\n");
        
        // Define global variables
        generateGlobals(program);
        
        // Define string literals in memory
        generateStringLiterals(program);
        
        // Generate all routines/functions
        for (Declaration decl : program.declarations) {
            if (decl instanceof RoutineDeclaration) {
                generateFunction((RoutineDeclaration) decl);
            }
        }
        
        // Export main function if it exists
        if (functions.contains("main")) {
            code.append("  (export \"main\" (func $main))\n");
        }
        
        code.append(")\n");
        return code.toString();
    }
    
    private void generateImports() {
        // Import console.log for printing
        code.append("  (import \"env\" \"printInt\" (func $printInt (param i32)))\n");
        code.append("  (import \"env\" \"printFloat\" (func $printFloat (param f64)))\n");
        code.append("  (import \"env\" \"printString\" (func $printString (param i32)))\n");
        code.append("  (import \"env\" \"printBool\" (func $printBool (param i32)))\n");
        code.append("  (import \"env\" \"printNewline\" (func $printNewline))\n");
    }
    
    private void generateGlobals(Program program) {
        // Collect global variables
        for (Declaration decl : program.declarations) {
            if (decl instanceof VariableDeclaration vd) {
                String wasmType = mapType(vd.type);
                globalVars.put(vd.name, globalVars.size());
                
                code.append("  (global $").append(vd.name).append(" (mut ").append(wasmType).append(") (")
                   .append(wasmType).append(".const ");
                
                // Initialize with value if provided
                if (vd.initializer != null && vd.initializer instanceof IntegerLiteral il) {
                    code.append(il.value);
                } else if (vd.initializer != null && vd.initializer instanceof BooleanLiteral bl) {
                    code.append(bl.value ? "1" : "0");
                } else {
                    code.append("0"); // default value
                }
                code.append("))\n");
            }
        }
        if (!globalVars.isEmpty()) {
            code.append("\n");
        }
    }
    
    private void generateStringLiterals(Program program) {
        // We'll store strings at the beginning of memory
        int currentOffset = 0;
        
        // Collect all string literals from the program
        collectStringLiterals(program);
        
        if (!stringLiterals.isEmpty()) {
            code.append("  ;; String literals\n");
            
            for (var entry : stringLiterals.entrySet()) {
                String str = entry.getKey();
                String label = entry.getValue();
                code.append("  (data (i32.const ").append(currentOffset).append(") \\\"")
                   .append(escapeString(str)).append("\\\\00\\\")\n");
                currentOffset += str.length() + 1; // +1 for null terminator
            }
            code.append("\n");
        }
    }
    
    private void collectStringLiterals(ASTNode node) {
        if (node == null) return;
        
        if (node instanceof StringLiteral sl) {
            if (!stringLiterals.containsKey(sl.value)) {
                String label = "str_" + stringCounter++;
                stringLiterals.put(sl.value, label);
            }
        }
        else if (node instanceof Program p) {
            for (Declaration d : p.declarations) collectStringLiterals(d);
        }
        else if (node instanceof RoutineDeclaration r) {
            if (r.body != null) collectStringLiterals(r.body);
            if (r.expressionBody != null) collectStringLiterals(r.expressionBody);
        }
        else if (node instanceof Body b) {
            for (ASTNode elem : b.elements) collectStringLiterals(elem);
        }
        else if (node instanceof PrintStatement ps) {
            for (Expression expr : ps.expressions) collectStringLiterals(expr);
        }
        else if (node instanceof BinaryExpression be) {
            collectStringLiterals(be.left);
            collectStringLiterals(be.right);
        }
        else if (node instanceof UnaryExpression ue) {
            collectStringLiterals(ue.operand);
        }
        else if (node instanceof FunctionCall fc) {
            for (Expression arg : fc.arguments) collectStringLiterals(arg);
        }
    }
    
    private void generateFunction(RoutineDeclaration routine) {
        functions.add(routine.name);
        localVars.clear();
        localVarOffset = 0;
        
        code.append("  (func $").append(routine.name);
        
        // Parameters
        for (Parameter param : routine.parameters) {
            code.append(" (param $").append(param.name).append(" ")
               .append(mapType(param.type)).append(")");
            localVars.put(param.name, localVarOffset++);
        }
        code.append("\n");
        
        // Return type
        if (routine.returnType != null) {
            code.append("    (result ").append(mapType(routine.returnType)).append(")\n");
        } else {
            code.append("\n");
        }
        
        // Local variables declaration
        if (routine.body != null) {
            declareLocalVariables(routine.body);
        }
        
        // Function body
        if (routine.body != null) {
            generateBody(routine.body);
        } else if (routine.expressionBody != null) {
            generateExpression(routine.expressionBody);
            if (routine.returnType != null) {
                code.append("    return\n");
            }
        } else {
            // Default return for void functions
            code.append("    return\n");
        }
        
        code.append("  )\n\n");
    }
    
    private void declareLocalVariables(Body body) {
        for (ASTNode element : body.elements) {
            if (element instanceof VariableDeclaration vd) {
                if (!localVars.containsKey(vd.name) && !globalVars.containsKey(vd.name)) {
                    localVars.put(vd.name, localVarOffset++);
                    code.append("    (local $").append(vd.name).append(" ").append(mapType(vd.type)).append(")\n");
                }
            }
        }
    }
    
    private void generateBody(Body body) {
        for (ASTNode element : body.elements) {
            if (element instanceof Statement stmt) {
                generateStatement(stmt);
            } else if (element instanceof VariableDeclaration vd) {
                generateVariableDeclaration(vd);
            }
        }
    }
    
    private void generateStatement(Statement stmt) {
        if (stmt instanceof Assignment a) {
            generateAssignment(a);
        } else if (stmt instanceof PrintStatement ps) {
            generatePrintStatement(ps);
        } else if (stmt instanceof IfStatement ifs) {
            generateIfStatement(ifs);
        } else if (stmt instanceof WhileLoop wl) {
            generateWhileLoop(wl);
        } else if (stmt instanceof ForLoop fl) {
            generateForLoop(fl);
        } else if (stmt instanceof RoutineCall rc) {
            generateRoutineCall(rc);
        }
    }
    
    private void generateAssignment(Assignment assignment) {
        generateExpression(assignment.value);
        
        if (assignment.target instanceof ModifiablePrimary mp) {
            String varName = mp.baseName;
            
            // Store in global or local variable
            if (globalVars.containsKey(varName)) {
                code.append("    global.set $").append(varName).append("\n");
            } else if (localVars.containsKey(varName)) {
                code.append("    local.set $").append(varName).append("\n");
            }
        }
    }
    
    private void generatePrintStatement(PrintStatement ps) {
        for (Expression expr : ps.expressions) {
            generateExpression(expr);
            
            // Determine type and call appropriate print function
            String type = inferType(expr);
            switch (type) {
                case "i32" -> code.append("    call $printInt\n");
                case "f64" -> code.append("    call $printFloat\n");
                case "string" -> code.append("    call $printString\n");
                case "boolean" -> code.append("    call $printBool\n");
            }
        }
        code.append("    call $printNewline\n");
    }
    
    private void generateIfStatement(IfStatement ifs) {
        generateExpression(ifs.condition);
        
        code.append("    if\n");
        if (ifs.thenBranch != null) {
            generateBody(ifs.thenBranch);
        }
        
        if (ifs.elseBranch != null) {
            code.append("    else\n");
            generateBody(ifs.elseBranch);
        }
        
        code.append("    end\n");
    }
    
    private void generateWhileLoop(WhileLoop wl) {
        String loopLabel = "loop_" + tempVarCounter++;
        
        code.append("    block $").append(loopLabel).append("_end\n");
        code.append("    loop $").append(loopLabel).append("_start\n");
        
        generateExpression(wl.condition);
        code.append("    i32.eqz\n");
        code.append("    br_if $").append(loopLabel).append("_end\n");
        
        if (wl.body != null) {
            generateBody(wl.body);
        }
        
        code.append("    br $").append(loopLabel).append("_start\n");
        code.append("    end\n");
        code.append("    end\n");
    }
    
    private void generateForLoop(ForLoop fl) {
        // Simple implementation for numeric ranges
        String loopVar = fl.loopVariable;
        String loopLabel = "loop_" + tempVarCounter++;
        
        // Declare loop variable if not already declared
        if (!localVars.containsKey(loopVar) && !globalVars.containsKey(loopVar)) {
            localVars.put(loopVar, localVarOffset++);
            code.append("    (local $").append(loopVar).append(" i32)\n");
        }
        
        // Initialize loop variable
        generateExpression(fl.range.start);
        if (globalVars.containsKey(loopVar)) {
            code.append("    global.set $").append(loopVar).append("\n");
        } else {
            code.append("    local.set $").append(loopVar).append("\n");
        }
        
        code.append("    block $").append(loopLabel).append("_end\n");
        code.append("    loop $").append(loopLabel).append("_start\n");
        
        // Check condition
        if (globalVars.containsKey(loopVar)) {
            code.append("    global.get $").append(loopVar).append("\n");
        } else {
            code.append("    local.get $").append(loopVar).append("\n");
        }
        generateExpression(fl.range.end);
        
        if (fl.reverse) {
            code.append("    i32.lt_s\n");
        } else {
            code.append("    i32.gt_s\n");
        }
        code.append("    br_if $").append(loopLabel).append("_end\n");
        
        // Loop body
        if (fl.body != null) {
            generateBody(fl.body);
        }
        
        // Increment/Decrement
        if (globalVars.containsKey(loopVar)) {
            code.append("    global.get $").append(loopVar).append("\n");
        } else {
            code.append("    local.get $").append(loopVar).append("\n");
        }
        
        if (fl.reverse) {
            code.append("    i32.const 1\n");
            code.append("    i32.sub\n");
        } else {
            code.append("    i32.const 1\n");
            code.append("    i32.add\n");
        }
        
        if (globalVars.containsKey(loopVar)) {
            code.append("    global.set $").append(loopVar).append("\n");
        } else {
            code.append("    local.set $").append(loopVar).append("\n");
        }
        
        code.append("    br $").append(loopLabel).append("_start\n");
        code.append("    end\n");
        code.append("    end\n");
    }
    
    private void generateExpression(Expression expr) {
        if (expr instanceof IntegerLiteral il) {
            code.append("    i32.const ").append(il.value).append("\n");
        }
        else if (expr instanceof RealLiteral rl) {
            code.append("    f64.const ").append(rl.value).append("\n");
        }
        else if (expr instanceof BooleanLiteral bl) {
            code.append("    i32.const ").append(bl.value ? "1" : "0").append("\n");
        }
        else if (expr instanceof StringLiteral sl) {
            // Get string pointer from stringLiterals map
            code.append("    i32.const ").append(getStringOffset(sl.value)).append("\n");
        }
        else if (expr instanceof Identifier id) {
            // Load from global or local variable
            if (globalVars.containsKey(id.name)) {
                code.append("    global.get $").append(id.name).append("\n");
            } else if (localVars.containsKey(id.name)) {
                code.append("    local.get $").append(id.name).append("\n");
            } else {
                // Undeclared variable - use default value
                code.append("    i32.const 0\n");
            }
        }
        else if (expr instanceof BinaryExpression be) {
            generateBinaryExpression(be);
        }
        else if (expr instanceof UnaryExpression ue) {
            generateUnaryExpression(ue);
        }
        else if (expr instanceof FunctionCall fc) {
            generateFunctionCall(fc);
        }
        else if (expr instanceof ModifiablePrimary mp) {
            generateModifiablePrimary(mp);
        }
    }
    
    private void generateModifiablePrimary(ModifiablePrimary mp) {
        // For now, just handle the base variable
        if (globalVars.containsKey(mp.baseName)) {
            code.append("    global.get $").append(mp.baseName).append("\n");
        } else if (localVars.containsKey(mp.baseName)) {
            code.append("    local.get $").append(mp.baseName).append("\n");
        } else {
            code.append("    i32.const 0\n");
        }
    }
    
    private void generateBinaryExpression(BinaryExpression be) {
        generateExpression(be.left);
        generateExpression(be.right);
        
        String op = be.operator.toString().toLowerCase();
        String leftType = inferType(be.left);
        String rightType = inferType(be.right);
        
        // Use the more specific type
        String type = "f64".equals(leftType) || "f64".equals(rightType) ? "f64" : "i32";
        
        switch (op) {
            case "plus" -> code.append("    ").append(type).append(".add\n");
            case "minus" -> code.append("    ").append(type).append(".sub\n");
            case "multiply" -> code.append("    ").append(type).append(".mul\n");
            case "divide" -> {
                if ("i32".equals(type)) {
                    code.append("    i32.div_s\n");
                } else {
                    code.append("    f64.div\n");
                }
            }
            case "less" -> code.append("    ").append(type).append(".lt_s\n");
            case "less_equal" -> code.append("    ").append(type).append(".le_s\n");
            case "greater" -> code.append("    ").append(type).append(".gt_s\n");
            case "greater_equal" -> code.append("    ").append(type).append(".ge_s\n");
            case "equals" -> code.append("    ").append(type).append(".eq\n");
            case "not_equals" -> code.append("    ").append(type).append(".ne\n");
            case "and" -> code.append("    i32.and\n");
            case "or" -> code.append("    i32.or\n");
            default -> code.append("    ;; unknown operator: ").append(op).append("\n");
        }
    }
    
    private void generateUnaryExpression(UnaryExpression ue) {
        generateExpression(ue.operand);
        
        String op = ue.operator.toString().toLowerCase();
        String type = inferType(ue.operand);
        
        if ("minus".equals(op)) {
            code.append("    ").append(type).append(".neg\n");
        } else if ("not".equals(op)) {
            code.append("    i32.eqz\n");
        }
    }
    
    private void generateFunctionCall(FunctionCall fc) {
        // Push arguments
        for (int i = fc.arguments.size() - 1; i >= 0; i--) {
            generateExpression(fc.arguments.get(i));
        }
        
        code.append("    call $").append(fc.functionName).append("\n");
    }
    
    private void generateRoutineCall(RoutineCall rc) {
        if ("return".equals(rc.routineName)) {
            if (!rc.arguments.isEmpty()) {
                generateExpression(rc.arguments.get(0));
            }
            code.append("    return\n");
        } else {
            // Push arguments
            for (int i = rc.arguments.size() - 1; i >= 0; i--) {
                generateExpression(rc.arguments.get(i));
            }
            
            code.append("    call $").append(rc.routineName).append("\n");
        }
    }
    
    private void generateVariableDeclaration(VariableDeclaration vd) {
        if (vd.initializer != null) {
            generateExpression(vd.initializer);
            if (globalVars.containsKey(vd.name)) {
                code.append("    global.set $").append(vd.name).append("\n");
            } else if (localVars.containsKey(vd.name)) {
                code.append("    local.set $").append(vd.name).append("\n");
            }
        }
    }
    
    // Helper methods
    private String mapType(Type type) {
        if (type instanceof ast.PrimitiveType pt) {
            return TYPE_MAPPING.getOrDefault(pt.typeName, "i32");
        }
        return "i32"; // Default
    }
    
    private String inferType(Expression expr) {
        // Simple type inference
        if (expr instanceof IntegerLiteral) return "i32";
        if (expr instanceof RealLiteral) return "f64";
        if (expr instanceof BooleanLiteral) return "i32";
        if (expr instanceof StringLiteral) return "string";
        if (expr instanceof BinaryExpression be) {
            String leftType = inferType(be.left);
            String rightType = inferType(be.right);
            return "f64".equals(leftType) || "f64".equals(rightType) ? "f64" : "i32";
        }
        return "i32"; // Default
    }
    
    private String escapeString(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
    
    private int getStringOffset(String str) {
        // Calculate offset based on string position
        int offset = 0;
        for (var entry : stringLiterals.entrySet()) {
            if (entry.getKey().equals(str)) {
                return offset;
            }
            offset += entry.getKey().length() + 1;
        }
        return 0;
    }
}
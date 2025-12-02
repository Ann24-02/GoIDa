package wasm;

import analyzer.SemanticContext;
import ast.*;
import java.util.*;

// WebAssembly code generator - converts AST to WAT format
// Handles arrays, loops, functions, and memory management
public class WasmCodeGenerator {
    private final StringBuilder code;
    private final SemanticContext semanticContext;
    private int tempVarCounter = 0;
    private int stringCounter = 0;
    private final Map<String, Integer> globalVars = new HashMap<>();
    private final Map<String, String> stringLiterals = new HashMap<>();
    private final List<String> functions = new ArrayList<>();
    private int stringOffset = 0;
    private int memoryOffset = 256; // Start after string data

    // Map language types to WASM types
    private static final Map<String, String> TYPE_MAPPING = Map.of(
        "integer", "i32",
        "real", "f64", 
        "boolean", "i32",
        "string", "i32"  // pointer to string in memory
    );

    // Track array variables for special handling
    private final Set<String> arrayVariables = new HashSet<>();

    public WasmCodeGenerator(SemanticContext context) {
        this.code = new StringBuilder();
        this.semanticContext = context;
    }

    public String generate(Program program) {
        code.setLength(0);
        tempVarCounter = 0;
        stringCounter = 0;
        stringOffset = 0;
        memoryOffset = 256;
        globalVars.clear();
        stringLiterals.clear();
        functions.clear();
        arrayVariables.clear();

        code.append("(module\n");
        generateImports();
        code.append("  (memory $memory 1)\n");
        code.append("  (export \"memory\" (memory $memory))\n\n");

        collectStringLiterals(program);
        generateStringLiterals();
        generateGlobals(program);

        for (Declaration decl : program.declarations) {
            if (decl instanceof RoutineDeclaration) {
                generateFunction((RoutineDeclaration) decl);
            }
        }

        if (functions.contains("main")) {
            code.append("  (export \"main\" (func $main))\n");
        }

        code.append(")\n");
        return code.toString();
    }

    // Import JavaScript print functions
    private void generateImports() {
        code.append("  (import \"env\" \"printInt\" (func $printInt (param i32)))\n");
        code.append("  (import \"env\" \"printFloat\" (func $printFloat (param f64)))\n");
        code.append("  (import \"env\" \"printString\" (func $printString (param i32)))\n");
        code.append("  (import \"env\" \"printBool\" (func $printBool (param i32)))\n");
        code.append("  (import \"env\" \"printNewline\" (func $printNewline))\n");
    }

    // Generate global variables
    private void generateGlobals(Program program) {
        // First find all array variables
        for (Declaration decl : program.declarations) {
            if (decl instanceof VariableDeclaration vd) {
                if (vd.type instanceof ArrayType) {
                    arrayVariables.add(vd.name);
                }
            }
        }

        // Generate globals for all variables
        for (Declaration decl : program.declarations) {
            if (decl instanceof VariableDeclaration vd) {
                String wasmType = mapType(vd.type);
                
                if (!(vd.type instanceof ArrayType) && !globalVars.containsKey(vd.name)) {
                    globalVars.put(vd.name, globalVars.size());

                    code.append("  (global $").append(vd.name).append(" (mut ").append(wasmType).append(") (")
                       .append(wasmType).append(".const ");

                    if (vd.initializer != null && vd.initializer instanceof IntegerLiteral il) {
                        code.append(il.value);
                    } else if (vd.initializer != null && vd.initializer instanceof RealLiteral rl) {
                        code.append(rl.value);
                    } else if (vd.initializer != null && vd.initializer instanceof BooleanLiteral bl) {
                        code.append(bl.value ? "1" : "0");
                    } else {
                        code.append("0");
                    }
                    code.append("))\n");
                }
            }
        }

        // Special globals for arrays (pointer + size)
        for (String arrayName : arrayVariables) {
            if (!globalVars.containsKey(arrayName)) {
                code.append("  (global $").append(arrayName).append(" (mut i32) (i32.const 0))\n");
                code.append("  (global $").append(arrayName).append("_size (mut i32) (i32.const 0))\n");
                globalVars.put(arrayName, globalVars.size());
                globalVars.put(arrayName + "_size", globalVars.size());
            }
        }

        if (!globalVars.isEmpty()) {
            code.append("\n");
        }
    }

    // Store string literals in memory
    private void generateStringLiterals() {
        if (!stringLiterals.isEmpty()) {
            code.append("  ;; String literals\n");
            
            int currentOffset = 0;
            
            for (var entry : stringLiterals.entrySet()) {
                String str = entry.getKey();
                code.append("  (data (i32.const ").append(currentOffset).append(") \"")
                .append(escapeWatString(str)).append("\\00\")\n");
                currentOffset += str.length() + 1;
            }
            code.append("\n");
            
            memoryOffset = Math.max(memoryOffset, currentOffset);
        }
    }

    // Escape special characters for WAT format
    private String escapeWatString(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

    // Collect all string literals in the program
    private void collectStringLiterals(ASTNode node) {
        if (node == null) return;

        if (node instanceof StringLiteral sl) {
            if (!stringLiterals.containsKey(sl.value)) {
                stringLiterals.put(sl.value, "str_" + stringCounter++);
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
        else if (node instanceof VariableDeclaration vd) {
            if (vd.initializer != null) collectStringLiterals(vd.initializer);
        }
        else if (node instanceof IfStatement ifs) {
            collectStringLiterals(ifs.condition);
            if (ifs.thenBranch != null) collectStringLiterals(ifs.thenBranch);
            if (ifs.elseBranch != null) collectStringLiterals(ifs.elseBranch);
        }
        else if (node instanceof WhileLoop wl) {
            collectStringLiterals(wl.condition);
            if (wl.body != null) collectStringLiterals(wl.body);
        }
        else if (node instanceof ForLoop fl) {
            if (fl.range.start != null) collectStringLiterals(fl.range.start);
            if (fl.range.end != null) collectStringLiterals(fl.range.end);
            if (fl.body != null) collectStringLiterals(fl.body);
        }
        else if (node instanceof RoutineCall rc) {
            for (Expression arg : rc.arguments) collectStringLiterals(arg);
        }
        else if (node instanceof ModifiablePrimary mp) {
            // Base name is just a string, not an AST node
            for (ModifiablePrimary.Access access : mp.accesses) {
                if (access.index != null) collectStringLiterals(access.index);
            }
        }
        else if (node instanceof Assignment a) {
            collectStringLiterals(a.target);
            collectStringLiterals(a.value);
        }
    }

    // Generate function definition
    private void generateFunction(RoutineDeclaration routine) {
        functions.add(routine.name);

        code.append("  (func $").append(routine.name);

        // Parameters
        for (Parameter param : routine.parameters) {
            String paramName = cleanParameterName(param.name);
            code.append(" (param $").append(paramName).append(" ")
            .append(mapType(param.type)).append(")");
        }

        // Return type
        if (routine.returnType != null) {
            code.append(" (result ").append(mapType(routine.returnType)).append(")");
        }
        code.append("\n");

        Set<String> localVars = new LinkedHashSet<>();
        Map<String, String> varTypes = new HashMap<>();

        // Add parameters
        for (Parameter param : routine.parameters) {
            String paramName = cleanParameterName(param.name);
            localVars.add(paramName);
            varTypes.put(paramName, mapType(param.type));
        }

        // Add temp variable for array operations
        localVars.add("$temp");
        varTypes.put("$temp", "i32");

        // Collect local variables
        if (routine.body != null) {
            collectLocalVariables(routine.body, localVars, varTypes);
        }

        // Declare all local variables
        for (String varName : localVars) {
            if (!varName.startsWith("$") && !routineParameters(routine).contains(varName)) {
                String type = varTypes.getOrDefault(varName, "i32");
                code.append("    (local $").append(varName).append(" ").append(type).append(")\n");
            }
        }

        // Declare temp variable
        code.append("    (local $temp i32)\n");

        // Generate function body
        if (routine.body != null) {
            generateBody(routine.body, localVars, varTypes, routine);
        } else if (routine.expressionBody != null) {
            generateExpression(routine.expressionBody, localVars, varTypes, routine);
            if (routine.returnType == null) {
                code.append("    drop\n");
            }
        }

        code.append("  )\n\n");
    }

    // Remove 'ref ' prefix from parameter names
    private String cleanParameterName(String paramName) {
        if (paramName.startsWith("ref ")) {
            return paramName.substring(4);
        }
        return paramName;
    }

    // Get all parameter names for a function
    private Set<String> routineParameters(RoutineDeclaration routine) {
        Set<String> params = new HashSet<>();
        for (Parameter p : routine.parameters) {
            params.add(cleanParameterName(p.name));
        }
        return params;
    }

    // Find all local variables in a code block
    private void collectLocalVariables(Body body, Set<String> localVars, Map<String, String> varTypes) {
        if (body == null) return;

        for (ASTNode element : body.elements) {
            if (element instanceof VariableDeclaration vd) {
                String varName = vd.name;
                if (!globalVars.containsKey(varName)) {
                    localVars.add(varName);
                    varTypes.put(varName, mapType(vd.type));
                }
            } else if (element instanceof ForLoop fl) {
                localVars.add(fl.loopVariable);
                varTypes.put(fl.loopVariable, "i32");
                
                // Add helper variables for for-each loops
                if (fl.range.start == null && fl.range.end instanceof Identifier) {
                    String indexVar = fl.loopVariable + "_idx";
                    String tempSizeVar = "temp_size";
                    
                    if (!localVars.contains(indexVar)) {
                        localVars.add(indexVar);
                        varTypes.put(indexVar, "i32");
                    }
                    
                    if (!localVars.contains(tempSizeVar)) {
                        localVars.add(tempSizeVar);
                        varTypes.put(tempSizeVar, "i32");
                    }
                }
                
                // Check loop body for more variables
                if (fl.body != null) {
                    collectLocalVariables(fl.body, localVars, varTypes);
                }
            } else if (element instanceof IfStatement ifs) {
                if (ifs.thenBranch != null) {
                    collectLocalVariables(ifs.thenBranch, localVars, varTypes);
                }
                if (ifs.elseBranch != null) {
                    collectLocalVariables(ifs.elseBranch, localVars, varTypes);
                }
            } else if (element instanceof WhileLoop wl) {
                if (wl.body != null) {
                    collectLocalVariables(wl.body, localVars, varTypes);
                }
            }
        }
    }

    // Generate code block
    private void generateBody(Body body, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        if (body == null) return;

        for (ASTNode element : body.elements) {
            if (element instanceof Statement stmt) {
                generateStatement(stmt, localVars, varTypes, currentRoutine);
            } else if (element instanceof VariableDeclaration vd) {
                generateVariableDeclaration(vd, localVars, varTypes, currentRoutine);
            }
        }
    }

    // Choose statement type to generate
    private void generateStatement(Statement stmt, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        if (stmt instanceof Assignment a) {
            generateAssignment(a, localVars, varTypes, currentRoutine);
        } else if (stmt instanceof PrintStatement ps) {
            generatePrintStatement(ps, localVars, varTypes, currentRoutine);
        } else if (stmt instanceof IfStatement ifs) {
            generateIfStatement(ifs, localVars, varTypes, currentRoutine);
        } else if (stmt instanceof WhileLoop wl) {
            generateWhileLoop(wl, localVars, varTypes, currentRoutine);
        } else if (stmt instanceof ForLoop fl) {
            generateForLoop(fl, localVars, varTypes, currentRoutine);
        } else if (stmt instanceof RoutineCall rc) {
            generateRoutineCall(rc, localVars, varTypes, currentRoutine);
        }
    }

    // Generate assignment code
    private void generateAssignment(Assignment assignment, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        generateExpression(assignment.value, localVars, varTypes, currentRoutine);
        
        if (assignment.target instanceof ModifiablePrimary mp) {
            String varName = mp.baseName;
            
            // Record field assignment
            if (!mp.accesses.isEmpty()) {
                ModifiablePrimary.Access access = mp.accesses.get(0);
                
                if (access.fieldName != null) {
                    // Save value to temp variable
                    code.append("    local.set $temp\n");
                    
                    // Calculate record field address
                    generateVariableLoad(varName, localVars, currentRoutine);
                    
                    // Add field offset
                    int fieldOffset = 0;
                    if ("age".equals(access.fieldName)) {
                        fieldOffset = 4; // age field is at offset 4 (after name at offset 0)
                    } else if ("name".equals(access.fieldName)) {
                        fieldOffset = 0; // name field is at offset 0
                    }
                    
                    if (fieldOffset > 0) {
                        code.append("    i32.const ").append(fieldOffset).append("\n");
                        code.append("    i32.add\n");
                    }
                    
                    // Store value
                    code.append("    local.get $temp\n");
                    code.append("    i32.store\n");
                    return;
                }
                
                // Array element assignment
                if (access.index != null) {
                    // Save value to temp variable
                    code.append("    local.set $temp\n");
                    
                    // Calculate array element address
                    generateVariableLoad(varName, localVars, currentRoutine);
                    
                    // Add index (arrays are 1-based)
                    generateExpression(access.index, localVars, varTypes, currentRoutine);
                    code.append("    i32.const 1\n");
                    code.append("    i32.sub\n");
                    code.append("    i32.const 4\n");
                    code.append("    i32.mul\n");
                    
                    // Skip size field (4 bytes)
                    code.append("    i32.const 4\n");
                    code.append("    i32.add\n");
                    code.append("    i32.add\n");
                    
                    // Store value
                    code.append("    local.get $temp\n");
                    code.append("    i32.store\n");
                    return;
                }
            }
            
            // Simple variable assignment
            String targetType = varTypes.getOrDefault(varName, "i32");
            String valueType = inferType(assignment.value, varTypes);
            
            // Type conversion if needed
            if (!targetType.equals(valueType)) {
                if ("f64".equals(valueType) && "i32".equals(targetType)) {
                    code.append("    i32.trunc_f64_s\n");
                } else if ("i32".equals(valueType) && "f64".equals(targetType)) {
                    code.append("    f64.convert_i32_s\n");
                }
            }
            
            // Store to global or local variable
            if (globalVars.containsKey(varName)) {
                code.append("    global.set $").append(varName).append("\n");
            } else if (localVars.contains(varName)) {
                code.append("    local.set $").append(varName).append("\n");
            }
        }
    }

    // Generate print statement
    private void generatePrintStatement(PrintStatement ps, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
    for (Expression expr : ps.expressions) {
        generateExpression(expr, localVars, varTypes, currentRoutine);

        // Call appropriate print function based on type
        String type = inferType(expr, varTypes);
        switch (type) {
            case "i32" -> {
                code.append("    call $printInt\n");
                // printInt ничего не оставляет на стеке
            }
            case "f64" -> {
                code.append("    call $printFloat\n");
                // printFloat ничего не оставляет на стеке
            }
            case "string" -> {
                code.append("    call $printString\n");
                // printString ничего не оставляет на стеке
            }
            case "boolean" -> {
                code.append("    call $printBool\n");
                // printBool ничего не оставляет на стеке
            }
            default -> {
                code.append("    call $printInt\n");
                // printInt ничего не оставляет на стеке
            }
        }
    }
    code.append("    call $printNewline\n");
}

    // Generate if statement
    private void generateIfStatement(IfStatement ifs, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        generateExpression(ifs.condition, localVars, varTypes, currentRoutine);

        code.append("    if\n");
        if (ifs.thenBranch != null) {
            generateBody(ifs.thenBranch, localVars, varTypes, currentRoutine);
        }

        if (ifs.elseBranch != null) {
            code.append("    else\n");
            generateBody(ifs.elseBranch, localVars, varTypes, currentRoutine);
        }

        code.append("    end\n");
    }

    // Generate while loop
    private void generateWhileLoop(WhileLoop wl, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        String loopLabel = "loop_" + tempVarCounter++;

        code.append("    block $").append(loopLabel).append("_end\n");
        code.append("    loop $").append(loopLabel).append("_start\n");

        generateExpression(wl.condition, localVars, varTypes, currentRoutine);
        code.append("    i32.eqz\n");
        code.append("    br_if $").append(loopLabel).append("_end\n");

        if (wl.body != null) {
            generateBody(wl.body, localVars, varTypes, currentRoutine);
        }

        code.append("    br $").append(loopLabel).append("_start\n");
        code.append("    end\n");
        code.append("    end\n");
    }

    // Generate for loop
    private void generateForLoop(ForLoop fl, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
    String loopVar = fl.loopVariable;
    String loopLabel = "loop_" + tempVarCounter++;

    // For-each loop over array
    if (fl.range.start == null && fl.range.end instanceof Identifier arrayId) {
        String arrayName = arrayId.name;
        
        String indexVar = loopVar + "_idx";
        String tempSizeVar = "temp_size";
        
        // Get array size (load from first 4 bytes of array)
        generateVariableLoad(arrayName, localVars, currentRoutine);
        code.append("    i32.load\n");  // Load array size
        code.append("    local.set $").append(tempSizeVar).append("\n");
        
        // Initialize index to 1 (1-based arrays)
        code.append("    i32.const 1\n");
        code.append("    local.set $").append(indexVar).append("\n");
        
        code.append("    block $").append(loopLabel).append("_end\n");
        code.append("    loop $").append(loopLabel).append("_start\n");
        
        // Check index <= array.size
        code.append("    local.get $").append(indexVar).append("\n");
        code.append("    local.get $").append(tempSizeVar).append("\n");
        code.append("    i32.gt_s\n");
        code.append("    br_if $").append(loopLabel).append("_end\n");
        
        // Load array[i] into loop variable
        generateVariableLoad(arrayName, localVars, currentRoutine);
        code.append("    i32.const 4\n");
        code.append("    i32.add\n");
        
        code.append("    local.get $").append(indexVar).append("\n");
        code.append("    i32.const 1\n");
        code.append("    i32.sub\n");
        code.append("    i32.const 4\n");
        code.append("    i32.mul\n");
        code.append("    i32.add\n");
        code.append("    i32.load\n");
        code.append("    local.set $").append(loopVar).append("\n");
        
        // Generate loop body
        if (fl.body != null) {
            generateBody(fl.body, localVars, varTypes, currentRoutine);
        }
        
        // Increment index
        code.append("    local.get $").append(indexVar).append("\n");
        code.append("    i32.const 1\n");
        code.append("    i32.add\n");
        code.append("    local.set $").append(indexVar).append("\n");
        
        code.append("    br $").append(loopLabel).append("_start\n");
        code.append("    end\n");
        code.append("    end\n");
        return;
    }
        
        // Regular numeric range loop
        if (fl.range.start != null) {
            generateExpression(fl.range.start, localVars, varTypes, currentRoutine);
            code.append("    local.set $").append(loopVar).append("\n");
        } else {
            code.append("    i32.const 0\n");
            code.append("    local.set $").append(loopVar).append("\n");
        }

        String endLabel = loopLabel + "_end";
        String startLabel = loopLabel + "_start";
        
        code.append("    block $").append(endLabel).append("\n");
        code.append("    loop $").append(startLabel).append("\n");
        
        // Loop condition
        code.append("    local.get $").append(loopVar).append("\n");
        if (fl.range.end != null) {
            generateExpression(fl.range.end, localVars, varTypes, currentRoutine);
        } else {
            code.append("    i32.const 0\n");
        }
        
        if (fl.reverse) {
            code.append("    i32.lt_s\n");
        } else {
            code.append("    i32.gt_s\n");
        }
        code.append("    br_if $").append(endLabel).append("\n");
        
        // Loop body
        if (fl.body != null) {
            generateBody(fl.body, localVars, varTypes, currentRoutine);
        }
        
        // Update loop variable
        code.append("    local.get $").append(loopVar).append("\n");
        if (fl.reverse) {
            code.append("    i32.const 1\n");
            code.append("    i32.sub\n");
        } else {
            code.append("    i32.const 1\n");
            code.append("    i32.add\n");
        }
        code.append("    local.set $").append(loopVar).append("\n");
        
        code.append("    br $").append(startLabel).append("\n");
        code.append("    end\n");
        code.append("    end\n");
    }

    // Generate expression code
    private void generateExpression(Expression expr, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
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
            code.append("    i32.const ").append(getStringOffset(sl.value)).append("\n");
        }
        else if (expr instanceof Identifier id) {
            generateVariableLoad(id.name, localVars, currentRoutine);
        }
        else if (expr instanceof BinaryExpression be) {
            generateBinaryExpression(be, localVars, varTypes, currentRoutine);
        }
        else if (expr instanceof UnaryExpression ue) {
            generateUnaryExpression(ue, localVars, varTypes, currentRoutine);
        }
        else if (expr instanceof FunctionCall fc) {
            generateFunctionCall(fc, localVars, varTypes, currentRoutine);
        }
        else if (expr instanceof ModifiablePrimary mp) {
            generateModifiablePrimary(mp, localVars, varTypes, currentRoutine);
        }
    }

    // Generate array/record field access code
    private void generateModifiablePrimary(ModifiablePrimary mp, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        String varName = mp.baseName;
        
        if (mp.accesses.isEmpty()) {
            generateVariableLoad(varName, localVars, currentRoutine);
            return;
        }

        ModifiablePrimary.Access access = mp.accesses.get(0);

        // Array size access
        if (access.fieldName != null && "size".equals(access.fieldName)) {
            generateVariableLoad(varName, localVars, currentRoutine);
            code.append("    i32.load\n");
            return;
        } 
        // Record field access
        else if (access.fieldName != null) {
            generateVariableLoad(varName, localVars, currentRoutine);
            
            // Add field offset
            int fieldOffset = 0;
            if ("age".equals(access.fieldName)) {
                fieldOffset = 4; // age field is at offset 4 (after name at offset 0)
            } else if ("name".equals(access.fieldName)) {
                fieldOffset = 0; // name field is at offset 0
            }
            
            if (fieldOffset > 0) {
                code.append("    i32.const ").append(fieldOffset).append("\n");
                code.append("    i32.add\n");
            }
            
            // Load field value
            code.append("    i32.load\n");
        } 
        // Array element access
        else if (access.index != null) {
            generateVariableLoad(varName, localVars, currentRoutine);
            
            // Calculate element address
            generateExpression(access.index, localVars, varTypes, currentRoutine);
            code.append("    i32.const 1\n");
            code.append("    i32.sub\n");
            code.append("    i32.const 4\n");
            code.append("    i32.mul\n");
            code.append("    i32.const 4\n");
            code.append("    i32.add\n");
            code.append("    i32.add\n");
            
            // Load element value
            code.append("    i32.load\n");
        }
    }

    // Load variable value
    private void generateVariableLoad(String varName, Set<String> localVars, RoutineDeclaration currentRoutine) {
        if (globalVars.containsKey(varName)) {
            code.append("    global.get $").append(varName).append("\n");
        } else if (localVars.contains(varName)) {
            code.append("    local.get $").append(varName).append("\n");
        } else if (routineParameters(currentRoutine).contains(varName)) {
            code.append("    local.get $").append(varName).append("\n");
        } else {
            code.append("    i32.const 0\n");
        }
    }

    // Generate binary operation
    private void generateBinaryExpression(BinaryExpression be, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        generateExpression(be.left, localVars, varTypes, currentRoutine);
        generateExpression(be.right, localVars, varTypes, currentRoutine);
        
        String op = be.operator.toString().toLowerCase();
        String leftType = inferType(be.left, varTypes);
        String rightType = inferType(be.right, varTypes);
        String type = "f64".equals(leftType) || "f64".equals(rightType) ? "f64" : "i32";
        
        // Modulo operation
        if ("modulo".equals(op)) {
            if ("i32".equals(type)) {
                code.append("    i32.rem_s\n");
            } else {
                code.append("    f64.rem\n");
            }
            return;
        }
        
        // Map operators to WASM instructions
        switch (op) {
            case "plus"     -> code.append(type).append(".add\n");
            case "minus"    -> code.append(type).append(".sub\n");
            case "multiply" -> code.append(type).append(".mul\n");
            case "divide"   -> {
                if ("i32".equals(type)) code.append("i32.div_s\n");
                else code.append("f64.div\n");
            }
            case "less"         -> code.append(type).append(".lt_s\n");
            case "less_equal"   -> code.append(type).append(".le_s\n");
            case "greater"      -> code.append(type).append(".gt_s\n");
            case "greater_equal"-> code.append(type).append(".ge_s\n");
            case "equals"       -> code.append(type).append(".eq\n");
            case "not_equals"   -> code.append(type).append(".ne\n");
            case "and"          -> code.append("i32.and\n");
            case "or"           -> code.append("i32.or\n");
            default             -> code.append("i32.add\n");
        }
    }

    // Generate unary operation
    private void generateUnaryExpression(UnaryExpression ue, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        String type = inferType(ue.operand, varTypes);
        generateExpression(ue.operand, localVars, varTypes, currentRoutine);

        String op = ue.operator.toString().toLowerCase();

        if ("minus".equals(op)) {
            code.append("    ").append(type).append(".neg\n");
        } else if ("not".equals(op)) {
            code.append("    i32.eqz\n");
        }
    }

    // Generate function call
    private void generateFunctionCall(FunctionCall fc, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        // Handle special built-in functions
        if ("array_literal".equals(fc.functionName)) {
            generateArrayLiteral(fc.arguments, localVars, varTypes, currentRoutine);
            return;
        }

        if ("record_literal".equals(fc.functionName)) {
            generateRecordLiteral(fc.arguments, localVars, varTypes, currentRoutine);
            return;
        }

        if ("field".equals(fc.functionName)) {
            if (fc.arguments.size() == 2) {
                generateExpression(fc.arguments.get(1), localVars, varTypes, currentRoutine);
            } else {
                code.append("    i32.const 0\n");
            }
            return;
        }

        // Regular function call
        for (Expression arg : fc.arguments) {
            generateExpression(arg, localVars, varTypes, currentRoutine);
        }

        code.append("    call $").append(fc.functionName).append("\n");
    }

    // Generate array literal [1, 2, 3]
    private void generateArrayLiteral(List<Expression> elements, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        if (elements.isEmpty()) {
            code.append("    i32.const 0\n");
            return;
        }

        int arrayPtr = memoryOffset;
        int totalSize = 4 + (elements.size() * 4);
        
        // Store array size
        code.append("    i32.const ").append(arrayPtr).append("\n");
        code.append("    i32.const ").append(elements.size()).append("\n");
        code.append("    i32.store\n");

        // Store array elements
        for (int i = 0; i < elements.size(); i++) {
            int elementPtr = arrayPtr + 4 + (i * 4);
            code.append("    i32.const ").append(elementPtr).append("\n");
            generateExpression(elements.get(i), localVars, varTypes, currentRoutine);
            code.append("    i32.store\n");
        }

        // Return array pointer
        code.append("    i32.const ").append(arrayPtr).append("\n");
        
        memoryOffset += totalSize;
    }

    // Generate record literal {x: 1, y: 2}
    private void generateRecordLiteral(List<Expression> fieldExprs, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
    if (fieldExprs.isEmpty()) {
        code.append("    i32.const 0\n");
        return;
    }

    int recordPtr = memoryOffset;
    int fieldCount = fieldExprs.size();
    
    // Обрабатываем каждый field в формате field("name", value)
    for (int i = 0; i < fieldExprs.size(); i++) {
        Expression fieldExpr = fieldExprs.get(i);
        
        if (fieldExpr instanceof FunctionCall fc && "field".equals(fc.functionName)) {
            if (fc.arguments.size() == 2) {
                Expression fieldNameExpr = fc.arguments.get(0);
                Expression fieldValueExpr = fc.arguments.get(1);
                
                // Вычисляем offset для этого поля
                int fieldOffset = 0;
                if (fieldNameExpr instanceof StringLiteral sl) {
                    if ("name".equals(sl.value)) {
                        fieldOffset = 0;
                    } else if ("age".equals(sl.value)) {
                        fieldOffset = 4;
                    }
                }
                
                // Сохраняем значение поля
                code.append("    i32.const ").append(recordPtr + fieldOffset).append("\n");
                generateExpression(fieldValueExpr, localVars, varTypes, currentRoutine);
                code.append("    i32.store\n");
            }
        }
    }
    
    // Возвращаем recordPtr (только один раз!)
    code.append("    i32.const ").append(recordPtr).append("\n");
    
    memoryOffset += fieldCount * 4;
}
    // Generate routine call (including return)
    private void generateRoutineCall(RoutineCall rc, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        if ("return".equals(rc.routineName)) {
            if (!rc.arguments.isEmpty()) {
                generateExpression(rc.arguments.get(0), localVars, varTypes, currentRoutine);
            }
            code.append("    return\n");
        } else {
            for (Expression arg : rc.arguments) {
                generateExpression(arg, localVars, varTypes, currentRoutine);
            }

            code.append("    call $").append(rc.routineName).append("\n");
            
            // If the called function returns a value but we don't use it in a void context,
            // we need to drop it
            // This is handled by checking if the current context expects a value
            // For now, we'll be conservative and not add drop
        }
    }

    // Generate variable declaration with initialization
    private void generateVariableDeclaration(VariableDeclaration vd, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        if (vd.initializer != null) {
            generateExpression(vd.initializer, localVars, varTypes, currentRoutine);
            String varType = varTypes.getOrDefault(vd.name, "i32");
            String valueType = inferType(vd.initializer, varTypes);
            
            // Type conversion
            if (!varType.equals(valueType)) {
                if ("f64".equals(valueType) && "i32".equals(varType)) {
                    code.append("    i32.trunc_f64_s\n");
                } else if ("i32".equals(valueType) && "f64".equals(varType)) {
                    code.append("    f64.convert_i32_s\n");
                }
            }
            
            // Store to variable
            if (globalVars.containsKey(vd.name)) {
                code.append("    global.set $").append(vd.name).append("\n");
                
                // Set array size for array literals
                if (vd.type instanceof ArrayType) {
                    if (vd.initializer instanceof FunctionCall fc && "array_literal".equals(fc.functionName)) {
                        code.append("    i32.const ").append(fc.arguments.size()).append("\n");
                        code.append("    global.set $").append(vd.name).append("_size\n");
                    } else {
                        code.append("    i32.const 0\n");
                        code.append("    global.set $").append(vd.name).append("_size\n");
                    }
                }
            } else if (localVars.contains(vd.name)) {
                code.append("    local.set $").append(vd.name).append("\n");
            }
        }
    }

    // Map language type to WASM type
    private String mapType(Type type) {
        if (type instanceof ast.PrimitiveType pt) {
            return TYPE_MAPPING.getOrDefault(pt.typeName, "i32");
        } else if (type instanceof ast.UserType ut) {
            Type resolved = semanticContext.getType(ut.typeName);
            if (resolved != null) {
                return mapType(resolved);
            }
            return "i32";
        } else if (type instanceof ast.ArrayType at) {
            return "i32";
        } else if (type instanceof ast.RecordType rt) {
            return "i32";
        }
        return "i32";
    }

    // Infer expression type for printing purposes
    private String inferType(Expression expr, Map<String, String> varTypes) {
        if (expr instanceof IntegerLiteral) return "i32";
        if (expr instanceof RealLiteral) return "f64";
        if (expr instanceof BooleanLiteral) return "boolean";
        if (expr instanceof StringLiteral) return "string";
        if (expr instanceof Identifier id) {
            // Try to get type from global variables or local variables
            if (globalVars.containsKey(id.name)) {
                // We don't store type info in globalVars, so we need another way
                // For now, assume integer for globals
                return "i32";
            }
            return varTypes.getOrDefault(id.name, "i32");
        }
        if (expr instanceof ModifiablePrimary mp) {
            if (!mp.accesses.isEmpty()) {
                ModifiablePrimary.Access access = mp.accesses.get(0);
                if (access.fieldName != null) {
                    if ("name".equals(access.fieldName)) {
                        return "string";
                    } else if ("age".equals(access.fieldName)) {
                        return "i32";
                    }
                }
            }
            return "i32";
        }
        if (expr instanceof BinaryExpression be) {
            String leftType = inferType(be.left, varTypes);
            String rightType = inferType(be.right, varTypes);
            return ("f64".equals(leftType) || "f64".equals(rightType)) ? "f64" : "i32";
        }
        return "i32";
    }

    // Get memory offset for a string literal
    private int getStringOffset(String str) {
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
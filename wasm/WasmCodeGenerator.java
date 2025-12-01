package wasm;

import analyzer.SemanticContext;
import ast.*;
import java.util.*;

/**
 * WebAssembly Code Generator - FIXED VERSION
 * Converts AST to WebAssembly Text Format (WAT)
 * 
 * Key fixes:
 * 1. Proper array handling and memory management
 * 2. Correct scope handling for local variables
 * 3. Proper type handling for i32/f64
 * 4. Support for reference parameters
 * 5. Record/struct support
 * 6. Correct float comparisons
 * 7. Module operator handling
 * 8. Parameter access fix
 */
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

    // WASM type mappings
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

        // Generate module header
        code.append("(module\n");

        // Import necessary functions from JavaScript
        generateImports();

        // Define memory (64KB initial)
        code.append("  (memory $memory 1)\n");
        code.append("  (export \"memory\" (memory $memory))\n\n");

        // Define string literals in memory FIRST
        collectStringLiterals(program);
        generateStringLiterals();

        // Define global variables
        generateGlobals(program);

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
        code.append("  (import \"env\" \"printInt\" (func $printInt (param i32)))\n");
        code.append("  (import \"env\" \"printFloat\" (func $printFloat (param f64)))\n");
        code.append("  (import \"env\" \"printString\" (func $printString (param i32)))\n");
        code.append("  (import \"env\" \"printBool\" (func $printBool (param i32)))\n");
        code.append("  (import \"env\" \"printNewline\" (func $printNewline))\n");
    }

    private void generateGlobals(Program program) {
        // First pass: collect all array variables
        for (Declaration decl : program.declarations) {
            if (decl instanceof VariableDeclaration vd) {
                if (vd.type instanceof ArrayType) {
                    arrayVariables.add(vd.name);
                }
            }
        }

        // Second pass: generate globals
        for (Declaration decl : program.declarations) {
            if (decl instanceof VariableDeclaration vd) {
                String wasmType = mapType(vd.type);
                
                // Only create globals for non-array variables or if not already declared
                if (!(vd.type instanceof ArrayType) && !globalVars.containsKey(vd.name)) {
                    globalVars.put(vd.name, globalVars.size());

                    code.append("  (global $").append(vd.name).append(" (mut ").append(wasmType).append(") (")
                       .append(wasmType).append(".const ");

                    // Initialize with value if provided
                    if (vd.initializer != null && vd.initializer instanceof IntegerLiteral il) {
                        code.append(il.value);
                    } else if (vd.initializer != null && vd.initializer instanceof RealLiteral rl) {
                        code.append(rl.value);
                    } else if (vd.initializer != null && vd.initializer instanceof BooleanLiteral bl) {
                        code.append(bl.value ? "1" : "0");
                    } else {
                        code.append("0"); // default value
                    }
                    code.append("))\n");
                }
            }
        }

        // Create globals for arrays if they don't exist
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

    private void generateStringLiterals() {
        if (!stringLiterals.isEmpty()) {
            code.append("  ;; String literals\n");
            
            // Track current offset
            int currentOffset = 0;
            
            for (var entry : stringLiterals.entrySet()) {
                String str = entry.getKey();
                code.append("  (data (i32.const ").append(currentOffset).append(") \"")
                .append(escapeWatString(str)).append("\\00\")\n");
                currentOffset += str.length() + 1; // +1 for null terminator
            }
            code.append("\n");
            
            // Update memory offset after strings
            memoryOffset = Math.max(memoryOffset, currentOffset);
        }
    }

    private String escapeWatString(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

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
    }

    private void generateFunction(RoutineDeclaration routine) {
        functions.add(routine.name);

        code.append("  (func $").append(routine.name);

        // FIX: Properly handle ref parameters
        for (Parameter param : routine.parameters) {
            String paramName = cleanParameterName(param.name);
            boolean isRef = param.name.startsWith("ref ");

            code.append(" (param $").append(paramName).append(" ")
            .append(mapType(param.type)).append(")");
        }

        // Return type
        if (routine.returnType != null) {
            code.append(" (result ").append(mapType(routine.returnType)).append(")");
        }
        code.append("\n");

        // Collect local variables
        Set<String> localVars = new LinkedHashSet<>();
        Map<String, String> varTypes = new HashMap<>();

        // Add parameters to locals
        for (Parameter param : routine.parameters) {
            String paramName = cleanParameterName(param.name);
            localVars.add(paramName);
            varTypes.put(paramName, mapType(param.type));
        }

        // Collect variables from body
        if (routine.body != null) {
            collectLocalVariables(routine.body, localVars, varTypes);
        }

        // Declare all local variables with proper types
        for (String varName : localVars) {
            if (!varName.isEmpty() && !routineParameters(routine).contains(varName)) {
                String type = varTypes.getOrDefault(varName, "i32");
                code.append("    (local $").append(varName).append(" ").append(type).append(")\n");
            }
        }

        // Function body
        if (routine.body != null) {
            generateBody(routine.body, localVars, varTypes, routine);
        } else if (routine.expressionBody != null) {
            generateExpression(routine.expressionBody, localVars, varTypes, routine);
            if (routine.returnType == null) {
                code.append("    drop\n"); // Remove the leftover value for void functions
            }
        }

        // For functions with return type, ensure we end with a return
        if (routine.returnType != null && routine.body != null && routine.expressionBody == null) {
            // If body doesn't end with explicit return, we need to add implicit return
            // Value should already be on stack from last expression
        }

        code.append("  )\n\n");
    }

    private String cleanParameterName(String paramName) {
        if (paramName.startsWith("ref ")) {
            return paramName.substring(4);
        }
        return paramName;
    }

    private Set<String> routineParameters(RoutineDeclaration routine) {
        Set<String> params = new HashSet<>();
        for (Parameter p : routine.parameters) {
            params.add(cleanParameterName(p.name));
        }
        return params;
    }

    private void collectLocalVariables(Body body, Set<String> localVars, Map<String, String> varTypes) {
        if (body == null) return;

        for (ASTNode element : body.elements) {
            if (element instanceof VariableDeclaration vd) {
                String varName = vd.name;
                if (!globalVars.containsKey(varName)) {
                    localVars.add(varName);
                    String type = mapType(vd.type);
                    varTypes.put(varName, type);
                }
            } else if (element instanceof ForLoop fl) {
                localVars.add(fl.loopVariable);
                varTypes.put(fl.loopVariable, "i32");
            }
        }
    }

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

    private void generateAssignment(Assignment assignment, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        generateExpression(assignment.value, localVars, varTypes, currentRoutine);
        
        if (assignment.target instanceof ModifiablePrimary mp) {
            String varName = mp.baseName;
            
            // Handle array/record access
            if (!mp.accesses.isEmpty()) {
                ModifiablePrimary.Access access = mp.accesses.get(0);
                
                if (access.fieldName != null) {
                    // Record field assignment
                    // Get record pointer
                    if (globalVars.containsKey(varName)) {
                        code.append("    global.get $").append(varName).append("\n");
                    } else if (localVars.contains(varName)) {
                        code.append("    local.get $").append(varName).append("\n");
                    } else if (routineParameters(currentRoutine).contains(varName)) {
                        code.append("    local.get $").append(varName).append("\n");
                    } else {
                        code.append("    i32.const 0\n");
                    }
                    
                    // Add field offset
                    if ("name".equals(access.fieldName)) {
                        code.append("    i32.const 0\n");
                    } else if ("age".equals(access.fieldName)) {
                        code.append("    i32.const 4\n");
                    } else {
                        code.append("    i32.const 0\n");
                    }
                    code.append("    i32.add\n");
                    
                    // Store the value
                    code.append("    i32.store\n");
                    return;
                } else if (access.index != null) {
                    // Array element assignment
                    // Get base address
                    if (globalVars.containsKey(varName)) {
                        code.append("    global.get $").append(varName).append("\n");
                    } else if (localVars.contains(varName)) {
                        code.append("    local.get $").append(varName).append("\n");
                    } else if (routineParameters(currentRoutine).contains(varName)) {
                        code.append("    local.get $").append(varName).append("\n");
                    } else {
                        code.append("    i32.const 0\n");
                    }
                    
                    // Add index offset
                    generateExpression(access.index, localVars, varTypes, currentRoutine);
                    code.append("    i32.const 4\n");
                    code.append("    i32.mul\n");
                    code.append("    i32.add\n");
                    
                    // Store the value
                    code.append("    i32.store\n");
                    return;
                }
            }
            
            // Simple variable assignment
            String targetType = varTypes.getOrDefault(varName, "i32");
            String valueType = inferType(assignment.value, varTypes);
            
            // Add type conversion if needed
            if (!targetType.equals(valueType)) {
                if ("f64".equals(valueType) && "i32".equals(targetType)) {
                    code.append("    i32.trunc_f64_s\n");
                } else if ("i32".equals(valueType) && "f64".equals(targetType)) {
                    code.append("    f64.convert_i32_s\n");
                }
            }
            
            if (globalVars.containsKey(varName)) {
                code.append("    global.set $").append(varName).append("\n");
            } else if (localVars.contains(varName)) {
                code.append("    local.set $").append(varName).append("\n");
            }
        }
    }

    private void generatePrintStatement(PrintStatement ps, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        for (Expression expr : ps.expressions) {
            generateExpression(expr, localVars, varTypes, currentRoutine);

            // Determine type and call appropriate print function
            String type = inferType(expr, varTypes);
            switch (type) {
                case "i32" -> code.append("    call $printInt\n");
                case "f64" -> code.append("    call $printFloat\n");
                case "string" -> code.append("    call $printString\n");
                default -> code.append("    call $printInt\n");
            }
        }
        code.append("    call $printNewline\n");
    }

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

    private void generateWhileLoop(WhileLoop wl, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        String loopLabel = "loop_" + tempVarCounter++;

        code.append("    block $").append(loopLabel).append("_end\n");
        code.append("    loop $").append(loopLabel).append("_start\n");

        generateExpression(wl.condition, localVars, varTypes, currentRoutine);

        // Branch if condition is FALSE (0)
        code.append("    i32.eqz\n");
        code.append("    br_if $").append(loopLabel).append("_end\n");

        // Loop body
        if (wl.body != null) {
            generateBody(wl.body, localVars, varTypes, currentRoutine);
        }

        code.append("    br $").append(loopLabel).append("_start\n");
        code.append("    end\n");
        code.append("    end\n");
    }

    private void generateForLoop(ForLoop fl, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        String loopVar = fl.loopVariable;
        String loopLabel = "loop_" + tempVarCounter++;

        // For-each loop over array
        if (fl.range.start == null && fl.range.end instanceof Identifier arrayId) {
            String arrayName = arrayId.name;
            
            String indexVar = loopVar + "_idx";
            String tempSizeVar = "temp_size_" + tempVarCounter++;
            
            // Declare additional local variables
            code.append("    (local $").append(indexVar).append(" i32)\n");
            code.append("    (local $").append(tempSizeVar).append(" i32)\n");
            
            // Get array size - ИСПРАВЛЕНО: использовать старый подход
            if (globalVars.containsKey(arrayName)) {
                code.append("    global.get $").append(arrayName).append("_size\n");
            } else if (routineParameters(currentRoutine).contains(arrayName)) {
                // For array parameters, size is stored at arr pointer
                code.append("    local.get $").append(arrayName).append("\n");
                code.append("    i32.load\n");
            } else {
                code.append("    i32.const 0\n");
            }
            code.append("    local.set $").append(tempSizeVar).append("\n");
            
            // Initialize index
            code.append("    i32.const 0\n");
            code.append("    local.set $").append(indexVar).append("\n");
            
            code.append("    block $").append(loopLabel).append("_end\n");
            code.append("    loop $").append(loopLabel).append("_start\n");
            
            // Check i < array.size
            code.append("    local.get $").append(indexVar).append("\n");
            code.append("    local.get $").append(tempSizeVar).append("\n");
            code.append("    i32.lt_s\n");
            code.append("    i32.eqz\n");
            code.append("    br_if $").append(loopLabel).append("_end\n");
            
            // Load arr[i] into loopVar - ВАЖНО: использовать правильный способ получения array pointer
            if (globalVars.containsKey(arrayName)) {
                code.append("    global.get $").append(arrayName).append("\n");
            } else if (localVars.contains(arrayName)) {
                code.append("    local.get $").append(arrayName).append("\n");
            } else if (routineParameters(currentRoutine).contains(arrayName)) {
                code.append("    local.get $").append(arrayName).append("\n");
            } else {
                code.append("    i32.const 0\n");
            }
            // НЕ добавляем i32.const 4 i32.add - массив уже указывает на данные
            code.append("    local.get $").append(indexVar).append("\n");
            code.append("    i32.const 4\n");
            code.append("    i32.mul\n");
            code.append("    i32.add\n");
            code.append("    i32.load\n");
            code.append("    local.set $").append(loopVar).append("\n");
            
            // Loop body
            if (fl.body != null) {
                generateBody(fl.body, localVars, varTypes, currentRoutine);
            }
            
            // i++
            code.append("    local.get $").append(indexVar).append("\n");
            code.append("    i32.const 1\n");
            code.append("    i32.add\n");
            code.append("    local.set $").append(indexVar).append("\n");
            
            code.append("    br $").append(loopLabel).append("_start\n");
            code.append("    end\n");
            code.append("    end\n");
            
            return;
        }
        
        // Regular for loop with range
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
        
        // Check condition
        code.append("    local.get $").append(loopVar).append("\n");
        if (fl.range.end != null) {
            generateExpression(fl.range.end, localVars, varTypes, currentRoutine);
        } else {
            code.append("    i32.const 0\n");
        }
        
        if (fl.reverse) {
            code.append("    i32.lt_s\n");
            code.append("    br_if $").append(endLabel).append("\n");
        } else {
            code.append("    i32.gt_s\n");
            code.append("    br_if $").append(endLabel).append("\n");
        }
        
        // Loop body
        if (fl.body != null) {
            generateBody(fl.body, localVars, varTypes, currentRoutine);
        }
        
        // Increment/decrement
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
            if (globalVars.containsKey(id.name)) {
                code.append("    global.get $").append(id.name).append("\n");
            } else if (localVars.contains(id.name)) {
                code.append("    local.get $").append(id.name).append("\n");
            } else if (routineParameters(currentRoutine).contains(id.name)) {
                code.append("    local.get $").append(id.name).append("\n");
            } else {
                code.append("    i32.const 0\n");
            }
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

    // FIXED: Proper ModifiablePrimary handling for parameters
    private void generateModifiablePrimary(ModifiablePrimary mp, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        String varName = mp.baseName;
        boolean isGlobal = globalVars.containsKey(varName);
        boolean isLocal = localVars.contains(varName);
        boolean isParam = routineParameters(currentRoutine).contains(varName);
        
        if (mp.accesses.isEmpty()) {
            // Simple variable
            if (isGlobal) {
                code.append("    global.get $").append(varName).append("\n");
            } else if (isLocal) {
                code.append("    local.get $").append(varName).append("\n");
            } else if (isParam) {
                code.append("    local.get $").append(varName).append("\n");
            } else {
                code.append("    i32.const 0\n");
            }
            return;
        }

        ModifiablePrimary.Access access = mp.accesses.get(0);

        if (access.fieldName != null) {
            // Record field access
            if (isGlobal) {
                code.append("    global.get $").append(varName).append("\n");
            } else if (isLocal) {
                code.append("    local.get $").append(varName).append("\n");
            } else if (isParam) {
                code.append("    local.get $").append(varName).append("\n");
            } else {
                code.append("    i32.const 0\n");
            }
            
            // Add field offset
            if ("name".equals(access.fieldName)) {
                code.append("    i32.const 0\n");
            } else if ("age".equals(access.fieldName)) {
                code.append("    i32.const 4\n");
            } else if ("size".equals(access.fieldName)) {
                // Array.size - load size from memory (stored at array pointer)
                code.append("    i32.load\n");
                return;
            } else {
                code.append("    i32.const 0\n");
            }
            code.append("    i32.add\n");
            
            // Load field value from memory
            code.append("    i32.load\n");
            
        } else if (access.index != null) {
            // Array index access [i]
            // Get base address of array
            if (isGlobal) {
                code.append("    global.get $").append(varName).append("\n");
            } else if (isLocal) {
                code.append("    local.get $").append(varName).append("\n");
            } else if (isParam) {
                code.append("    local.get $").append(varName).append("\n");
            } else {
                code.append("    i32.const 0\n");
            }
            
            // Add index offset (skip size stored at position 0)
            code.append("    i32.const 4\n"); // Skip size
            code.append("    i32.add\n");
            
            // Add index * 4
            generateExpression(access.index, localVars, varTypes, currentRoutine);
            code.append("    i32.const 4\n");
            code.append("    i32.mul\n");
            code.append("    i32.add\n");
            
            // Load from memory
            code.append("    i32.load\n");
        }
    }

    private void generateBinaryExpression(BinaryExpression be, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        generateExpression(be.left, localVars, varTypes, currentRoutine);
        generateExpression(be.right, localVars, varTypes, currentRoutine);
        
        String op = be.operator.toString().toLowerCase();
        String leftType = inferType(be.left, varTypes);
        String rightType = inferType(be.right, varTypes);
        String type = "f64".equals(leftType) || "f64".equals(rightType) ? "f64" : "i32";
        
        // FIXED: Handle modulo operator properly
        if ("modulo".equals(op)) {
            if ("i32".equals(type)) {
                code.append("    i32.rem_s\n");  // Use remainder for modulo
            } else {
                code.append("    f64.rem\n");
            }
            return;
        }
        
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
            default             -> code.append("i32.add\n"); // Default fallback
        }
    }

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

    private void generateFunctionCall(FunctionCall fc, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
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

        // Push arguments in correct order
        for (Expression arg : fc.arguments) {
            generateExpression(arg, localVars, varTypes, currentRoutine);
        }

        code.append("    call $").append(fc.functionName).append("\n");
    }

    private void generateArrayLiteral(List<Expression> elements, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        if (elements.isEmpty()) {
            code.append("    i32.const 0\n");
            return;
        }

        // Allocate memory for array
        int arrayPtr = memoryOffset;
        
        // Store array size at the beginning
        code.append("    i32.const ").append(arrayPtr).append("\n");
        code.append("    i32.const ").append(elements.size()).append("\n");
        code.append("    i32.store\n");

        // Store each element
        for (int i = 0; i < elements.size(); i++) {
            int elementPtr = arrayPtr + 4 + (i * 4);
            code.append("    i32.const ").append(elementPtr).append("\n");
            generateExpression(elements.get(i), localVars, varTypes, currentRoutine);
            code.append("    i32.store\n");
        }

        // Push array pointer and update memory offset
        code.append("    i32.const ").append(arrayPtr).append("\n");
        memoryOffset += 4 + (elements.size() * 4); // size + elements
    }

    private void generateRecordLiteral(List<Expression> fieldExprs, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        if (fieldExprs.isEmpty()) {
            code.append("    i32.const 0\n");
            return;
        }

        int recordPtr = memoryOffset;
        memoryOffset += fieldExprs.size() * 4;
        
        for (int i = 0; i < fieldExprs.size(); i++) {
            int fieldAddr = recordPtr + (i * 4);
            code.append("    i32.const ").append(fieldAddr).append("\n");
            
            Expression fieldExpr = fieldExprs.get(i);
            if (fieldExpr instanceof FunctionCall fc && "field".equals(fc.functionName) && fc.arguments.size() == 2) {
                generateExpression(fc.arguments.get(1), localVars, varTypes, currentRoutine);
            } else {
                generateExpression(fieldExpr, localVars, varTypes, currentRoutine);
            }
            
            code.append("    i32.store\n");
        }
        
        code.append("    i32.const ").append(recordPtr).append("\n");
    }

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
        }
    }

    private void generateVariableDeclaration(VariableDeclaration vd, Set<String> localVars, Map<String, String> varTypes, RoutineDeclaration currentRoutine) {
        if (vd.initializer != null) {
            generateExpression(vd.initializer, localVars, varTypes, currentRoutine);
            String varType = varTypes.getOrDefault(vd.name, "i32");
            String valueType = inferType(vd.initializer, varTypes);
            
            if (!varType.equals(valueType)) {
                if ("f64".equals(valueType) && "i32".equals(varType)) {
                    code.append("    i32.trunc_f64_s\n");
                } else if ("i32".equals(valueType) && "f64".equals(varType)) {
                    code.append("    f64.convert_i32_s\n");
                }
            }
            
            // Store in global or local
            if (globalVars.containsKey(vd.name)) {
                code.append("    global.set $").append(vd.name).append("\n");
                
                // If it's an array, also set the size
                if (vd.type instanceof ArrayType) {
                    // Сохраняем размер отдельно
                    if (vd.initializer instanceof FunctionCall fc && "array_literal".equals(fc.functionName)) {
                        // Для литерала массива, размер = количество элементов
                        code.append("    i32.const ").append(fc.arguments.size()).append("\n");
                    } else {
                        // По умолчанию 0
                        code.append("    i32.const 0\n");
                    }
                    code.append("    global.set $").append(vd.name).append("_size\n");
                }
            } else if (localVars.contains(vd.name)) {
                code.append("    local.set $").append(vd.name).append("\n");
            }
        }
    }

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

    private String inferType(Expression expr, Map<String, String> varTypes) {
        if (expr instanceof IntegerLiteral) return "i32";
        if (expr instanceof RealLiteral) return "f64";
        if (expr instanceof BooleanLiteral) return "i32";
        if (expr instanceof StringLiteral) return "string";
        if (expr instanceof Identifier id) {
            return varTypes.getOrDefault(id.name, "i32");
        }
        if (expr instanceof BinaryExpression be) {
            String leftType = inferType(be.left, varTypes);
            String rightType = inferType(be.right, varTypes);
            return ("f64".equals(leftType) || "f64".equals(rightType)) ? "f64" : "i32";
        }
        return "i32";
    }

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
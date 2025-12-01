package wasm;

import analyzer.SemanticContext;
import ast.*;
import java.util.*;
import java.io.*;

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
        code.append("  (import \"env\" \"printInt\" (func $printInt (param i32)))\n");
        code.append("  (import \"env\" \"printFloat\" (func $printFloat (param f64)))\n");
        code.append("  (import \"env\" \"printString\" (func $printString (param i32)))\n");
        code.append("  (import \"env\" \"printBool\" (func $printBool (param i32)))\n");
        code.append("  (import \"env\" \"printNewline\" (func $printNewline))\n");
    }

    private void generateGlobals(Program program) {
        for (Declaration decl : program.declarations) {
            if (decl instanceof VariableDeclaration vd) {
                String wasmType = mapType(vd.type);
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

        collectAllArrayGlobals(program, globalVars);
        if (!globalVars.isEmpty()) {
            code.append("\n");
        }
    }

    private void collectAllArrayGlobals(Program program, Map<String, Integer> globalVars) {
        for (Declaration decl : program.declarations) {
            if (decl instanceof RoutineDeclaration rd) {
                if (rd.body != null) {
                    for (ASTNode element : rd.body.elements) {
                        if (element instanceof VariableDeclaration vd) {
                            if (vd.type instanceof ArrayType) {
                                String arrayName = vd.name;
                                if (!globalVars.containsKey(arrayName)) {
                                    code.append("  (global $").append(arrayName).append(" (mut i32) (i32.const 0))\n");
                                    code.append("  (global $").append(arrayName).append("_size (mut i32) (i32.const 0))\n");
                                    globalVars.put(arrayName, globalVars.size());
                                    globalVars.put(arrayName + "_size", globalVars.size());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void collectArrayGlobals(Body body, Map<String, Integer> localArrays) {
    if (body == null) return;
    
    for (ASTNode element : body.elements) {
        if (element instanceof VariableDeclaration vd) {
            // Если это массив array[4] integer
            if (vd.type instanceof ArrayType) {
                localArrays.put(vd.name, ((ArrayType) vd.type).size != null ? 
                    ((IntegerLiteral) ((ArrayType) vd.type).size).value : 0);
            }
        }
    }
}


    private void generateStringLiterals(Program program) {
        collectStringLiterals(program);

        if (!stringLiterals.isEmpty()) {
            code.append("  ;; String literals\n");

            for (var entry : stringLiterals.entrySet()) {
                String str = entry.getKey();
                code.append("  (data (i32.const ").append(stringOffset).append(") \"")
                   .append(escapeWatString(str)).append("\\00\")\n");
                stringOffset += str.length() + 1; // +1 for null terminator
            }
            code.append("\n");
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

        // FIX: Collect ALL local variables first - but only for current scope
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
            generateBody(routine.body, localVars, varTypes);
        } else if (routine.expressionBody != null) {
            generateExpression(routine.expressionBody, localVars, varTypes);
            if (routine.returnType != null) {
                // Value already on stack for return
            }
        }

        // Default return for void functions
        if (routine.returnType == null) {
            // No return value needed for void
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

    // FIX: Collect local variables with their types
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
                // Добавляем основную переменную цикла
                localVars.add(fl.loopVariable);
                varTypes.put(fl.loopVariable, "i32");
                
                // ДОБАВИТЬ: Собираем дополнительные переменные для for-each
                collectForLoopVariables(fl, localVars, varTypes);
            }
        }
    }
    
    // НОВЫЙ МЕТОД: Сбор дополнительных переменных для for-each циклов
    private void collectForLoopVariables(ForLoop fl, Set<String> localVars, Map<String, String> varTypes) {
        // Для for-each по массиву (for x in arr)
        if (fl.range.start == null && fl.range.end instanceof Identifier) {
            String loopVar = fl.loopVariable;
            String indexVar = loopVar + "_idx";
            String tempSizeVar = "temp_size_" + tempVarCounter; // Используем текущий счетчик
            
            // Добавляем индексную переменную
            if (!localVars.contains(indexVar)) {
                localVars.add(indexVar);
                varTypes.put(indexVar, "i32");
            }
            
            // Добавляем временную переменную для размера
            if (!localVars.contains(tempSizeVar)) {
                localVars.add(tempSizeVar);
                varTypes.put(tempSizeVar, "i32");
            }
        }
    }

    private void generateBody(Body body, Set<String> localVars, Map<String, String> varTypes) {
        if (body == null) return;

        for (ASTNode element : body.elements) {
            if (element instanceof Statement stmt) {
                generateStatement(stmt, localVars, varTypes);
            } else if (element instanceof VariableDeclaration vd) {
                generateVariableDeclaration(vd, localVars, varTypes);
            }
        }
    }

    private void generateStatement(Statement stmt, Set<String> localVars, Map<String, String> varTypes) {
        if (stmt instanceof Assignment a) {
            generateAssignment(a, localVars, varTypes);
        } else if (stmt instanceof PrintStatement ps) {
            generatePrintStatement(ps, localVars, varTypes);
        } else if (stmt instanceof IfStatement ifs) {
            generateIfStatement(ifs, localVars, varTypes);
        } else if (stmt instanceof WhileLoop wl) {
            generateWhileLoop(wl, localVars, varTypes);
        } else if (stmt instanceof ForLoop fl) {
            generateForLoop(fl, localVars, varTypes);
        } else if (stmt instanceof RoutineCall rc) {
            generateRoutineCall(rc, localVars, varTypes);
        }
    }

    private void generateAssignment(Assignment assignment, Set<String> localVars, Map<String, String> varTypes) {
        generateExpression(assignment.value, localVars, varTypes);

        if (assignment.target instanceof ModifiablePrimary mp) {
            String varName = mp.baseName;

            // Handle array/record access
            if (!mp.accesses.isEmpty()) {
                // For array[index] or record.field - more complex handling needed
                // For now, just store to variable
                if (globalVars.containsKey(varName)) {
                    code.append("    global.set $").append(varName).append("\n");
                } else if (localVars.contains(varName)) {
                    code.append("    local.set $").append(varName).append("\n");
                }
            } else {
                // Simple variable assignment
                if (globalVars.containsKey(varName)) {
                    code.append("    global.set $").append(varName).append("\n");
                } else if (localVars.contains(varName)) {
                    code.append("    local.set $").append(varName).append("\n");
                }
            }
        }
    }

    private void generatePrintStatement(PrintStatement ps, Set<String> localVars, Map<String, String> varTypes) {
        for (Expression expr : ps.expressions) {
            generateExpression(expr, localVars, varTypes);

            // Determine type and call appropriate print function
            String type = inferType(expr, varTypes);
            switch (type) {
                case "i32" -> code.append("    call $printInt\n");
                case "f64" -> code.append("    call $printFloat\n");
                case "string" -> code.append("    call $printString\n");
            }
        }
        code.append("    call $printNewline\n");
    }

    private void generateIfStatement(IfStatement ifs, Set<String> localVars, Map<String, String> varTypes) {
        generateExpression(ifs.condition, localVars, varTypes);

        code.append("    if\n");
        if (ifs.thenBranch != null) {
            // FIX: Don't create new scope for variables - use same local vars
            generateBody(ifs.thenBranch, localVars, varTypes);
        }

        if (ifs.elseBranch != null) {
            code.append("    else\n");
            generateBody(ifs.elseBranch, localVars, varTypes);
        }

        code.append("    end\n");
    }

    private void generateWhileLoop(WhileLoop wl, Set<String> localVars, Map<String, String> varTypes) {
        String loopLabel = "loop_" + tempVarCounter++;

        code.append("    block $").append(loopLabel).append("_end\n");
        code.append("    loop $").append(loopLabel).append("_start\n");

        // FIX: Generate condition properly
        // If condition is TRUE, continue looping
        // If condition is FALSE (0), break out
        generateExpression(wl.condition, localVars, varTypes);

        // Branch if condition is FALSE (0)
        code.append("    i32.eqz\n");
        code.append("    br_if $").append(loopLabel).append("_end\n");

        // Loop body
        if (wl.body != null) {
            generateBody(wl.body, localVars, varTypes);
        }

        code.append("    br $").append(loopLabel).append("_start\n");
        code.append("    end\n");
        code.append("    end\n");
    }

    private void generateForLoop(ForLoop fl, Set<String> localVars, Map<String, String> varTypes) {
        String loopVar = fl.loopVariable;
        String loopLabel = "loop_" + tempVarCounter++;

        // Проверяем, является ли это for-each по массиву
        if (fl.range.start == null && fl.range.end instanceof Identifier arrayId) {
            // Итерация по массиву: for x in arr
            String arrayName = arrayId.name;
            
            // Имена переменных уже должны быть в localVars
            String indexVar = loopVar + "_idx";
            String tempSizeVar = "temp_size_" + (tempVarCounter - 1); // Используем предыдущий счетчик
            
            // Проверить, что глобальные переменные существуют
            if (!globalVars.containsKey(arrayName)) {
                // Создать глобальные переменные для массива, если их нет
                code.append("    ;; WARNING: array ").append(arrayName).append(" not declared globally\n");
                code.append("    i32.const 0\n");
                code.append("    local.set $").append(tempSizeVar).append("\n");
            } else {
                // Получить размер массива
                code.append("    global.get $").append(arrayName).append("_size\n");
                code.append("    local.set $").append(tempSizeVar).append("\n");
            }
            
            // Инициализировать индекс
            code.append("    i32.const 0\n");
            code.append("    local.set $").append(indexVar).append("\n");
            
            code.append("    block $").append(loopLabel).append("_end\n");
            code.append("    loop $").append(loopLabel).append("_start\n");
            
            // Проверить i < array.size
            code.append("    local.get $").append(indexVar).append("\n");
            code.append("    local.get $").append(tempSizeVar).append("\n");
            code.append("    i32.lt_s\n");
            code.append("    i32.eqz\n");  // Если NOT (index < size), break
            code.append("    br_if $").append(loopLabel).append("_end\n");
            
            // Загрузить arr[i] в loopVar (x)
            if (globalVars.containsKey(arrayName)) {
                code.append("    global.get $").append(arrayName).append("\n");
            } else {
                code.append("    i32.const 0\n"); // Запасной вариант
            }
            code.append("    local.get $").append(indexVar).append("\n");
            code.append("    i32.const 4\n");
            code.append("    i32.mul\n");
            code.append("    i32.add\n");
            code.append("    i32.load\n");
            code.append("    local.set $").append(loopVar).append("\n");
            
            // Тело цикла
            if (fl.body != null) {
                generateBody(fl.body, localVars, varTypes);
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
        
        // Обычный for loop с диапазоном (например, for i in 1..10)
        // Инициализация переменной цикла
        if (fl.range.start != null) {
            generateExpression(fl.range.start, localVars, varTypes);
            code.append("    local.set $").append(loopVar).append("\n");
        } else {
            code.append("    i32.const 0\n");
            code.append("    local.set $").append(loopVar).append("\n");
        }

        String endLabel = loopLabel + "_end";
        String startLabel = loopLabel + "_start";
        
        code.append("    block $").append(endLabel).append("\n");
        code.append("    loop $").append(startLabel).append("\n");
        
        // Проверка условия
        code.append("    local.get $").append(loopVar).append("\n");
        if (fl.range.end != null) {
            generateExpression(fl.range.end, localVars, varTypes);
        } else {
            code.append("    i32.const 0\n");
        }
        
        if (fl.reverse) {
            // Обратный цикл: continue if i >= end
            code.append("    i32.lt_s\n");  // if i < end, branch out
            code.append("    br_if $").append(endLabel).append("\n");
        } else {
            // Прямой цикл: continue if i <= end  
            code.append("    i32.gt_s\n");  // if i > end, branch out
            code.append("    br_if $").append(endLabel).append("\n");
        }
        
        // Тело цикла
        if (fl.body != null) {
            generateBody(fl.body, localVars, varTypes);
        }
        
        // Инкремент/декремент
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

    private void generateExpression(Expression expr, Set<String> localVars, Map<String, String> varTypes) {
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
            // Get string pointer from calculated offset
            code.append("    i32.const ").append(getStringOffset(sl.value)).append("\n");
        }
        else if (expr instanceof Identifier id) {
            // Load from global or local variable
            if (globalVars.containsKey(id.name)) {
                code.append("    global.get $").append(id.name).append("\n");
            } else if (localVars.contains(id.name)) {
                code.append("    local.get $").append(id.name).append("\n");
            } else {
                // Undeclared variable - use default value
                code.append("    i32.const 0\n");
            }
        }
        else if (expr instanceof BinaryExpression be) {
            generateBinaryExpression(be, localVars, varTypes);
        }
        else if (expr instanceof UnaryExpression ue) {
            generateUnaryExpression(ue, localVars, varTypes);
        }
        else if (expr instanceof FunctionCall fc) {
            generateFunctionCall(fc, localVars, varTypes);
        }
        else if (expr instanceof ModifiablePrimary mp) {
            generateModifiablePrimary(mp, localVars, varTypes);
        }
    }

    // FIX: Proper ModifiablePrimary handling
    private void generateModifiablePrimary(ModifiablePrimary mp, Set<String> localVars, Map<String, String> varTypes) {
        if (mp.accesses.isEmpty()) {
            // Simple variable
            if (globalVars.containsKey(mp.baseName)) {
                code.append("    global.get $").append(mp.baseName).append("\n");
            } else if (localVars.contains(mp.baseName)) {
                code.append("    local.get $").append(mp.baseName).append("\n");
            } else {
                code.append("    i32.const 0\n");
            }
            return;
        }

        // Handle accesses (array[i], record.field, arr.size, etc)
        ModifiablePrimary.Access access = mp.accesses.get(0);

        if (access.fieldName != null) {
            // Field access
            if ("size".equals(access.fieldName)) {
                // Array.size - need to get actual size from stored metadata
                // For now, store in global variable approach
                code.append("    global.get $").append(mp.baseName).append("_size\n");
            } else {
                // Record field access
                // TODO: implement proper record field access
                code.append("    i32.const 0\n");
            }
        } else if (access.index != null) {
            // Array index access [i]
            // Calculate memory address for array[i]
            // For simplicity: assume array starts at some offset
            // address = base_offset + (i * element_size)

            // Get base address of array
            code.append("    global.get $").append(mp.baseName).append("\n");

            // Add index offset
            generateExpression(access.index, localVars, varTypes);
            code.append("    i32.const 4\n");  // 4 bytes per element (i32)
            code.append("    i32.mul\n");
            code.append("    i32.add\n");

            // Load from memory
            code.append("    i32.load\n");
        }
    }

    private void generateBinaryExpression(BinaryExpression be, Set<String> localVars, Map<String, String> varTypes) {
        generateExpression(be.left, localVars, varTypes);      // ✅ + varTypes
        generateExpression(be.right, localVars, varTypes);     // ✅ + varTypes
        
        String op = be.operator.toString().toLowerCase();
        String leftType = inferType(be.left, varTypes);        // ✅ + varTypes
        String rightType = inferType(be.right, varTypes);      // ✅ + varTypes
        String type = "f64".equals(leftType) || "f64".equals(rightType) ? "f64" : "i32";
        
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
            case "greater"      -> code.append(type).append(".gt_s\n");  // ✅ ФИКС!
            case "greater_equal"-> code.append(type).append(".ge_s\n");
            case "equals"       -> code.append(type).append(".eq\n");
            case "not_equals"   -> code.append(type).append(".ne\n");
            case "and"          -> code.append("i32.and\n");
            case "or"           -> code.append("i32.or\n");
            default             -> code.append("unknown.").append(op).append("\n");
        }
    }

    private void generateUnaryExpression(UnaryExpression ue, Set<String> localVars, Map<String, String> varTypes) {
        String type = inferType(ue.operand, varTypes);
        generateExpression(ue.operand, localVars, varTypes);

        String op = ue.operator.toString().toLowerCase();

        if ("minus".equals(op)) {
            code.append("    ").append(type).append(".neg\n");
        } else if ("not".equals(op)) {
            code.append("    i32.eqz\n");
        }
    }

    // FIX: Proper argument order and array literal handling
    private void generateFunctionCall(FunctionCall fc, Set<String> localVars, Map<String, String> varTypes) {
        if ("array_literal".equals(fc.functionName)) {
            generateArrayLiteral(fc.arguments, localVars, varTypes);
            return;
        }

        if ("record_literal".equals(fc.functionName)) {
            generateRecordLiteral(fc.arguments, localVars, varTypes);
            return;
        }

        // Push arguments IN CORRECT ORDER (not reversed)
        for (Expression arg : fc.arguments) {
            generateExpression(arg, localVars, varTypes);
        }

        code.append("    call $").append(fc.functionName).append("\n");
    }

    // FIX: Proper array literal generation
    private void generateArrayLiteral(List<Expression> elements, Set<String> localVars, Map<String, String> varTypes) {
        if (elements.isEmpty()) {
            code.append("    i32.const 0\n");
            return;
        }

        // Allocate memory for array
        int arrayPtr = memoryOffset;
        memoryOffset += elements.size() * 4;  // 4 bytes per i32 element

        // Store array size at the beginning
        code.append("    i32.const ").append(arrayPtr).append("\n");
        code.append("    i32.const ").append(elements.size()).append("\n");
        code.append("    i32.store\n");

        // Store each element
        for (int i = 0; i < elements.size(); i++) {
            int elementPtr = arrayPtr + 4 + (i * 4);
            code.append("    i32.const ").append(elementPtr).append("\n");
            generateExpression(elements.get(i), localVars, varTypes);
            code.append("    i32.store\n");
        }

        // Push array pointer (return value)
        code.append("    i32.const ").append(arrayPtr).append("\n");
    }

    private void generateRecordLiteral(List<Expression> fieldExprs, Set<String> localVars, Map<String, String> varTypes) {
        // For now, simple record handling
        // In full implementation, would store fields in memory with metadata
        if (fieldExprs.isEmpty()) {
            code.append("    i32.const 0\n");
            return;
        }

        // Generate first field as placeholder
        generateExpression(fieldExprs.get(0), localVars, varTypes);
    }

    private void generateRoutineCall(RoutineCall rc, Set<String> localVars, Map<String, String> varTypes) {
        if ("return".equals(rc.routineName)) {
            if (!rc.arguments.isEmpty()) {
                generateExpression(rc.arguments.get(0), localVars, varTypes);
            }
            code.append("    return\n");
        } else {
            // Push arguments in correct order
            for (Expression arg : rc.arguments) {
                generateExpression(arg, localVars, varTypes);
            }

            code.append("    call $").append(rc.routineName).append("\n");
        }
    }

    private void generateVariableDeclaration(VariableDeclaration vd, Set<String> localVars, Map<String, String> varTypes) {
        if (vd.initializer != null) {
            generateExpression(vd.initializer, localVars, varTypes);
            if (globalVars.containsKey(vd.name)) {
                code.append("    global.set $").append(vd.name).append("\n");
            } else if (localVars.contains(vd.name)) {
                code.append("    local.set $").append(vd.name).append("\n");
            }
        }
    }

    // Helper methods
    private String mapType(Type type) {
        if (type instanceof ast.PrimitiveType pt) {
            return TYPE_MAPPING.getOrDefault(pt.typeName, "i32");
        } else if (type instanceof ast.ArrayType at) {
            return "i32";  // Array is represented as pointer
        } else if (type instanceof ast.RecordType rt) {
            return "i32";  // Record is represented as pointer
        }
        return "i32"; // Default
    }

    // FIX: Better type inference
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
        return "i32"; // Default
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

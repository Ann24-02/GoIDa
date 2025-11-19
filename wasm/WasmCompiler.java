package wasm;

import analyzer.*;
import parser.*;
import ast.*;
import java.io.*;
import java.nio.file.*;

/**
 * WASM Compiler - compiles the language to WebAssembly
 */
public class WasmCompiler {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: WasmCompiler <input-file> [output-file]");
            System.err.println("Example: WasmCompiler program.rout program.wasm");
            return;
        }
        
        String inputFile = args[0];
        String outputFile = args.length > 1 ? args[1] : 
            inputFile.replace(".rout", ".wasm");
        
        compileToWasm(inputFile, outputFile);
    }
    
    public static void compileToWasm(String inputFile, String outputFile) throws Exception {
        System.out.println("🚀 Compiling " + inputFile + " to WebAssembly...");
        
        // Read source code
        String source = Files.readString(Path.of(inputFile));
        
        // Parse
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        System.out.println("✅ Parsing successful");
        
        // Semantic analysis
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(program);
        System.out.println("✅ Semantic analysis passed");
        
        // Optimization
        OptimizationEngine optimizer = new OptimizationEngine();
        Program optimized = optimizer.optimize(program);
        System.out.println("✅ Optimization completed");
        
        // Generate WASM
        WasmCodeGenerator generator = new WasmCodeGenerator(analyzer.getSemanticContext());
        String watCode = generator.generate(optimized);
        
        // Write WAT file (intermediate)
        String watFile = outputFile.replace(".wasm", ".wat");
        Files.writeString(Path.of(watFile), watCode);
        System.out.println("📄 Generated: " + watFile);
        
        // Convert to WASM binary
        boolean success = convertWatToWasm(watFile, outputFile);
        
        if (success) {
            System.out.println("🎉 Successfully compiled: " + outputFile);
            System.out.println("🌐 You can now run: node wasm-runner.js " + outputFile);
        } else {
            System.err.println("❌ Failed to create WASM binary");
            System.err.println("💡 Install wat2wasm or use online converter");
        }
    }
    
    private static boolean convertWatToWasm(String watFile, String wasmFile) {
        try {
            System.out.println("🔧 Converting WAT to WASM...");
            
            // Try wat2wasm command
            Process process = Runtime.getRuntime().exec(new String[]{
                "wat2wasm", watFile, "-o", wasmFile
            });
            
            int result = process.waitFor();
            
            // Read any error output
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println("wat2wasm: " + line);
            }
            
            if (result == 0) {
                System.out.println("✅ Generated: " + wasmFile);
                return true;
            } else {
                System.err.println("❌ wat2wasm failed with exit code: " + result);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error running wat2wasm: " + e.getMessage());
            System.err.println("💡 Make sure wat2wasm is installed and in PATH");
            return false;
        }
    }
}
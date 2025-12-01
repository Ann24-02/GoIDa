package wasm;

import analyzer.*;
import ast.*;
import java.io.*;
import java.nio.file.*;
import parser.*;

// WebAssembly compiler - main entry point
// Compiles source files to WebAssembly binary format
public class WasmCompiler {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: WasmCompiler <input-file> [output-dir]");  // uses default 'output' directory
            return;
        }
        
        String inputFile = args[0];
        String outputDir = args.length > 1 ? args[1] : "output";
        
        compileToWasm(inputFile, outputDir);
    }
    
    // Compile source file to WebAssembly
    public static void compileToWasm(String inputFile, String outputDir) throws Exception {
        System.out.println("Compiling " + inputFile + " to WebAssembly...");
        
        // Create output directory
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        System.out.println("Output directory: " + outputPath.toAbsolutePath());
        
        // Read source code
        String source = Files.readString(Path.of(inputFile));
        
        // Parse
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        System.out.println("Parsing successful");
        
        // Semantic analysis
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(program);
        System.out.println("Semantic analysis passed");
        
        // Optimization
        OptimizationEngine optimizer = new OptimizationEngine();
        Program optimized = optimizer.optimize(program);
        System.out.println("Optimization completed");
        
        // Generate WASM
        WasmCodeGenerator generator = new WasmCodeGenerator(analyzer.getSemanticContext());
        String watCode = generator.generate(optimized);
        
        // Get base filename without extension
        String baseName = getBaseName(new File(inputFile).getName());
        
        // Write WAT text file
        String watFile = outputPath.resolve(baseName + ".wat").toString();
        Files.writeString(Path.of(watFile), watCode);
        System.out.println("Generated: " + watFile);
        
        // Convert to WASM binary
        String wasmFile = outputPath.resolve(baseName + ".wasm").toString();
        boolean success = convertWatToWasm(watFile, wasmFile);
        
        if (success) {
            System.out.println("Successfully compiled: " + wasmFile);
            System.out.println("You can now run the WASM file in browser");
        } else {
            System.err.println("Failed to create WASM binary");
            System.err.println("Install wat2wasm or use online converter");
        }
    }
    
    // Extract filename without extension
    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
    
    // Convert WAT text to WASM binary using wat2wasm
    private static boolean convertWatToWasm(String watFile, String wasmFile) {
        try {
            System.out.println("Converting WAT to WASM...");
            
            // Run wat2wasm command
            Process process = Runtime.getRuntime().exec(new String[]{
                "wat2wasm", watFile, "-o", wasmFile
            });
            
            int result = process.waitFor();
            
            // Read error output
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println("wat2wasm: " + line);
            }
            
            if (result == 0) {
                System.out.println("Generated: " + wasmFile);
                return true;
            } else {
                System.err.println("wat2wasm failed with exit code: " + result);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error running wat2wasm: " + e.getMessage());
            System.err.println("Make sure wat2wasm is installed and in PATH");
            return false;
        }
    }
}
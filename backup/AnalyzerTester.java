import analyzer.*;
import parser.*;
import ast.*;
import java.io.*;
import java.nio.file.*;

// Test runner for semantic analyzer and optimizer
// Tests a single source file and shows results
public class AnalyzerTester {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java AnalyzerTester <file.rout>");
            return;
        }
        
        String filename = args[0];
        boolean updateMode = args.length > 1 && ("--update".equals(args[1]) || "-u".equals(args[1]));
        
        try {
            testFile(filename, updateMode);
        } catch (Exception e) {
            System.err.println("Error testing " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testFile(String filename, boolean updateMode) throws Exception {
        System.out.println("== Testing: " + filename + " ==");
        
        // Read source code from file
        String source = new String(Files.readAllBytes(Paths.get(filename)));
        
        // Parse the program
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        
        // Run semantic analysis
        testSemanticAnalysis(program, filename, updateMode);
        
        // Run optimization
        testOptimization(program, filename, updateMode);
    }
    
    private static void testSemanticAnalysis(Program program, String filename, boolean updateMode) {
        System.out.println("\n-- Semantic Analysis --");
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        
        try {
            analyzer.analyze(program);
            System.out.println(" Semantic analysis passed");
            
            // Print any warnings
            for (String warning : analyzer.getWarnings()) {
                System.out.println("  Warning: " + warning);
            }
            
        } catch (SemanticException e) {
            System.out.println(" Semantic error: " + e.getMessage());
        }
    }
    
    private static void testOptimization(Program program, String filename, boolean updateMode) {
        System.out.println("\n-- Optimization --");
        
        OptimizationEngine optimizer = new OptimizationEngine();
        Program optimized = optimizer.optimize(program);
        
        System.out.println(" Optimization completed: " + optimizer.getOptimizationCount() + " optimizations applied");
        
        // Show AST size comparison
        printASTComparison(program, optimized);
    }
    
    private static void printASTComparison(Program original, Program optimized) {
        System.out.println("\nOriginal AST size: " + countNodes(original));
        System.out.println("Optimized AST size: " + countNodes(optimized));
        
        // Indicate if changes were made
        if (countNodes(original) != countNodes(optimized)) {
            System.out.println(" AST was modified by optimizations");
        }
    }
    
    private static int countNodes(ASTNode node) {
        return 1; // Simplified node counter
    }
}
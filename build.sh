#!/bin/bash

echo "Compiling ALL .rout tests to WebAssembly"
echo "============================================"

# Create output directory
OUTPUT_DIR="output"
mkdir -p $OUTPUT_DIR

echo " Output directory: $OUTPUT_DIR/"
echo ""

# Compile Java sources first
echo " Compiling Java sources..."
javac -d out analyzer/*.java ast/*.java parser/*.java wasm/*.java

if [ $? -ne 0 ]; then
    echo "❌ Java compilation failed!"
    exit 1
fi
echo "✅ Java sources compiled successfully"
echo ""

# Counter for success/failure
success_count=0
fail_count=0

# Compile all .rout files in tests directory
for test_file in tests/*.rout; do
    if [ -f "$test_file" ]; then
        filename=$(basename "$test_file")
        echo " Compiling: $filename"
        
        # Compile with 2-second timeout to avoid hanging
        timeout 10s java -cp out wasm.WasmCompiler "$test_file" "$OUTPUT_DIR" 2>/dev/null
        
        if [ $? -eq 0 ]; then
            echo "✅ SUCCESS: $filename"
            ((success_count++))
        else
            echo "❌ FAILED: $filename (syntax error or timeout)"
            ((fail_count++))
        fi
        echo ""
    fi
done

echo "COMPILATION SUMMARY:"
echo "   ✅ Successful: $success_count"
echo "   ❌ Failed: $fail_count"
echo "   Total files processed: $((success_count + fail_count))"
echo ""
echo " Generated files in: $OUTPUT_DIR/"
ls -la $OUTPUT_DIR/
echo ""
echo "To run any program, use the HTML runtime:"
echo "   open runtime/real-wasm-runner.html"
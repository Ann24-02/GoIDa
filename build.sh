echo "🔨 Building compiler..."
javac -d out analyzer/*.java ast/*.java parser/*.java wasm/*.java

echo ""
echo "🚀 Compiling test program to WASM..."
java -cp out wasm.WasmCompiler program.rout program.wasm

echo ""
echo "📁 Generated files:"
ls -la program.*

echo ""
echo "🌐 To run in browser:"
echo "   open runtime/real-wasm-runner.html"
echo ""
echo "🐍 To run with Python (install wasmtime first):"
echo "   pip install wasmtime"
echo "   python wasm_runner.py program.wasm"
echo ""
echo "📋 Or use online WAT converter:"
echo "   https://webassembly.github.io/wabt/demo/wat2wasm/"
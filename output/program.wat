(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $main
    (local $x i32)
    (local $temp i32)
    i32.const 42
    local.set $x
    local.get $x
    call $printInt
    call $printNewline
  )

  (export "main" (func $main))
)

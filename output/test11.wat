(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $main
    (local $p i32)
    i32.const 0
    i32.const 0
    call $field
    local.set $p
    i32.const 0
    i32.const 1
i32.add
    local.set $p
    i32.const 0
    call $printInt
    i32.const 0
    call $printInt
    call $printNewline
  )

  (export "main" (func $main))
)

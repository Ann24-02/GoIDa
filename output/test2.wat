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
    (local $y i32)
    (local $z i32)
    i32.const 5
    local.set $x
    i32.const 3
    local.set $y
    local.get $x
    local.get $y
i32.mul
    i32.const 2
i32.add
    local.set $z
    local.get $z
    call $printInt
    call $printNewline
  )

  (export "main" (func $main))
)

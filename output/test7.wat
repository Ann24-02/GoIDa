(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $main
    (local $a1 i32)
    (local $a2 i32)
    i32.const 256
    i32.const 3
    i32.store
    i32.const 260
    i32.const 10
    i32.store
    i32.const 264
    i32.const 20
    i32.store
    i32.const 268
    i32.const 30
    i32.store
    i32.const 256
    local.set $a1
    local.get $a1
    local.set $a2
    i32.const 99
    local.get $a1
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.store
    local.get $a2
    i32.const 4
    i32.add
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.load
    call $printInt
    call $printNewline
  )

  (export "main" (func $main))
)

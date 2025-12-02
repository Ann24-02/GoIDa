(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  ;; String literals
  (data (i32.const 0) "Alice\00")
  (data (i32.const 6) "name\00")
  (data (i32.const 11) "age\00")

  (func $main
    (local $p i32)
    (local $temp i32)
    i32.const 256
    i32.const 0
    i32.store
    i32.const 260
    i32.const 30
    i32.store
    i32.const 256
    local.set $p
    local.get $p
    i32.const 4
    i32.add
    i32.load
    i32.const 1
i32.add
    local.set $temp
    local.get $p
    i32.const 4
    i32.add
    local.get $temp
    i32.store
    local.get $p
    i32.load
    call $printString
    local.get $p
    i32.const 4
    i32.add
    i32.load
    call $printInt
    call $printNewline
  )

  (export "main" (func $main))
)

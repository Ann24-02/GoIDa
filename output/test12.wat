(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $main
    (local $a i32)
    (local $b i32)
    (local $c i32)
    (local $d i32)
    (local $result i32)
    i32.const 10
    local.set $a
    i32.const 20
    local.set $b
    i32.const 1
    local.set $c
    i32.const 0
    local.set $d
    local.get $a
    local.get $b
i32.lt_s
    local.get $c
    local.get $d
i32.eq
i32.and
    local.get $a
    local.get $b
i32.le_s
i32.or
    local.set $result
    local.get $result
    if
    i32.const 1
    call $printInt
    call $printNewline
    else
    i32.const 0
    call $printInt
    call $printNewline
    end
    local.get $c
    local.get $d
i32.eq
    local.get $a
    local.get $b
i32.le_s
    local.get $a
    local.get $b
i32.lt_s
i32.and
i32.or
    local.set $result
    local.get $result
    if
    i32.const 1
    call $printInt
    call $printNewline
    else
    i32.const 0
    call $printInt
    call $printNewline
    end
  )

  (export "main" (func $main))
)

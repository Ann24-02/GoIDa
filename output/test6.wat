(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $main
    (local $r f64)
    (local $i i32)
    (local $b i32)
    (local $temp i32)
    f64.const 3.6
    local.set $r
    i32.const 0
    local.set $b
    local.get $r
    i32.trunc_f64_s
    local.set $i
    local.get $b
    f64.convert_i32_s
    local.set $r
    local.get $b
    local.set $i
    local.get $i
    call $printInt
    local.get $r
    call $printFloat
    call $printNewline
  )

  (export "main" (func $main))
)

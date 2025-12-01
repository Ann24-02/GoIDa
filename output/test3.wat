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
    i32.const 10
    local.set $a
    local.get $a
    i32.const 5
i32.gt_s
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

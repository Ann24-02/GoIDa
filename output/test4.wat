(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $main
    (local $i i32)
    i32.const 1
    local.set $i
    block $loop_0_end
    loop $loop_0_start
    local.get $i
    i32.const 5
i32.le_s
    i32.eqz
    br_if $loop_0_end
    local.get $i
    call $printInt
    call $printNewline
    local.get $i
    i32.const 1
i32.add
    local.set $i
    br $loop_0_start
    end
    end
  )

  (export "main" (func $main))
)

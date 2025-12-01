(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $main
    (local $sum i32)
    (local $i i32)
    (local $temp i32)
    i32.const 0
    local.set $sum
    i32.const 1
    local.set $i
    block $loop_0_end
    loop $loop_0_start
    local.get $i
    i32.const 5
    i32.gt_s
    br_if $loop_0_end
    local.get $sum
    local.get $i
i32.add
    local.set $sum
    local.get $i
    i32.const 1
    i32.add
    local.set $i
    br $loop_0_start
    end
    end
    local.get $sum
    call $printInt
    call $printNewline
  )

  (export "main" (func $main))
)

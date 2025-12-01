(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  ;; String literals
  (data (i32.const 0) "Result:\00")
  (data (i32.const 8) "Total:\00")

  (global $x (mut i32) (i32.const 5))
  (global $y (mut i32) (i32.const 10))

  (func $calculate (param $a i32) (param $b i32)
    (local $result i32)
    local.get $a
    local.get $b
i32.add
    local.set $result
    i32.const 0
    call $printString
    local.get $result
    call $printInt
    call $printNewline
  )

  (func $main
    (local $total i32)
    (local $i i32)
    global.get $x
    global.get $y
    call $calculate
    global.get $x
    global.get $y
i32.add
    local.set $total
    i32.const 8
    call $printString
    local.get $total
    call $printInt
    call $printNewline
    local.get $total
    i32.const 0
i32.gt_s
    if
    i32.const 0
    call $printString
    call $printNewline
    else
    i32.const 0
    call $printString
    call $printNewline
    end
    i32.const 1
    local.set $i
    block $loop_0_end
    loop $loop_0_start
    local.get $i
    i32.const 5
    i32.gt_s
    br_if $loop_0_end
    i32.const 0
    call $printString
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

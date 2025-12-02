(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  ;; String literals
  (data (i32.const 0) "d =\00")
  (data (i32.const 4) "c =\00")
  (data (i32.const 8) "X is large\00")
  (data (i32.const 19) "(should be 8)\00")
  (data (i32.const 33) "Constants:\00")
  (data (i32.const 44) "(should be true)\00")
  (data (i32.const 61) "Before return\00")
  (data (i32.const 75) "=== Optimization tests completed ===\00")
  (data (i32.const 112) "=== Testing Optimizations ===\00")
  (data (i32.const 142) "(should be 11)\00")
  (data (i32.const 157) "(should be 36)\00")
  (data (i32.const 172) "Always printed\00")
  (data (i32.const 187) "This else removed\00")
  (data (i32.const 205) "b =\00")
  (data (i32.const 209) "a =\00")

  (global $a (mut i32) (i32.const 11))
  (global $b (mut i32) (i32.const 36))
  (global $c (mut i32) (i32.const 1))
  (global $d (mut i32) (i32.const 8))

  (func $test_dead_code
    (local $temp i32)
    i32.const 61
    call $printString
    call $printNewline
    return
  )

  (func $test_if_simple
    (local $temp i32)
    i32.const 1
    if
    i32.const 172
    call $printString
    call $printNewline
    end
  )

  (func $test_combined
    (local $x i32)
    (local $temp i32)
    i32.const 20
    local.set $x
    local.get $x
    i32.const 10
i32.gt_s
    if
    i32.const 8
    call $printString
    call $printNewline
    else
    i32.const 187
    call $printString
    call $printNewline
    end
  )

  (func $main
    (local $temp i32)
    i32.const 112
    call $printString
    call $printNewline
    i32.const 33
    call $printString
    call $printNewline
    i32.const 209
    call $printString
    global.get $a
    call $printInt
    i32.const 142
    call $printString
    call $printNewline
    i32.const 205
    call $printString
    global.get $b
    call $printInt
    i32.const 157
    call $printString
    call $printNewline
    i32.const 4
    call $printString
    global.get $c
    call $printInt
    i32.const 44
    call $printString
    call $printNewline
    i32.const 0
    call $printString
    global.get $d
    call $printInt
    i32.const 19
    call $printString
    call $printNewline
    call $test_dead_code
    call $test_if_simple
    call $test_combined
    i32.const 75
    call $printString
    call $printNewline
  )

  (export "main" (func $main))
)

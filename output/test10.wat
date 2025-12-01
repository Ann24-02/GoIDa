(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $sum (param $arr i32) (result i32)
    (local $s i32)
    (local $x i32)
    (local $x_idx_7_5 i32)
    (local $temp_size_7_5 i32)
    i32.const 0
    local.set $s
    local.get $arr
    i32.load
    local.set $temp_size_7_5
    i32.const 0
    local.set $x_idx_7_5
    block $loop_0_end
    loop $loop_0_start
    local.get $x_idx_7_5
    local.get $temp_size_7_5
    i32.lt_s
    i32.eqz
    br_if $loop_0_end
    local.get $arr
    i32.const 4
    i32.add
    local.get $x_idx_7_5
    i32.const 4
    i32.mul
    i32.add
    i32.load
    local.set $x
    local.get $s
    local.get $x
i32.add
    local.set $s
    local.get $x_idx_7_5
    i32.const 1
    i32.add
    local.set $x_idx_7_5
    br $loop_0_start
    end
    end
    local.get $s
    return
  )

  (func $main
    (local $a i32)
    (local $total i32)
    i32.const 256
    i32.const 4
    i32.store
    i32.const 260
    i32.const 2
    i32.store
    i32.const 264
    i32.const 2
    i32.store
    i32.const 268
    i32.const 2
    i32.store
    i32.const 272
    i32.const 2
    i32.store
    i32.const 256
    local.set $a
    local.get $a
    call $sum
    local.set $total
    local.get $total
    call $printInt
    call $printNewline
  )

  (export "main" (func $main))
)

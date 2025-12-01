(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $main
    (local $first i32)
    (local $last i32)
    (local $k i32)
    (local $j i32)
    (local $acc i32)
    (local $n i32)
    (local $i i32)
    i32.const 3
    local.set $first
    i32.const 6
    local.set $last
    local.get $first
    local.set $k
    block $loop_0_end
    loop $loop_0_start
    local.get $k
    local.get $last
    i32.gt_s
    br_if $loop_0_end
    local.get $k
    call $printInt
    call $printNewline
    local.get $k
    i32.const 1
    i32.add
    local.set $k
    br $loop_0_start
    end
    end
    i32.const 2
    local.set $j
    block $loop_1_end
    loop $loop_1_start
    local.get $j
    i32.const 5
    i32.lt_s
    br_if $loop_1_end
    local.get $j
    call $printInt
    call $printNewline
    local.get $j
    i32.const 1
    i32.sub
    local.set $j
    br $loop_1_start
    end
    end
    i32.const 1
    local.set $acc
    i32.const 4
    local.set $n
    i32.const 1
    local.set $i
    block $loop_2_end
    loop $loop_2_start
    local.get $i
    local.get $n
i32.le_s
    i32.eqz
    br_if $loop_2_end
    local.get $acc
    local.get $i
i32.mul
    local.set $acc
    local.get $i
    i32.const 1
i32.add
    local.set $i
    br $loop_2_start
    end
    end
    local.get $acc
    i32.const 24
i32.eq
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

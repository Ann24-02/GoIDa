(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (global $myData (mut i32) (i32.const 0))
  (global $myData_size (mut i32) (i32.const 0))

  (func $printReverse (param $arr i32)
    (local $i i32)
    global.get $arr_size
    local.set $i
    block $loop_0_end
    loop $loop_0_start
    local.get $i
    i32.const 1
i32.ge_s
    i32.eqz
    br_if $loop_0_end
    global.get $arr
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load
    call $printInt
    call $printNewline
    local.get $i
    i32.const 1
i32.sub
    local.set $i
    br $loop_0_start
    end
    end
  )

  (func $main
    i32.const 256
    i32.const 4
    i32.store
    i32.const 260
    i32.const 10
    i32.store
    i32.const 264
    i32.const 20
    i32.store
    i32.const 268
    i32.const 30
    i32.store
    i32.const 272
    i32.const 40
    i32.store
    i32.const 256
    global.set $myData
    global.get $myData
    call $printReverse
  )

  (export "main" (func $main))
)

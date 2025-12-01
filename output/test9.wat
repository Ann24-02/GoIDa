(module
  (import "env" "printInt" (func $printInt (param i32)))
  (import "env" "printFloat" (func $printFloat (param f64)))
  (import "env" "printString" (func $printString (param i32)))
  (import "env" "printBool" (func $printBool (param i32)))
  (import "env" "printNewline" (func $printNewline))
  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $add (param $a i32) (param $b i32) (result i32)
    (local $temp i32)
    local.get $a
    local.get $b
i32.add
  )

  (func $is_even (param $x i32) (result i32)
    (local $temp i32)
    local.get $x
    i32.const 2
    i32.rem_s
    i32.const 0
i32.eq
  )

  (func $main
    (local $s i32)
    (local $temp i32)
    i32.const 7
    i32.const 5
    call $add
    local.set $s
    local.get $s
    call $printInt
    call $printNewline
    local.get $s
    call $is_even
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

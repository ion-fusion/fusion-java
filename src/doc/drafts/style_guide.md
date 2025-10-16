# Fusion Style Guide

## Naming Conventions

In general lisp languages tend to use `lisp-case`. Ion requires single quotes around symbols with
hyphens so instead Fusion variable naming uses `snake_case`.

All variable and procedure names match: `^[a-zA-Z_][a-zA-Z0-9_]*$`.

* Constants in Capitals `snake_case`: `MY_CONSTANT`
* All other names: `snake_case`
* Predicates prefixed with `is_`: `is_foo`

## Indentation

These are the basic rules:

* Use 2 spaces to indent. No tabs.
* Use spaces to indent wrapped lines.
* Align function parameters in columns if you need to wrap lines. Always use a consistent alignment
  at the top level of any given
  expression. [Control statements](#Control_Statements)
  and [declaration statements](#Declaration_Statements)
  are special cases where the body statements are left at two-space indentation to deliberately
  separate them from the condition/binding statements.

Ultimately, good indentation should allow you to very quickly parse code visually by clearly showing
the scope of any statement. Good indentation eliminates the need to count parenthesis.

### **Why 2 Spaces?**

Two-space indentation is the largely used standard for languages in the Lisp family. Since
indentation levels in these languages commonly nest much more quickly than in a C-like language,
more aggressive indentation can quickly lead to lines running off to the right once you have
multiple nested expressions. The choice of two spaces is intended to minimize the need to wrap lines
while still providing a way to quickly distinguish expression scope visually.

## Parentheses

Close blocks with parentheses on the same line. Good indentation already indicates scope, so closing
parens on their own line is wasted vertical space and visual clutter.

```
(when thing1
  (when thing2
    (when thing3
      this)))
```

## White Space

## Blank Lines

## New Lines

## Control Statements

### **if**

```
(**if** short_condition
  then_body
  else_body)
or
(if
  short|long_condition
  then_body
  else_body)
```

### **when**

```
(when short_condition
  then_body)
or
(when
  short|long_condition
  then_body)
```

### **unless**

```
(unless short_condition
  then_body)
or
(unless
  short|long_condition
  then_body)

```

### **cond**

```
(cond
  (condition1 (* 1 2))
  (condition2
    (* 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20))
  (true 0)) 
```

### **do**

```
(**do** (**lambda** (thing) (short_lambda thing))
  things)

(do
  (**lambda** (thing)
    (longer_lambda_body thing))
  things)
```

### **for_list**

```
(for_list [(list_element1 [1, 2, 3])
           (list_element2 [4, 5, 6])]
  (* list_element1 list_element2))
```

## Declaration Statements

### **procedure**

Single argument.

```
(**define** (my_procedure arg1)
  (is_truthy arg1))
```

Multiple arguments.

```
(**define** (my_procedure
          arg1
          argN)
  (is_truthy (**or** arg1 argN)))
```

### **let/lets**

```
(**let** [(variable1 1),
      (variable2
        (a_procedure_to_get_data_that_is_very_long
          2)),]
  (+ variable1 variable2))
```

## Line Wrapping

Follow the [indentation](#Indentation) rules when wrapping a line that gets too long.

### **Examples**

If the arguments to the outermost expression are short enough to put inline but an inner expression
makes the line long enough that you want to wrap it, you can vertically align the inner expression's
arguments

```
(few_args arg1 (lengthy_inner_call another_arg
                                   more_arg))
```

As an alternative solution to the above, the outermost expression can be vertically aligned and the
inner expression written inline.

```
(few_args arg1
          (lengthy_inner_call another_arg more_arg))
```

If a procedure's name is so long that vertically aligning its arguments after its name would still
cause the line to be undesirably long, you can vertically align all of its arguments at two spaces
beneath it.

```
(few_args arg1
          (incredibly_lengthy_procedure_name
            another_arg
            more_arg))
```

Sometimes expression arguments are all long enough to merit doing the above.

```
(long_args
  super_long_first_arg
  (incredibly_lengthy_procedure_name
    another_arg
    more_arg))
```

### **Common Mistakes**

`more_arg` is not immediately evident as a parameter to `lengthy_inner_call`.

```
(few_args arg1 (lengthy_inner_call another_arg
                 more_arg))
```

`arg2` is easily overlooked when you mix vertical and inline arguments.

```
(more_args arg1 arg2
           (lengthy_inner_call another_arg more_arg))
```

### Immutable Struct Definition

Single variable.

```
{ field_1: 1 }
```

Single or Multi-variable.

```
{
  field_1: 1,
  field_2: 2,
}
or
{ field_1: 1,
  field_2: 2,
}
```

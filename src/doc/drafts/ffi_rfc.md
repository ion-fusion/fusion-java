# FFI RFC

> Credit to Ted Sandler

As an embeddable language that can live within host applications, Fusion needs a simple foreign
function interface (FFI) in order to get data in and out of the Fusion runtime. Here I outline a
very simple (read as "brain-dead simple") interface which meets this need. This RFC is intended to
serve as a conversation starter for designing an initial "worse is better" Fusion FFI, which can
later be superceded by more comprehensive APIs.

## Proposal / Request for Comment

1. Foreign functions should be programmatically bindable to names in the outermost lexical scope
   during construction of the Fusion runtime.

    ```
    FusionBinding myFFIBinding = new FusionBinding(
         String varname,
         new FusionForeignFunction( (FusionCallable) myFFIInstance )
    );
    
    List<FusionBinding> someGlobalBindings = new ArrayList<FusionBinding>();
    
    myGlobalBindings.add(myFFIBinding);
    
    FusionRuntime fusionRuntime = new FusionRuntime(
         someGlobalBindings,
         ...  /* the rest of the runtime constructor args */
    );
    
    fusionRuntime.start();
    ```

2. Foreign function objects need to implement the `FusionCallable` API, which is a simple
   Ion-in/Ion-out interface.

    ```
    public interface FusionCallable {
        public IonValue apply(IonValue[] argv);
        public int getArity();
    }
    ```

3. During the Fusion runtime's initialization phase, lexical bindings to Fusion foreign functions
   are transformed such that:

    ```
    new FusionBinding(
         String varname,
         new FusionForeignFunction(FusionCallable myFFIInstance)
    )
    ```

   would be equivalent to writing the following Fusion source code, if Fusion actually had a read
   syntax for `FusionCallable` objects:

    ```
    `(define ,varname
       (lambda (arg0 arg1 ... argN)
         (ffi_apply ,myFFIInstance arg0 arg1 ... argN)))
    ```

   This surface syntax is purely for illustrative purposes. Since `FusionCallable` objects are
   compiled java code, they have no read syntax and therefore it's impossible to write the form
    ```
    `(define ... ,myFFIInstance ...)
    ```
   within a Fusion program. Also note that "N" is the arity that's returned by the getArity() method
   defined in the FusionCallable interface.

4. The `ffi_apply` form is meant to be a Fusion builtin function. It may be considered a reserved
   word that is unallowed in Fusion source code, or it could behave like other identifiers for other
   builtin functions. The ffi_apply form is responsible for packaging up the runtime arguments and
   converting them from Fusion runtime types to standard IonValues. In a continuation passing
   virtual machine, it's also responsible for passing the result of the foreign-function call to the
   current continuation.

5. Since different FFI interfaces will be desired, the FFI apply builtins should probably be
   versioned, e.g., `ffi_apply_1_0`, `ffi_apply_1_1`, ...

## Open Questions

1. Q: How to handle exceptions thrown within a foreign function?

    * A: One solution I can envision is to treat a foreign-function like a dependent service call.
      Some errors are transient/retryable, some are non-fatal but not retryable, and others are
      plain fatal. A foreign function could be required to trap exceptions and rethrow them wrapped
      in an exception container representing one of these Fusion-FFI exception classes.

1. Q: Why does `FusionCallable` limit inputs and outputs to `IonValue`? This seems like
   `FusionCallable` is second-class with respect to Fusion itself. Why not have the parameters and
   return be `FusionValue` or some such type?

    * A: The idea here is to design something simple which can be shipped in the near future which
      avoids sticky issues surrounding foreign-functions invoking fusion procedures which were
      passed to them as arguments.

1. Q: How does the `getArity():int` work for variadic functions?

    * A: Instead, `getArity()` could return a sum-type called ‘`Arity`' (aka an enum in java) which
      is either `VariadicArity` or `IntegerArity(x)` with `x` an integer.

1. Q: Does Fusion have keyword functions, and if so, how does this facility map?
2. Q: What extant FFI systems have we evaluated to create this proposal? What impedance mismatches
   do other systems have that we're trying to avoid?

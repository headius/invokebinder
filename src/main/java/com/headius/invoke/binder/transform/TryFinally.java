package com.headius.invoke.binder.transform;

import com.headius.invoke.binder.Binder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An try-finally transform.
 *
 * Equivalent call: A combination of folds and catches.
 *
 * MethodHandle exceptionHandler = [drop exception and invoke post logic]
 * target = MethodHandles.catchException(target, Throwable.class, exceptionHandler)
 * target = MethodHandles.foldArguments(post, target)
 */
public class TryFinally extends Transform {

    private final MethodHandle post;

    public TryFinally(MethodHandle post) {
        this.post = post;
    }

    public MethodHandle up(MethodHandle target) {
        MethodHandle rethrow = Binder
                .from(target.type().insertParameterTypes(0, Throwable.class))
                .drop(1, target.type().parameterCount())
                .throwException();

        MethodHandle exceptionHandler = Binder
                .from(target.type().insertParameterTypes(0, Throwable.class))
                .drop(0)
                .invoke(post);
        exceptionHandler = MethodHandles.foldArguments(rethrow, exceptionHandler);

        target = MethodHandles.catchException(target, Throwable.class, exceptionHandler);
        return  MethodHandles.foldArguments(post, target);
    }

    public MethodType down(MethodType type) {
        return type;
    }

    public String toString() {
        return "try/finally with " + post;
    }
}

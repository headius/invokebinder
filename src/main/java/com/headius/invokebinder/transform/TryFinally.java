/*
 * Copyright 2012-2014 headius.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.headius.invokebinder.transform;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;

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

    private static final MethodHandle tryFinallyJava9;

    public TryFinally(MethodHandle post) {
        this.post = post;
    }

    public MethodHandle up(MethodHandle target) {
        if (Util.isJava9()) return nativeTryFinally(target, post);

        MethodHandle exceptionHandler = Binder
                .from(target.type().insertParameterTypes(0, Throwable.class).changeReturnType(void.class))
                .drop(0)
                .invoke(post);

        MethodHandle rethrow = Binder
                .from(target.type().insertParameterTypes(0, Throwable.class))
                .fold(exceptionHandler)
                .drop(1, target.type().parameterCount())
                .throwException();

        target = MethodHandles.catchException(target, Throwable.class, rethrow);

        // if target returns a value, we must return it regardless of post
        MethodHandle realPost = post;
        if (target.type().returnType() != void.class) {
            // modify post to ignore return value
            MethodHandle newPost = Binder
                    .from(target.type().insertParameterTypes(0, target.type().returnType()).changeReturnType(void.class))
                    .drop(0)
                    .invoke(post);

            // fold post into an identity chain that only returns the value
            realPost = Binder
                    .from(target.type().insertParameterTypes(0, target.type().returnType()))
                    .fold(newPost)
                    .drop(1, target.type().parameterCount())
                    .identity();
        }

        return MethodHandles.foldArguments(realPost, target);
    }

    private MethodHandle nativeTryFinally(MethodHandle target, MethodHandle post) {
        MethodType targetType = target.type();
        boolean voidReturn = targetType.returnType() == Void.TYPE;
        MethodType finallyType = targetType.insertParameterTypes(0, Throwable.class);
        int dropCount = 1;

        if (!voidReturn) {
            finallyType = finallyType.insertParameterTypes(1, targetType.returnType());
            dropCount = 2;
        }

        MethodHandle wrapPost = Binder
                .from(finallyType)
                .drop(0, dropCount)
                .invoke(post);

        if (!voidReturn) {
            wrapPost = Binder.from(finallyType)
                    .foldVoid(wrapPost)
                    .permute(1)
                    .identity();
        }

        try {
            return (MethodHandle) tryFinallyJava9.invokeExact(target, wrapPost);
        } catch (Throwable t) {
            throw new RuntimeException("Java 9 detected but MethodHandles.tryFinally missing", t);
        }
    }

    public MethodType down(MethodType type) {
        return type;
    }

    public String toString() {
        return "try/finally with " + post;
    }

    static {
        if (Util.isJava9()) {
            tryFinallyJava9 = Binder.from(MethodHandle.class, MethodHandle.class, MethodHandle.class).invokeStaticQuiet(MethodHandles.lookup(), MethodHandles.class, "tryFinally");
        } else {
            tryFinallyJava9 = null;
        }
    }
}

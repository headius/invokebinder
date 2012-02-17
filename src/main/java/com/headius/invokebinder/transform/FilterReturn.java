package com.headius.invokebinder.transform;

import com.headius.invokebinder.InvalidTransformException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
* An return-filtering transform.
*
* Equivalent call: MethodHandles.filterReturn(MethodHandle, MethodHandle).
*/
public class FilterReturn extends Transform {

private final MethodHandle function;

public FilterReturn(MethodHandle function) {
    this.function = function;
}

public MethodHandle up(MethodHandle target) {
    return MethodHandles.filterReturnValue(target, function);
}

public MethodType down(MethodType type) {
    int count = function.type().parameterCount();
    switch (count) {
        case 0:
            return type.changeReturnType(void.class);
        case 1:
            return type.changeReturnType(function.type().parameterType(0));
        default:
            throw new InvalidTransformException("return filter " + function + " does not accept zero or one argument");
    }
}

public String toString() {
    return "filter return with " + function;
}
}

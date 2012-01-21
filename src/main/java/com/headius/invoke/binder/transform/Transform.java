package com.headius.invoke.binder.transform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * Abstract reprensentation of some handle transform.
*/
public abstract class Transform {
    /**
     * Apply this transform upward from the given MethodHandle, producing
     * a new handle.
     *
     * @param target the target handle
     * @return the adapted handle
     */
    public abstract MethodHandle up(MethodHandle target);

    /**
     * Apply this transform downward from an incoming MethodType, producing
     * a new type.
     *
     * @param source the source type
     * @return the new type
     */
    public abstract MethodType down(MethodType source);

    /**
     * Return a string representation of this transform.
     *
     * @return
     */
    public abstract String toString();
}

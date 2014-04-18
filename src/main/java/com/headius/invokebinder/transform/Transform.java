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

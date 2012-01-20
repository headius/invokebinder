package com.headius.invoke.binder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Binder {

    private final Logger logger = Logger.getLogger("Invoke Binder");
    private final List<Transform> transforms = new ArrayList<>();
    private final List<MethodType> types = new ArrayList<>();
    private final MethodType start;

    public Binder(MethodType start) {
        this.start = start;
        this.types.add(0, start);
    }

    public static Binder from(MethodType start) {
        return new Binder(start);
    }

    public static Binder from(Class returnType, Class... argTypes) {
        return from(MethodType.methodType(returnType, argTypes));
    }

    private void add(Transform transform) {
        add(transform, transform.down(types.get(0)));
    }

    private void add(Transform transform, MethodType target) {
        types.add(0, target);
        transforms.add(0, transform);
    }

    private MethodType currentType() {
        return types.get(0);
    }

    public Binder insert(int index, Object value) {
        add(new Insert(index, value));
        return this;
    }

    public Binder drop(int index) {
        return drop(index, 1);
    }

    public Binder drop(int index, int count) {
        add(new Drop(index, Arrays.copyOfRange(currentType().parameterArray(), index, index + count)));
        return this;
    }

    public Binder convert(MethodType target) {
        add(new Convert(types.get(0)), target);
        return this;
    }

    public Binder cast(MethodType type) {
        add(new Cast(type));
        return this;
    }

    public Binder cast(Class returnType, Class... argTypes) {
        add(new Cast(MethodType.methodType(returnType, argTypes)));
        return this;
    }

    // Differing API between docs and OS X OpenJDK build
//    public Binder spread(int count) {
//        transforms.add(0, new Spread(count));
//        return this;
//    }

    public Binder reorder(int... reorder) {
        transforms.add(0, new Reorder(types.get(0), reorder));
        return this;
    }

    /**
     * Apply the chain of transforms with the target method handle as the final
     * endpoint. Produces a handle that has the transforms in given sequence.
     *
     * @param target the endpoint handle to bind to
     * @return a handle that has all transforms applied in sequence up to endpoint
     */
    public MethodHandle invoke(MethodHandle target) {
        MethodHandle current = target;
        for (Transform t : transforms) {
            current = t.up(current);
        }
        assert current.type().equals(start);

        return current;
    }

    public static abstract class Transform {

        public abstract MethodHandle up(MethodHandle target);

        public abstract MethodType down(MethodType source);

        public abstract String toString();
    }

    public static class Insert extends Transform {

        private final int position;
        private final Object value;

        public Insert(int position, Object value) {
            this.position = position;
            this.value = value;
        }

        public MethodHandle up(MethodHandle target) {
            return MethodHandles.insertArguments(target, position, value);
        }

        public MethodType down(MethodType type) {
            return type.insertParameterTypes(position, value.getClass());
        }

        public String toString() {
            return "insert " + value.getClass() + " at " + position;
        }
    }

    public static class Drop extends Transform {

        private final int position;
        private final Class[] types;

        public Drop(int position, Class... types) {
            this.position = position;
            this.types = types;
        }

        public MethodHandle up(MethodHandle target) {
            return MethodHandles.dropArguments(target, position, types);
        }

        public MethodType down(MethodType type) {
            return type.dropParameterTypes(position, types.length);
        }

        public String toString() {
            return "drop " + Arrays.toString(types) + " at " + position;
        }
    }

    public static class Convert extends Transform {

        private final MethodType type;

        public Convert(MethodType type) {
            this.type = type;
        }

        public MethodHandle up(MethodHandle target) {
            return target.asType(type);
        }

        public MethodType down(MethodType type) {
            for (int i = 0; i < type.parameterCount(); i++) {
                type = type.changeParameterType(i, type.parameterArray()[i]);
            }
            return type;
        }

        public String toString() {
            return "convert args to " + type;
        }
    }

    public static class Cast extends Transform {

        private final MethodType type;

        public Cast(MethodType type) {
            this.type = type;
        }

        public MethodHandle up(MethodHandle target) {
            return MethodHandles.explicitCastArguments(target, type);
        }

        public MethodType down(MethodType type) {
            for (int i = 0; i < type.parameterCount(); i++) {
                type = type.changeParameterType(i, type.parameterArray()[i]);
            }
            return type;
        }

        public String toString() {
            return "cast args to " + type;
        }
    }

    public static class Reorder extends Transform {

        private final MethodType source;
        private final int[] reorder;

        public Reorder(MethodType source, int... reorder) {
            this.source = source;
            this.reorder = reorder;
        }

        public MethodHandle up(MethodHandle target) {
            return MethodHandles.permuteArguments(target, source, reorder);
        }

        public MethodType down(MethodType type) {
            Class[] types = new Class[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                types[i] = type.parameterType(i);
            }
            return MethodType.methodType(type.returnType(), types);
        }

        public String toString() {
            return "reorder " + source + " with " + Arrays.toString(reorder);
        }
    }
}

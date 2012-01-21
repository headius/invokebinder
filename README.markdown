Introduction
============

This library hopes to provide a more friendly DSL for binding method handles.
Unlike the normal MethodHandle API, handles are bound forward from a source
MethodType and eventually adapted to a final target MethodHandle. Along the way
the transformations are pushed onto a stack and eventually applied in reverse
order, as the standard API demands.

Examples
========

Transformation calls can be chained. They are not applied until an eventual
"invoke" is called with the target endpoint MethodHandle.

    MethodHandle mh = Binder
       .from(String.class, String.class, String.class) // String w(String, String)
       .drop(1, String.class) // String x(String)
       .insert(0, 'hello') // String y(String, String)
       .cast(String.class, CharSequence.class, Object.class) // String z(CharSequence, Object)
       .invoke(someTargetHandle);

Status
======

This is currently under development. Not all transformations from the MethodHandle
API are yet supported.

Contributors are welcome :)
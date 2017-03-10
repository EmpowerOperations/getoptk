package com.empowerops.getoptk

class TreeIteratorSandbox {


    // so currently I'm not building any object which is directly traversable in post order.
    // in particular, to do dependency validation, I need to check that every type you've specified
    // through getOpt is "reachable" from string form. The easiest way to express this
    // is with a traversal of the dependencies.

    // such a dependency graph would also allow me to more easily reduce the token set,
    // and express errors (eg
    // "failed to convert 'xyz' to 'A',
    //   while generating arg2 for A,
    //   while generating arg1:A for C.constructor(a: A, str: String) specified by ArgsThing.c = getObjectOpt<C>()
    //
    //
}
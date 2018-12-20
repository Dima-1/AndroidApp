package com.strikelines.app.utils


/**
 * Creation and initialization logic for Singletons w/arguments (objects are easier, but can't use constructors with args)
 */
open class SingletonHolder<out T, in A, in C>(creator: (A,C) -> T) {
    private var creator: ((A,C) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg1: A, arg2:C): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg1, arg2)
                instance = created
                creator = null
                created
            }
        }
    }
}

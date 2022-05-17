// KT-46643

external abstract class A {
    open val foo: String
}

class B : A() {
    override var foo: String = "Error: setter was not called."
        set(k) { result = "O$k"}

    lateinit var result: String
}

fun box(): String {
    return B().result
}
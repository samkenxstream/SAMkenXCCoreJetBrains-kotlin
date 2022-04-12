// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_STDLIB
// ALLOW_KOTLIN_PACKAGE
// LINK_VIA_SIGNATURES
// ISSUE: KT-52007

package kotlin

@Deprecated("Use maxOrNull instead.", ReplaceWith("this.maxOrNull()"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
@SinceKotlin("1.1")
@Suppress("CONFLICTING_OVERLOADS")
public fun Array<out Int>.myMax(): String? {
    return null
}

@SinceKotlin("1.7")
@kotlin.jvm.JvmName("maxOrThrow")
@Suppress("CONFLICTING_OVERLOADS")
public fun Array<out Int>.myMax(): String {
    return "OK"
}

fun box(): String {
    val array = arrayOf(1, 2)
    return array.myMax() ?: "Fail"
}

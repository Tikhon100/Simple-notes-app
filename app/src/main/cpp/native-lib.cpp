#include <jni.h>

extern "C" JNIEXPORT jint JNICALL
Java_com_example_lab2_NativeLib_invertNumber(
        JNIEnv* env,
        jobject /* this */,
        jint number) {
    return number == 0 ? 1 : 0;
}
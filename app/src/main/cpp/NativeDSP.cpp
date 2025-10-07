
#include <jni.h>
#include <cmath>

extern "C" {

JNIEXPORT void JNICALL
Java_com_radiomodem_mt63_modem_DSPUtils_fftNative(JNIEnv*, jclass, jdoubleArray, jdoubleArray) {
    // Placeholder (Java fallback used)
}
JNIEXPORT void JNICALL
Java_com_radiomodem_mt63_modem_DSPUtils_ifftNative(JNIEnv*, jclass, jdoubleArray, jdoubleArray) {
    // Placeholder (Java fallback used)
}
JNIEXPORT void JNICALL
Java_com_radiomodem_mt63_modem_DSPUtils_hannNative(JNIEnv* env, jclass, jdoubleArray arr) {
    jsize n = env->GetArrayLength(arr);
    jdouble* a = env->GetDoubleArrayElements(arr, 0);
    for(int i=0;i<n;i++){ a[i] *= 0.5*(1-std::cos(2*M_PI*i/(n-1))); }
    env->ReleaseDoubleArrayElements(arr, a, 0);
}
JNIEXPORT jshort JNICALL
Java_com_radiomodem_mt63_modem_DSPUtils_sat16Native(JNIEnv*, jclass, jdouble v) {
    if(v>32767) return 32767;
    if(v<-32768) return -32768;
    return (short)v;
}

}


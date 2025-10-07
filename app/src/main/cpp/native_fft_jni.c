#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
typedef struct { float r; float i; } kiss_fft_cpx;
struct kiss_fft_state; typedef struct kiss_fft_state* kiss_fft_cfg;
kiss_fft_cfg kiss_fft_alloc(int nfft,int inverse_fft,void* mem,size_t* lenmem);
void kiss_fft(kiss_fft_cfg cfg,const kiss_fft_cpx *fin,kiss_fft_cpx *fout);
void kiss_fft_free(kiss_fft_cfg cfg);

JNIEXPORT void JNICALL
Java_com_radiomodem_mt63_dsp_NativeFFT_forwardFFT(JNIEnv* env, jclass,
                                                  jfloatArray re, jfloatArray im,
                                                  jint n){
    jfloat* pr = (*env)->GetFloatArrayElements(env, re, 0);
    jfloat* pi = (*env)->GetFloatArrayElements(env, im, 0);
    kiss_fft_cpx* in  = (kiss_fft_cpx*)malloc(n*sizeof(*in));
    kiss_fft_cpx* out = (kiss_fft_cpx*)malloc(n*sizeof(*out));
    for(int i=0;i<n;i++){ in[i].r=pr[i]; in[i].i=pi[i]; }
    kiss_fft_cfg cfg = kiss_fft_alloc(n, 0, 0, 0);
    kiss_fft(cfg, in, out);
    for(int i=0;i<n;i++){ pr[i]=out[i].r; pi[i]=out[i].i; }
    kiss_fft_free(cfg); free(in); free(out);
    (*env)->ReleaseFloatArrayElements(env, re, pr, 0);
    (*env)->ReleaseFloatArrayElements(env, im, pi, 0);
}
JNIEXPORT void JNICALL
Java_com_radiomodem_mt63_dsp_NativeFFT_inverseFFT(JNIEnv* env, jclass,
                                                  jfloatArray re, jfloatArray im,
                                                  jint n){
    jfloat* pr = (*env)->GetFloatArrayElements(env, re, 0);
    jfloat* pi = (*env)->GetFloatArrayElements(env, im, 0);
    kiss_fft_cpx* in  = (kiss_fft_cpx*)malloc(n*sizeof(*in));
    kiss_fft_cpx* out = (kiss_fft_cpx*)malloc(n*sizeof(*out));
    for(int i=0;i<n;i++){ in[i].r=pr[i]; in[i].i=pi[i]; }
    kiss_fft_cfg cfg = kiss_fft_alloc(n, 1, 0, 0);
    kiss_fft(cfg, in, out);
    for(int i=0;i<n;i++){ pr[i]=out[i].r; pi[i]=out[i].i; }
    kiss_fft_free(cfg); free(in); free(out);
    (*env)->ReleaseFloatArrayElements(env, re, pr, 0);
    (*env)->ReleaseFloatArrayElements(env, im, pi, 0);
}
jint JNI_OnLoad(JavaVM* vm, void*){ return JNI_VERSION_1_6; }


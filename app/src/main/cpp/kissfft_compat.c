#include <stdlib.h>
#include <math.h>
#include <string.h>
typedef struct { float r; float i; } kiss_fft_cpx;
typedef struct { int n; int inverse; } kiss_fft_state;
typedef kiss_fft_state* kiss_fft_cfg;
kiss_fft_cfg kiss_fft_alloc(int nfft,int inverse_fft,void* mem,size_t* lenmem){
    size_t need = sizeof(kiss_fft_state);
    if(lenmem) *lenmem = need;
    if(!mem){ mem = malloc(need); if(!mem) return NULL; }
    kiss_fft_cfg st = (kiss_fft_cfg)mem; st->n=nfft; st->inverse=inverse_fft; return st;
}
void kiss_fft_free(kiss_fft_cfg cfg){ if(cfg) free(cfg); }
static void fft_rec(kiss_fft_cpx* a, int n, int inv){
    if(n<=1) return;
    int m=n/2;
    kiss_fft_cpx* e=(kiss_fft_cpx*)malloc(m*sizeof(*e));
    kiss_fft_cpx* o=(kiss_fft_cpx*)malloc(m*sizeof(*o));
    for(int i=0;i<m;i++){ e[i]=a[2*i]; o[i]=a[2*i+1]; }
    fft_rec(e,m,inv); fft_rec(o,m,inv);
    float s = inv? 1.f : -1.f;
    for(int k=0;k<m;k++){
        float ang = 2.f*3.14159265358979323846f*k/n;
        float c = cosf(ang);
        float sn = s*sinf(ang);
        float tr = c*o[k].r - sn*o[k].i;
        float ti = sn*o[k].r + c*o[k].i;
        a[k].r   = e[k].r + tr;
        a[k].i   = e[k].i + ti;
        a[k+m].r = e[k].r - tr;
        a[k+m].i = e[k].i - ti;
    }
    free(e); free(o);
    if(inv){ /* scaling by caller */ }
}
void kiss_fft(kiss_fft_cfg cfg, const kiss_fft_cpx* fin, kiss_fft_cpx* fout){
    int n=cfg->n;
    memcpy(fout, fin, n*sizeof(kiss_fft_cpx));
    fft_rec(fout, n, cfg->inverse);
    if(cfg->inverse){ for(int i=0;i<n;i++){ fout[i].r/=n; fout[i].i/=n; } }
}


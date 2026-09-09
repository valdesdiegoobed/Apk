package com.vaguer.virtualcamshizuku;

interface ICamProbe {
    void destroy() = 16777114;
    String getIdentity() = 1;
    String runCameraProbe() = 2;
}

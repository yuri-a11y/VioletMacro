package com.violet.macro;

// implemented by InjectorService, which runs inside the Shizuku process (shell uid)
interface IInjector {
    void destroy() = 16777114;
    boolean tap(float x, float y, int holdMs, int deviceId, int pointerId) = 1;
    boolean swipe(float x1, float y1, float x2, float y2, int durMs, int deviceId, int pointerId) = 2;
    String info() = 3;
    boolean key(int keyCode, int holdMs) = 4;
}

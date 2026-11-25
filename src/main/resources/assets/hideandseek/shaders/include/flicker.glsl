#version 330

float sniffaFlicker(float flame, float phase, float seconds) {
    float p = phase * 6.2831853;
    if (flame < 0.5) {
        return 0.97 + 0.03 * sin(seconds * (1.3 + phase) + p);
    }
    if (flame < 1.5) {
        return 0.93 + 0.04 * sin(seconds * 7.3 + p) + 0.03 * sin(seconds * 13.1 + p * 2.0);
    }
    if (flame < 2.5) {
        float gust = pow(0.5 + 0.5 * sin(seconds * (0.7 + phase * 0.4) + p * 3.0), 6.0);
        return 0.86 + 0.06 * sin(seconds * 9.7 + p) + 0.05 * sin(seconds * 23.3 + p * 3.0) - 0.12 * gust;
    }
    return 0.8 + 0.09 * sin(seconds * 5.1 + p) + 0.07 * sin(seconds * 11.7 + p * 2.0) + 0.04 * sin(seconds * 19.3 + p * 5.0);
}

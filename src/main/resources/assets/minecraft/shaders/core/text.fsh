#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <hideandseek:text_shine.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#endif

in vec4 vertexColor;
in vec2 texCoord0;
flat in int shineKind;
in vec3 shineLight;

out vec4 fragColor;

void main() {
#ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
#else
    vec4 texColor = texture(Sampler0, texCoord0);
#endif

    vec4 tinted = vertexColor;
    if (shineKind >= SHADOW_OFFSET) {
        tinted = shine_glow(shineKind - SHADOW_OFFSET, vertexColor.a);
        tinted.rgb *= shineLight;
    } else if (shineKind > 0) {
        vec2 texel = floor(texCoord0 * vec2(textureSize(Sampler0, 0)));
        tinted = vec4(shine_colour(shineKind, tinted.rgb / max(shineLight, vec3(0.0001)), gl_FragCoord.xy, ScreenSize, texel, GameTime * 1200.0) * shineLight, vertexColor.a);
    }

#ifdef IS_SEE_THROUGH
    vec4 color = texColor * tinted;
#else
    vec4 color = texColor * tinted * ColorModulator;
#endif
    if (color.a < 0.1) {
        discard;
    }

#ifdef IS_SEE_THROUGH
    fragColor = color * ColorModulator;
#elif defined(IS_GUI)
    fragColor = color;
#else
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
#endif
}

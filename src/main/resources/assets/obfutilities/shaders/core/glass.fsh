#version 150

in vec4 vColor;
in vec2 vLocal;
flat in vec2 vHalf;
flat in vec3 vShape;
flat in float vBackdrop;

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform vec4 ColorModulator;

out vec4 fragColor;

void main() {
    float alpha = 1.0;
    // a plain rectangle (no radius, stroke or blur) keeps hard edges so adjacent ones never show a seam
    if (vShape.x > 0.0 || vShape.y > 0.0 || vShape.z > 0.0) {
        float r = min(vShape.x, min(vHalf.x, vHalf.y));
        vec2 q = abs(vLocal) - vHalf + r;
        // signed distance to the rounded rectangle, negative inside
        float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        float px = max(max(fwidth(vLocal.x), fwidth(vLocal.y)), 0.0001);
        if (vShape.z > 0.0) {
            // soft shadow, drawn outside the shape only so it never darkens a see-through panel
            float f = 1.0 - smoothstep(0.0, vShape.z, d);
            alpha = f * f * clamp(d / px + 0.5, 0.0, 1.0);
        } else {
            alpha = clamp(0.5 - d / px, 0.0, 1.0);
            if (vShape.y > 0.0) alpha *= clamp(0.5 + (d + vShape.y) / px, 0.0, 1.0);
        }
    }
    vec4 color;
    if (vBackdrop > 0.5) {
        // frosted glass: the blurred copy of the screen, tinted; the tint's alpha is how much of it shows
        vec3 behind = texture(Sampler0, gl_FragCoord.xy / ScreenSize).rgb;
        color = vec4(mix(behind, vColor.rgb, vColor.a), alpha * smoothstep(0.0, 0.12, vColor.a));
    } else {
        color = vec4(vColor.rgb, vColor.a * alpha);
    }
    color *= ColorModulator;
    if (color.a <= 0.0) discard;
    fragColor = color;
}

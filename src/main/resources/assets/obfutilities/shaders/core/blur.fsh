#version 150

uniform sampler2D Sampler0;
uniform vec2 Dir;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // 9-tap gaussian folded into 5 fetches by sampling between texels
    vec3 c = texture(Sampler0, texCoord).rgb * 0.2270270270;
    c += (texture(Sampler0, texCoord + Dir * 1.3846153846).rgb + texture(Sampler0, texCoord - Dir * 1.3846153846).rgb) * 0.3162162162;
    c += (texture(Sampler0, texCoord + Dir * 3.2307692308).rgb + texture(Sampler0, texCoord - Dir * 3.2307692308).rgb) * 0.0702702703;
    fragColor = vec4(c, 1.0);
}

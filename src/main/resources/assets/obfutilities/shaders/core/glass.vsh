#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vColor;
out vec2 vLocal;
flat out vec2 vHalf;
flat out vec3 vShape;
flat out float vBackdrop;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    // UV0: position from the centre of the shape. UV1: half size, 1/8 units. UV2: radius and stroke, 1/16 units.
    vLocal = UV0;
    vHalf = vec2(UV1) / 8.0;
    vShape = vec3(vec2(UV2) / 16.0, Normal.x * 31.75);
    vBackdrop = Normal.y;
}

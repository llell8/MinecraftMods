#version 330

// Phoenix Client: Meteor-style "shader" ESP. Replaces Minecraft's soft blurred glow with a crisp
// outline around the entity's real shape (armour and held items included) plus a see-through fill.
// The input has each glowing entity's silhouette drawn in its outline colour.

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

const float WIDTH = 2.0;  // outline thickness in pixels
const float FILL = 0.3;   // opacity of the fill inside the entity

void main() {
    vec2 texel = 1.0 / InSize;
    vec4 center = texture(InSampler, texCoord);

    // Inside the entity: translucent fill
    if (center.a > 0.0) {
        fragColor = vec4(center.rgb, FILL);
        return;
    }

    // Just outside it: solid outline, in the colour of the nearest entity pixel
    for (float x = -WIDTH; x <= WIDTH; x += 1.0) {
        for (float y = -WIDTH; y <= WIDTH; y += 1.0) {
            if (x * x + y * y > WIDTH * WIDTH + 0.5) continue;
            vec4 near = texture(InSampler, texCoord + vec2(x, y) * texel);
            if (near.a > 0.0) {
                fragColor = vec4(near.rgb, 1.0);
                return;
            }
        }
    }
    fragColor = vec4(0.0);
}

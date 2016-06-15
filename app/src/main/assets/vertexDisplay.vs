precision highp float;

attribute vec4 vDPos;
attribute vec2 vDTex;

varying vec2 oDTex;

void main()
{
    gl_Position = vDPos;
    oDTex = vDTex;
}
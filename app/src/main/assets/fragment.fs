#extension GL_OES_EGL_image_external : require
precision highp float;

uniform samplerExternalOES uTexture;
varying vec2 vTextureCoord;

void main()
{
    gl_FragColor = texture2D(uTexture, vTextureCoord);
}
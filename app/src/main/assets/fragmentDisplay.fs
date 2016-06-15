precision highp float;

uniform sampler2D g_TexSrcLeft;
uniform sampler2D g_TexSrcRight;

varying vec2 oDTex;

void main()
{
    vec4 vImgColor = vec4(0.0, 0.0, 0.0, 0.0);
    vec2 tmpTex = vec2(oDTex.x * 2.0, oDTex.y);
    if(tmpTex.x <= 1.0)
    {
        vImgColor = texture2D(g_TexSrcLeft, tmpTex);
    }
    else if(tmpTex.x > 1.0)
    {
        vImgColor = texture2D(g_TexSrcRight, tmpTex - vec2(1.0, 0.0));
    }

    gl_FragColor = vImgColor;
}
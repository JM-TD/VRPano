package com.vr.utils;

import android.content.Context;
import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by BJ-CHXB on 2016/6/15.
 */
public class GLUtils {

    private static final String TAG = "GLUtils";
    public static String SOURCE_SHADER_FRAGMENT = "fragment.fs";
    public static String SOURCE_SHADER_VERTEX = "vertex.vs";
    public static String SOURCE_SHADER_FRAGMENT_DIS = "fragmentDisplay.fs";
    public static String SOURCE_SHADER_VERTEX_DIS = "vertexDisplay.vs";

    public static void checkGLError(String op)
    {
        int nError;
        while((nError = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
        {
            Log.e("GLES ERROR", op + ": glError " + nError);
        }
    }

    public static int getProgram(Context context)
    {
        String vertexStr = getShaderSource(context, SOURCE_SHADER_VERTEX);
        String fragmentStr = getShaderSource(context, SOURCE_SHADER_FRAGMENT);
        return getProgram(vertexStr, fragmentStr);
    }

    public static int getDisplayProgram(Context context)
    {
        String vertexStrDis = getShaderSource(context, SOURCE_SHADER_VERTEX_DIS);
        String fragmentStrDis = getShaderSource(context, SOURCE_SHADER_FRAGMENT_DIS);
        return getProgram(vertexStrDis, fragmentStrDis);
    }

    public static int getProgram(String vertexStr, String fragmentStr)
    {
        int program = GLES20.glCreateProgram();
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexStr);
        GLES20.glShaderSource(fragmentShader, fragmentStr);

        GLES20.glCompileShader(vertexShader);
        int[] compiledV = new int[1];
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, compiledV, 0);
        if(0 == compiledV[0])
        {
            Log.e(TAG, "Could not compile shader " + vertexStr + ":");
            Log.e(TAG, GLES20.glGetShaderInfoLog(vertexShader));
            GLES20.glDeleteShader(vertexShader);
            vertexShader = 0;
        }

        GLES20.glCompileShader(fragmentShader);
        int[] compiledF = new int[1];
        GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, compiledF, 0);
        if(0 == compiledF[0])
        {
            Log.e(TAG, "Could not compile shader " + fragmentStr + ":");
            Log.e(TAG, GLES20.glGetShaderInfoLog(fragmentShader));
            GLES20.glDeleteShader(fragmentShader);
            fragmentShader = 0;
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        return program;
    }

    @NonNull
    public static String getShaderSource(Context context, String sourceName)
    {
        StringBuffer shaderSource = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(sourceName)
            ));
            String tmpStr = null;
            while (null != (tmpStr = br.readLine()))
            {
                shaderSource.append(tmpStr);
                shaderSource.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return shaderSource.toString();
    }
}

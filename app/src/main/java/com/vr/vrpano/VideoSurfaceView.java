package com.vr.vrpano;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.Surface;

import com.vr.utils.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import static android.graphics.Matrix.*;

/**
 * Created by BJ-CHXB on 2016/6/15.
 */
public class VideoSurfaceView extends GLSurfaceView {

    public Surface mSurface;
    private PanoRenderer mPanoRenderer;

    public VideoSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setEGLContextClientVersion(2);
        mPanoRenderer = new PanoRenderer(context);
        setRenderer(mPanoRenderer);
    }

    @Override
    public void onResume()
    {
        mPanoRenderer.createSurface();
        super.onResume();
    }

    public void setSensorAngle(float x, float y)
    {
        mPanoRenderer.mXAngle += x;
        mPanoRenderer.mYAngle += y;
    }

    private class PanoRenderer implements Renderer, SurfaceTexture.OnFrameAvailableListener {

        private static final int GL_TEXTURE_EXTERNAL_OES = 0X8D65;

        private boolean mUpdateSurface;
        private Context mContext;
        private int mWidth;
        private int mHeight;
        private float mFOV;
        public float mXAngle;
        public float mYAngle;
        public float mZAngle;

        private int mSize;
        private FloatBuffer mVertexBuffer;
        private FloatBuffer mTextureBuffer;
        private FloatBuffer mVertexBufferDis;
        private FloatBuffer mTextureBufferDis;

        private SurfaceTexture mSurfaceTexture;
        private int mTextureID;
        private int mFBO;
        private int[] mEyesTexID;
        private int mProgram;
        private int mProgramDis;
        private int mAPositionHandler;
        private int mATextureCoordHandler;
        private int mUProjectMatrixHandler;
        private int mUSTMatrixHandler;
        private int mDisPosHandler;
        private int mDisTexHandler;
        private int mDisLeftHandler;
        private int mDisRightHandler;

        private float[] mSTMatrix = new float[16];
        private float[] mCurrMatrix = new float[16];
        private float[] mMVPMatrix = new float[16];
        private float[] mProjectMatrix = new float[16];

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mUpdateSurface = true;
        }

        public PanoRenderer(Context context) {
            mContext = context;
            Matrix.setIdentityM(mSTMatrix, 0);
            init();
            createSurface();
        }

        private void init()
        {
            int perVertex = 36;

            double perRadius = 2.0 * Math.PI / (float)perVertex;
            double perW = 1.0 / (float)perVertex;
            double perH = 1.0 / (float)perVertex;

            ArrayList<Float> vertexList = new ArrayList<Float>();
            ArrayList<Float> textureList = new ArrayList<Float>();
            float w1, h1, w2, h2, w3, h3, w4, h4;
            float x1, y1, z1;
            float x2, y2, z2;
            float x3, y3, z3;
            float x4, y4, z4;

            for (int a = 0; a != perVertex; ++a)
            {
                for (int b = 0; b != perVertex; ++b)
                {
                    w1 = (float) (a * perH);
                    h1 = (float) (b * perW);

                    w2 = (float) ((a + 1) * perH);
                    h2 = (float) (b * perW);

                    w3 = (float) ((a + 1) * perH);
                    h3 = (float) ((b + 1) * perW);

                    w4 = (float) (a * perH);
                    h4 = (float) ((b + 1) * perW);

                    textureList.add(h1);
                    textureList.add(w1);

                    textureList.add(h2);
                    textureList.add(w2);

                    textureList.add(h3);
                    textureList.add(w3);

                    textureList.add(h3);
                    textureList.add(w3);

                    textureList.add(h4);
                    textureList.add(w4);

                    textureList.add(h1);
                    textureList.add(w1);

                    x1 = (float) (Math.sin(a * perRadius / 2) * Math.cos(b * perRadius));
                    z1 = (float) (Math.sin(a * perRadius / 2) * Math.sin(b * perRadius));
                    y1 = (float) (Math.cos(a * perRadius / 2));

                    x2 = (float) (Math.sin((a + 1) * perRadius / 2) * Math.cos(b * perRadius));
                    z2 = (float) (Math.sin((a + 1) * perRadius / 2) * Math.sin(b * perRadius));
                    y2 = (float) (Math.cos((a + 1) * perRadius / 2));

                    x3 = (float) (Math.sin((a + 1) * perRadius / 2) * Math.cos((b + 1) * perRadius));
                    z3 = (float) (Math.sin((a + 1) * perRadius / 2) * Math.sin((b + 1) * perRadius));
                    y3 = (float) (Math.cos((a + 1) * perRadius / 2));

                    x4 = (float) (Math.sin(a * perRadius / 2) * Math.cos((b + 1) * perRadius));
                    z4 = (float) (Math.sin(a * perRadius / 2) * Math.sin((b + 1) * perRadius));
                    y4 = (float) (Math.cos(a * perRadius / 2));

                    vertexList.add(x1);
                    vertexList.add(y1);
                    vertexList.add(z1);

                    vertexList.add(x2);
                    vertexList.add(y2);
                    vertexList.add(z2);

                    vertexList.add(x3);
                    vertexList.add(y3);
                    vertexList.add(z3);

                    vertexList.add(x3);
                    vertexList.add(y3);
                    vertexList.add(z3);

                    vertexList.add(x4);
                    vertexList.add(y4);
                    vertexList.add(z4);

                    vertexList.add(x1);
                    vertexList.add(y1);
                    vertexList.add(z1);
                }
            }

            mSize = vertexList.size() / 3;
            float texture[] = new float[mSize * 2];
            for (int i = 0; i != texture.length; ++i)
            {
                texture[i] = textureList.get(i);
            }
            mTextureBuffer = ByteBuffer.allocateDirect(texture.length * 4).order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mTextureBuffer.put(texture);
            mTextureBuffer.position(0);

            float vetex[] = new float[mSize * 3];
            for (int i = 0; i != vetex.length; ++i)
            {
                vetex[i] = vertexList.get(i);
            }
            mVertexBuffer = ByteBuffer.allocateDirect(vetex.length * 4).order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mVertexBuffer.put(vetex);
            mVertexBuffer.position(0);

            float[] vetexDis = new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
            float[] textureDis = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
            mVertexBufferDis = ByteBuffer.allocateDirect(vetexDis.length * 4).order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mVertexBufferDis.put(vetexDis);
            mVertexBufferDis.position(0);
            mTextureBufferDis = ByteBuffer.allocateDirect(textureDis.length * 4).order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mTextureBufferDis.put(textureDis);
            mTextureBufferDis.position(0);
        }

        private void createSurface()
        {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mTextureID = textures[0];
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            mSurfaceTexture = new SurfaceTexture(mTextureID);
            mSurfaceTexture.setOnFrameAvailableListener(this);
            mSurface = new Surface(mSurfaceTexture);
        }

        private float[] getfinalMVPMatrix()
        {
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mCurrMatrix, 0);
            Matrix.setIdentityM(mCurrMatrix, 0);
            return mMVPMatrix;
        }

        private void getEyesTexture()
        {
            mEyesTexID = new int[2];
            GLES20.glGenTextures(2, mEyesTexID, 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mEyesTexID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth / 2, mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, null);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mEyesTexID[1]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth / 2, mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, null);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        private void renderEyesTexture()
        {
            GLES20.glViewport(0, 0, mWidth / 2, mHeight);
            for (int i = 0; i != 2; ++i)
            {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBO);
                if(mEyesTexID[i] != 0 && GLES20.glIsTexture(mEyesTexID[i]))
                {
                    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                            GLES20.GL_TEXTURE_2D, mEyesTexID[i], 0);
                    int nStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
                    if (nStatus == GLES20.GL_FRAMEBUFFER_COMPLETE)
                    {
                        renderShpere();
                    }
                }
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }

        private void renderShpere()
        {
            synchronized (this) {
                if(mUpdateSurface)
                {
                    mSurfaceTexture.updateTexImage();
                    mSurfaceTexture.getTransformMatrix(mSTMatrix);
                    mUpdateSurface = false;
                }
            }

            mSurfaceTexture.updateTexImage();

            if(mWidth < mHeight)
            {
                Matrix.rotateM(mCurrMatrix, 0, -mXAngle, 1, 0, 0);
                Matrix.rotateM(mCurrMatrix, 0, -mYAngle, 0, 1, 0);
            }
            else
            {
                Matrix.rotateM(mCurrMatrix, 0, -mYAngle, 1, 0, 0);
                Matrix.rotateM(mCurrMatrix, 0, mXAngle, 0, 1, 0);
            }
            Matrix.rotateM(mCurrMatrix, 0, -mZAngle, 0, 0, 1);

            GLES20.glUseProgram(mProgram);
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

            GLES20.glEnableVertexAttribArray(mAPositionHandler);
            GLES20.glVertexAttribPointer(mAPositionHandler, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
            GLES20.glEnableVertexAttribArray(mATextureCoordHandler);
            GLES20.glVertexAttribPointer(mATextureCoordHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);

            GLES20.glUniformMatrix4fv(mUProjectMatrixHandler, 1, false, getfinalMVPMatrix(), 0);
            GLES20.glUniformMatrix4fv(mUSTMatrixHandler, 1, false, mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mSize);

            GLES20.glUseProgram(0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            int[] fbos = new int[1];
            GLES20.glGenFramebuffers(1, fbos, 0);
            mFBO = fbos[0];
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height)
        {
            mWidth = width;
            mHeight = height;
            getEyesTexture();

            GLES20.glEnable(GLES20.GL_CULL_FACE);

            mFOV = 46.0f;
            float ratio = (float) Math.tan(Math.toRadians(mFOV / 2.0)) * 1.0f;
            Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -ratio, ratio, 1, 800);
            Matrix.setIdentityM(mCurrMatrix, 0);
            Matrix.setIdentityM(mMVPMatrix, 0);
            Matrix.translateM(mProjectMatrix, 0, 0, 0, -5);
            Matrix.scaleM(mProjectMatrix, 0, 6, 6, 6);

            mProgram = GLUtils.getProgram(mContext);
            mProgramDis = GLUtils.getDisplayProgram(mContext);

            mAPositionHandler = GLES20.glGetAttribLocation(mProgram, "aPosition");
            mATextureCoordHandler = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            mUProjectMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uProjectMatrix");
            mUSTMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");

            mDisPosHandler = GLES20.glGetAttribLocation(mProgramDis, "vDPos");
            mDisTexHandler = GLES20.glGetAttribLocation(mProgramDis, "vDTex");
            mDisLeftHandler = GLES20.glGetUniformLocation(mProgramDis, "g_TexSrcLeft");
            mDisRightHandler = GLES20.glGetUniformLocation(mProgramDis, "g_TexSrcRight");
        }

        @Override
        public void onDrawFrame(GL10 gl)
        {
            renderEyesTexture();

            GLES20.glViewport(0, 0, mWidth, mHeight);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            GLES20.glUseProgram(mProgramDis);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mEyesTexID[0]);
            GLES20.glUniform1i(mDisLeftHandler, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mEyesTexID[1]);
            GLES20.glUniform1i(mDisRightHandler, 1);

            GLES20.glEnableVertexAttribArray(mDisPosHandler);
            GLES20.glVertexAttribPointer(mDisPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBufferDis);
            GLES20.glEnableVertexAttribArray(mDisTexHandler);
            GLES20.glVertexAttribPointer(mDisTexHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBufferDis);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glUseProgram(0);
            GLES20.glFinish();
        }
    }
}

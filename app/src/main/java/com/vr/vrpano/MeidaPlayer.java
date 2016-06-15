package com.vr.vrpano;

import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.nfc.Tag;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by BJ-CHXB on 2016/6/15.
 */
public class MeidaPlayer {

    private static final String TAG = "MediaPlayer";

    public static final int MEDIA_ENGINE_PLAY = 1;
    public static final int MEDIA_ENGINE_PAUSE = 2;
    public static final int MEDIA_ENGINE_STOP = 3;
    public static final int MEDIA_ENGINE_NEW = 0;

    private VideoEngine mVideoPlayer;
    private AudioEngine mAudioPlayer;
    private AudioTiming mAudioTiming;

    private VideoSurfaceView mView;

    public MeidaPlayer(VideoSurfaceView view)
    {
        mView = view;
        mVideoPlayer = null;
        mAudioPlayer = null;
    }

    public void start(String fileName)
    {
        mVideoPlayer = new VideoEngine(mView.mSurface, fileName);
        mAudioPlayer = new AudioEngine(fileName);

        mAudioTiming = new AudioTiming();

        mVideoPlayer.start();
        mAudioPlayer.start();
    }

    public void stop()
    {
        if(mVideoPlayer != null && mAudioPlayer != null)
        {
            mVideoPlayer.stopEngine();
            mAudioPlayer.stopEngine();
            if(mVideoPlayer.isAlive())
            {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mVideoPlayer.interrupt();
            }
            mVideoPlayer = null;

            if(mAudioPlayer.isAlive())
            {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mAudioPlayer.interrupt();
            }
            mAudioPlayer = null;
        }
    }

    private class AudioTiming {
        long mStartMS;
        long mRealUS;
        boolean mConfigError;

        public AudioTiming()
        {
            mStartMS = 0;
            mRealUS = 0;
            mConfigError = false;
        }
    }

    private class VideoEngine extends Thread {

        private MediaExtractor mVExtractor;
        private MediaCodec mVDecoder;
        private ByteBuffer[] mVInputBuffers;
        private ByteBuffer[] mVOutputBuffers;
        private String mFileName = null;
        private int mVideoState;

        public VideoEngine(Surface surface, String fileName)
        {
            mFileName = fileName;
            prepareEngine(surface);
            mVideoState = MEDIA_ENGINE_NEW;
        }

        public void stopEngine()
        {
            synchronized (this)
            {
                mVideoState = MEDIA_ENGINE_STOP;
            }
        }

        private void prepareEngine(Surface surface)
        {
            mVExtractor = new MediaExtractor();
            try {
                mVExtractor.setDataSource(mFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int nTracks = mVExtractor.getTrackCount();
            for(int i = 0; i != nTracks; ++i)
            {
                MediaFormat exFormat = mVExtractor.getTrackFormat(i);
                String mime = exFormat.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "Track " + String.valueOf(i) + " " + "MIME: " + mime);

                if(mime.startsWith("video/"))
                {
                    mVExtractor.selectTrack(i);
                    try {
                        mVDecoder = MediaCodec.createDecoderByType(mime);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        mVDecoder.configure(exFormat, surface, null, 0);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        mAudioTiming.mConfigError = true;
                        return;
                    }
                    break;
                }
            }
        }

        @Override
        public void run()
        {
            if(mVDecoder == null)
            {
                return;
            }

            mVDecoder.start();
            if(Build.VERSION.SDK_INT <= 20)
            {
                mVInputBuffers = mVDecoder.getInputBuffers();
                mVOutputBuffers = mVDecoder.getOutputBuffers();
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean isEOS = false;
            int nWaitTime = 10000;

            while (!Thread.interrupted())
            {
                synchronized (this)
                {
                    if(mVideoState == MEDIA_ENGINE_STOP)
                    {
                        break;
                    }
                }

                if(!isEOS)
                {
                    int inIdx = mVDecoder.dequeueInputBuffer(nWaitTime);
                    if(inIdx >= 0)
                    {
                        ByteBuffer inBuffer = null;
                        if(Build.VERSION.SDK_INT <= 20)
                        {
                            inBuffer = mVInputBuffers[inIdx];
                        }
                        else
                        {
                            inBuffer = mVDecoder.getInputBuffer(inIdx);
                        }
                        int sampleSize = mVExtractor.readSampleData(inBuffer, 0);
                        if(sampleSize < 0)
                        {
                            mVDecoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        }
                        else
                        {
                            mVDecoder.queueInputBuffer(inIdx, 0, sampleSize, mVExtractor.getSampleTime(), 0);
                            mVExtractor.advance();
                        }
                    }
                }

                int outIdx = mVDecoder.dequeueOutputBuffer(bufferInfo, nWaitTime);
                switch (outIdx)
                {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        //Log.d(TAG, "VideoEngine: INFO_OUTPUT_BUFFERS_CHANGED");
                        mVOutputBuffers = mVDecoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        //Log.d(TAG, "VideoEngine: INFO_OUTPUT_FORMAT_CHANGED");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        //Log.d(TAG, "VideoEngine: INFO_TRY_AGAIN_LATER");
                        break;
                    default:
                        ByteBuffer outBuffer = null;
                        if(Build.VERSION.SDK_INT <= 20)
                        {
                            outBuffer = mVOutputBuffers[outIdx];
                        }
                        else
                        {
                            outBuffer = mVDecoder.getOutputBuffer(outIdx);
                        }

                        long realVideoTimeUS = bufferInfo.presentationTimeUs + mAudioTiming.mStartMS * 1000;
                        int avDif;
                        synchronized (this)
                        {
                            avDif = (int)(realVideoTimeUS - mAudioTiming.mRealUS - 300000);
                        }

                        if(avDif > 15000)
                        {
                            try {
                                sleep(avDif / 1000 + 15);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        else if(avDif < -15000)
                        {
                            mVDecoder.releaseOutputBuffer(outIdx, true);
                            nWaitTime = 1000;
                            continue;
                        }
                        else
                        {
                            try {
                                sleep(15);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        nWaitTime = 1000;
                        mVDecoder.releaseOutputBuffer(outIdx, true);
                        break;
                }

                if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                {
                    break;
                }
            }
            mVDecoder.stop();
            mVDecoder.release();
            mVExtractor.release();
        }
    }

    private class AudioEngine extends Thread {

        private MediaExtractor mAExtractor;
        private MediaCodec mADecoder;
        ByteBuffer[] mAInputBuffers;
        ByteBuffer[] mAOutputBuffers;
        int mAudioTrackNum;
        MediaFormat mAFormat;
        AudioTrack mAudioTrack;
        String mFileName;
        int mAudioState;

        public AudioEngine(String fileName)
        {
            mFileName = fileName;
            mAudioTrackNum = 0;
            mAudioState = MEDIA_ENGINE_NEW;
        }

        public void stopEngine()
        {
            synchronized (this)
            {
                mAudioState = MEDIA_ENGINE_STOP;
            }
        }

        @Override
        public void run()
        {
            mAExtractor = new MediaExtractor();
            try {
                mAExtractor.setDataSource(mFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int nTracks = mAExtractor.getTrackCount();
            for(int i = 0; i != nTracks; ++i)
            {
                MediaFormat exFormat = mAExtractor.getTrackFormat(i);
                String mime = exFormat.getString(MediaFormat.KEY_MIME);

                if(mime.startsWith("audio/"))
                {
                    mAudioTrackNum = i;
                    mAFormat = exFormat;
                    Log.d(TAG, "AudioEngine: mime: " + mime);
                    try {
                        mADecoder = MediaCodec.createDecoderByType(mime);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mADecoder.configure(exFormat, null, null, 0);
                    break;
                }
            }

            if(mADecoder == null)
            {
                Log.e(TAG, "AudioEngine: Audio Decoder created failed");
                return;
            }
            mADecoder.start();
            mAExtractor.selectTrack(mAudioTrackNum);

            if(Build.VERSION.SDK_INT <= 20)
            {
                mAInputBuffers = mADecoder.getInputBuffers();
                mAOutputBuffers = mADecoder.getOutputBuffers();
            }

            int initSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            int initSampleRate = mAFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    initSampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    initSize,
                    AudioTrack.MODE_STREAM);

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean isEOS = false;
            mAudioTiming.mStartMS = System.currentTimeMillis();

            mAudioTrack.play();
            while (!Thread.interrupted())
            {
                synchronized (this)
                {
                    if(mAudioState == MEDIA_ENGINE_STOP)
                    {
                        break;
                    }
                }

                mAExtractor.selectTrack(mAudioTrackNum);
                int inIdx = mADecoder.dequeueInputBuffer(1000);
                if(inIdx >= 0)
                {
                    ByteBuffer inputBuffer = null;
                    if(Build.VERSION.SDK_INT <= 20)
                    {
                        inputBuffer = mAInputBuffers[inIdx];
                    }
                    else
                    {
                        inputBuffer = mADecoder.getInputBuffer(inIdx);
                    }

                    int sampleSize = mAExtractor.readSampleData(inputBuffer, 0);
                    long presentationTimeUS = 0;
                    if (sampleSize < 0)
                    {
                        isEOS = true;
                        sampleSize = 0;
                    }
                    else
                    {
                        presentationTimeUS = mAExtractor.getSampleTime();
                        mAudioTiming.mRealUS = presentationTimeUS + mAudioTiming.mStartMS * 1000;
                    }

                    mADecoder.queueInputBuffer(inIdx, 0, sampleSize, presentationTimeUS,
                            isEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if(!isEOS)
                    {
                        mAExtractor.advance();
                    }
                }

                int outIdx = mADecoder.dequeueOutputBuffer(bufferInfo, 100000);
                if(outIdx > 0)
                {
                    ByteBuffer outBuffer = null;
                    if(Build.VERSION.SDK_INT <= 20)
                    {
                        outBuffer = mAOutputBuffers[outIdx];
                    }
                    else
                    {
                        outBuffer = mADecoder.getOutputBuffer(outIdx);
                    }

                    byte[] chunk = new byte[bufferInfo.size];
                    outBuffer.get(chunk);
                    outBuffer.clear();

                    if(chunk.length > 0)
                    {
                        mAudioTrack.write(chunk, 0, chunk.length);
                    }
                    mADecoder.releaseOutputBuffer(outIdx, false);

                    if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                    {
                        isEOS = true;
                    }
                }
                else if(outIdx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
                {
                    //Log.d(TAG, "AudioEngine: INFO_OUTPUT_BUFFERS_CHANGED");
                    if(Build.VERSION.SDK_INT <= 20)
                    {
                        mAOutputBuffers = mADecoder.getOutputBuffers();
                    }
                }
                else if(outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                {
                    //Log.d(TAG, "AudioEngine: INFO_OUTPUT_FORMAT_CHANGED");
                    MediaFormat oFormat = mADecoder.getOutputFormat();
                    int newRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    mAudioTrack.setPlaybackRate(newRate);
                }

                synchronized (mAudioTiming)
                {
                    if(mAudioTiming.mConfigError)
                    {
                        mAudioTiming.mConfigError = false;
                        Log.e(TAG, "AudioEngine: stopped because the video engine configure failed");
                        return;
                    }
                }
            }

            mADecoder.stop();
            mAudioTrack.stop();
            mAudioTrack.release();
            mADecoder.release();
            mAExtractor.release();
        }
    }
}

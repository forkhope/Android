package com.example.audiodecode;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.AudioFormat;
import android.media.MediaFormat;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Intent;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.net.Socket;

public class AudioDecode extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "AudioDecode";

    private VideoPlayerThread mVideoPlayer = null;
    private AudioPlayerThread mAudioPlayer = null;
    private SurfaceHolder mSurfaceHolder;
    private InputStream mediaInputStream;

    private static final int SERVER_PORT = 9527;
    private static final int SERVER_VIDEO_PORT = 9528;
    private static final int SERVER_AUDIO_PORT = 9529;
    private String mIpAddress = null;
    private ReceiveThread mReceiveThread = null;

    private static final String RECEIVED_SAMPLE_FILE = "/sdcard/sample.mp4";
    // private static final String MEDIA_FILE = "/sdcard/good.h264";

    private int mMdatIndex = 36;
    private String mAudioMime;
    private MediaFormat mAudioMediaFormat;
    private String mVideoMime;
    private MediaFormat mVideoMediaFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mIpAddress = intent.getStringExtra("mIpAddress");

        mReceiveThread = new ReceiveThread();
        mReceiveThread.start();

        SurfaceView sv = new SurfaceView(AudioDecode.this);
        sv.getHolder().addCallback(AudioDecode.this);
        setContentView(sv);
    }

    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mVideoPlayer != null) {
            mVideoPlayer.interrupt();
        }
    }

    private boolean receiveSampleFile(InputStream is) {
        byte[] buffer = new byte[4096];
        FileOutputStream fos = null;

        Log.d(TAG, "receiveSampleFile");

        try {
            File sampeFile = new File(RECEIVED_SAMPLE_FILE);
            if (sampeFile.exists()) {
                Log.d(TAG, "Sample file has existed!");
                return true;
            }

            fos = new FileOutputStream(sampeFile);
            DataInputStream dis = new DataInputStream(is);

            if (!sampeFile.delete()) {
                Log.d(TAG, "receiveSampleFile: delete sample file error!");
            }

            long fileLength = dis.readLong();
            Log.d(TAG, "receiveSampleFile: fileLength = " + fileLength);

            int number;
            long nleft = fileLength;
            // 这里不能判断 read() 函数是否返回 -1, 因为服务端在发送完样本文件后,
            // 并没有关闭socket,而是继续发送其他数据, read()函数不会遇到EOF.所以,
            // 修改服务端的代码,让它先发送文件大小,再发送文件内容,这里先读取文件
            // 大小,再根据这个大小来读取特定字节的内容.
            while (nleft > 0) {
                number = dis.read(buffer);
                Log.d(TAG, "receiveSampleFile: read number = " + number);
                fos.write(buffer, 0, number);
                nleft -= number;
            }

            Log.d(TAG, "sampeFile.exists() = " + sampeFile.exists());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private class ReceiveThread extends Thread {
        ReceiveThread() {
        }

        @Override
        public void run() {
            Log.d(TAG, "ReceiveThread: run");
            try {
                Socket clientSocket = new Socket(mIpAddress, SERVER_PORT);
                Log.d(TAG, "ReceiveThread: clientSocket = " + clientSocket);
                // clientSocket.setReceiveBufferSize(500000);
                Log.d(TAG, "clientSocket: Receive Buffer Size = " + clientSocket.getReceiveBufferSize());
                mediaInputStream = clientSocket.getInputStream();

                if (receiveSampleFile(mediaInputStream) == false) {
                    Log.d(TAG, "ReceiveThread: Could not receive sample file!");
                    return;
                }

                // 从接收到的样本文件中提取出mdat和音视频的MediaFormat.
                getMdatIndex(RECEIVED_SAMPLE_FILE);
                extractorMediaFormat(RECEIVED_SAMPLE_FILE);

                Thread.sleep(1000);
                if (mVideoPlayer == null) {
                    mVideoPlayer = new VideoPlayerThread(mSurfaceHolder.getSurface());
                    mVideoPlayer.start();
                }
                if (mAudioPlayer == null) {
                    mAudioPlayer = new AudioPlayerThread();
                    mAudioPlayer.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] int2bytes(int value) {
        byte[] result = new byte[4];
        int shift = 24;
        for (int i = 0; i < result.length; ++i, shift -= 8) {
            result[i] = (byte) ((value >> shift) & 0xff);
            Log.d(TAG, String.format("result[%d] = %x", i, result[i]));
        }
        return result;
    }

    private int bytes2int(byte[] buffer) {
        int ret = 0;
        final int shift = 8;
        for (int i = 0; i < buffer.length; ++i) {
            // Log.d(TAG, String.format("buffer[%d] = %x", i, buffer[i]));
            ret <<= shift;
            ret |= buffer[i] & 0x000000ff;
        }
        // Log.d(TAG, "final: bytes2int: ret = " + ret);
        return ret;
    }

    private boolean extractorMediaFormat(String filename) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(filename);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "mime = " + mime);
            if (mime.startsWith("audio/")) {
                mAudioMime = mime;
                mAudioMediaFormat = format;
                Log.d(TAG, "AudioFormat format = " + format);

                // ByteBuffer sps = format.getByteBuffer("csd-0");
                // Log.d(TAG, "sps.capacity() = " + sps.capacity());
                // byte[] spsbuffer = new byte[sps.capacity()];
                // sps.get(spsbuffer);
                // for (int j = 0; j < spsbuffer.length; ++j) {
                //     Log.d(TAG, String.format("PlayerAudioThread: sps: %x", spsbuffer[j]));
                // }
            } else if (mime.startsWith("video/")) {
                mVideoMime = mime;
                mVideoMediaFormat = format;
                Log.d(TAG, "VideoFormat format = " + format);

                // ByteBuffer sps = format.getByteBuffer("csd-0");
                // byte[] spsbuffer = new byte[14];
                // sps.get(spsbuffer);
                // for (int j = 0; j < spsbuffer.length; ++j) {
                //     Log.d(TAG, String.format("sps: %x", spsbuffer[j]));
                // }
                // Log.d(TAG, "END sps ---------");

                // ByteBuffer pps = format.getByteBuffer("csd-1");
                // byte[] ppsbuffer = new byte[8];
                // pps.get(ppsbuffer);
                // for (int j = 0; j < ppsbuffer.length; ++j) {
                //     Log.d(TAG, String.format("pps: %x", ppsbuffer[j]));
                // }
                // Log.d(TAG, "END pps +++++++++");
            }
        }

        extractor.release();
        return true;
    }

    private boolean getMdatIndex(String filename) {
        Log.d(TAG, "getMdatIndex: filename = " + filename);
        try {
            FileInputStream fis = new FileInputStream(filename);
            // mdat的标识. 'm'=0x6d, 'd'=0x64, 'a'=0x61, 't'=0x74
            // private final byte[] mdatData = new byte[] {0x6d, 0x64, 0x61, 0x74};
            final byte[] mdatData = new byte[] {'m', 'd', 'a', 't'};
            byte[] buffer = new byte[4096];
            int i, nread;

            nread = fis.read(buffer);
            fis.close();

            for (i = 0; i < nread; ++i) {
                if (buffer[i] != mdatData[0]) {
                    continue;
                }
                if (buffer[i+1] != mdatData[1]) {
                    continue;
                }
                if (buffer[i+2] != mdatData[2]) {
                    i += 1;
                    continue;
                }
                if (buffer[i+3] != mdatData[3]) {
                    i += 2;
                    continue;
                }

                mMdatIndex = i + 4;
                Log.d(TAG, "getMdatIndex: mMdatIndex = " + mMdatIndex);
                return true;
            }
            if (i >= nread) {
                Log.d(TAG, "getMdatIndex: Could not find mdat!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    private class VideoPlayerThread extends Thread {
        private MediaCodec mVideoDecoder;
        private Surface mSurface;
        private byte[] buffer = new byte[4096 * 20];

        public VideoPlayerThread(Surface surface) {
            mSurface = surface;
        }

        private int readSampleData(ByteBuffer bytebuf, DataInputStream dis) {
            int totalLength;
            int nleft;
            int count;

            bytebuf.mark();
            try {
                // byte[] h264LengthBuffer = new byte[4];
                // nleft = h264LengthBuffer.length;
                // while (nleft > 0) {
                //     int number = dataMediaInputStream.read(h264LengthBuffer, 0, nleft);
                //     nleft -= number;
                //     bytebuf.put(h264LengthBuffer, 0, number);
                // }
                // totalLength = bytes2int(h264LengthBuffer);
                // Log.d(TAG, "readSampleData: totalLength = " + totalLength);

                totalLength = dis.readInt();

                nleft = totalLength;
                Log.d(TAG, "VideoPlayerThread: BEFORE totoal readSampleData: totalLength = " + totalLength);
                while (nleft > 0) {
                    count = (nleft > buffer.length ? buffer.length : nleft);
                    Log.d(TAG, "VideoPlayerThread: before single read: nleft = " + nleft + ", count = " + count);
                    int number = dis.read(buffer, 0, count);
                    Log.d(TAG, "VideoPlayerThread: after single read: number = " + number);
                    if (number == -1) {
                        bytebuf.reset();
                        return -1;  /* EOF */
                    }
                    nleft -= number;
                    bytebuf.put(buffer, 0, number);
                }
                Log.d(TAG, "VideoPlayerThread: AFTER  totoal readSampleData: totalLength = " + totalLength);
            } catch (IOException e) {
                e.printStackTrace();
                bytebuf.reset();
                return -1;
            }
            bytebuf.reset();
            return totalLength;
        }

        @Override
        public void run() {
            try {
                Socket videoSocket = new Socket(mIpAddress, SERVER_VIDEO_PORT);
                DataInputStream dis = new DataInputStream(videoSocket.getInputStream());

                mVideoDecoder = MediaCodec.createDecoderByType(mVideoMime);
                mVideoDecoder.configure(mVideoMediaFormat, mSurface, null, 0);

                if (mVideoDecoder == null) {
                    Log.e(TAG, "ERROR: Could not find video info!");
                    return;
                }

                mVideoDecoder.start();

                ByteBuffer[] inputBuffers = mVideoDecoder.getInputBuffers();
                ByteBuffer[] outputBuffers = mVideoDecoder.getOutputBuffers();
                BufferInfo info = new BufferInfo();
                boolean isEOS = false;
                long startMs = System.currentTimeMillis();
                int count = 0;

                while (!Thread.interrupted()) {
                    if (!isEOS) {
                        int inIndex = mVideoDecoder.dequeueInputBuffer(10000);
                        Log.d(TAG, "inIndex = " + inIndex);
                        if (inIndex >= 0) {
                            ByteBuffer buffer = inputBuffers[inIndex];
                            // Log.d(TAG, "buffer.postion = " + buffer.position());
                            int sampleSize = readSampleData(buffer, dis);
                            // Log.d(TAG, "after: buffer.postion = " + buffer.position());
                            // Log.d(TAG, "after rewind: buffer.postion = " + buffer.position());
                            // Log.d(TAG, "sampleSize="+sampleSize);
                            if (sampleSize < 0) {
                                // We shouldn't stop the playback at this point, just pass the EOS
                                // flag to decoder, we will get it again from the
                                // dequeueOutputBuffer
                                Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                mVideoDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                isEOS = true;
                            } else {
                                mVideoDecoder.queueInputBuffer(inIndex, 0, sampleSize, count, 0);
                                count += 1000 * 1000 / 20;
                            }
                        }
                    }

                    int outIndex = mVideoDecoder.dequeueOutputBuffer(info, 10000);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = mVideoDecoder.getOutputBuffers();
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED: New format " + mVideoDecoder.getOutputFormat());
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d(TAG, "dequeueOutputBuffer timed out!");
                            break;
                        default:
                            ByteBuffer buffer = outputBuffers[outIndex];
                            Log.d(TAG, "outputBuffers, outIndex = " + outIndex);
                            //Log.v(TAG, "We can't use this buffer but render it due to the API limit, " + buffer);

                            // We use a very simple clock to keep the video FPS, or the video
                            // playback will be too fast
                            while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                                Log.d(TAG, "sleep ---------------------------------");

                                try {
                                    sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    break;
                                }
                            }

                            // final byte[] chunk = new byte[info.size];
                            // buffer.get(chunk);
                            // buffer.clear();

                            // if (chunk.length > 0) {
                            //     mAudioTrack.write(chunk, 0, chunk.length);
                            // }
                            mVideoDecoder.releaseOutputBuffer(outIndex, true);
                            break;
                    }

                    // All decoded frames have been rendered, we can stop playing now
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        break;
                    }
                }

                mVideoDecoder.stop();
                mVideoDecoder.release();
                mediaInputStream.close();
                dis.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }

        }
    }

    private class AudioPlayerThread extends Thread {
        private byte[] buffer = new byte[4096 * 20];
        private MediaCodec mAudioDecoder;
        private AudioTrack mAudioTrack;

        public AudioPlayerThread() {
        }

        private int readSampleData(ByteBuffer bytebuf, DataInputStream dis) {
            int totalLength;
            int nleft;
            int count;

            Log.d(TAG, "VideoPlayerThread: readSampleData: bytebuf.capacity() = " + bytebuf.capacity());

            bytebuf.mark();
            try {
                // byte[] h264LengthBuffer = new byte[4];
                // nleft = h264LengthBuffer.length;
                // while (nleft > 0) {
                //     int number = dataMediaInputStream.read(h264LengthBuffer, 0, nleft);
                //     nleft -= number;
                //     bytebuf.put(h264LengthBuffer, 0, number);
                // }
                // totalLength = bytes2int(h264LengthBuffer);
                // Log.d(TAG, "readSampleData: totalLength = " + totalLength);

                totalLength = dis.readInt();

                nleft = totalLength;
                Log.d(TAG, "VideoPlayerThread: BEFORE totoal readSampleData: totalLength = " + totalLength);
                while (nleft > 0) {
                    count = (nleft > buffer.length ? buffer.length : nleft);
                    Log.d(TAG, "VideoPlayerThread: before single read: nleft = " + nleft + ", count = " + count);
                    int number = dis.read(buffer, 0, count);
                    Log.d(TAG, "VideoPlayerThread: after single read: number = " + number);
                    if (number == -1) {
                        bytebuf.reset();
                        return -1;  /* EOF */
                    }
                    nleft -= number;
                    bytebuf.put(buffer, 0, number);
                }
                Log.d(TAG, "VideoPlayerThread: AFTER  totoal readSampleData: totalLength = " + totalLength);
            } catch (IOException e) {
                e.printStackTrace();
                bytebuf.reset();
                return -1;
            }
            bytebuf.reset();
            return totalLength;
        }

        @Override
        public void run() {
            try {
                Socket audioSocket = new Socket(mIpAddress, SERVER_AUDIO_PORT);
                DataInputStream dis = new DataInputStream(audioSocket.getInputStream());

                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        mAudioMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        mAudioMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                        AudioFormat.ENCODING_PCM_16BIT,
                        mAudioMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE),
                        AudioTrack.MODE_STREAM);
                mAudioTrack.play();

                mAudioDecoder = MediaCodec.createDecoderByType(mAudioMime);
                mAudioDecoder.configure(mAudioMediaFormat, null, null, 0);

                if (mAudioDecoder == null) {
                    Log.e(TAG, "ERROR: Could not find audio info!");
                    return;
                }

                mAudioDecoder.start();

                ByteBuffer[] inputBuffers = mAudioDecoder.getInputBuffers();
                ByteBuffer[] outputBuffers = mAudioDecoder.getOutputBuffers();
                BufferInfo info = new BufferInfo();
                boolean isEOS = false;
                long startMs = System.currentTimeMillis();
                int count = 0;

                while (!Thread.interrupted()) {
                    if (!isEOS) {
                        int inIndex = mAudioDecoder.dequeueInputBuffer(10000);
                        Log.d(TAG, "inIndex = " + inIndex);
                        if (inIndex >= 0) {
                            ByteBuffer buffer = inputBuffers[inIndex];
                            // Log.d(TAG, "buffer.postion = " + buffer.position());
                            int sampleSize = readSampleData(buffer, dis);
                            // Log.d(TAG, "after: buffer.postion = " + buffer.position());
                            // Log.d(TAG, "after rewind: buffer.postion = " + buffer.position());
                            // Log.d(TAG, "sampleSize="+sampleSize);
                            if (sampleSize < 0) {
                                // We shouldn't stop the playback at this point, just pass the EOS
                                // flag to decoder, we will get it again from the
                                // dequeueOutputBuffer
                                Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                mAudioDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                isEOS = true;
                            } else {
                                mAudioDecoder.queueInputBuffer(inIndex, 0, sampleSize, count, 0);
                                count += 1000 * 1000 / 20;
                            }
                        }
                    }

                    int outIndex = mAudioDecoder.dequeueOutputBuffer(info, 10000);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = mAudioDecoder.getOutputBuffers();
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED: New format " + mAudioDecoder.getOutputFormat());
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d(TAG, "dequeueOutputBuffer timed out!");
                            break;
                        default:
                            ByteBuffer buffer = outputBuffers[outIndex];
                            Log.d(TAG, "outputBuffers, outIndex = " + outIndex);
                            //Log.v(TAG, "We can't use this buffer but render it due to the API limit, " + buffer);

                            // We use a very simple clock to keep the video FPS, or the video
                            // playback will be too fast
                            // while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            //     Log.d(TAG, "sleep ---------------------------------");

                            //     try {
                            //         sleep(10);
                            //     } catch (InterruptedException e) {
                            //         e.printStackTrace();
                            //         break;
                            //     }
                            // }

                            final byte[] chunk = new byte[info.size];
                            buffer.get(chunk);
                            buffer.clear();

                            if (chunk.length > 0) {
                                mAudioTrack.write(chunk, 0, chunk.length);
                            }
                            mAudioDecoder.releaseOutputBuffer(outIndex, false);
                            break;
                    }

                    // All decoded frames have been rendered, we can stop playing now
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        break;
                    }
                }

                mAudioDecoder.stop();
                mAudioDecoder.release();
                mediaInputStream.close();
                dis.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }

        }
    }

}

package com.example.mediacodecstream;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String LOG_TAG = "DecodeActivity";

    private PlayerThread mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
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
        if (mPlayer == null) {
            mPlayer = new PlayerThread(holder.getSurface());
            mPlayer.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.interrupt();
        }
    }
    
    private byte[] int2bytes(int value) {
        byte[] result = new byte[4];
        int shift = 24;
        for (int i = 0; i < result.length; ++i, shift -= 8) {
            result[i] = (byte) ((value >> shift) & 0xff);
            Log.d(LOG_TAG, String.format("result[%d] = %x", i, result[i]));
        }
        return result;
    }

    private int bytes2int(byte[] buffer) {
        int ret = 0;
        final int shift = 8;
        for (int i = 0; i < buffer.length; ++i) {
            Log.d(LOG_TAG, String.format("buffer[%d] = %x", i, buffer[i]));
            ret <<= shift;
            ret |= buffer[i] & 0x000000ff;
        }
        Log.d(LOG_TAG, "final: bytes2int: ret = " + ret);
        return ret;
    }

   private class PlayerThread extends Thread {
    	
        // private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;
        private FileInputStream mediaInputStream;
        private DataInputStream dataMediaInputStream;
        private byte[] spsBuffer = new byte[] {0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0x00, 0x1f, (byte)0xe5, 0x40, (byte)0xb0, 0x4b, 0x20};
        private byte[] ppsBuffer = new byte[] {0x00, 0x00, 0x00, 0x01, 0x68, (byte)0xce, 0x31, 0x12};

        public PlayerThread(Surface surface) {
            this.surface = surface;
            File mediaFile = new File("/sdcard/codec.h264");
            try {
                mediaInputStream = new FileInputStream(mediaFile);
                dataMediaInputStream = new DataInputStream(mediaInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private int readSampleData(ByteBuffer bytebuf) {
            int totalLength;
            int nleft;
            try {
                // byte[] h264LengthBuffer = new byte[4];
                // nleft = h264LengthBuffer.length;
                // while (nleft > 0) {
                //     int number = dataMediaInputStream.read(h264LengthBuffer, 0, nleft);
                //     nleft -= number;
                //     bytebuf.put(h264LengthBuffer, 0, number);
                // }
                // totalLength = bytes2int(h264LengthBuffer);
                // Log.d(LOG_TAG, "readSampleData: totalLength = " + totalLength);

                totalLength = dataMediaInputStream.readInt();
                Log.d(LOG_TAG, "readSampleData: totalLength = " + totalLength);

                nleft = totalLength;
                byte[] buffer = new byte[4096 * 4];
                while (nleft > 0) {
                    int number = mediaInputStream.read(buffer, 0, nleft);
                    if (number == -1) {
                        return -1;  /* EOF */
                    }
                    nleft -= number;
                    bytebuf.put(buffer, 0, number);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
            return totalLength;
        }

        @Override
        public void run() {
            try {
                // extractor = new MediaExtractor();
                // try {
                //     extractor.setDataSource("/sdcard/test.h264"); // THIS WORKS
                // } catch (IOException e1) {
                //     e1.printStackTrace();
                // }

                // for (int i = 0; i < extractor.getTrackCount(); i++) {
                //     MediaFormat format = extractor.getTrackFormat(i);
                //     String mime = format.getString(MediaFormat.KEY_MIME);
                //     if (mime.startsWith("video/")) {
                //         extractor.selectTrack(i);
                //         decoder = MediaCodec.createDecoderByType(mime);
                //         decoder.configure(format, surface, null, 0);
                //         break;
                //     }
                // }

                decoder = MediaCodec.createDecoderByType("video/avc");
                MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 352, 288);

                // mediaFormat.setLong(MediaFormat.KEY_DURATION, 9999000); 

                // 在自定义 MediaFormat 时,需要预置 SPS 和 PPS 数据.上面定义的 SPS 和
                // PPS 数据是从完整格式的文件上提取出来的.
                ByteBuffer spsByteBuffer = ByteBuffer.wrap(spsBuffer);
                mediaFormat.setByteBuffer("csd-0", spsByteBuffer);

                ByteBuffer ppsByteBuffer = ByteBuffer.wrap(ppsBuffer);
                mediaFormat.setByteBuffer("csd-1", ppsByteBuffer);

                Log.d(LOG_TAG, "mediaFormat = " + mediaFormat);

                decoder.configure(mediaFormat, surface, null, 0);

                if (decoder == null) {
                    Log.e("DecodeActivity", "Can't find video info!");
                    return;
                }

                decoder.start();

                ByteBuffer[] inputBuffers = decoder.getInputBuffers();
                ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
                BufferInfo info = new BufferInfo();
                boolean isEOS = false;
                long startMs = System.currentTimeMillis();
                int count = 1;

                while (!Thread.interrupted()) {
                    if (!isEOS) {
                        int inIndex = decoder.dequeueInputBuffer(10000);
                        Log.d(LOG_TAG, "inIndex = " + inIndex);
                        if (inIndex >= 0) {
                            ByteBuffer buffer = inputBuffers[inIndex];
                            Log.d(LOG_TAG, "buffer.postion = " + buffer.position());
                            int sampleSize = readSampleData(buffer);
                            Log.d(LOG_TAG, "after: buffer.postion = " + buffer.position());
                            // 执行 readSampleData() 函数写输入到 buffer 后, buffer 的
                            // position 值被更新了,需要回滚这个值,使得后面在操作buffer时,
                            // 所读取到的数据是完整的数据,否则会从更新过来的position位置
                            // 开始读取数据,位于position之前的数据没有被读取到.
                            buffer.rewind();
                            Log.d(LOG_TAG, "after rewind: buffer.postion = " + buffer.position());
                            Log.d("DecodeActivity", "sampleSize="+sampleSize);
                            if (sampleSize < 0) {
                                // We shouldn't stop the playback at this point, just pass the EOS
                                // flag to decoder, we will get it again from the
                                // dequeueOutputBuffer
                                Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                isEOS = true;
                            } else {
                                // byte[] debugBuffer = new byte[sampleSize];
                                // buffer.get(debugBuffer);
                                // for (int i = 0; i < debugBuffer.length; ++i) {
                                //     Log.d(LOG_TAG, String.format("%x", debugBuffer[i]));
                                // }

                                decoder.queueInputBuffer(inIndex, 0, sampleSize, count, 0);
                                ++count;
                                // 一定要执行 rewind() 函数回滚 buffer 中的 position 值,否则
                                // 视频帧不会更新.
                                buffer.rewind();
                                // extractor.advance();
                            }
                        }
                    }

                    int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                    switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_FORMAT_CHANGED: New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
                        Log.d(LOG_TAG, "outputBuffers, buffer = " + buffer);
                        //Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, true);
                        break;
                    }

                    // All decoded frames have been rendered, we can stop playing now
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        break;
                    }
                }

                decoder.stop();
                decoder.release();
                mediaInputStream.close();
                // extractor.release();
            } catch (Exception e2) {
                e2.printStackTrace();
            }

        }
    }
}

package com.example.h264decoder;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity implements SurfaceHolder.Callback,
    MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {
    private static final int mVideoEncoder =MediaRecorder.VideoEncoder.H264;
    private static final String LOG_TAG = "VideoCamera";
    LocalSocket receiver, sender;
    LocalServerSocket lss;
    private MediaRecorder mMediaRecorder = null;
    boolean mMediaRecorderRecording = false;
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    Thread t;
    Context mContext = this;
    RandomAccessFile raf = null;

    // mdat所在索引值,默认设为32.这个值根据不同机器有所不同.
    private int mMdatIndex = 32;
    private byte[] mSPSData;
    private byte[] mPPSData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        mSurfaceView = (SurfaceView) this.findViewById(R.id.surface_camera);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.setVisibility(View.VISIBLE);
        receiver = new LocalSocket();
        try {
            lss = new LocalServerSocket("VideoCamera");
            receiver.connect(new LocalSocketAddress("VideoCamera"));
            receiver.setReceiveBufferSize(500000);
            receiver.setSendBufferSize(500000);
            sender = lss.accept();
            sender.setReceiveBufferSize(500000);
            sender.setSendBufferSize(500000);
        } catch (IOException e) {
            e.printStackTrace();
            finish();
            return;
        }

        getSPSAndPPS();
        getMdatIndex();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaRecorderRecording) {
            stopVideoRecording();
            try {
                lss.close();
                receiver.close();
                sender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        finish();
    }

    private void stopVideoRecording() {
        Log.d(LOG_TAG, "stopVideoRecording");
        if (mMediaRecorderRecording || mMediaRecorder != null) {
            if (t != null)
                t.interrupt();
            try {
                raf.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            releaseMediaRecorder();
        }
    }

    private void startVideoRecording() {
        Log.d(LOG_TAG, "startVideoRecording");
        (t = new Thread() {
            public void run() {
                int frame_size = 1024;
                byte[] buffer = new byte[1024 * 64];
                int num, number = 0;
                InputStream fis = null;
                try {
                    fis = receiver.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                // initializeVideo();
                number = 0;
                // 重新启动捕获，以获取视频流
                DataInputStream dis=new DataInputStream(fis);

                try {
                    // 读取最前面的32个空头,再掉过4个字节的"mdat"标识,总共要跳过36个字节.
                    // 36 这个值是根据实际文件的文件头手动算出来的,后面调用函数来获取具体的值
                    dis.read(buffer,0, mMdatIndex);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                try {
                    File file = new File("/sdcard/stream.h264");
                    if (file.exists())
                        file.delete();
                    raf = new RandomAccessFile(file, "rw");
                } catch (Exception ex) {
                    Log.v("System.out", ex.toString());
                    ex.printStackTrace();
                }				


                byte[] h264head={0,0,0,1};
                try {
                    raf.write(h264head);
                    raf.write(mSPSData);
                    raf.write(h264head);
                    raf.write(mPPSData);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                while (true)
                {
                    try {
                        //读取每场的长度
                        int h264length=dis.readInt();
                        Log.d(LOG_TAG, "h264length = " + h264length);
                        number =0;
                        raf.write(h264head);
                        while(number<h264length)
                        {
                            int lost=h264length-number;
                            num = fis.read(buffer,0,frame_size<lost?frame_size:lost);
                            Log.d(LOG_TAG,String.format("H264 %d,%d,%d", h264length,number,num));
                            number+=num;
                            raf.write(buffer, 0, num);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();
    }

    private boolean initializeVideo() {
        Log.d(LOG_TAG, "initializeVideo");
        if (mSurfaceHolder==null) {
            return false;
        }

        mMediaRecorderRecording = true;
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        } else {
            mMediaRecorder.reset();
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoFrameRate(20);
        mMediaRecorder.setVideoSize(352, 288);
        mMediaRecorder.setVideoEncoder(mVideoEncoder);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setMaxDuration(0);
        mMediaRecorder.setMaxFileSize(0);
        mMediaRecorder.setOutputFile(sender.getFileDescriptor());

        try {
            mMediaRecorder.setOnInfoListener(this);
            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException exception) {
            exception.printStackTrace();
            releaseMediaRecorder();
            finish();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        Log.v(LOG_TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
            if (mMediaRecorderRecording) {
                try {
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "stop fail: " + e.getMessage());
                }
                mMediaRecorderRecording = false;
            }
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(LOG_TAG, "surfaceChanged");
        mSurfaceHolder = holder;
        if (!mMediaRecorderRecording) {
            initializeVideo();
            startVideoRecording();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(LOG_TAG, "surfaceCreated");
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(LOG_TAG, "surfaceDestroyed");
        mSurfaceHolder = null;
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                Log.d(LOG_TAG, "MEDIA_RECORDER_INFO_UNKNOWN");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                Log.d(LOG_TAG, "MEDIA_RECORDER_INFO_MAX_DURATION_REACHED");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                Log.d(LOG_TAG, "MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED");
                break;
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            Log.d(LOG_TAG, "MEDIA_RECORDER_ERROR_UNKNOWN");
            finish();
        }
    }

    private void getSPSAndPPS() {
        Log.d(LOG_TAG, "getSPSAndPPS");
        try {
            File file = new File("/sdcard/test.h264");
            FileInputStream fis = new FileInputStream(file);

            long fileLength = file.length();
            byte[] fileData = new byte[(int)fileLength];
            fis.read(fileData);

            // avcC数据的标识. 'a'=0x61, 'v'=0x76, 'c'=0x63, 'C'=0x43
            final byte[] avcC = new byte[] {0x61, 0x76, 0x63, 0x43};

            int avcCRecord = -1;
            for (int i = 0; i < fileLength; ++i) {
                // 将当前字符和avcC标识的第一个字符 'a' 进行比较
                // 如果不相等,则重新执行循环,获取文件中的下一个字符
                // 如果相等,则比较avcC的第二个字符
                if (fileData[i] != avcC[0]) {
                    continue;
                }
                // 将下一次字符和avcC标识的第二个字符 'v' 进行比较
                if (fileData[i+1] != avcC[1]) {
                    continue;
                }
                if (fileData[i+2] != avcC[2]) {
                    // 前面两个字符相等,由于avcC中的'a'和'v'不相等,所以下一次字符不用
                    // 再比较,直接跳过当前字符和下一个字符,直接从其后第二个字符开始比较
                    // 由于循环体中有一个加1的操作,这里只加1,总共是加2,跳过两个字符.
                    i += 1;
                    continue;
                }
                if (fileData[i+3] != avcC[3]) {
                    i += 2;
                    continue;
                }

                // 至此,就是找到了 avcC 这四个字符.跳过这四个字符,就是avcC数据,
                // 将 avcCRecord 变量指向avcC数据这个位置.
                avcCRecord = i + 4;
                break;
            }
            if (avcCRecord == -1) {
                Log.d(LOG_TAG, "getSPSAndPPS: not found avcC!");
                return;
            }

            // 加6的目的是为了跳过  
            // (1)8字节的 configurationVersion  
            // (2)8字节的 AVCProfileIndication  
            // (3)8字节的 profile_compatibility  
            // (4)8 字节的 AVCLevelIndication  
            // (5)6 bit 的 reserved  
            // (6)2 bit 的 lengthSizeMinusOne  
            // (7)3 bit 的 reserved  
            // (8)5 bit 的numOfSequenceParameterSets  
            // 共6个字节，然后到达sequenceParameterSetLength的位置 
            int spsStartIndex = avcCRecord + 6;

            // 获取 SPS 数据长度和 SPS 数据
            byte[] spsLengthBuffer = new byte[] {fileData[spsStartIndex], fileData[spsStartIndex+1]};
            int spsLength = bytes2int(spsLengthBuffer);
            // 跳过2个字符的 sps 数据长度 (sequenceParameterSetLength)
            int spsDataIndex = spsStartIndex + 2;
            mSPSData = new byte[spsLength];
            System.arraycopy(fileData, spsDataIndex, mSPSData, 0, spsLength);
            Log.d(LOG_TAG, "SPS长度为: " + spsLength);
            for (int i = 0; i < spsLength; ++i) {
                Log.d(LOG_TAG, "SPS[ + " + i + "] = " + mSPSData[i]);
            }

            // 获取 PPS 数据长度和 PPS 数据
            // spsdataIndex + spsLength 可以跳到pps位置  
            // 再加1的目的是跳过1字节的 numOfPictureParameterSets  
            int ppsStartIndex = spsDataIndex + spsLength + 1;
            byte[] ppsLengthBuffer = new byte[] {fileData[ppsStartIndex], fileData[ppsStartIndex + 1]};
            int ppsLength = bytes2int(ppsLengthBuffer);
            // 跳过2个字符的 pps 数据长度 (sequenceParameterSetLength)
            int ppsDataIndex = ppsStartIndex + 2;
            mPPSData = new byte[ppsLength];
            System.arraycopy(fileData, ppsDataIndex, mPPSData, 0, ppsLength);
            Log.d(LOG_TAG, "PPS长度为: " + ppsLength);
            for (int i = 0; i < ppsLength; ++i) {
                Log.d(LOG_TAG, "PPS[ + " + i + "] = " + mPPSData[i]);
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int bytes2int(byte[] buffer) {
        int ret = buffer[0];
        ret <<= 8;
        ret |= buffer[1];
        return ret;
    }

    private void getMdatIndex() {
        Log.d(LOG_TAG, "getMdatIndex");
        try {
            File file = new File("/sdcard/test.h264");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            fis.read(buffer);
            int i;

            // mdat的标识. 'm'=0x6d, 'd'=0x64, 'a'=0x61, 't'=0x74
            final byte[] mdatData = new byte[] {0x6d, 0x64, 0x61, 0x74};

            for (i = 0; i < buffer.length; ++i) {
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
                break;
            }
            if (i >= buffer.length) {
                Log.d(LOG_TAG, "not found mdat");
                return;
            }

            Log.d(LOG_TAG, "getMdatIndex: mMdatIndex = " + mMdatIndex);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

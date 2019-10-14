package com.example.mediamonitor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.hardware.Camera;
import android.view.View;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.widget.FrameLayout;
import android.widget.Button;
import android.util.Log;
import android.net.LocalSocketAddress;
import android.net.LocalSocket;
import android.net.LocalServerSocket;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.net.ServerSocket;

public class MediaMonitorActivity extends Activity implements SurfaceHolder.Callback {
    private static final String LOG_TAG = "MediaMonitor";

    private static final int MEDIA_TYPE_VIDEO = 1;

    private static final int EVENT_REMOTE_SOCKET_CONNECT = 1;

    private static final int SERVER_PORT = 9527;

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private boolean mMediaRecorderRecording = false;
    private Button mCaptureButton;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    // private RandomAccessFile randomAccessStream;
    private OutputStream mediaOutputStream;

    private Thread handleMediaThread;
    private LocalSocket mLocalSocketReceiver;
    private LocalSocket mLocalSocketSender;
    private LocalServerSocket mLocalServerSocket;
    private Socket mClientSocket = null;
    private ServerSocket mServerSocket;
    private ServerSocketThread mServerSocketThread;

    private int mMdatIndex = 32;
    private byte[] mSPSData;
    private byte[] mPPSData;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.d(LOG_TAG, "onCreate");

        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mLocalSocketReceiver = new LocalSocket();
        try {
            // 创建本地服务端Socket
            mLocalServerSocket = new LocalServerSocket("MediaMonitor");
            // 试图连接本地服务端Socket
            mLocalSocketReceiver.connect(new LocalSocketAddress("MediaMonitor"));
            Log.d(LOG_TAG, "mLocalSocketReceiver. receive buffer size = " + mLocalSocketReceiver.getReceiveBufferSize());
            Log.d(LOG_TAG, "mLocalSocketReceiver. send buffer size = " + mLocalSocketReceiver.getSendBufferSize());
            mLocalSocketReceiver.setReceiveBufferSize(500000);
            mLocalSocketReceiver.setSendBufferSize(500000);
            // 本地服务端Socket接受连接.
            mLocalSocketSender = mLocalServerSocket.accept();
            Log.d(LOG_TAG, "mLocalSocketSender. receive buffer size = " + mLocalSocketSender.getReceiveBufferSize());
            Log.d(LOG_TAG, "mLocalSocketSender. send buffer size = " + mLocalSocketSender.getSendBufferSize());
            mLocalSocketSender.setReceiveBufferSize(500000);
            mLocalSocketSender.setSendBufferSize(500000);
        } catch (IOException e) {
            e.printStackTrace();
            finish();
            return;
        }
        mServerSocketThread = new ServerSocketThread();
        mServerSocketThread.start();

        mCaptureButton = (Button) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mMediaRecorderRecording == true) {
                            stopMediaRecord();
                        } else {
                            startMediaRecord();
                        }
                    }
                }
        );

        getSPSAndPPS();
        getMdatIndex();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "MediaMonitor: onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "MediaMonitor: onPause");

        if (mMediaRecorderRecording == true) {
            stopMediaRecord();
            try {
                mLocalServerSocket.close();
                mLocalSocketReceiver.close();
                mLocalSocketSender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopMediaRecord() {
        Log.d(LOG_TAG, "stopMediaRecord: mMediaRecorderRecording = " + mMediaRecorderRecording);
        if (mMediaRecorderRecording == true) {
            try {
                // randomAccessStream.close();
                mLocalSocketReceiver.close();
                mClientSocket.close();
                mClientSocket = null;

                // inform the user that recording has stopped
                setCaptureButtonText(R.string.capture_text);
                mMediaRecorderRecording = false;

                if (handleMediaThread != null) {
                    handleMediaThread.interrupt();
                }

                releaseMediaRecorder();
                mLocalServerSocket.close();
                mLocalSocketSender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void startMediaRecord() {
        Log.d(LOG_TAG, "startMediaRecord: mMediaRecorderRecording = " + mMediaRecorderRecording + ", mClientSocket = " + mClientSocket);
        if (mMediaRecorderRecording == false && initializeVideo() == true) {
            mMediaRecorderRecording = true;
            setCaptureButtonText(R.string.stop_text);
            (handleMediaThread = new Thread() {
                @Override
                public void run() {
                    try {
                        byte[] buffer = new byte[1024 * 10];
                        InputStream inputStream = mLocalSocketReceiver.getInputStream();

                        DataInputStream dataInputStream = new DataInputStream(inputStream);
                        dataInputStream.read(buffer, 0, mMdatIndex);

                        // File file = new File("/sdcard/stream.h264");
                        // if (file.exists()) {
                        //     file.delete();
                        // }
                        // randomAccessStream = new RandomAccessFile(file, "rw");
                        mediaOutputStream = mClientSocket.getOutputStream();

                        final byte[] h264Head = {0, 0, 0, 1};

                        // 将首部数组的长度写入客户端
                        // int prefixHeadLength = h264Head.length * 2 + mSPSData.length + mPPSData.length;
                        // Log.d(LOG_TAG, "prefixHeadLength = " + prefixHeadLength);
                        // byte[] prefixHeadLengthBuffer = int2bytes(prefixHeadLength);
                        // mediaOutputStream.write(prefixHeadLengthBuffer);

                        // mediaOutputStream.write(h264Head);
                        // mediaOutputStream.write(mSPSData);
                        // mediaOutputStream.write(h264Head);
                        // mediaOutputStream.write(mPPSData);

                        // randomAccessStream.write(h264Head);
                        // randomAccessStream.write(mSPSData);
                        // randomAccessStream.write(h264Head);
                        // randomAccessStream.write(mPPSData);

                        int nleft = 0;
                        int number = 0;
                        for ( ;; ) {
                            // int h264Length = dataInputStream.readInt();
                            byte[] h264LengthBuffer = new byte[4]; 
                            int h264Length;
                            nleft = h264LengthBuffer.length;
                            while (nleft > 0) {
                                number = inputStream.read(h264LengthBuffer, 0, nleft);
                                nleft -= number;
                            }
                            h264Length = bytes2int(h264LengthBuffer);


                            Log.d(LOG_TAG, "startMediaRecord: read h264Length = " + h264Length);

                            // 先把下一次要写的 h264头和h264场 的长度写入客户端.
                            // 加上 4 个字节的 h264Head 数组长度.
                            int frameLength = h264Length + 4;

                            // 改变 socket 的输出缓冲区大小,让内核尽快将数据发送到网络
                            mClientSocket.setSendBufferSize(frameLength);

                            h264LengthBuffer = int2bytes(frameLength);
                            mediaOutputStream.write(h264LengthBuffer);

                            mediaOutputStream.write(h264Head);
                            // randomAccessStream.write(h264Head);

                            // 实际调试发现,不能直接直接 fis.read(buffer, 0, h264Length) 语句
                            // 来读取 h264Length 个字符,否则在读取一段时间后,上面readInt()读取
                            // 到的值很大,或者是负数,导致读取出错,程序崩溃.
                            // 原因这个语句表示最多读取h264Length个字符,它实际上返回的字符个数
                            // 可能会小于h264Length.而这个代码的意图是刚好读取h264Length个字符!
                            // 例如当h264Length等于680时,下面read()语句返回的num可能是242.
                            // 当这边的读取速度快过MediaRecorder写入的速度时,就可能发生这种情况.
                            // 所以,要改成用循环读,要刚好读取到h264Length个字符才再次调用readInt().
                            nleft = h264Length;
                            while (nleft > 0) {
                                number = inputStream.read(buffer, 0, nleft);
                                Log.d(LOG_TAG, "startMediaRecord: read number = " + number);
                                mediaOutputStream.write(buffer, 0, number);
                                // randomAccessStream.write(buffer, 0, number);
                                nleft -= number;
                            }
                            mediaOutputStream.flush();
                            sleep(10);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            Log.d(LOG_TAG, "MediaRecorder prepare failed!");
            releaseMediaRecorder();
        }
    }

    private void setCaptureButtonText(int id) {
        mCaptureButton.setText(id);
    }

    private boolean initializeVideo() {
        if (mSurfaceHolder == null || mClientSocket == null) {
            return false;
        }

        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        } else {
            mMediaRecorder.reset();
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoFrameRate(20);
        mMediaRecorder.setVideoSize(352, 288);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setMaxDuration(0);
        mMediaRecorder.setMaxFileSize(0);
        mMediaRecorder.setOutputFile(mLocalSocketSender.getFileDescriptor());

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            Log.d(LOG_TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null && mMediaRecorderRecording == true) {
            mMediaRecorder.stop();      // stop the recording
            mMediaRecorder.reset();     // clear recorder configuration
            mMediaRecorder.release();   // release the recorder object
            mMediaRecorder = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(LOG_TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(LOG_TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(LOG_TAG, "surfaceDestroyed");
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_REMOTE_SOCKET_CONNECT:
                    startMediaRecord();
                    break;
                default:
                    break;
            }
        }
    };

    private class ServerSocketThread extends Thread {
        @Override
        public void run() {
            Log.d(LOG_TAG, "ServerSocketThread: run");
            try {
                mServerSocket = new ServerSocket(SERVER_PORT);
                for ( ;; ) {
                    Log.d(LOG_TAG, "wait for accept");
                    mClientSocket = mServerSocket.accept();
                    Log.d(LOG_TAG, "accept: mClientSocket = " + mClientSocket);

                    Log.d(LOG_TAG, "mClientSocket: receive buffer size = " + mClientSocket.getReceiveBufferSize());
                    Log.d(LOG_TAG, "mClientSocket: send buffer size = " + mClientSocket.getSendBufferSize());

                    Message msg = mHandler.obtainMessage();
                    msg.what = EVENT_REMOTE_SOCKET_CONNECT;
                    mHandler.sendMessage(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
                mClientSocket = null;
            }
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
                Log.d(LOG_TAG, "SPS[" + i + "] = " + mSPSData[i]);
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
                Log.d(LOG_TAG, "PPS[" + i + "] = " + mPPSData[i]);
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            // 一定要先与上 0x000000ff,避免符号扩展.如果只写为ret |= buffer[i];
            // 会遇到类似于下面的情况:
            // 当 buffer[2] = 2 时, ret 等于 0x200
            // 而 buffer[3] = 0x9d 时, ret 不是等于 0x29d,而是等于 0xffffff9d,
            // 得到一个负数,即出现了将最高位的1进行符号扩展的情况.
            // 而写为 ret |= buffer[i] & 0x000000ff; 后,
            // 当 buffer[2] = 5 时, ret 等于 0x500
            // 而 buffer[3] = 97 时, ret 等于 0x597. 没有出现符号扩展的情况.
            ret |= buffer[i] & 0x000000ff;
        }
        Log.d(LOG_TAG, "final: bytes2int: ret = " + ret);
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

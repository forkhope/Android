package com.example.cameracapture;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.net.Uri;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String LOG_TAG = "MyCapture";

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_TAKE_VIDEO = 2;

    private ImageView mImageView;
    private VideoView mVideoView;
    private String mCurrentPhotoPath;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mVideoView = (VideoView) findViewById(R.id.video_view);
        mImageView = (ImageView) findViewById(R.id.photo_view);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,      /* prefix */
                ".jpg",             /* suffix */
                storageDir          /* directory */
                );

        /* Android在线网址上,这里写的是
         * mCurrentPhotoPath = "file:" + image.getAbsolutePath();
         * 实际调试发现,这个写法有问题.后面的galleryAddPicture()函数再调用Uri.fromFile()时
         * 获取到的Uri是错误的,因为多了"file:"这个部分.而setPicture()函数在执行
         * BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
         * 时,会提示文件找不到:
         * E/BitmapFactory(20434): Unable to decode stream: java.io.FileNotFoundException:
         * /file:/mnt/sdcard/Pictures/JPEG_20140718_113927_-1619218081.jpg: open failed:
         * ENOENT (No such file or directory)
         * 可以看到,由于在mCurrentPhotoPath变量中多加了"file:",导致将mCurrentPhotoPath作为
         * 路径参数时,获取到的路径名是错误的.
         * 所以,要把"file:"去掉,这样就是正确的.后面的代码会调用Uri.fromFile()来获取Uri.如
         * 果预先加了"file:",反而是多余的.例如:
         * galleryAddPicture: contentUri = file:///file%3A/mnt/sdcard/Pictures/
         * JPEG_20140718_112917_-1619218081.jpg --> 可以看到了,多了一个"file:".%3A就是':'
         */
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.d(LOG_TAG, "imageFileName = " + imageFileName);
        Log.d(LOG_TAG, "mCurrentPhotoPath = " + mCurrentPhotoPath);
        return image;
    }

    private void galleryAddPicture() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File targetFile = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(targetFile);
        mediaScanIntent.setData(contentUri);
        Log.d(LOG_TAG, "galleryAddPicture: targetFile = " + targetFile);
        Log.d(LOG_TAG, "galleryAddPicture: contentUri = " + contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void setPicture() {
        /* 注意,当mImageView还没有被填充内容时,不能在其布局文件(该程序中是main.xml)中
         * 将它的layout_width和layout_height设成 "wrap_content",否则其宽度和高度会是0,
         * 导致后面的代码报异常: java.lang.ArithmeticException: divide by zero
         * 要么在main.xml中将layout_height和layout_width设成"match_parent",要么指定具体
         * 的宽度和高度.例如 "100dp"
         */
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();
        Log.d(LOG_TAG, "setPicture: targetW = " + targetW + ", targetH = " + targetH);

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        Log.d(LOG_TAG, "setPicture: photoW = " + photoW + ", photoH = " + photoH);

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPicture();
            setPicture();
            /* 在传递所要保存照片的文件路径给Camera后,它回调onActivityResult()时,传递过
             * 来的 data 参数是空.此时,不能再调用"data.getExtras()"了,会触发空指针异常.
             */
            // Bundle extras = data.getExtras();
            // Bitmap imageBitmap = (Bitmap) extras.get("data");
            // mImageView.setImageBitmap(imageBitmap);
        }
        else if (requestCode == REQUEST_TAKE_VIDEO && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
            Log.d(LOG_TAG, "videoUri = " + videoUri);
            mVideoView.setVideoURI(videoUri);
            /* Android在线网址中,没有下面这一句,不会播放录好的视频,必须执行
             * start()函数之后才播放.
             */
            mVideoView.start();
        }
    }

    public void takePhoto(View view) {
        dispatchTakePictureIntent();
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_TAKE_VIDEO);
        }
    }

    public void recordVideo(View view) {
        dispatchTakeVideoIntent();
    }
}

package com.bytedance.component.vedioplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import java.io.File;

import io.julian.appchooser.AppChooser;
import io.julian.appchooser.util.Preconditions;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_MODIFY_FILE = 100 ;
    private LinearLayout VideoLayout;
    private MyVideoView videoView;
    private ImageView VideoPauseImg;
    private ImageView VideoStatusImg;
    private LinearLayout VideoStatusBtn;
    private TextView VideoTotalTime;
    private TextView VideoCurTime;

    private int state;
    private int duration;
    private SeekBar VideoSeekBar;

    //定义两个变量：代表当前屏幕的宽和屏幕的高
    private int screen_width, screen_height;


    //@Nullable
    //private AppChooser mAppChooser;

    //刷新机制的标志
    private final int UPDATE_UI = 1;

    /**
     * 定义Handler刷新时间
     * 得到并设置当前视频播放的时间
     * 得到并设置视频播放的总时间
     * 设置SeekBar总进度和当前视频播放的进度
     * 并反复执行Handler刷新时间
     * 指定标识用于关闭Handler
     */

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressLint("DefaultLocale")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI) {

                int currentPosition = videoView.getCurrentPosition();
                int total_duration = videoView.getDuration();

                VideoTotalTime.setText(String.format("%02d:%2d",total_duration/1000/60,total_duration/1000%60));

                VideoSeekBar.setMax(total_duration);
                VideoSeekBar.setProgress(currentPosition);

                mHandler.sendEmptyMessageDelayed(UPDATE_UI, 500);

            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化AppChooser
        //mAppChooser = AppChooser.with(this);
        Uri uri = getIntent().getData();

        VideoLayout = findViewById(R.id.video_layout);
        videoView = findViewById(R.id.video_view);
        videoView.requestFocus();
        if(uri == null){
            videoView.setVideoPath(getVideoPath(R.raw.bytedance));
        }else{
            videoView.setVideoURI(uri);
        }

        //自己设计的应用调用其他软件
        //String video = "视频地址";
        //Intent openVideo = new Intent(Intent.ACTION_VIEW);
        //videoView.setDataAndType(Uri.parse(video), "video/*");
        //startActivity(openVideo);

        VideoPauseImg = findViewById(R.id.video_pause_img);
        VideoStatusImg = findViewById(R.id.screen_status_img);
        VideoStatusBtn = findViewById(R.id.screen_status_btn);
        VideoTotalTime = findViewById(R.id.video_total_time);
        VideoCurTime = findViewById(R.id.video_cur_time);
        VideoSeekBar = findViewById(R.id.video_seek_bar);

        state = 1;
        screen_width = getResources().getDisplayMetrics().widthPixels;
        screen_height = getResources().getDisplayMetrics().heightPixels;

        VideoPauseImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    mHandler.removeMessages(UPDATE_UI);
                    VideoPauseImg.setImageResource(R.mipmap.icon_video_play);
                } else {
                    videoView.start();
                    mHandler.sendEmptyMessageDelayed(UPDATE_UI, 500);
                    VideoPauseImg.setImageResource(R.mipmap.icon_video_pause);
                }
            }
        });

        VideoStatusBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SourceLockedOrientationActivity")
            @Override
            public void onClick(View v) {
                if (state == 1) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    VideoStatusImg.setImageResource(R.mipmap.iconfont_exit);
//                    getWindow().clearFlags((WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN));
//                    getWindow().addFlags((WindowManager.LayoutParams.FLAG_FULLSCREEN));
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    getWindow().setAttributes(attrs);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                    state = 0;
                } else if (state == 0) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    VideoStatusImg.setImageResource(R.mipmap.iconfont_enter_32);
                    //WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    //attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    //getWindow().setAttributes(attrs);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                    state = 1;
                }
            }
        });

        VideoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                VideoCurTime.setText(String.format("%02d:%02d", progress / 1000 / 60, progress / 1000 % 60));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //拖动到时候关闭刷新机制
                mHandler.removeMessages(UPDATE_UI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //同步VideoView和拖动停止开启刷新机制
                // 当进度条停止修改的时候触发
                // 取得当前进度条的刻度
                int progress_new = seekBar.getProgress();
                // 设置当前播放的位置
                videoView.seekTo(progress_new);
                mHandler.sendEmptyMessage(UPDATE_UI);

            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onPrepared(final MediaPlayer mediaPlayer) {   //监听视频准备好
                mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
                        //VideoSeekBar.setMax(mp.getDuration());
                        //VideoSeekBar.setProgress(mp.getCurrentPosition());
                        //VideoCurTime.setText(String.format("%d",mp.getCurrentPosition()));
                    }
                });
                duration = videoView.getDuration() / 1000;
                // 这里获取到的时间是ms为单位的，所以要转换成秒的话就要除以1000
                VideoTotalTime.setText(String.format("%02d:%02d", duration / 60, duration % 60));
                VideoSeekBar.setMax(duration);
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onCompletion(MediaPlayer mp) {
                VideoPauseImg.setImageResource(R.mipmap.icon_video_play);
                mHandler.removeMessages(UPDATE_UI);
                duration = videoView.getDuration() / 1000;
                // 这里获取到的时间是ms为单位的，所以要转换成秒的话就要除以1000
                VideoCurTime.setText(String.format("%02d:%02d", duration / 60, duration % 60));
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig); 		// 检测屏幕的方向：纵向或横向
         if (this.getResources().getConfiguration().orientation
               == Configuration.ORIENTATION_LANDSCAPE){
             setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
             //主动取消半屏，该设置为全屏
             getWindow().clearFlags((WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN));
             getWindow().addFlags((WindowManager.LayoutParams.FLAG_FULLSCREEN));
         }
         else if (this.getResources().getConfiguration().orientation
               == Configuration.ORIENTATION_PORTRAIT) {
             setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(this, 240));
             //主动取消全屏，该设置为半屏
             getWindow().clearFlags((WindowManager.LayoutParams.FLAG_FULLSCREEN));
             getWindow().addFlags((WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN));
         }
    }

    private String getVideoPath(int resId) {
        return "android.resource://" + this.getPackageName() + "/" + resId;
    }

    /*设置VideoView和最外层相对布局的宽和高
     * @param width  : 像素的单位
     * @param height : 像素的单位*/
    private void setVideoViewScale(int width, int height) {
        //获取VideoView宽和高
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        //赋值给VideoView的宽和高
        layoutParams.width = width;
        layoutParams.height = height;
        //设置VideoView的宽和高
        videoView.setLayoutParams(layoutParams);

        //同上
        ViewGroup.LayoutParams layoutParams1 = VideoLayout.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        VideoLayout.setLayoutParams(layoutParams1);
    }
}

//    @Override
//    public void onStart() {
//        super.onStart();
//
//        // 绑定 AppChooser
//        mAppChooser.bind();
//    }
//    @Override
//    public void onStop() {
//        super.onStop();
//
//        // 解绑 AppChooser
//        mAppChooser.unbind();
//    }

//    private void showFile(@NonNull File file) {
//        // 检查文件非空
//        Preconditions.checkNotNull(file);
//        // 必须是文件
//        Preconditions.checkArgument(file.isFile());
//        mAppChooser.file(file).load();
//    }

/*
 * 打开文件并将编辑的结果回传给 Activity 或 Fragment
 * @param file 待打开的文件
 * @see android.app.Activity#onActivityResult(int, int, Intent)
 * @see android.support.v4.app.Fragment#onActivityResult(int, int, Intent)
 */
//    private void modifyFile(@NonNull File file) {
//        // 检查文件非空
//        Preconditions.checkNotNull(file);
//        // 必须是文件
//        Preconditions.checkArgument(file.isFile());
//        mAppChooser.file(file).requestCode(REQUEST_CODE_MODIFY_FILE).load();
//    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_MODIFY_FILE) {
//            // 编辑结果的回调
//        }
//    }

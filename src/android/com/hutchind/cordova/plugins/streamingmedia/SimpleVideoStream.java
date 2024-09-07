package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.widget.MediaController;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.MotionEvent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import android.widget.TextView;
import android.os.Build;
import android.content.res.ColorStateList;

public class SimpleVideoStream extends Activity implements
MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {
	private String TAG = getClass().getSimpleName();
	private VideoView mVideoView = null;
	private MediaPlayer mMediaPlayer = null;
	private MediaController mMediaController = null;
	private ProgressBar mProgressBar = null;
	private TextView close = null;
	private String mVideoUrl;
	private Boolean mShouldAutoClose = true;
	private boolean mControls;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		FullScreencall();

		Bundle b = getIntent().getExtras();
		mVideoUrl = b.getString("mediaUrl");
		mShouldAutoClose = b.getBoolean("shouldAutoClose", true);
		mControls = b.getBoolean("controls", true);

		RelativeLayout relLayout = new RelativeLayout(this);
		relLayout.setBackgroundColor(Color.BLACK);
		RelativeLayout.LayoutParams relLayoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		relLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mVideoView = new VideoView(this);
		mVideoView.setLayoutParams(relLayoutParam);
		relLayout.addView(mVideoView);

				// START EDIT
		close = new TextView(this);
		close.setText("Close");
		close.setBackgroundColor(Color.BLACK);
		close.getBackground().setAlpha(128);
		close.setTextColor(Color.WHITE);
		close.setTextSize(18);
		close.setPadding(10, 10, 10, 10);
		RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		close.setLayoutParams(closeLayoutParams);
		close.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				wrapItUp(RESULT_OK, null);
			}
		});
		relLayout.addView(close);
		close.bringToFront();
		// END EDIT


		// Create progress throbber
		mProgressBar = new ProgressBar(this);
		mProgressBar.setIndeterminate(true);
		// Center the progress bar
		RelativeLayout.LayoutParams pblp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		pblp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		mProgressBar.setLayoutParams(pblp);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.BLUE));
                }
		
		// Add progress throbber to view
		relLayout.addView(mProgressBar);
		mProgressBar.bringToFront();

		setOrientation(b.getString("orientation"));

		setContentView(relLayout, relLayoutParam);

		play();
	}
	
	@Override
    public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
            FullScreencall();
//             View decorView = getWindow().getDecorView();
//             if(hasFocus){
//                 decorView.setSystemUiVisibility(
//                         View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
//                         View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//                 );
//             }
    }

	private void FullScreencall() {
	     	// VISIV START
		if(Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
			View v = this.getWindow().getDecorView();
			v.setSystemUiVisibility(View.GONE);
		} else if(Build.VERSION.SDK_INT >= 19) {
			//for new api versions.
			View decorView = this.getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			decorView.setSystemUiVisibility(uiOptions);
		}
		// VISIV STOP
	}

	private void play() {
		mProgressBar.setVisibility(View.VISIBLE);
		close.setVisibility(View.VISIBLE);
		Uri videoUri = Uri.parse(mVideoUrl);
		try {
			mVideoView.setOnCompletionListener(this);
			mVideoView.setOnPreparedListener(this);
			mVideoView.setOnErrorListener(this);
			mVideoView.setVideoURI(videoUri);
			mMediaController = new MediaController(this);
			mMediaController.setAnchorView(mVideoView);
			mMediaController.setMediaPlayer(mVideoView);
			if (!mControls) {
				mMediaController.setVisibility(View.GONE);
				close.setVisibility(View.GONE);
			}
			mVideoView.setMediaController(mMediaController);
		} catch (Throwable t) {
			Log.d(TAG, t.toString());
		}
	}

	private void setOrientation(String orientation) {
		if ("landscape".equals(orientation)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}else if("portrait".equals(orientation)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}

	private Runnable checkIfPlaying = new Runnable() {
		@Override
		public void run() {
			if (mVideoView.getCurrentPosition() > 0) {
				// Video is not at the very beginning anymore.
				// Hide the progress bar.
				mProgressBar.setVisibility(View.GONE);
				close.setVisibility(View.GONE);
			} else {
				// Video is still at the very beginning.
				// Check again after a small amount of time.
				mVideoView.postDelayed(checkIfPlaying, 100);
			}
		}
	};

	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "Stream is prepared");
		mMediaPlayer = mp;
		mMediaPlayer.setOnBufferingUpdateListener(this);
		mVideoView.requestFocus();
		mVideoView.start();
		mVideoView.postDelayed(checkIfPlaying, 0);
	}

	private void pause() {
		Log.d(TAG, "Pausing video.");
		mVideoView.pause();
	}

	private void stop() {
		Log.d(TAG, "Stopping video.");
		mVideoView.stopPlayback();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy triggered.");
		stop();
	}

	private void wrapItUp(int resultCode, String message) {
		Log.d(TAG, "wrapItUp was triggered.");
		Intent intent = new Intent();
		intent.putExtra("message", message);
		setResult(resultCode, intent);
		finish();
	}

	public void onCompletion(MediaPlayer mp) {
		Log.d(TAG, "onCompletion triggered.");
		stop();
		if (mShouldAutoClose) {
			wrapItUp(RESULT_OK, null);
		}
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		StringBuilder sb = new StringBuilder();
		sb.append("MediaPlayer Error: ");
		switch (what) {
			case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			sb.append("Not Valid for Progressive Playback");
			break;
			case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			sb.append("Server Died");
			break;
			case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			sb.append("Unknown");
			break;
			default:
			sb.append(" Non standard (");
			sb.append(what);
			sb.append(")");
		}
		sb.append(" (" + what + ") ");
		sb.append(extra);
		Log.e(TAG, sb.toString());

		wrapItUp(RESULT_CANCELED, sb.toString());
		return true;
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.d(TAG, "onBufferingUpdate : " + percent + "%");
	}

	@Override
	public void onBackPressed() {
		// If we're leaving, let's finish the activity
		wrapItUp(RESULT_OK, null);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// The screen size changed or the orientation changed... don't restart the activity
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mMediaController != null)
			mMediaController.show();
		        close.setVisibility(View.VISIBLE);
		return false;
	}
}

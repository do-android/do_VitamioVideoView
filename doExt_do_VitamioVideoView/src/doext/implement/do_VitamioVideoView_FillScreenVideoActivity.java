package doext.implement;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import core.helper.DoScriptEngineHelper;
import core.object.DoInvokeResult;
import core.object.DoUIModule;

public class do_VitamioVideoView_FillScreenVideoActivity extends Activity implements OnInfoListener, OnBufferingUpdateListener, OnErrorListener, OnCompletionListener {

	private Context mContext;
	private DoUIModule model;
	private VideoView mVideoView;
	private long position;
	private String playUrl;
	private VitamioCloseFullActivityBroadcastReceiver myBroadcastReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initReceiver();
		mContext = this;
		String modelAddress = getIntent().getStringExtra("modelAddress");
		if (!TextUtils.isEmpty(modelAddress)) {
			model = DoScriptEngineHelper.parseUIModule(DoScriptEngineHelper.getCurrentPageScriptEngine(), modelAddress);
		}

		mVideoView = new VideoView(mContext, model, true);
		mVideoView.setMediaController(new MediaController(mContext));
		mVideoView.requestFocus();
		mVideoView.setOnInfoListener(this);
		mVideoView.setOnBufferingUpdateListener(this);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mediaPlayer) {
				mediaPlayer.setPlaybackSpeed(1.0f);
			}
		});

		RelativeLayout mRootView = new RelativeLayout(mContext);
		RelativeLayout.LayoutParams _relativeLP = new RelativeLayout.LayoutParams(-1, -1);
		_relativeLP.addRule(RelativeLayout.CENTER_VERTICAL);
		mRootView.addView(mVideoView, _relativeLP);
		mRootView.setBackgroundColor(Color.BLACK);
		setContentView(mRootView, new LayoutParams(-1, -1));

		playUrl = getIntent().getStringExtra("path");
		position = getIntent().getLongExtra("point", 0);
		try {
			mVideoView.setVideoURI(playUrl);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mVideoView.seekTo(position);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		fireInfoEvent(1, percent);
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			if (mVideoView.isPlaying()) {
				mVideoView.pause();
			}
			fireInfoEvent(0, 0);
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			mVideoView.start();
			fireInfoEvent(2, 0);
			break;
		case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
			fireInfoEvent(3, extra);
			break;
		}
		return true;
	}

	private DoInvokeResult infoResult;

	private void fireInfoEvent(int type, int val) {
		if (infoResult == null) {
			infoResult = new DoInvokeResult(model.getUniqueKey());
		}
		JSONObject _obj = new JSONObject();
		try {
			_obj.put("type", type);
			_obj.put("value", val);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		infoResult.setResultNode(_obj);
		model.getEventCenter().fireEvent("info", infoResult);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		model.getEventCenter().fireEvent("finished", new DoInvokeResult(model.getUniqueKey()));
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		model.getEventCenter().fireEvent("error", new DoInvokeResult(model.getUniqueKey()));
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			close();
		}
		return super.onKeyDown(keyCode, event);
	}

	public void close() {
		if (myBroadcastReceiver != null) {
			this.unregisterReceiver(myBroadcastReceiver);
		}
		Intent _resultIntent = new Intent();
		_resultIntent.putExtra("point", mVideoView.getCurrentPosition());
		setResult(2000, _resultIntent);
		finish();
	}

	private void initReceiver() {
		myBroadcastReceiver = new VitamioCloseFullActivityBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(do_VitamioVideoView_View.DO_VITAMIOVIDEO_VIEW_FINISH_FILL_SCREEN_VIDEO);
		registerReceiver(myBroadcastReceiver, filter);
	}

	private class VitamioCloseFullActivityBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent arg1) {
			if (arg1.getAction().equals(do_VitamioVideoView_View.DO_VITAMIOVIDEO_VIEW_FINISH_FILL_SCREEN_VIDEO)) {
				close();
			}
		}
	}

}

package doext.implement;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.MediaController.OnFullScreenClickListener;
import io.vov.vitamio.widget.VideoView;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.RelativeLayout;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoResourcesHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoActivityResultListener;
import core.interfaces.DoBaseActivityListener;
import core.interfaces.DoIPageView;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.define.do_VitamioVideoView_IMethod;
import doext.define.do_VitamioVideoView_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,
 * do_VitamioVideoView_IMethod接口； #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_VitamioVideoView_View extends RelativeLayout implements DoIUIModuleView, do_VitamioVideoView_IMethod, OnInfoListener, OnBufferingUpdateListener, OnErrorListener, OnCompletionListener,
		OnFullScreenClickListener, DoActivityResultListener, DoBaseActivityListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private String playUrl;
	private long palyTime;
	private Context mContext;
	private ProgressDialog mPD;
	private VideoView mVideoView;
	private do_VitamioVideoView_MAbstract model;
	public static final String DO_VITAMIOVIDEO_VIEW_FINISH_FILL_SCREEN_VIDEO = "do_vitamiovideo_view_finish_fill_screen_video";

	public do_VitamioVideoView_View(Context context) {
		super(context);
		this.mContext = context;
		if (!Vitamio.isInitialized(mContext)) { // 需要解压so文件
			new AsyncTask<Object, Object, Boolean>() {
				@Override
				protected void onPreExecute() {
					mPD = new ProgressDialog(mContext);
					mPD.setCancelable(false);
					mPD.setMessage(mContext.getString(DoResourcesHelper.getRIdByString("vitamio_init_decoders", "do_VitamioVideoView")));
					mPD.show();
				}

				@Override
				protected Boolean doInBackground(Object... params) {
					return Vitamio.initialize(mContext);
				}

				@Override
				protected void onPostExecute(Boolean inited) {
					if (inited) {
						mPD.dismiss();
					}
				}

			}.execute();
		}
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_VitamioVideoView_MAbstract) _doUIModule;
		mVideoView = new VideoView(mContext, model, false);
		MediaController _mController = new MediaController(mContext);
		_mController.setOnFullScreenClickListener(this);
		mVideoView.setMediaController(_mController);
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
		RelativeLayout.LayoutParams _relativeLP = new RelativeLayout.LayoutParams((int) model.getRealWidth(), (int) model.getRealHeight());
		_relativeLP.addRule(CENTER_IN_PARENT);
		this.addView(mVideoView, _relativeLP);
		((DoIPageView) mContext).setBaseActivityListener(this);

	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("path")) {
			String _path = _changedValues.get("path");
			try {
//				if (null == DoIOHelper.getHttpUrlPath(_path)) {
//					_path = DoIOHelper.getLocalFileFullPath(this.model.getCurrentPage().getCurrentApp(), _path);
//				}
				if (_path != null && !_path.equals("")) {
					playUrl = _path;
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("do_VideoView_View path \n\t", e);
			}
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("play".equals(_methodName)) {
			this.play(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("pause".equals(_methodName)) { // 暂停播放
			this.pause(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("resume".equals(_methodName)) { // 继续播放
			this.resume(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("stop".equals(_methodName)) {
			this.stop(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("isPlaying".equals(_methodName)) {
			this.isPlaying(_dictParas, _scriptEngine, _invokeResult);
		}
		if ("getCurrentPosition".equals(_methodName)) {
			this.getCurrentPosition(_dictParas, _scriptEngine, _invokeResult);
		}
		if ("expand".equals(_methodName)) {
			this.expand(_dictParas, _scriptEngine, _invokeResult);
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 暂停播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void pause(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (mVideoView.isPlaying()) {
			mVideoView.pause();
		}
		palyTime = mVideoView.getCurrentPosition();
		_invokeResult.setResultFloat(palyTime);
	}

	/**
	 * 开始播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void play(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (playUrl == null)
			throw new Exception("url 不能为空");
		int pos = DoJsonHelper.getInt(_dictParas, "point", 0);
		mVideoView.setVideoURI(playUrl);
		if (!mVideoView.isPlaying()) {
			// 按照初始位置播放
			mVideoView.start();
			mVideoView.seekTo(pos);
		}
	}

	// Uri uri = Uri.parse("cache:/sdcard/download.mp4:" + playUrl);

	/**
	 * 继续播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void resume(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (!mVideoView.isPlaying()) {
			mVideoView.resume();
		}
	}

	/**
	 * 停止播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void stop(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		mVideoView.stopPlayback();
	}

	/**
	 * 获取视频播放状态
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void isPlaying(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		_invokeResult.setResultBoolean(mVideoView.isPlaying());
	}

	@Override
	public void getCurrentPosition(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (mVideoView.isPlaying()) {
			_invokeResult.setResultFloat(mVideoView.getCurrentPosition());
		} else {
			_invokeResult.setResultInteger(0);
		}
	}

	@Override
	public void expand(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		boolean _isFullScreen = DoJsonHelper.getBoolean(_dictParas, "isFullScreen", false);
		if (_isFullScreen) {
			fillScreen();
		} else {
			Intent intent = new Intent(DO_VITAMIOVIDEO_VIEW_FINISH_FILL_SCREEN_VIDEO);
			mContext.sendBroadcast(intent);
		}
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
	public void onFullScreenClick(View view) {
		fillScreen();
	}

	private void fillScreen() {
		Intent i = new Intent(mContext, do_VitamioVideoView_FillScreenVideoActivity.class);
		i.putExtra("path", this.playUrl);
		i.putExtra("modelAddress", model.getUniqueKey());
		i.putExtra("point", mVideoView.getCurrentPosition());
//		i.putExtra("controlVisible", videoPlayer.isControlVisible());
		((DoIPageView) mContext).registActivityResultListener(this);
		((Activity) mContext).startActivityForResult(i, 1000);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 1000) {
			((DoIPageView) mContext).unregistActivityResultListener(this);
			if (resultCode == 2000) {
				palyTime = intent.getLongExtra("point", palyTime);
				try {
					mVideoView.setVideoURI(playUrl);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (!mVideoView.isPlaying()) {
					// 按照初始位置播放
					mVideoView.start();
					mVideoView.seekTo(palyTime);
				}
			}
		}
	}

	@Override
	public void onResume() {

	}

	@Override
	public void onPause() {
		palyTime = mVideoView.getCurrentPosition();
		mVideoView.pause();
	}

	@Override
	public void onRestart() {

	}

	@Override
	public void onStop() {
		
	}
}
package doext.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;


public abstract class do_VitamioVideoView_MAbstract extends DoUIModule{

	protected do_VitamioVideoView_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
		this.registProperty(new DoProperty("path", PropertyDataType.String, "", false));
		this.registProperty(new DoProperty("isCache", PropertyDataType.Bool, "false", false));
	}
}
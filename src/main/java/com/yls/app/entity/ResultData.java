package com.yls.app.entity;

/**
 * 返回结果类
 * 
 * @author lishibo
 * @2018年2月12日 @上午10:32:28
 */
public class ResultData {

	public String ret;// 返回结果；0-失败，1-成功
	public Object returnData;// 返回的数据
	public String info;// 返回说明，成功则写成功，失败需要说明失败原因

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getRet() {
		return ret;
	}

	public void setRet(String ret) {
		this.ret = ret;
	}

	public Object getReturnData() {
		return returnData;
	}

	public void setReturnData(Object returnData) {
		this.returnData = returnData;
	}

}

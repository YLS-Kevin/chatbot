/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年6月12日下午5:41:36
 */
public class DynaException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1162796870817143979L;
	
	private int errorCode;

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * 
	 */
	public DynaException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}

/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年6月11日下午5:09:44
 */
public class DialogTypeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -550120456945863237L;

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
	public DialogTypeException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}

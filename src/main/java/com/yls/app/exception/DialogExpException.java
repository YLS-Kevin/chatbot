/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年6月12日下午3:41:59
 */
public class DialogExpException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2012276977386226688L;

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
	public DialogExpException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}

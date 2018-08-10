/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年6月21日下午6:21:16
 */
public class DialogTypeSelfException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -785448766009142935L;
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
	public DialogTypeSelfException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}

/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年6月8日下午3:08:14
 */
public class DialogException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 93498263470007677L;
	
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
	public DialogException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}

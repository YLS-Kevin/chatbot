/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年6月27日上午10:09:37
 */
public class DialogInterDataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6404677544603950566L;
	
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
	public DialogInterDataException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
	
}

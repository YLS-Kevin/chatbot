/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年3月23日下午3:26:10
 */
public class ReloadCustomDictionaryException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5315912453684124902L;
	
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
	public ReloadCustomDictionaryException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
	
}

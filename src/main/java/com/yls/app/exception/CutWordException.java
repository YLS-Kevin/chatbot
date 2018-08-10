/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年3月16日上午11:02:05
 */
public class CutWordException extends Exception {
	
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
	public CutWordException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}

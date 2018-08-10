/**
 * 
 */
package com.yls.app.exception;

/**
 * @author huangsy
 * @date 2018年3月14日上午11:21:48
 */
public class ChatBotException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4603752358131457488L;

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
	public ChatBotException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}

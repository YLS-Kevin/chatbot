/**
 * 
 */
package com.yls.app.entity;

import com.yls.app.chatbot.History;
import com.yls.app.chatbot.KeywordHistory;

/**
 * @author huangsy
 * @date 2018年3月15日下午5:15:19
 */
public class ChatThat {

	private String that;
	private History answerHistory;
	private String id_ap;
	private KeywordHistory keywordHistory;

	public String getThat() {
		return that;
	}

	public void setThat(String that) {
		this.that = that;
	}

	public History getAnswerHistory() {
		return answerHistory;
	}

	public void setAnswerHistory(History answerHistory) {
		this.answerHistory = answerHistory;
	}

	public String getId_ap() {
		return id_ap;
	}

	public void setId_ap(String id_ap) {
		this.id_ap = id_ap;
	}

	public KeywordHistory getKeywordHistory() {
		return keywordHistory;
	}

	public void setKeywordHistory(KeywordHistory keywordHistory) {
		this.keywordHistory = keywordHistory;
	}

}

/**
 * 
 */
package com.yls.app.chatbot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import com.google.gson.Gson;
import com.hankcs.hanlp.seg.common.Term;
import com.yls.app.entity.Answer;

/**
 * 对话类
 * @author huangsy
 * @date 20182018年2月9日上午10:23:55
 */
public class Chat implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8552387840106595154L;

	private final static Logger logger = Logger.getLogger(Chat.class);

	private String sessionId;

	public String getSessionId() {
		return this.sessionId;
	}

	private Bot bot;
	private String that;
	private String id_ap;
	private History answerHistory = new History("answerHistory");
	private KeywordHistory keywordHistory = new KeywordHistory("keywordHistory");

	public String getThat() {
		return that;
	}

	public void setThat(String that) {
		this.that = that;
	}
	
	public String getId_ap() {
		return id_ap;
	}

	public void setId_ap(String id_ap) {
		this.id_ap = id_ap;
	}

	public History getAnswerHistory() {
		return answerHistory;
	}

	public void setAnswerHistory(History answerHistory) {
		this.answerHistory = answerHistory;
	}
	
	public KeywordHistory getKeywordHistory() {
		return keywordHistory;
	}

	public void setKeywordHistory(KeywordHistory keywordHistory) {
		this.keywordHistory = keywordHistory;
	}

	public Chat() {

	}

	/**
	 * @param bot 机器人
	 * @param sessionId 用户会话id，唯一标记一个用户会话
	 */
	public Chat(Bot bot, String sessionId) {
		this.bot = bot;
		this.sessionId = sessionId;
	}

	/**
	 * 回复
	 * @param question
	 * @return
	 */
	public String respond(String question, String cid, String acid, String lat, String lon, String cip, String os, 
			String region, String scity, String saddr, String cid_m) {
		logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.info("本轮对话，机器人id：" + cid + "，模块id：" + cid_m + "，账号id：" + acid + "，经度：" + lat + "，纬度：" + lon + "，ip地址：" + cip + "，操作系统：" + os 
				+ "，region：" + region + "，scity：" + scity + "，saddr：" + saddr);
		logger.info("人说的话：" + question);
		Answer answer = bot.process3(question, this.that, cid, acid, this.id_ap, cid_m);
		this.that = answer.getTemplate();
		String temp_id_ap = answer.getId_ap();
		if(temp_id_ap!=null && !"".equals(temp_id_ap)) {
			this.id_ap = temp_id_ap;
		} 
		this.answerHistory.add(answer);
		if(answer.getKeywords() != null) {
			this.addHistoryKeywords(answer.getKeywords());
		}
		return new Gson().toJson(answer);
	}
	
	private void addHistoryKeywords(List<Term> keywords) {
		
		for(Term term : keywords) {
			keywordHistory.add(term);
		}
		
	}
	
	public List<Term> getKeywords() {
		List<Term> result = new ArrayList<Term>();
		for(int i=0;i<keywordHistory.getCurrentSize();i++) {
			result.add(keywordHistory.get(i));
		}
		return result;
	}

}

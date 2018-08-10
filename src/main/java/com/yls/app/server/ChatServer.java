/**
 * 
 */
package com.yls.app.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.hankcs.hanlp.seg.common.Term;
import com.yls.app.chatbot.Chat;
import com.yls.app.chatbot.History;
import com.yls.app.entity.Answer;
import com.yls.app.entity.ChatThat;
import com.yls.app.entity.ResultData;
import com.yls.app.entity.Word;
import com.yls.app.repository.AIKey;

/**
 * 对话服务
 * @author huangsy
 * @date 20182018年2月9日上午10:23:42
 */
@Service
public class ChatServer {
	
	private final static Logger logger = Logger.getLogger(ChatServer.class);
	
	private Chat chat;
	private String mansay;
	private String cid;
	private String sbid;
	private String cip;
	private String lon;
	private String lat;
	private String scity;
	private String saddr;
	private String robotsay;
	private String participle;
	private List<Term> keywords;
	private String isfind;
	private String os;
	private String region;
	private String cid_m;
	
	public String getMansay() {
		return mansay;
	}

	public String getCid() {
		return cid;
	}

	public String getSbid() {
		return sbid;
	}

	public String getCip() {
		return cip;
	}

	public String getLon() {
		return lon;
	}

	public String getLat() {
		return lat;
	}

	public String getScity() {
		return scity;
	}

	public String getSaddr() {
		return saddr;
	}

	public String getRobotsay() {
		return robotsay;
	}

	public String getParticiple() {
		return participle;
	}
	
	public List<Term> getKeywords() {
		return keywords;
	}
	
	public String getIsfind() {
		return isfind;
	}
	
	public String getOs() {
		return os;
	}
	
	public String getRegion() {
		return region;
	}
	
	public String getCid_m() {
		return cid_m;
	}
	
	/**
	 * 用于处理用户输入的一句话
	 * @param chat
	 * @param question
	 * @return
	 */
	public String respond(Chat chat, String question, String cid, String sbid, String lon, String lat, 
			String scity, String saddr, String cip, String os, String acid, String region, String cid_m) {
		long startTime = System.currentTimeMillis();
		this.chat = chat;//保存chat对象用于生成chatThatJson
		this.cid = cid;
		this.sbid = sbid;
		this.lon = lon;
		this.lat = lat;
		this.scity = scity;
		this.saddr = saddr;
		this.cip = cip;
		this.mansay = question;
		this.os = os;
		this.region = region;
		this.cid_m = cid_m;
		
		String result = "";
		
		String json = chat.respond(question, cid, acid, lat, lon, cip, os, region, scity, saddr, cid_m);
		Answer answer = new Gson().fromJson(json, Answer.class);
		//获取动态词组类型
		dynas = answer.getDynas();
		logger.info("命中的pattern为：" + answer.getPattern());
		//是否需要前缀
		String is_contain_kw = answer.getIs_contain_kw();
		//是否接口回复
		String type = answer.getType();
		//获取分词结果
		this.keywords = answer.getKeywords();
		//获取上下文动态词
		List<Term> lastKeywords = chat.getKeywords();
		//默认返回的脚本
		Map<String, String> defscript = new HashMap<String, String>();
		defscript.put("action", "normal");
//		chat.getKeywordHistory();
		//type=1需要调用数据接口获取数据
		if("1".equals(type)) {
			String url = answer.getTemplate();
			List<Term> keywords = answer.getKeywords();
//			if(chat.getAnswerHistory().get(1)!=null) {
//				lastKeywords = chat.getAnswerHistory().get(1).getKeywords();
//			}
			//获取未知词部分
			String wz = this.getUnknowString(answer, question);
			logger.info("原来的url为：" + url);
			String urlR = this.urlProcess(url, keywords, lastKeywords, scity, saddr, lon, lat, os, wz, region);
			logger.info("拼接的url为：" + urlR);
			//获取调用接口返回的result和script
			String doGet = this.doGet(urlR);
			String ret = new Gson().fromJson(doGet, ResultData.class).getRet();
			
			//ret为1说明调用数据接口成功
			if("1".equals(ret)) {
				Object doGetResult = "";
				Object doGetScript = "";
				Object doGetResultMap = "";
				
				if(new Gson().fromJson(doGet, ResultData.class).getReturnData() instanceof Map) {
					Map<String, Object> returnData = (Map<String, Object>)new Gson().fromJson(doGet, ResultData.class).getReturnData();
					doGetResult = returnData.get("result");
					doGetResultMap = returnData.get("resultMap");
				} else if(new Gson().fromJson(doGet, ResultData.class).getReturnData() instanceof String) {
					doGetResult = (String)new Gson().fromJson(doGet, ResultData.class).getReturnData();
					doGetResultMap = (String)new Gson().fromJson(doGet, ResultData.class).getReturnData();
				}
				
				ResultData rd = new ResultData();
				rd.setInfo("接口调用成功");
				rd.setRet("1");
				Map<String, Object> resultMap = new HashMap<String, Object>();
				resultMap.put("result", doGetResult);
				resultMap.put("resultMap", doGetResultMap);
				resultMap.put("lastKeywords", lastKeywords);
				resultMap.put("is_contain_kw", is_contain_kw);
				//判断script类型
				String stype = (String)answer.getScript().get("stype");
				if("1".equals(stype)) {
//					resultMap.put("result", doGetResult);
					resultMap.put("script", defscript);
					rd.setReturnData(resultMap);
				} else if("2".equals(stype)) {
					if(new Gson().fromJson(doGet, ResultData.class).getReturnData() instanceof Map) {
						Map<String, Object> returnData = (Map<String, Object>)new Gson().fromJson(doGet, ResultData.class).getReturnData();
						doGetScript = returnData.get("script");
					} else if(new Gson().fromJson(doGet, ResultData.class).getReturnData() instanceof String) {
						doGetScript = new HashMap<String, Object>();
					}
//					resultMap.put("result", doGetResult);
					resultMap.put("script", doGetScript);
					rd.setReturnData(resultMap);
				} else if("3".equals(stype)) {
					String sin = (String)answer.getScript().get("sin");
					String sinwords = (String)answer.getScript().get("sinword");
					String[] sinwordArray = sinwords.split("\\|");
					Map script = new Gson().fromJson(answer.getScript().get("scripts").toString(), Map.class);
					if(doGetResult instanceof String) {
						if("1".equals(sin)) {
//						resultMap.put("result", doGetResult);
							resultMap.put("script", defscript);
							for(String sinword : sinwordArray) {
								if(((String) doGetResult).contains(sinword)) {
//								resultMap.put("result", doGetResult);
									resultMap.put("script", script);
									break;
								}
								
							}
						} else if("2".equals(sin)) {
//						resultMap.put("result", doGetResult);
							resultMap.put("script", defscript);
							if(this.isUnContain((String)doGetResult, sinwordArray, 0)) {
//							resultMap.put("result", doGetResult);
								resultMap.put("script", script);
							}
						} else if("3".equals(sin)) {
//						resultMap.put("result", doGetResult);
							resultMap.put("script", defscript);
							if(sinwords.equals((String)doGetResult)) {
//							resultMap.put("result", doGetResult);
								resultMap.put("script", script);
							}
							
						} else if("4".equals(sin)) {
//						resultMap.put("result", doGetResult);
							resultMap.put("script", defscript);
							if(!sinwords.equals((String)doGetResult)) {
//							resultMap.put("result", doGetResult);
								resultMap.put("script", script);
							}
						}
					}
				}
				rd.setReturnData(resultMap);
				result = new Gson().toJson(rd);
				this.isfind = "1";
			} else {//ret不为1说明调用数据接口失败
				result = doGet;
				this.isfind = "0";
			}
		} else if("0".equals(type)) {//type=0直接返回
			String template = answer.getTemplate();
			String pattern = answer.getPattern();
			String script = "";
			if(answer.getScript().get("scripts")!=null && !"".equals(answer.getScript().get("scripts"))) {
				script = (String)answer.getScript().get("scripts");
			}
			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultMap.put("result", template);
			resultMap.put("lastKeywords", lastKeywords);
			resultMap.put("is_contain_kw", is_contain_kw);
			if(new Gson().fromJson(script, Map.class)!=null) {
				resultMap.put("script", new Gson().fromJson(script, Map.class));
			} else {
				resultMap.put("script", defscript);
			}
			ResultData rd = new ResultData();
			rd.setInfo("接口调用成功");
			if(AIKey.EXP_ANSWER.equals(pattern)) {
				String code = answer.getCode();
				resultMap.put("code", code);
				rd.setRet("9");
				this.isfind = "0";
			} else if(AIKey.TURING_ANSWER.equals(pattern)) {
				String code = answer.getCode();
				resultMap.put("code", code);
				rd.setRet("10");
				this.isfind = "0";
			} else {
				rd.setRet("1");
				this.isfind = "1";
			}
			rd.setReturnData(resultMap);
			result = new Gson().toJson(rd);
		} else if("2".equals(type)) {//返回接口url由前端调用
			String url = answer.getTemplate();
			List<Term> keywords = answer.getKeywords();
			//获取未知词部分
			String wz = this.getUnknowString(answer, question);
			logger.info("原来的url为：" + url);
			String urlR = this.urlProcess(url, keywords, lastKeywords, scity, saddr, lon, lat, os, wz, region);
			logger.info("拼接的url为：" + urlR);
			String script = "";
			if(answer.getScript().get("scripts")!=null && !"".equals(answer.getScript().get("scripts"))) {
				script = (String)answer.getScript().get("scripts");
			}
			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultMap.put("result", urlR);
			resultMap.put("lastKeywords", lastKeywords);
			resultMap.put("is_contain_kw", is_contain_kw);
			if(new Gson().fromJson(script, Map.class)!=null) {
				resultMap.put("script", new Gson().fromJson(script, Map.class));
			} else {
				resultMap.put("script", defscript);
			}
			ResultData rd = new ResultData();
			rd.setInfo("接口调用成功");
			rd.setRet("1");
			this.isfind = "1";
			rd.setReturnData(resultMap);
			result = new Gson().toJson(rd);
		}
		this.robotsay = result;
		logger.info("机器人回复：" + result);
		logger.info("总耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 获取关键词
	 * @param history
	 * @return
	 */
	public List<Term> getLastKeywords(History history) {
		List<Term> result = new ArrayList<Term>();
		for(int i=0;i<history.getCurrentSize();i++) {
			List<Term> keywords = history.get(i).getKeywords();
			result.addAll(keywords);
		}
		LinkedHashSet<Term> set = new LinkedHashSet<Term>(result.size());
	    set.addAll(result);
	    List<Term> result2 = new ArrayList<Term>();
	    result2.addAll(set);
		return result2;
	}
	
	/**
	 * 获取未知词部分
	 * @param answer
	 * @return
	 */
	public String getUnknowString(Answer answer, String question) {
		
		String wz = "";
		String pattern = answer.getPattern();
		List<String> keywords = this.pattern2Keywords(pattern);
		List<Term> terms = answer.getKeywords();
		List<String> natures = this.term2Nature(terms);
		Map<String, Integer> naturesMap = this.term2NatureMap(terms);
		//分词集转词集
		List<String> words = this.term2Word(terms);
		if(keywords.contains(AIKey.UNKNOW)) {
			int index = keywords.indexOf(AIKey.UNKNOW);
			if(index == 0) {
				int right = index + 1;
				String rightString = keywords.get(right);
				if(this.isDyna(rightString)) {
					int i = natures.indexOf(rightString);
					rightString = terms.get(i).word;
				}
				int right2 = question.indexOf(rightString);
				wz = question.substring(0, right2);
			} else if(index == (keywords.size()-1)) {
				int left = index - 1;
				String leftString = keywords.get(left);
				if(this.isDyna(leftString)) {
					int i = natures.indexOf(leftString);
					leftString = terms.get(i).word;
				}
				int left2 = question.indexOf(leftString) + leftString.length();
				wz = question.substring(left2);
			} else {
				int left = index -1;
				int right = index + 1;
				String leftString = keywords.get(left);
				String rightString = keywords.get(right);
				if(this.isDyna(rightString)) {
					int i = natures.indexOf(rightString);
					rightString = terms.get(i).word;
				}
				if(this.isDyna(leftString)) {
					int i = natures.indexOf(leftString);
					leftString = terms.get(i).word;
				}
				int left2 = question.indexOf(leftString) + leftString.length();
				int right2 = question.indexOf(rightString);
				if(left2 < right2) {
					wz = question.substring(left2, right2);
				} else {
					
				}
			}
		}
		return wz;
		
	}
	
	/**
	 * 动态词类型集合
	 */
	private List<String> dynas = new ArrayList<String>();
	
	/**
	 * 判断关键词是否为动态词
	 * @param word
	 * @return
	 */
	private boolean isDyna(String type) {
		boolean flag = false;
		if(type==null || "".equals(type)) {
			flag = false;
		} else if(dynas.contains(type)) {
			flag = true;
		}
		return flag;
	}
	
	/**
	 * 分词集合转词性集合
	 * @param terms
	 * @return
	 */
	private List<String> term2Nature(List<Term> terms) {
		
		List<String> result = new ArrayList<String>();
		
		for(Term term : terms) {
//			if(term.nature.equals(Nature.ns)) {
//				result.add(AIKey.SITUS);
//			} else if(term.nature.equals(Nature.t)) {
//				result.add(AIKey.TIME);
//			} else {
//				result.add(term.nature.toString());
//			}
			if(term.type==null) {
				result.add(AIKey.DEFAUT_DYNA);
			} else {
				result.add(term.type.get(0));
			}
		}
		
		return result;
	}
	
	/**
	 * 分词集合转词性集合（map）
	 * @param terms
	 * @return
	 */
	private Map<String, Integer> term2NatureMap(List<Term> terms) {
		
		Map<String, Integer> result = new HashMap<String, Integer>();
		int index = 0;
		for(Term term : terms) {
			if(term.type==null) {
				result.put(AIKey.DEFAUT_DYNA, index);
			} else {
				for(String type : term.type) {
					result.put(type, index);
				}
			}
			index++;
		}
		return result;
		
	}
	
	/**
	 * 分词集合转词集合
	 * @param terms
	 * @return
	 */
	private List<String> term2Word(List<Term> terms) {
		
		List<String> result = new ArrayList<String>();
		
		for(Term term : terms) {
			result.add(term.word.toString());
		}
		
		return result;
	}
	
	/**
	 * 句式转关键词
	 * @param sentence
	 * @return
	 */
	private List<String> pattern2Keywords(String pattern) {
		List<String> keywords = new ArrayList<String>();
		
		String[] sub = pattern.split("\\(@\\)");
		keywords = Arrays.asList(sub);
		
		return keywords;
	}
	
	/**
	 * 获取对应chat的ChatThatJson字符串
	 * @return
	 */
	public String getChatThatJson() {
		
		String chatThatJson = "";
		
		ChatThat chatThat = new ChatThat();
		chatThat.setThat(this.chat.getThat());
		chatThat.setAnswerHistory(this.chat.getAnswerHistory());
		chatThat.setId_ap(this.chat.getId_ap());
		chatThat.setKeywordHistory(this.chat.getKeywordHistory());
		
		chatThatJson = new Gson().toJson(chatThat);
		
		return chatThatJson;
		
	}
	
	/**
	 * 递归判断不包含关系
	 */
	private boolean isUnContain(String doGetResult, String[] sinwordArray, int i) {
		
		if(i==sinwordArray.length) {
			return true;
		}
		if(!doGetResult.contains(sinwordArray[i])) {
			if(isUnContain(doGetResult, sinwordArray, i+1)) {
				return true;
			} else {
				return false;
			}
		}
		return false;
		
	}
	
	/**
	 * 处理url，返回可以直接调用的url
	 * @param url
	 * @param terms
	 * @return
	 */
	private String urlProcess(String url, List<Term> terms, List<Term> lastTerms, String scity, String saddr, String lon, 
			String lat, String os, String wz, String region) {
		
		String urlP = "";
		for(int i=0;i<terms.size();i++) {
			Term t = terms.get(i);
			//替换动态词
			url = this.replaceUrlDyna(t, url);
//			if(t.nature == Nature.t) {
//				url = url.replaceFirst("\\(&t&\\)", t.word);
//			} 
//			if(t.nature == Nature.ns) {
//				url = url.replaceFirst("\\(&ns&\\)", t.word);
//			}
		}
		if(lastTerms!=null) {
			for(int i=0;i<lastTerms.size();i++) {
				Term t = lastTerms.get(i);
				//替换动态词
				url = this.replaceUrlDyna(t, url);
//				if(t.nature == Nature.t) {
//					url = url.replaceFirst("\\(&t&\\)", t.word);
//				} 
//				if(t.nature == Nature.ns) {
//					url = url.replaceFirst("\\(&ns&\\)", t.word);
//				}
			}
		}
		if(wz != null) {
			logger.info("替换url参数值为(&wz&)的部分");
			url = url.replaceAll("\\(&wz&\\)", wz);
		}
		url = url.replaceFirst("\\(&ns&\\)", scity);
		url = url + "&saddr=" + saddr + "&lon=" + lon + "&lat=" + lat + "&os=" + os + "&region=" + region + "&scity=" + scity;
		urlP = url;
		return urlP;
		
	}
	
	/**
	 * 替换动态词
	 * @param term
	 * @param url
	 * @return
	 */
	private String replaceUrlDyna(Term term, String url) {
//		String type = term.type;
		if(term.type!=null) {
			for(String type : term.type) {
				if(type!=null && !"".equals(type)) {
					type = type.replace("(", "\\(");
					type = type.replace(")", "\\)");
					url = url.replaceFirst(type, term.trueWord);
				}
			}
		}
		return url;
	}

	/**
	 * 传入url，直接返回调用接口的结果
	 * @param url
	 * @return
	 */
	private String doGet(String url) {
		long startTime = System.currentTimeMillis();
		String jsonResult = "";
		// 创建一个httpclient对象
		CloseableHttpClient httpClient = HttpClients.createDefault();
		// 创建一个GET对象
		HttpGet get = new HttpGet(url);
		// 执行请求
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(get);
			// 取响应的结果
			int statusCode = response.getStatusLine().getStatusCode();
//			System.out.println(statusCode);
			HttpEntity entity = response.getEntity();
			jsonResult = EntityUtils.toString(entity, "utf-8");
//			System.out.println(jsonResult);
		} catch (Exception e) {
			logger.error("执行doGet出错", e);
		} finally {
			// 关闭httpclient
			if(response!=null) {
				try {
					response.close();
				} catch (Exception e) {
					logger.error("关闭httpclient response出错", e);
				}
			}
			if(httpClient!=null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					logger.error("关闭httpclient httpclient出错", e);
				}
			}
		}
		logger.info("调用url耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return jsonResult;
	}

}

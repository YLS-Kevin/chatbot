/**
 * 
 */
package com.yls.app.chatbot;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import javax.annotation.Resource;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.gson.Gson;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.yls.app.entity.Answer;
import com.yls.app.entity.Dyna;
import com.yls.app.entity.Sentence;
import com.yls.app.hanlp.MyNShortSegment;
import com.yls.app.hanlp.MyTextRankKeyword;
import com.yls.app.repository.AIKey;
import com.yls.app.repository.impl.RedisApiImpl;
import com.yls.app.util.Combination;
import com.yls.app.util.CosineSimilarity;
import com.yls.app.util.Permutation;

/**
 * 机器人
 * 
 * @author huangsy
 * @date 20182018年2月9日上午10:24:22
 */
@Component
public class Bot implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7491273635025129773L;

	private static Logger logger = Logger.getLogger(Bot.class);

	// private RedisUtils redisApiImpl = new RedisUtils("127.0.0.1", 6379,
	// "123456");

	@Resource
	private MyNShortSegment myNShortSegment;
	
	@Resource
	private MyTextRankKeyword myTextRankKeyword;
	
	@Resource
	private RedisApiImpl redisApiImpl;
	
	@Value("#{chatbot.turingUrl}")
	private String turingUrl;
	
	@Value("#{chatbot.turingApiKey}")
	private String turingApiKey;

	/**
	 * Bot回复处理
	 * 
	 * @param question 用户发过了的问题
	 * @param that 上一轮对话机器人的回复
	 * @param cid 终端id
	 * @param acid 账号id
	 * @param atype 上一轮对话命中的对话类型（单轮，多伦入口，多伦对话，其中一种）
	 * @return
	 */
	public Answer process3(String question, String that, String cid, String acid, String id_ap, String cid_m) {
		//返回的答案实体
		Answer answer = new Answer();
		//查询redis获取该cid对应有哪些对话类型（已经去除重复）
		List<String> clientDialogTypes = this.findClientDialogType(cid, cid_m);
		//模糊查询
		List<Answer> similarAnswers = this.searchSimilarAnswer(question, clientDialogTypes, id_ap);
		if(similarAnswers != null && similarAnswers.size()>0) {
			answer = this.randomAnswer(similarAnswers);
			return answer;
		}
		//关键分词结果集
		List<Term> keyWords2 = new ArrayList<Term>();
		//判读问题字数是否少于等于规定的字数限制
		boolean less15 = this.isLess15(question);
		//一句话字数少于等于规定的字数限制
		if (less15 || true) {
			//答案集合
			List<Answer> answers = new ArrayList<Answer>();
			//句式字符串集合
			List<String> sentenceStrs = new ArrayList<String>();
			//提取关键分词
			keyWords2 = this.cutWord3(question, acid);
			
			//合并相邻的时间词，合并相邻的地点词
			this.combineDynaWord(keyWords2);
			
			//查询redis获取该acid对应的动态词类型（已经去除重复）
			dynas = this.searchDynaByACID(acid);
			
			//用关键分词查找句式
			sentenceStrs = this.searchSentenceStrs(keyWords2, clientDialogTypes);
			//找不到句式，返回异常回复
			if(sentenceStrs.size() == 0) {
				logger.info("找不到句式，随机回复一句");
				answer = this.getTuringAnswer(question, cid);
				answer.setKeywords(keyWords2);//
				answer.setDynas(dynas);
				return answer;
			}
			//获取最优句式
			List<Sentence> niceSentences = this.getNiceSentenceStrs(sentenceStrs, keyWords2);
			//去除重复的nicesentence
			List<Sentence> distinctNiceSentence = this.getDistinctSentence(niceSentences);
			//找不到句式，返回异常回复
			if(distinctNiceSentence.size() == 0) {
				logger.info("找不到句式，随机回复一句");
				answer = this.getTuringAnswer(question, cid);
				answer.setKeywords(keyWords2);//
				answer.setDynas(dynas);
				return answer;
			}
			//降序排序句式集合
			List<Sentence> sortSentences = this.sortSentences(distinctNiceSentence);
			//移除句式关键词个数比问题关键词个数多的答案
			List<Sentence> smallSentences = this.removeBigSentence(sortSentences, keyWords2);
			//移除未知词刚好是变化词的句式
			List<Sentence> notSameSentences = this.removeSameSentences(smallSentences, keyWords2, question);
			//获取字数最长的句式
			List<Sentence> longestSentences = this.getLongestSentences(notSameSentences);
			//通过句式查找对话
			answers = this.searchAnswer(longestSentences, clientDialogTypes, id_ap);
			//如果查询不到，返回异常回复
			if (answers.size() == 0) {
				logger.info("找不到答案，随机回复一句");
				answer = this.getTuringAnswer(question, cid);
				answer.setKeywords(keyWords2);//
				answer.setDynas(dynas);
				return answer;
			}
			//获取that字段不为空，且和上一轮对话that相等的对话
			List<Answer> thatAnswers = this.getThatAnswers(answers, that);
			//thatAnswers不为空，说明存在多轮对话，随机回复一个
			if (thatAnswers.size() > 0) {
				answer = this.randomAnswer(thatAnswers);
				answer.setKeywords(keyWords2);//
				answer.setDynas(dynas);
				return answer;
			}
			//过滤that字段不为空的对话
			List<Answer> withoutThatAnswers = this.removeThatAnswer(answers);
			//过滤完，如果没有答案，返回异常回复
			if (withoutThatAnswers.size() == 0) {
				answer = this.getTuringAnswer(question, cid);
				answer.setKeywords(keyWords2);//
				answer.setDynas(dynas);
				return answer;
			}
			//获取不含变化词的答案集合
			List<Answer> specificAnswers = this.getSpecificAnswers(withoutThatAnswers);
			//如果specificAnswers不为空，则随机回复一句
			if (specificAnswers.size() > 0) {
				answer = this.randomAnswer(specificAnswers);
				answer.setKeywords(keyWords2);//
				answer.setDynas(dynas);
				return answer;
			}
			//获取不含未知词的答案集合
			List<Answer> withoutUnknowAnswers = getWithoutUnknowAnswer(withoutThatAnswers);
			if (withoutUnknowAnswers.size() > 0) {
				answer = this.randomAnswer(withoutUnknowAnswers);
				answer.setKeywords(keyWords2);//
				answer.setDynas(dynas);
				return answer;
			}
			//随机回复一句
			answer = this.randomAnswer(withoutThatAnswers);
			answer.setKeywords(keyWords2);//
			answer.setDynas(dynas);
		} else {
			logger.info("这句话字数：" + question.length() + "个，超过15个字");
			answer = this.getTuringAnswer(question, cid);
			answer.setKeywords(keyWords2);//
		}

		return answer;
	}
	
	/**
	 * 根据Term.offset从小到大排序
	 * @param keyTerms
	 */
	public void sortBubble(List<Term> keyTerms) {
        Term temp = null;
        // 第一层循环:表明比较的次数, 比如 length 个元素,比较次数为 length-1 次（肯定不需和自己比）
        for (int i = 0; i < keyTerms.size() - 1; i++) {
            for (int j = keyTerms.size() - 1; j > i; j--) {
                if (keyTerms.get(j).offset < keyTerms.get(j-1).offset) {
                    temp = keyTerms.get(j);
                    keyTerms.set(j, keyTerms.get(j-1));
                    keyTerms.set(j-1, temp);
                }
            }
        }
    }
	
	/**
	 * 查询redis获取该账户定义的动态词类型
	 * @param acid
	 * @return
	 */
	private List<String> searchDynaByACID(String acid) {
		long startTime = System.currentTimeMillis();
		List<String> result = new ArrayList<String>();
		String key = AIKey.DYNA + ":" + acid;
		Set<String> dynaSet = redisApiImpl.zrange(key, 0, -1);
		if (dynaSet != null && dynaSet.size() > 0) {
			for (String json : dynaSet) {
				Dyna dyna = new Gson().fromJson(json, Dyna.class);
				String type = dyna.getType();
				if(type!=null && !"".equals(type) && !type.equals(AIKey.DEFAUT_DYNA)) {
					//去除重复
					if(!result.contains(type)) {
						result.add(type);
					}
				}
			}
		}
		logger.info("searchDynaByACID获取动态词类型，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 移除未知词刚好是变化词的句式
	 * @param sentences
	 * @param keyTerms
	 * @param question
	 * @return
	 */
	private List<Sentence> removeSameSentences(List<Sentence> sentences, List<Term> keyTerms, String question) {
		long startTime = System.currentTimeMillis();
		List<Sentence> result = new ArrayList<Sentence>();
		//分词集转词性集
//		List<String> natures = this.term2Nature(keyTerms);
		Map<String, Integer> naturesMap = this.term2NatureMap(keyTerms);
		//分词集转词集
		List<String> words = this.term2Word(keyTerms);
		for(Sentence sentence : sentences) {
			//如果句式包含未知词就要处理
			if(sentence.getSentence().contains(AIKey.UNKNOW)) {
				String pattern = sentence.getSentence();
				List<String> keywords = this.pattern2Keywords(pattern);
				int index = keywords.indexOf(AIKey.UNKNOW);
				String wz = "";
				if(index == 0) {
					int right = index + 1;
					int right2 = 0;
					String rightString = keywords.get(right);
					if(this.isDyna(rightString)) {
						right2 = naturesMap.get(rightString);
						rightString = keyTerms.get(right2).word;
					} else {
						right2 = words.indexOf(rightString);
					}
					if(right2 != 0) {
						wz = question.substring(0, question.indexOf(rightString));
						List<Term> subTerms = this.getWZTerm(0, right2, keyTerms);
						if(!this.isWZDyna(subTerms)) {
							result.add(sentence);
						}
					}
				} else if(index == keywords.size()-1) {
					int left = index - 1;
					int left2 = 0;
					String leftString = keywords.get(left);
					if(this.isDyna(leftString)) {
						left2 = naturesMap.get(leftString);
						leftString = keyTerms.get(left2).word;
					} else {
						left2 = words.indexOf(leftString);
					}
					if(left2 != (keyTerms.size()-1)) {
						wz = question.substring(question.indexOf(left2)+leftString.length());
						List<Term> subTerms = this.getWZTerm(left2+1, keyTerms.size()-1, keyTerms);
						if(!this.isWZDyna(subTerms)) {
							result.add(sentence);
						}
					}
				} else {
					int left = index - 1;
					int right = index + 1;
					int left2 = 0;
					int right2 = 0;
					String leftString = keywords.get(left);
					String rightString = keywords.get(right);
					if(this.isDyna(rightString)) {
						right2 = naturesMap.get(rightString);
						rightString = keyTerms.get(right2).word;
					} else {
						right2 = words.indexOf(rightString);
					}
					if(this.isDyna(leftString)) {
						left2 = naturesMap.get(leftString);
						leftString = keyTerms.get(left2).word;
					} else {
						left2 = words.indexOf(leftString);
					}
					if(left2 < right2 && (right2-left2)>1) {
						wz = question.substring(question.indexOf(leftString)+leftString.length(), question.indexOf(rightString));
						List<Term> subTerms = this.getWZTerm(left2+1, right2, keyTerms);
						if(!this.isWZDyna(subTerms)) {
							result.add(sentence);
						}
					}
				}
			} else {
				//不包含未知词的句式直接加入
				result.add(sentence);
			}
		}
		logger.info("removeSameSentences移除未知词刚好是变化词的句式，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 获取未知词分词集
	 * @param left
	 * @param right
	 * @param keyTerms
	 * @return
	 */
	private List<Term> getWZTerm(int left, int right, List<Term> keyTerms) {
		
		List<Term> result = new ArrayList<Term>();
		
		result = keyTerms.subList(left, right);
		
		return result;
		
	}
	
	/**
	 * 判断未知词集合是否为动态词
	 * @param wz
	 * @param natures
	 * @param words
	 * @return
	 */
	private boolean isWZDyna(List<Term> subTerms) {
		boolean flag = false;
		for(Term t : subTerms) {
			if(t.type!=null) {
				for(String type : t.type) {
					if(this.isDyna(type)) {
						flag = flag || true;
					}
				}
			}
		}
		return flag;
	}
	
	/**
	 * 移除句式关键词个数比问题关键词个数多的答案
	 * @param answers
	 * @param keywords2
	 * @return
	 */
	private List<Sentence> removeBigSentence(List<Sentence> sentences, List<Term> keywords2) {
		long startTime = System.currentTimeMillis();
		List<Sentence> result = new ArrayList<Sentence>();
		for(Sentence sentence : sentences) {
			String pattern = sentence.getSentence();
			List<String> keywords = pattern2Keywords(pattern);
			if(keywords.size() <= keywords2.size()) {
				result.add(sentence);
			}
		}
		logger.info("removeBigSentence移除句式关键词个数比问题关键词个数多的句式，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 获取未知词部分
	 * @param answer
	 * @return
	 */
	public String getUnknowString(Sentence sentence, List<Term> terms, String question) {
		
		String wz = "";
		String pattern = sentence.getSentence();
		List<String> keywords = this.pattern2Keywords(pattern);
		List<String> natures = this.term2Nature(terms);
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
				String temp = question.substring(0, right2);
				if("".equals(temp)) {
					return wz;
				}
				int tempIndex = words.indexOf(temp);
				if(tempIndex < 0) {
					wz = temp;
					return wz;
				}
				String tempNature = natures.get(tempIndex);
				int kIndex = keywords.indexOf(temp);
				if(kIndex < 0) {
					kIndex = keywords.indexOf(tempNature);
				}
				if(words.indexOf(temp) == kIndex) {
					wz = temp;
				}
			} else if(index == (keywords.size()-1)) {
				int left = index - 1;
				String leftString = keywords.get(left);
				if(this.isDyna(leftString)) {
					int i = natures.indexOf(leftString);
					leftString = terms.get(i).word;
				}
				int left2 = question.indexOf(leftString) + leftString.length();
				String temp = question.substring(left2);
				if("".equals(temp)) {
					return wz;
				}
				int tempIndex = words.indexOf(temp);
				if(tempIndex < 0) {
					wz = temp;
					return wz;
				}
				String tempNature = natures.get(tempIndex);
				int kIndex = keywords.indexOf(temp);
				if(kIndex < 0) {
					kIndex = keywords.indexOf(tempNature);
				}
				if(words.indexOf(temp) == kIndex) {
					wz = temp;
				}
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
	 * 判读关键词是否为未知词
	 * @param word
	 * @return
	 */
	private boolean isWZ(String word) {
		boolean flag = false;
		if(word==null || "".equals(word)) {
			flag = false;
		} else if(word.equals(AIKey.UNKNOW)) {
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
	 * 模胡匹配
	 * @param question
	 * @param clientDialogTypes
	 * @return
	 */
	private List<Answer> searchSimilarAnswer(String question, List<String> clientDialogTypes, String id_ap) {
		List<Answer> result = new ArrayList<Answer>();
		List<Answer> moreFirst = this.searchSimilarMoreFirst(question, clientDialogTypes);
		if(moreFirst!=null && moreFirst.size()>0) {
			result = moreFirst;
			return result;
		}
		if(id_ap!=null && !"".equals(id_ap)) {
			List<Answer> moreNext = this.searchSimilarMoreNext(question, clientDialogTypes, id_ap);
			if(moreNext!=null && moreNext.size()>0) {
				result = moreNext;
				return result;
			}
		}
		List<Answer> one = this.searchSimilarOne(question, clientDialogTypes);
		if(one!=null && one.size()>0) {
			result = one;
			return result;
		}
		return result;
	}
	
	/**
	 * 多伦入口主题模胡匹配
	 * @param question
	 * @param dialogType
	 * @return
	 */
	private List<Answer> searchSimilarMoreFirst(String question, List<String> clientDialogTypes) {
		long startTime = System.currentTimeMillis();
		List<Answer> result = new ArrayList<Answer>();
		double max = 0;
		String maxPattern = "";
		String maxDialogType = "";
		//如果question不是空，就要模糊匹配
		if(question!=null && !"".equals(question)) {
			//1.多伦对话，入口主题
			for (String dialogType : clientDialogTypes) {
				String key = AIKey.AWORD_MOREFIRST + ":" + dialogType + ":*"; 
				Set<String> keySet = redisApiImpl.keys(key);
				for(String elem : keySet) {
					String pattern = elem.substring(key.length()-1);
					double temp = CosineSimilarity.getSimilarity(question, pattern);
					if(temp >= max) {
						max = temp;
						maxPattern = pattern;
						maxDialogType = dialogType;
					}
				}
			}
			if(max >= AIKey.SIMILARITY) {
				String key = AIKey.AWORD_MOREFIRST + ":" + maxDialogType + ":" + maxPattern;
				Set<String> answerSet = redisApiImpl.zrange(key, 0, -1);
				for(String json : answerSet) {
					Answer answer = new Gson().fromJson(json, Answer.class);
					result.add(answer);
				}
				logger.info("模糊查询-多伦入口主题，命中pattern：" + maxPattern + "，相识度为：" + max);
			}
		}
		logger.info("模糊查询-多伦入口主题，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 多伦对话主题模胡匹配
	 * @param question
	 * @param dialogType
	 * @return
	 */
	private List<Answer> searchSimilarMoreNext(String question, List<String> clientDialogTypes, String id_ap) {
		long startTime = System.currentTimeMillis();
		List<Answer> result = new ArrayList<Answer>();
		double max = 0;
		String maxPattern = "";
		String maxDialogType = "";
		//如果question不是空，就要模糊匹配
		if(question!=null && !"".equals(question)) {
			//2.多伦对话，对话主题
			for (String dialogType : clientDialogTypes) {
				String key = AIKey.AWORD_MORENEXT + ":" + dialogType + ":" + id_ap + ":*"; 
				Set<String> keySet = redisApiImpl.keys(key);
				for(String elem : keySet) {
					String pattern = elem.substring(key.length()-1);
					double temp = CosineSimilarity.getSimilarity(question, pattern);
					if(temp >= max) {
						max = temp;
						maxPattern = pattern;
						maxDialogType = dialogType;
					}
				}
			}
			if(max >= AIKey.SIMILARITY) {
				String key = AIKey.AWORD_MORENEXT + ":" + maxDialogType + ":" + id_ap + ":" + maxPattern;
				Set<String> answerSet = redisApiImpl.zrange(key, 0, -1);
				for(String json : answerSet) {
					Answer answer = new Gson().fromJson(json, Answer.class);
					result.add(answer);
				}
				logger.info("模糊查询-多伦对话主题，命中pattern：" + maxPattern + "，相识度为：" + max);
			}
			//3.单轮对话
		}
		logger.info("模糊查询-多伦对话主题，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 单伦对话模胡匹配
	 * @param question
	 * @param dialogType
	 * @return
	 */
	private List<Answer> searchSimilarOne(String question, List<String> clientDialogTypes) {
		long startTime = System.currentTimeMillis();
		List<Answer> result = new ArrayList<Answer>();
		double max = 0;
		String maxPattern = "";
		String maxDialogType = "";
		//如果question不是空，就要模糊匹配
		if(question!=null && !"".equals(question)) {
			//3.单轮对话
			for (String dialogType : clientDialogTypes) {
				String key = AIKey.AWORD_ONE + ":" + dialogType + ":*"; 
				Set<String> keySet = redisApiImpl.keys(key);
				for(String elem : keySet) {
					String pattern = elem.substring(key.length()-1);
					double temp = CosineSimilarity.getSimilarity(question, pattern);
					if(temp >= max) {
						max = temp;
						maxPattern = pattern;
						maxDialogType = dialogType;
					}
				}
			}
			if(max >= AIKey.SIMILARITY) {
				String key = AIKey.AWORD_ONE + ":" + maxDialogType + ":" + maxPattern;
				Set<String> answerSet = redisApiImpl.zrange(key, 0, -1);
				for(String json : answerSet) {
					Answer answer = new Gson().fromJson(json, Answer.class);
					result.add(answer);
				}
				logger.info("模糊查询-单轮对话，命中pattern：" + maxPattern + "，相识度为：" + max);
			}
		}
		logger.info("模糊查询-单轮对话，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 获取不含未知词的答案集合
	 * @param answers
	 * @return
	 */
	private List<Answer> getWithoutUnknowAnswer(List<Answer> answers) {
		
		List<Answer> result = new ArrayList<Answer>();
		for(Answer answer : answers) {
			if(!answer.getPattern().contains(AIKey.UNKNOW)) {
				result.add(answer);
			}
		}
		return result;
		
	}
	
	/**
	 * 去除重复的sentence
	 * @param sentences
	 * @return
	 */
	private List<Sentence> getDistinctSentence(List<Sentence> sentences) {
		long startTime = System.currentTimeMillis();
//		List<Sentence> result = new ArrayList<Sentence>();
//		for(Sentence sentence : sentences) {
//			if(!result.contains(sentence)) {
//				result.add(sentence);
//			}
//		}
//		logger.info("去除重复sentence，sentences个数：" + result.size() + "，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
//		startTime = System.currentTimeMillis();
		List<Sentence> result2 = new ArrayList<Sentence>();
		LinkedHashSet<Sentence> set = new LinkedHashSet<Sentence>(sentences.size());
	    set.addAll(sentences);
	    result2.addAll(set);
		logger.info("getDistinctSentence去除重复句式，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		logger.info("以下为distinctNiceSentence：");
		for(Sentence s : result2) {
			
			logger.info(s.getSentence());
		}
		return result2;
	}
	
	/**
	 * 通过句式查找answer
	 * @param sentences
	 * @param userid
	 * @return
	 */
	private List<Answer> searchRedisBySentence(List<Sentence> sentences, List<String> clientDialogTypes) {
		long startTime = System.currentTimeMillis();
		List<Answer> answers = new ArrayList<Answer>();
		for (String dialogType : clientDialogTypes) {
			for (Sentence sentence : sentences) {
				Set<String> answerSet = redisApiImpl.zrange(AIKey.PERSONAL_ANSWER + ":" + dialogType + ":" + sentence.getSentence(), 0, -1);
				if (answerSet != null && answerSet.size() > 0) {
					for (String json : answerSet) {
						Answer answer = new Gson().fromJson(json, Answer.class);
						answers.add(answer);
					}
				}
			}
		}
		logger.info("searchRedisBySentence通过句式查找对话，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return answers;
	}
	
	/**
	 * 关键词匹配
	 * @param sentences
	 * @param clientDialogTypes
	 * @return
	 */
	private List<Answer> searchAnswer(List<Sentence> sentences, List<String> clientDialogTypes, String id_ap) {
		List<Answer> result = new ArrayList<Answer>();
		List<Answer> answerMoreFirst = this.searchAnswerMoreFirst(sentences, clientDialogTypes);
		if(answerMoreFirst!=null && answerMoreFirst.size()>0) {
			result = answerMoreFirst;
			return result;
		}
		//上一轮对话id_ap不为空
		if(id_ap!=null && !"".equals(id_ap)) {
			List<Answer> answerMoreNext = this.searchAnswerMoreNext(sentences, clientDialogTypes, id_ap);
			if(answerMoreNext!=null && answerMoreNext.size()>0) {
				result = answerMoreNext;
				return result;
			}
		}
		List<Answer> answerOne = this.searchAnswerOne(sentences, clientDialogTypes);
		if(answerOne!=null && answerOne.size()>0) {
			result = answerOne;
			return result;
		}
		return result;
	}
	
	/**
	 * 关键词匹配-多伦入口主题
	 * @param sentences
	 * @param userid
	 * @return
	 */
	private List<Answer> searchAnswerMoreFirst(List<Sentence> sentences, List<String> clientDialogTypes) {
		long startTime = System.currentTimeMillis();
		List<Answer> answers = new ArrayList<Answer>();
		for (String dialogType : clientDialogTypes) {
			for (Sentence sentence : sentences) {
				Set<String> answerSet = redisApiImpl.zrange(AIKey.DIALOG_MOREFIRST + ":" + dialogType + ":" + sentence.getSentence(), 0, -1);
				if (answerSet != null && answerSet.size() > 0) {
					for (String json : answerSet) {
						Answer answer = new Gson().fromJson(json, Answer.class);
						answers.add(answer);
					}
				}
			}
		}
		logger.info("关键词匹配-多伦入口主题，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return answers;
	}
	
	/**
	 * 关键词匹配-多伦对话主题
	 * @param sentences
	 * @param userid
	 * @return
	 */
	private List<Answer> searchAnswerMoreNext(List<Sentence> sentences, List<String> clientDialogTypes, String id_ap) {
		long startTime = System.currentTimeMillis();
		List<Answer> answers = new ArrayList<Answer>();
		for (String dialogType : clientDialogTypes) {
			for (Sentence sentence : sentences) {
				Set<String> answerSet = redisApiImpl.zrange(AIKey.DIALOG_MORENEXT + ":" + dialogType + ":" + id_ap + 
						":" + sentence.getSentence(), 0, -1);
				if (answerSet != null && answerSet.size() > 0) {
					for (String json : answerSet) {
						Answer answer = new Gson().fromJson(json, Answer.class);
						answers.add(answer);
					}
				}
			}
		}
		logger.info("关键词匹配-多伦对话主题，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return answers;
	}
	
	/**
	 * 关键词匹配-单轮对话
	 * @param sentences
	 * @param userid
	 * @return
	 */
	private List<Answer> searchAnswerOne(List<Sentence> sentences, List<String> clientDialogTypes) {
		long startTime = System.currentTimeMillis();
		List<Answer> answers = new ArrayList<Answer>();
		for (String dialogType : clientDialogTypes) {
			for (Sentence sentence : sentences) {
				Set<String> answerSet = redisApiImpl.zrange(AIKey.DIALOG_ONE + ":" + dialogType + ":" + sentence.getSentence(), 0, -1);
				if (answerSet != null && answerSet.size() > 0) {
					for (String json : answerSet) {
						Answer answer = new Gson().fromJson(json, Answer.class);
						answers.add(answer);
					}
				}
			}
		}
		logger.info("关键词匹配-单轮对话，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return answers;
	}
	
	/**
	 * 获取最长的句式集合
	 * @param sentences
	 * @return
	 */
	private List<Sentence> getLongestSentences(List<Sentence> sentences) {
		long startTime = System.currentTimeMillis();
		List<Sentence> longestList = new ArrayList<Sentence>();
		if (sentences.size() > 0) {
			//经过降序排序，第一个sentence的lenght最长
			double longest = sentences.get(0).getLength();
			for (Sentence sentence : sentences) {
				if (sentence.getLength() >= longest) {
					longestList.add(sentence);
				}
			}
		}
		logger.info("getLongestSentences获取最长的句式集合，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		logger.info("以下为longestSentence：");
		for(Sentence s : longestList) {
			
			logger.info(s.getSentence());
		}
		return longestList;
	}
	
	/**
	 * 降序排序句式集合
	 * @param sentences
	 * @return
	 */
	private List<Sentence> sortSentences(List<Sentence> sentences) {
		long startTime = System.currentTimeMillis();
		List<Sentence> afters = new ArrayList<Sentence>();
		for(Sentence sentence : sentences) {
			double len = 0;
			String[] words = sentence.getSentence().split("\\(@\\)");
			for(String word : words) {
				if(this.isDyna(word)) {
					len = len + AIKey.DYNA_WEIGHT;
				} else if(this.isWZ(word)) {
					len = len + AIKey.WZ_WEIGHT;
				} else {
					len = len + AIKey.WORD_WEIGHT;
				}
			}
			sentence.setLength(len);
			afters.add(sentence);
		}
		//排序
		Collections.sort(afters);
		logger.info("sortSentences降序排序句式集合，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return afters;
	}

	/**
	 * @param sentences
	 * @param question
	 * @return
	 */
	private String bestSentence2(List<String> sentences, String question) {
		
		double max = 0.0;
		String best = "";
		
		for(String sentence : sentences) {
			String aftersentence = sentence.replaceAll("\\(@\\)", "");
			double sm = CosineSimilarity.getSimilarity(aftersentence, question);
			if(sm >= max) {
				max = sm;
				best = sentence;
			}
		}
		
		return best;
	}
	
	/**
	 * 筛选符合的句式
	 * @param sentences
	 * @param terms
	 * @return
	 */
	private List<Sentence> getNiceSentences(List<Sentence> sentences, List<Term> terms, String question) {
		
		List<Sentence> result = new ArrayList<Sentence>();
		for(Sentence sentence : sentences) {
			//句式转关键分词
			List<String> keywords = new ArrayList<String>(sentence2Keywords(sentence));
//			if(keywords.contains(AIKey.UNKNOW)) {
//				String wz = this.getUnknowString(sentence, terms, question);
//				if(!"".equals(wz) && wz != null) {
//					result.add(sentence);
//				}
//			} else {
				//句式含有未知词的处理
				if(keywords.contains(AIKey.UNKNOW)) {
					int index = keywords.indexOf(AIKey.UNKNOW);
					//移除未知词
					keywords.remove(index);
				}
				//对比句式和关键分词，查找最优句式
				boolean isbreak = false;
				for(String kw : keywords) {
					boolean flag = false;
					for(Term t : terms) {
						if(/*this.isSimilarity(t.word, kw)*/t.trueWord.equals(kw) || 
								t.nature.toString().equals(kw.replace("(&", "").replace("&)", ""))) {
							flag = flag || true;
						} else {
							flag = flag || false;
						}
					}
					if(!flag) {
						isbreak = true;
						break;
					}
				}
				if(!isbreak) {
					result.add(sentence);
				}
//			}
		}
		return result;
	}
	
	/**
	 * 筛选符合的句式（多线程方式）
	 * @param sentences
	 * @param terms
	 * @return
	 */
	private List<Sentence> getNiceSentenceStrs(List<String> sentences, List<Term> terms) {
		long startTime = System.currentTimeMillis();
		
		//最大并发数4
		ForkJoinPool fjp = new ForkJoinPool(4);
        ForkJoinTask<List<Sentence>> task = new GetNiceSentenceStrTask(sentences, 0, sentences.size(), terms);
        List<Sentence> result = fjp.invoke(task);
        
        logger.info("getNiceSentenceStrs获取最优句式，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 判断两个词语是否相似
	 * @param doc1
	 * @param doc2
	 * @return
	 */
	private boolean isSimilarity(String doc1, String doc2) {
		return CosineSimilarity.getSimilarity(doc1, doc2) >= AIKey.SIMILARITY;
	}
	
	/**
	 * 句式转关键词
	 * @param sentence
	 * @return
	 */
	private List<String> sentence2Keywords(Sentence sentence) {
		List<String> keywords = new ArrayList<String>();
		
		String[] sub = sentence.getSentence().split("\\(@\\)");
		keywords = Arrays.asList(sub);
		
		return keywords;
	}
	
	/**
	 * 查找句式
	 * @param terms
	 * @param dialogType
	 * @return
	 */
	private List<Sentence> searchSentences(List<Term> terms, String dialogType) {
		
		List<Sentence> sentences = new ArrayList<Sentence>();
		for (Term term : terms) {
			if(this.isDyna("(&" + term.nature + "&)")) {
				String key = "(&" + term.nature + "&)";
				Set<String> answerSet = redisApiImpl.zrange(AIKey.SENTENCE + ":" + dialogType + ":" + key, 0, -1);
				if (answerSet != null && answerSet.size() > 0) {
					for (String json : answerSet) {
						Sentence sentence = new Gson().fromJson(json, Sentence.class);
						//去除重复
//						if(!sentences.contains(sentence)) {
							sentences.add(sentence);
//						}
					}
				}
			} else {
				Set<String> answerSet = redisApiImpl.zrange(AIKey.SENTENCE + ":" + dialogType + ":" + term.word, 0, -1);
				if (answerSet != null && answerSet.size() > 0) {
					for (String json : answerSet) {
						Sentence sentence = new Gson().fromJson(json, Sentence.class);
						//去除重复
//						if(!sentences.contains(sentence)) {
							sentences.add(sentence);
//						}
					}
				}
			}
		}
		return sentences;
	}
	
	/**
	 * 查找句式
	 * @param terms
	 * @param dialogType
	 * @return
	 */
	private List<String> searchSentenceStrs(List<Term> terms, List<String> clientDialogTypes) {
		long startTime = System.currentTimeMillis();
		List<String> sentences = new ArrayList<String>();
		for(String dialogType : clientDialogTypes) {
			for (Term term : terms) {
				if(term.type!=null) {
					for(String type : term.type) {
						if(this.isDyna(type)) {
							String key = type;
							Set<String> answerSet = redisApiImpl.zrange(AIKey.SENTENCE + ":" + dialogType + ":" + key, 0, -1);
							if (answerSet != null && answerSet.size() > 0) {
								sentences.addAll(answerSet);
							}
						}
					}
				}
				Set<String> answerSet = redisApiImpl.zrange(AIKey.SENTENCE + ":" + dialogType + ":" + term.trueWord, 0, -1);
				if (answerSet != null && answerSet.size() > 0) {
					sentences.addAll(answerSet);
				}
			}
		}
		logger.info("searchSentenceStrs用关键分词查找句式，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return sentences;
	}
	
	/**
	 * 对比分词集和关键词集，获取关键词集的词性
	 * @param keywords
	 * @param terms
	 * @return
	 */
	public List<Term> keyword2Term(List<String> keywords, List<Term> terms) {
		long startTime = System.currentTimeMillis();
		List<Term> result = new ArrayList<Term>();
		for (Term t : terms) {
			for (String kw : keywords) {
				if (kw.equals(t.word)) {
					result.add(t);
				}
			}
		}
		// 如果没有相同的部分就以keywords为主
//		if (result.size() == 0) {
//			for (String keyword : keywords) {
//				Term t = new Term(keyword, Nature.n);
//				result.add(t);
//			}
//		}
		if(result.size() < keywords.size()) {
			for(String kw : keywords) {
				boolean flag = false;
				for(Term t : result) {
					flag = flag || kw.equals(t.word);
				}
				if(!flag) {
					Term term = new Term(kw, Nature.n);
					result.add(term);
				}
			}
		}
		logger.info("keyword2Term关键词转分词结果：" + result + "，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 查找终端对应有哪些对话类型
	 * @param cid
	 * @return
	 */
	private List<String> findClientDialogType(String cid, String cid_m) {
		long startTime = System.currentTimeMillis();
		List<String> result = new ArrayList<String>();
		String key = AIKey.CLIENT_DIALOGTYPE + ":" + cid + ":" + cid_m;
		Set<String> dialogTypes = redisApiImpl.zrange(key, 0, -1);
		for (String dt : dialogTypes) {
			Map json = new Gson().fromJson(dt, Map.class);
			String dialogType = (String) json.get("dialogType");
			if(!result.contains(dialogType)) {
				result.add(dialogType);
			}
		}
		logger.info("findClientDialogType查找终端有哪些对话类型，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}

	/**
	 * 获取不含变化词的答案
	 * @param answers
	 * @return
	 */
	private List<Answer> getSpecificAnswers(List<Answer> answers) {
		List<Answer> specificAnswers = new ArrayList<Answer>();
		for (Answer answer : answers) {
			//如果pattern不包含动态词或者未知词就加入
			if (!isPatternDyna(answer.getPattern())) {
				specificAnswers.add(answer);
			}
		}
		return specificAnswers;
	}
	
	/**
	 * 判读pattern是否包含动态词或者未知词
	 * @param pattern
	 * @return
	 */
	private boolean isPatternDyna(String pattern) {
		boolean flag = false;
		for(String dyna : this.dynas) {
			if(pattern.contains(dyna)) {
				flag = flag || true;
			} else if(pattern.contains(AIKey.UNKNOW)) {
				flag = flag || true;
			} else {
				flag = flag || false;
			}
		}
		return flag;
	}

	/**
	 * 递归组合关键词成pattern
	 * 
	 * @param terms
	 *            分词列表
	 * @param i
	 *            初始值为0
	 * @param result
	 *            结果pattern字符串集合
	 * @param left
	 *            中间字符串
	 */
	private void termTree(List<Term> terms, int i, List<String> result, String left) {
		if (i == terms.size()) {
			result.add(left);
			return;
		} else {
			if (terms.get(i).nature == Nature.t || terms.get(i).nature == Nature.ns) {
				String left1 = left + "(&" + terms.get(i).nature + "&)" + "(@)";
				termTree(terms, i + 1, result, left1);
				String left2 = left + terms.get(i).word + "(@)";
				termTree(terms, i + 1, result, left2);
			} else {
				left = left + terms.get(i).word + "(@)";
				termTree(terms, i + 1, result, left);
			}
		}
	}

	/**
	 * 处理分词集，组合成pattern集合
	 * 
	 * @param terms
	 * @return
	 */
	private List<String> termProcess(List<Term> terms) {
		List<String> testResult = new ArrayList<String>();
		this.termTree(terms, 0, testResult, "");
		logger.info("开始处理分词集，分词总共：" + testResult.size() + "个");
		logger.info("分词组合：" + testResult);

		// List<String> result = new ArrayList<String>();
		// String em = "";
		// String em2 = "";
		//
		// for(int i=0;i<terms.size();i++) {
		// Term t = terms.get(i);
		// em = em + t.word + "(@)";
		// }
		// result.add(em);
		//
		// for(int i=0;i<terms.size();i++) {
		// Term t = terms.get(i);
		// if(t.nature == Nature.t || t.nature == Nature.ns) {
		// em2 = em2 + "(&" + t.nature + "&)" + "(@)";
		// } else {
		// em2 =em2 + t.word + "(@)";
		// }
		// }
		// result.add(em2);
		// return result;

		return testResult;

	}

	/**
	 * 获取不包含that字段的答案
	 * 
	 * @param answers
	 * @return
	 */
	private List<Answer> removeThatAnswer(List<Answer> answers) {
		List<Answer> withoutThatAnswers = new ArrayList<Answer>();
		for (Answer answer : answers) {
			if ("".equals(answer.getThat())) {
				withoutThatAnswers.add(answer);
			}
		}
		return withoutThatAnswers;
	}

	/**
	 * 找不到答案是回复
	 * @param cid 机器人id
	 * @return
	 */
	private Answer getDefautAnswer(String cid) {
		List<Answer> anList = new ArrayList<Answer>();
		Answer answer = new Answer(AIKey.EXP_ANSWER, AIKey.DEFAULT_EXP_ANSWER, "0", "");
		String key = AIKey.EXP_ANSWER + ":" + cid + ":" + "1";// 默认取类型1。异常类型，类型：1-无答案时，2-接口异常时，3-系统出错时
		Set<String> set = redisApiImpl.zrange(key, 0, -1);
		for (String s : set) {
			Answer answer2 = new Gson().fromJson(s, Answer.class);
			anList.add(answer2);
		}
		if (anList.size() > 0) {
			answer = this.randomAnswer(anList);
		}
		Map<String, Object> script = new HashMap<String, Object>();
		answer.setScript(script);
		return answer;
	}

	/**
	 * 找不到答案是调用图灵接口
	 * @param question
	 * @param cid 机器人id
	 * @return
	 */
	private Answer getTuringAnswer(String question, String cid) {
		
		logger.info("系统无答案，从图灵api接口获取答案");
		
		Answer answer = new Answer();
		
		String json = "{'reqType':'0','perception':{'inputText':{'text':'"+question+"'}},'userInfo':{'apiKey':'"+turingApiKey+"','userId':'"+cid+"'}}";
		
		String resultJson = this.doPost(turingUrl, json);
		
		logger.info("图灵返回json：" + resultJson);
		
		JSONObject jo = new JSONObject(resultJson);
		JSONArray ja = jo.getJSONArray("results");
		String result = ja.getJSONObject(0).getJSONObject("values").getString("text");
		int code = jo.getJSONObject("intent").getInt("code");
		
		//图灵接口也找不到答案
		if(code==5000) {
			answer = this.getDefautAnswer(cid);
			
			Map<String, Object> script = new HashMap<String, Object>();
			answer.setScript(script);
			answer.setCode("");
		} else {
			answer.setType("0");
			answer.setThat("");
			answer.setTemplate(result);
			answer.setPattern(AIKey.TURING_ANSWER);
			
			Map<String, Object> script = new HashMap<String, Object>();
			answer.setScript(script);
			answer.setCode(code+"");
		}
		
		return answer;

	}

	private String doPost(String url, String json) {
		String result = "";

		// 创建HttpClient对象
		CloseableHttpClient closeHttpClient = HttpClients.createDefault();
		CloseableHttpResponse httpResponse = null;
		// 发送Post请求
		HttpPost httpPost = new HttpPost(url);
		try {
			// 转换参数并设置编码格式
			httpPost.setEntity(new StringEntity(json, Charset.forName("UTF-8")));
			// 执行Post请求 得到Response对象
			httpResponse = closeHttpClient.execute(httpPost);
			// 返回对象 向上造型
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity != null) {
				result = EntityUtils.toString(httpEntity, "utf-8");
			}
		} catch (Exception e) {
			logger.info("获取图灵接口数据出错");
		} finally {
			if (httpResponse != null) {
				try {
					httpResponse.close();
				} catch (IOException e) {
					logger.info("关闭response出错");
				}
			}
			if (closeHttpClient != null) {
				try {
					closeHttpClient.close();
				} catch (IOException e) {
					logger.info("关闭httpclient出错");
				}
			}
		}

		return result;
	}

	/**
	 * 获取答案集pattern字段最长的Answer，组成子集返回
	 * 
	 * @param answers
	 * @return
	 */
	private List<Answer> getLongestAnswers(List<Answer> answers) {
		List<Answer> longestList = new ArrayList<Answer>();
		if (answers.size() > 0) {
			int longest = answers.get(0).getPattern().length();// 经过降序排序，第一个的pattern字段长度最长
			for (Answer answer : answers) {
				if (answer.getPattern().length() >= longest) {
					longestList.add(answer);
				}
			}
		}
		return longestList;
	}

	/**
	 * 从答案集随机获取一个Answer返回
	 * 
	 * @param longestList
	 * @return
	 */
	private Answer randomAnswer(List<Answer> longestList) {

		Answer answer = new Answer();
		if (longestList.size() > 0) {
			Random random = new Random();
			int ran = Math.abs(random.nextInt()) % longestList.size();
			answer = longestList.get(ran);
		}
		return answer;

	}

	/**
	 * 获取答案集中that字段和传入that相同的答案子集
	 * 
	 * @param answers
	 * @param that
	 * @return
	 */
	private List<Answer> getThatAnswers(List<Answer> answers, String that) {
		List<Answer> thatAnswers = new ArrayList<Answer>();
		if (that != null && !"".equals(that)) {
			for (Answer answer : answers) {
				if (that.equals(answer.getThat())) {
					thatAnswers.add(answer);
				}
			}
		}
		return thatAnswers;
	}

	/**
	 * 排列组合关键词，返回关键词集合
	 * 
	 * @param keyWords
	 * @return
	 */
	private List<String> permuteCombine(List<String> keyWords) {
		List<String> pcKeyWords = new ArrayList<String>();

		Combination com = new Combination();
		com.combination(keyWords);
		List<String> comKeyWords = com.getResult();
		for (String comKeyWord : comKeyWords) {
			String comKeyWord2 = comKeyWord.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(" ", "");
			List<String> comKeyWordList = Arrays.asList(comKeyWord2.split(","));

			Permutation per = new Permutation();
			per.permutation(comKeyWordList);
			List<String> perComKeyWordList = per.getResult();
			for (String perComKeyWord : perComKeyWordList) {
				pcKeyWords.add(perComKeyWord);
			}
		}

		return pcKeyWords;
	}

	/**
	 * 提取关键词
	 * @param question
	 * @return
	 */
	public List<String> cutWord(String question, String acid) {
		long startTime = System.currentTimeMillis();
		List<String> result = new ArrayList<String>();
		// result = MyIKAnalyzer.getTermList(question);
//		StandardTokenizer.SEGMENT = new MyNShortSegment();
//		StandardTokenizer.SEGMENT.enableCustomDictionaryForcing(true);
		result = myTextRankKeyword.getKeywordList(question, 5, acid);// 更好的nlp开源工具包，默认提取5个关键词
//		if (result.size() == 0) {// 如果提取不到关键词就总结这句话
//			result = HanLP.extractSummary(question, 5);
//		}
		logger.info("cutWord提取关键词，结果：" + result + "，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}
	
	/**
	 * 提取关键分词
	 * @param question
	 * @return
	 */
	public List<Term> cutWord3(String question, String acid) {
		long startTime = System.currentTimeMillis();
		List<Term> result = new ArrayList<Term>();
		myNShortSegment.enableNameRecognize(false).enableCustomDictionaryForcing(true).enableOffset(true);
		result = myNShortSegment.seg(question, AIKey.KEYTERM_SIZE, acid);
		this.sortBubble(result);
		logger.info("关键词结果：" + result + "，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}

	/**
	 * 递归合并相邻的时间词
	 * @param terms
	 * @param i
	 */
	public void combineTime(List<Term> terms, int i) {

		if (terms.size() == i + 1) {
			return;
		} else {
			if (terms.get(i).nature == Nature.t && terms.get(i + 1).nature == Nature.t) {
				Term term = new Term(terms.get(i).word + terms.get(i + 1).word, 
						Nature.t, terms.get(i).frequency + terms.get(i + 1).frequency, terms.get(i).type,
						terms.get(i).offset, terms.get(i).trueWord);
				terms.set(i, term);
				terms.remove(i + 1);
				combineTime(terms, i);
			} else {
				combineTime(terms, i + 1);
			}
		}

	}
	
	/**
	 * 递归合并相邻的地点词
	 * @param terms
	 * @param i
	 */
	public void combineSitus(List<Term> terms, int i) {

		if (terms.size() == i + 1) {
			return;
		} else {
			if (terms.get(i).nature == Nature.ns && terms.get(i + 1).nature == Nature.ns) {
				Term term = new Term(terms.get(i).word + terms.get(i + 1).word, 
						Nature.t, terms.get(i).frequency + terms.get(i + 1).frequency, terms.get(i).type,
						terms.get(i).offset, terms.get(i).trueWord);
				terms.set(i, term);
				terms.remove(i + 1);
				combineSitus(terms, i);
			} else {
				combineSitus(terms, i + 1);
			}
		}

	}
	
	/**
	 * 合并时间词和地点词结果
	 * @param keyword2
	 */
	public void combineDynaWord(List<Term> keyword2) {
		long startTime = System.currentTimeMillis();
		if(keyword2.size() > 0) {
			this.combineTime(keyword2, 0);
			this.combineSitus(keyword2, 0);
		}
		logger.info("combineDynaWord合并时间词和地点词结果：" + keyword2 + "，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
	}

	/**
	 * 分词
	 * @param question
	 * @return
	 */
	public List<Term> cutWord2(String question, String id_ac) {
		long startTime = System.currentTimeMillis();
		List<Term> result = new ArrayList<Term>();
		// List<Term> terms = HanLP.segment(question);// 更好的nlp开源工具包，默认提取5个关键词
//		MyNShortSegment nShortSegment = new MyNShortSegment();
		myNShortSegment.enableCustomDictionaryForcing(true);
		List<Term> terms = myNShortSegment.seg(question, id_ac);
//		this.combineTime(terms, 0);
		result = terms;
		logger.info("分词结果：" + result + "，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
		return result;
	}

	/**
	 * 判断一句话的字数是否少于规定的字数限制
	 * @param question
	 * @return
	 */
	private boolean isLess15(String question) {
		if (question.length() <= AIKey.QUESTION_LENGHT) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断关键词集个数是否少于5个
	 * 
	 * @param keyWords
	 * @return
	 */
	private boolean wordLess5(List<String> keyWords) {

		if (keyWords.size() <= 5) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 判断关键词集个数是否少于5个
	 * 
	 * @param keyWords
	 * @return
	 */
	private boolean wordLess5_2(List<Term> keyWords) {

		if (keyWords.size() <= 5) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 用pttern集合查询redis获取答案集
	 * 
	 * @param keyWords
	 * @return
	 */
	private List<Answer> searchRedis(List<String> keyWords, String userid) {
		List<Answer> answers = new ArrayList<Answer>();

		for (String keyWord : keyWords) {
			Set<String> answerSet = redisApiImpl.zrange(AIKey.PERSONAL_ANSWER + ":" + userid + ":" + keyWord, 0, -1);
			if (answerSet != null && answerSet.size() > 0) {
				for (String json : answerSet) {
					Answer answer = new Gson().fromJson(json, Answer.class);
					answers.add(answer);
				}
			}
		}
		// if(answers.size()==0) {//如果个性语料库没有则需要查询通用语料库
		// for (String keyWord : keyWords) {
		// Set<String> answerSet =
		// redisApiImpl.smembers(AIKey.POPULAR_ANSWER+":"+keyWord);
		// if(answerSet!=null && answerSet.size()>0) {
		// for (String json : answerSet) {
		// Answer answer = new Gson().fromJson(json, Answer.class);
		// answers.add(answer);
		// }
		// }
		// }
		// }
		return answers;
	}

	/**
	 * 根据pattern字段长度，降序排序答案集
	 * 
	 * @param answers
	 * @return
	 */
	private List<Answer> sortAnswers(List<Answer> answers) {
		// 排序
		Collections.sort(answers);
		return answers;
	}

}

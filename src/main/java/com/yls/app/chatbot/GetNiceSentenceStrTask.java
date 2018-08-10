package com.yls.app.chatbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import com.alibaba.fastjson.JSON;
import com.hankcs.hanlp.seg.common.Term;
import com.yls.app.entity.Sentence;
import com.yls.app.repository.AIKey;

/**
 * 线程类
 * @author huangsy
 * @date 2018年5月25日上午11:19:52
 */
public class GetNiceSentenceStrTask extends RecursiveTask<List<Sentence>> {

	/**
	 * 序列号
	 */
	private static final long serialVersionUID = 5346880611803856334L;

	/**
	 * 最小任务大小
	 */
	private static final int THRESHOLD = 10000;

	/**
	 * json格式的句式集合
	 */
	private List<String> sentenceStr;

	/**
	 * 句式集合左边界
	 */
	private int start;

	/**
	 * 句式集合右边界
	 */
	private int end;

	/**
	 * 分词集合
	 */
	private List<Term> terms;

	/**
	 * @param sentenceStr json格式的句式集合
	 * @param start 句式集合左边界
	 * @param end 句式集合右边界
	 * @param terms 分词集合
	 */
	public GetNiceSentenceStrTask(List<String> sentenceStr, int start, int end, List<Term> terms) {
		this.sentenceStr = sentenceStr;
		this.start = start;
		this.end = end;
		this.terms = terms;
	}

	@Override
	protected List<Sentence> compute() {
		if (end - start <= THRESHOLD) {
			List<Sentence> result = new ArrayList<Sentence>();
			for (int i = start; i < end; i++) {
				Sentence sentence = JSON.parseObject(sentenceStr.get(i), Sentence.class);
				List<String> keywords = new ArrayList<String>(sentence2Keywords(sentence));
				// 句式含有未知词的处理
				if (keywords.contains(AIKey.UNKNOW)) {
					int index = keywords.indexOf(AIKey.UNKNOW);
					// 移除未知词
					keywords.remove(index);
				}
				// 对比句式和关键分词，查找最优句式
				boolean isbreak = false;
				for (String kw : keywords) {
					boolean flag = false;
					for (Term t : terms) {
						if(t.type!=null) {
							for(String type : t.type) {
								if (kw.equals(t.word) || kw.equals(type)) {
									flag = flag || true;
								} else {
									flag = flag || false;
								}
							}
						}
					}
					if (!flag) {
						isbreak = true;
						break;
					}
				}
				if (!isbreak) {
					result.add(sentence);
				}
			}
			return result;
		} else {
			// 任务太大，一分为二
			int middle = (end + start) / 2;
			GetNiceSentenceStrTask subtask1 = new GetNiceSentenceStrTask(this.sentenceStr, start, middle, this.terms);
			GetNiceSentenceStrTask subtask2 = new GetNiceSentenceStrTask(this.sentenceStr, middle, end, this.terms);
			invokeAll(subtask1, subtask2);
			List<Sentence> subresult1 = subtask1.join();
			List<Sentence> subresult2 = subtask2.join();
			subresult1.addAll(subresult2);
			return subresult1;
		}
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
	
}
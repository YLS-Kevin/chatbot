/**
 * 
 */
package com.yls.app.hanlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.algorithm.MaxHeap;
import com.hankcs.hanlp.collection.trie.DoubleArrayTrie;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.dictionary.CoreDictionary.Attribute;
import com.hankcs.hanlp.dictionary.other.CharTable;
import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankKeyword;
import com.yls.app.chatbot.Chat;
import com.yls.app.repository.AIKey;
import com.yls.app.repository.impl.RedisApiImpl;
import com.yls.app.util.SerializeUtil;

/**
 * @author huangsy
 * @date 2018年4月11日上午11:26:12
 */
@Component
public class MyTextRankKeyword extends TextRankKeyword {
	
	private final static Logger logger = Logger.getLogger(MyTextRankKeyword.class);

	/**
	 * 提取多少个关键字
	 */
	int nKeyword = 10;

	/**
	 * 阻尼系数（ＤａｍｐｉｎｇＦａｃｔｏｒ），一般取值为0.85
	 */
	final static float d = 0.85f;
	/**
	 * 最大迭代次数
	 */
	public static int max_iter = 200;
	final static float min_diff = 0.001f;

	@Resource
	private MyNShortSegment myNShortSegment;

	@Resource
	private RedisApiImpl redisApiImpl;
	
	/**
	 * 分词结果
	 */
	private List<Term> termList;
	
	public List<Term> getTermList() {
		return termList;
	}

	public void setTermList(List<Term> termList) {
		this.termList = termList;
	}

	/**
	 * 提取关键词
	 *
	 * @param document
	 *            文档内容
	 * @param size
	 *            希望提取几个关键词
	 * @param acid
	 *            账户id
	 * @return 一个列表
	 */
	public List<String> getKeywordList(String document, int size, String acid) {
		this.nKeyword = size;

		return this.getKeyword(document, acid);
	}

	/**
	 * 提取关键词
	 *
	 * @param content
	 * @return
	 */
	private List<String> getKeyword(String content, String acid) {
		Set<Map.Entry<String, Float>> entrySet = getTermAndRank(content, nKeyword, acid).entrySet();
		List<String> result = new ArrayList<String>(entrySet.size());
		for (Map.Entry<String, Float> entry : entrySet) {
			result.add(entry.getKey());
		}
		return result;
	}

	/**
	 * 返回分数最高的前size个分词结果和对应的rank
	 *
	 * @param content
	 * @param size
	 * @return
	 */
	private Map<String, Float> getTermAndRank(String content, Integer size, String acid) {
		
		Map<String, Float> map = getTermAndRank(content, acid);
		Map<String, Float> result = new LinkedHashMap<String, Float>();
		for (Map.Entry<String, Float> entry : new MaxHeap<Map.Entry<String, Float>>(size,
				new Comparator<Map.Entry<String, Float>>() {
					@Override
					public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
						return o1.getValue().compareTo(o2.getValue());
					}
				}).addAll(map.entrySet()).toList()) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}

	/**
	 * 返回全部分词结果和对应的rank
	 *
	 * @param content
	 * @return
	 */
	private Map<String, Float> getTermAndRank(String content, String acid) {
		assert content != null;
		myNShortSegment.enableCustomDictionaryForcing(true);
		this.termList = myNShortSegment.seg(content, acid);
		return getRank2(termList, acid);
	}

	private Map<String, Float> sortMap(Map<String, Integer> useMap) {

		List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(useMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			// 升序排序
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue() - o1.getValue();
			}

		});
		
		Map<String, Float> result = new HashMap<String, Float>();
		float v = list.size();
		for(Map.Entry<String, Integer> map : list ) {
			result.put(map.getKey(), v --);
		}
		return result;

	}

	/**
	 * 使用已经分好的词来计算rank
	 *
	 * @param termList
	 * @return
	 */
	public Map<String, Float> getRank(List<Term> termList, String acid) {
		DoubleArrayTrie<CoreDictionary.Attribute> dat = this.getDatFromRedis(acid);
		List<String> wordList = new ArrayList<String>(termList.size());
		Map<String, Integer> useMap = new HashMap<String, Integer>();
		for (Term t : termList) {
			if (shouldInclude(t)) {
				wordList.add(t.word);
				useMap.put(t.word, this.getFrequency(t.word, acid, dat));
			}
		}
//		System.out.println(useMap);
//		System.out.println(sortMap(useMap));
		Map<String, Float> sortedMap = sortMap(useMap);
		Map<String, Set<String>> words = new TreeMap<String, Set<String>>();
		Queue<String> que = new LinkedList<String>();
		for (String w : wordList) {
			if (!words.containsKey(w)) {
				words.put(w, new TreeSet<String>());
			}
			// 复杂度O(n-1)
			if (que.size() >= 5) {
				que.poll();
			}
			for (String qWord : que) {
				if (w.equals(qWord)) {
					continue;
				}
				// 既然是邻居,那么关系是相互的,遍历一遍即可
				words.get(w).add(qWord);
				words.get(qWord).add(w);
			}
			que.offer(w);
		}
		// System.out.println(words);
		Map<String, Float> score = new HashMap<String, Float>();
		for (int i = 0; i < max_iter; ++i) {
			Map<String, Float> m = new HashMap<String, Float>();
			float max_diff = 0;
			for (Map.Entry<String, Set<String>> entry : words.entrySet()) {
				String key = entry.getKey();
				Set<String> value = entry.getValue();
				m.put(key, 1 - d + sortedMap.get(key));
				for (String element : value) {
					int size = words.get(element).size();
					if (key.equals(element) || size == 0)
						continue;
					m.put(key, m.get(key) + d / size * (score.get(element) == null ? 0 : score.get(element)));
				}
				max_diff = Math.max(max_diff, Math.abs(m.get(key) - (score.get(key) == null ? 0 : score.get(key))));
			}
			score = m;
			if (max_diff <= min_diff)
				break;
		}

		return score;
	}
	
	public Map<String, Float> getRank2(List<Term> termList, String acid) {
		DoubleArrayTrie<CoreDictionary.Attribute> dat = this.getDatFromRedis(acid);
		Map<String, Integer> useMap = new HashMap<String, Integer>();
		for (Term t : termList) {
			if (this.shouldInclude(t, acid, dat)) {
				useMap.put(t.word, this.getFrequency(t.word, acid, dat));
			}
		}
		Map<String, Float> sortedMap = sortMap(useMap);
		
		return sortedMap;
	}
	
	public boolean shouldInclude(Term term, String acid, DoubleArrayTrie<CoreDictionary.Attribute> dat) {
        // 除掉停用词
        String nature = term.nature != null ? term.nature.toString() : "空";
        char firstChar = nature.charAt(0);
        switch (firstChar)
        {
            case 'm':
            case 'b':
            case 'c':
            case 'e':
            case 'o':
            case 'p':
            case 'q':
            case 'u':
            case 'y':
            case 'z':
            case 'r':
            case 'w':
            {
                return false;
            }
            default:
            {
            	if(this.contains(term.word, acid, dat)) {
            		return true;
            	}
            	
                if (!CoreStopWordDictionary.contains(term.word))
                {
                    return true;
                }
            }
            break;
        }

        return false;
    }
	
	/**
     * 个性词典中是否含有词语
     * @param key 词语
     * @return 是否包含
     */
    public boolean contains(String text, String acid, DoubleArrayTrie<CoreDictionary.Attribute> dat) {
        if (dat.exactMatchSearch(text) >= 0) {
        	return true;
        } else {
        	return false;
        }
    }
    
    /**
     * 从redis获取个性词库缓存
     * @param acid
     * @return
     */
    public DoubleArrayTrie<CoreDictionary.Attribute> getDatFromRedis(String acid) {
    	String key = AIKey.CUSTOMER_DICTIONARY + ":" + acid;
		byte[] batValue = redisApiImpl.get(key.getBytes());
		DoubleArrayTrie<CoreDictionary.Attribute> dat = (DoubleArrayTrie<Attribute>) SerializeUtil.unserialize(batValue);
		return dat;
    }

	/**
	 * 获取本词语在HanLP词库中的频次
	 * 
	 * @return 频次，0代表这是个OOV
	 */
	private int getFrequency(String word, String acid, DoubleArrayTrie<CoreDictionary.Attribute> dat) {

		CoreDictionary.Attribute attribute = this.getAttribute(word, acid, dat);
		if (attribute == null)
			return 0;
		return attribute.totalFrequency;

	}

	/**
	 * 从HanLP的词库中提取某个单词的属性（包括核心词典和用户词典）
	 *
	 * @param word
	 *            单词
	 * @return 包含词性与频次的信息
	 */
	private CoreDictionary.Attribute getAttribute(String word, String acid, DoubleArrayTrie<CoreDictionary.Attribute> dat) {

		// CoreDictionary.Attribute attribute = CoreDictionary.get(word);
		// if (attribute != null) return attribute;
		return this.get(word, acid, dat);
	}

	/**
	 * 查单词
	 *
	 * @param key
	 * @return
	 */
	private CoreDictionary.Attribute get(String word, String acid, DoubleArrayTrie<CoreDictionary.Attribute> dat) {
		if (HanLP.Config.Normalization) {
			word = CharTable.convert(word);
		}
		// DAT合并
		CoreDictionary.Attribute attribute = dat.get(word);
		return attribute;
	}

}

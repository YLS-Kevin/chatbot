/**
 * 
 */
package com.yls.app.hanlp;

import static com.hankcs.hanlp.utility.Predefine.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.algorithm.Dijkstra;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.collection.trie.DoubleArrayTrie;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.CoreDictionary.Attribute;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.dictionary.other.CharTable;
import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary;
import com.hankcs.hanlp.recognition.nr.JapanesePersonRecognition;
import com.hankcs.hanlp.recognition.nr.PersonRecognition;
import com.hankcs.hanlp.recognition.nr.TranslatedPersonRecognition;
import com.hankcs.hanlp.recognition.ns.PlaceRecognition;
import com.hankcs.hanlp.recognition.nt.OrganizationRecognition;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.seg.NShort.Path.NShortPath;
import com.hankcs.hanlp.seg.common.Graph;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.seg.common.Vertex;
import com.hankcs.hanlp.seg.common.WordNet;
import com.hankcs.hanlp.utility.SentencesUtil;
import com.hankcs.hanlp.utility.TextUtility;
import com.yls.app.repository.AIKey;
import com.yls.app.repository.impl.RedisApiImpl;
import com.yls.app.util.SerializeUtil;

/**
 * @author huangsy
 * @date 2018年4月9日下午5:22:45
 */
@Component
public class MyNShortSegment extends NShortSegment {
	
	@Resource
	private RedisApiImpl redisApiImpl;
	
	@Resource
	private MyCustomDictionary myCustomDictionary;
	
	//线程安全dat
	private ThreadLocal<DoubleArrayTrie<CoreDictionary.Attribute>> threadDat = new ThreadLocal<DoubleArrayTrie<CoreDictionary.Attribute>>();
	
	/**
	 * 提取词频最高的前size个关键分词
	 * @param text
	 * @param size
	 * @param acid
	 * @return
	 */
	public List<Term> seg(String text, Integer size, String acid) {
		List<Term> result = new ArrayList<Term>();
		//先分词
		List<Term> terms = this.seg(text, acid);
		//再提取关键词
		for (Term t : terms) {
			if (this.shouldInclude(t, acid, threadDat.get())) {
				result.add(t);
			}
		}
		//最后根据词频排序
		Collections.sort(result);
		//使用完ThreadLocal要清除掉，避免内存泄漏
		threadDat.remove();
		return result;
	}
	
	/**
	 * 如果是停用词就不包含
	 * @param term
	 * @param acid
	 * @param dat
	 * @return
	 */
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
     * 分词<br>
     * 此方法是线程安全的
     *
     * @param text 待分词文本
     * @return 单词列表
     */
    public List<Term> seg(String text, String acid)
    {
        char[] charArray = text.toCharArray();
        if (HanLP.Config.Normalization)
        {
            CharTable.normalization(charArray);
        }
        if (config.threadNumber > 1 && charArray.length > 10000)    // 小文本多线程没意义，反而变慢了
        {
            List<String> sentenceList = SentencesUtil.toSentenceList(charArray);
            String[] sentenceArray = new String[sentenceList.size()];
            sentenceList.toArray(sentenceArray);
            //noinspection unchecked
            List<Term>[] termListArray = new List[sentenceArray.length];
            final int per = sentenceArray.length / config.threadNumber;
            WorkThread[] threadArray = new WorkThread[config.threadNumber];
            for (int i = 0; i < config.threadNumber - 1; ++i)
            {
                int from = i * per;
                threadArray[i] = new WorkThread(sentenceArray, termListArray, from, from + per);
                threadArray[i].start();
            }
            threadArray[config.threadNumber - 1] = new WorkThread(sentenceArray, termListArray, (config.threadNumber - 1) * per, sentenceArray.length);
            threadArray[config.threadNumber - 1].start();
            try
            {
                for (WorkThread thread : threadArray)
                {
                    thread.join();
                }
            }
            catch (InterruptedException e)
            {
                logger.severe("线程同步异常：" + TextUtility.exceptionToString(e));
                return Collections.emptyList();
            }
            List<Term> termList = new LinkedList<Term>();
            if (config.offset || config.indexMode > 0)  // 由于分割了句子，所以需要重新校正offset
            {
                int sentenceOffset = 0;
                for (int i = 0; i < sentenceArray.length; ++i)
                {
                    for (Term term : termListArray[i])
                    {
                        term.offset += sentenceOffset;
                        termList.add(term);
                    }
                    sentenceOffset += sentenceArray[i].length();
                }
            }
            else
            {
                for (List<Term> list : termListArray)
                {
                    termList.addAll(list);
                }
            }

            return termList;
        }
        return this.segSentence(charArray, acid);
    }

	private List<Term> segSentence(char[] sentence, String acid) {
		WordNet wordNetOptimum = new WordNet(sentence);
		WordNet wordNetAll = new WordNet(sentence);
		// char[] charArray = text.toCharArray();
		// 粗分
		List<List<Vertex>> coarseResult = BiSegment(sentence, 2, wordNetOptimum, wordNetAll, acid);
		boolean NERexists = false;
		for (List<Vertex> vertexList : coarseResult) {
			if (HanLP.Config.DEBUG) {
				System.out.println("粗分结果" + convert(vertexList, false));
			}
			// 实体命名识别
			if (config.ner) {
				wordNetOptimum.addAll(vertexList);
				int preSize = wordNetOptimum.size();
				if (config.nameRecognize) {
					PersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
				}
				if (config.translatedNameRecognize) {
					TranslatedPersonRecognition.recognition(vertexList, wordNetOptimum, wordNetAll);
				}
				if (config.japaneseNameRecognize) {
					JapanesePersonRecognition.recognition(vertexList, wordNetOptimum, wordNetAll);
				}
				if (config.placeRecognize) {
					PlaceRecognition.recognition(vertexList, wordNetOptimum, wordNetAll);
				}
				if (config.organizationRecognize) {
					// 层叠隐马模型——生成输出作为下一级隐马输入
					vertexList = Dijkstra.compute(generateBiGraph(wordNetOptimum));
					wordNetOptimum.addAll(vertexList);
					OrganizationRecognition.recognition(vertexList, wordNetOptimum, wordNetAll);
				}
				if (!NERexists && preSize != wordNetOptimum.size()) {
					NERexists = true;
				}
			}
		}

		List<Vertex> vertexList = coarseResult.get(0);
		if (NERexists) {
			Graph graph = generateBiGraph(wordNetOptimum);
			vertexList = Dijkstra.compute(graph);
			if (HanLP.Config.DEBUG) {
				System.out.printf("细分词网：\n%s\n", wordNetOptimum);
				System.out.printf("细分词图：%s\n", graph.printByTo());
			}
		}

		// 数字识别
		if (config.numberQuantifierRecognize) {
			mergeNumberQuantifier(vertexList, wordNetAll, config);
		}

		// 如果是索引模式则全切分
		if (config.indexMode > 0) {
			return decorateResultForIndexMode(vertexList, wordNetAll);
		}

		// 是否标注词性
		if (config.speechTagging) {
			speechTagging(vertexList);
		}

		if (config.useCustomDictionary) {
			if (config.indexMode > 0) {
				this.combineByCustomDictionary2(vertexList, wordNetAll, acid);
			} else {
//				long startTime = System.currentTimeMillis();
				this.combineByCustomDictionary2(vertexList, acid);
//				System.out.println("combineByCustomDictionary2，耗时：" + ((System.currentTimeMillis() - startTime) * 0.001) + "秒");
			}
		}

		return convert(vertexList, config.offset);
	}
	
	/**
     * 生成一元词网
     *
     * @param wordNetStorage
     */
    private void GenerateWordNet(final WordNet wordNetStorage, String acid)
    {
        final char[] charArray = wordNetStorage.charArray;

        // 核心词典查询
        DoubleArrayTrie<CoreDictionary.Attribute>.Searcher searcher = CoreDictionary.trie.getSearcher(charArray, 0);
        while (searcher.next())
        {
            wordNetStorage.add(searcher.begin + 1, new Vertex(new String(charArray, searcher.begin, searcher.length), searcher.value, searcher.index));
        }
        // 强制用户词典查询
        if (config.forceCustomDictionary)
        {
            myCustomDictionary.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<CoreDictionary.Attribute>()
            {
                @Override
                public void hit(int begin, int end, CoreDictionary.Attribute value)
                {
                    wordNetStorage.add(begin + 1, new Vertex(new String(charArray, begin, end - begin), value));
                }
            }, acid);
        }
        // 原子分词，保证图连通
        LinkedList<Vertex>[] vertexes = wordNetStorage.getVertexes();
        for (int i = 1; i < vertexes.length; )
        {
            if (vertexes[i].isEmpty())
            {
                int j = i + 1;
                for (; j < vertexes.length - 1; ++j)
                {
                    if (!vertexes[j].isEmpty()) break;
                }
                wordNetStorage.add(i, quickAtomSegment(charArray, i - 1, j - 1));
                i = j;
            }
            else i += vertexes[i].getLast().realWord.length();
        }
    }

	/**
	 * 二元语言模型分词
	 * 
	 * @param sSentence
	 *            待分词的句子
	 * @param nKind
	 *            需要几个结果
	 * @param wordNetOptimum
	 * @param wordNetAll
	 * @return 一系列粗分结果
	 */
	public List<List<Vertex>> BiSegment(char[] sSentence, int nKind, WordNet wordNetOptimum, WordNet wordNetAll, String acid) {
		List<List<Vertex>> coarseResult = new LinkedList<List<Vertex>>();
		//////////////// 生成词网////////////////////
		this.GenerateWordNet(wordNetAll, acid);
		// logger.trace("词网大小：" + wordNetAll.size());
		// logger.trace("打印词网：\n" + wordNetAll);
		/////////////// 生成词图////////////////////
		Graph graph = generateBiGraph(wordNetAll);
		// logger.trace(graph.toString());
		if (HanLP.Config.DEBUG) {
			System.out.printf("打印词图：%s\n", graph.printByTo());
		}
		/////////////// N-最短路径////////////////////
		NShortPath nShortPath = new NShortPath(graph, nKind);
		List<int[]> spResult = nShortPath.getNPaths(nKind * 2);
		if (spResult.size() == 0) {
			throw new RuntimeException(nKind + "-最短路径求解失败，请检查上面的词网是否存在负圈或悬孤节点");
		}
		// logger.trace(nKind + "-最短路径");
		// for (int[] path : spResult)
		// {
		// logger.trace(Graph.parseResult(graph.parsePath(path)));
		// }
		////////////// 日期、数字合并策略
		for (int[] path : spResult) {
			List<Vertex> vertexes = graph.parsePath(path);
			generateWord(vertexes, wordNetOptimum);
			coarseResult.add(vertexes);
		}
		return coarseResult;
	}

	/**
	 * 一句话分词
	 *
	 * @param text
	 * @return
	 */
	public static List<Term> parse(String text) {
		return new NShortSegment().seg(text);
	}

	/**
	 * 开启词性标注
	 * 
	 * @param enable
	 * @return
	 */
	public NShortSegment enablePartOfSpeechTagging(boolean enable) {
		config.speechTagging = enable;
		return this;
	}

	/**
	 * 开启地名识别
	 * 
	 * @param enable
	 * @return
	 */
	public NShortSegment enablePlaceRecognize(boolean enable) {
		config.placeRecognize = enable;
		config.updateNerConfig();
		return this;
	}

	/**
	 * 开启机构名识别
	 * 
	 * @param enable
	 * @return
	 */
	public NShortSegment enableOrganizationRecognize(boolean enable) {
		config.organizationRecognize = enable;
		config.updateNerConfig();
		return this;
	}

	/**
	 * 是否启用音译人名识别
	 *
	 * @param enable
	 */
	public NShortSegment enableTranslatedNameRecognize(boolean enable) {
		config.translatedNameRecognize = enable;
		config.updateNerConfig();
		return this;
	}

	/**
	 * 是否启用日本人名识别
	 *
	 * @param enable
	 */
	public NShortSegment enableJapaneseNameRecognize(boolean enable) {
		config.japaneseNameRecognize = enable;
		config.updateNerConfig();
		return this;
	}

	/**
	 * 是否启用偏移量计算（开启后Term.offset才会被计算）
	 * 
	 * @param enable
	 * @return
	 */
	public NShortSegment enableOffset(boolean enable) {
		config.offset = enable;
		return this;
	}

	public NShortSegment enableAllNamedEntityRecognize(boolean enable) {
		config.nameRecognize = enable;
		config.japaneseNameRecognize = enable;
		config.translatedNameRecognize = enable;
		config.placeRecognize = enable;
		config.organizationRecognize = enable;
		config.updateNerConfig();
		return this;
	}
	
	/**
     * 使用用户词典合并粗分结果
     * @param vertexList 粗分结果
     * @return 合并后的结果
     */
    private List<Vertex> combineByCustomDictionary2(List<Vertex> vertexList, String acid)
    {
        assert vertexList.size() > 2 : "vertexList至少包含 始##始 和 末##末";
        Vertex[] wordNet = new Vertex[vertexList.size()];
        vertexList.toArray(wordNet);
        // DAT合并
        String key = AIKey.CUSTOMER_DICTIONARY + ":" + acid;
        byte[] batValue = redisApiImpl.get(key.getBytes());
        DoubleArrayTrie<CoreDictionary.Attribute> dat = (DoubleArrayTrie<Attribute>)SerializeUtil.unserialize(batValue);
        //保存为线程安全全局变量
        threadDat.set(dat);
        int length = wordNet.length - 1; // 跳过首尾
        for (int i = 1; i < length; ++i)
        {
            int state = 1;
            state = dat.transition(wordNet[i].realWord, state);
            if (state > 0)
            {
                int to = i + 1;
                int end = to;
                CoreDictionary.Attribute value = dat.output(state);
                for (; to < length; ++to)
                {
                    state = dat.transition(wordNet[to].realWord, state);
                    if (state < 0) break;
                    CoreDictionary.Attribute output = dat.output(state);
                    if (output != null)
                    {
                        value = output;
                        end = to + 1;
                    }
                }
                if (value != null)
                {
                    combineWords(wordNet, i, end, value);
                    i = end - 1;
                }
            }
        }
        // BinTrie合并
//        if (CustomDictionary.trie != null)
//        {
//            for (int i = 1; i < length; ++i)
//            {
//                if (wordNet[i] == null) continue;
//                BaseNode<CoreDictionary.Attribute> state = CustomDictionary.trie.transition(wordNet[i].realWord.toCharArray(), 0);
//                if (state != null)
//                {
//                    int to = i + 1;
//                    int end = to;
//                    CoreDictionary.Attribute value = state.getValue();
//                    for (; to < length; ++to)
//                    {
//                        if (wordNet[to] == null) continue;
//                        state = state.transition(wordNet[to].realWord.toCharArray(), 0);
//                        if (state == null) break;
//                        if (state.getValue() != null)
//                        {
//                            value = state.getValue();
//                            end = to + 1;
//                        }
//                    }
//                    if (value != null)
//                    {
//                        combineWords(wordNet, i, end, value);
//                        i = end - 1;
//                    }
//                }
//            }
//        }
        vertexList.clear();
        for (Vertex vertex : wordNet)
        {
            if (vertex != null) vertexList.add(vertex);
        }
        return vertexList;
    }
    
    /**
     * 使用用户词典合并粗分结果，并将用户词语收集到全词图中
     * @param vertexList 粗分结果
     * @param wordNetAll 收集用户词语到全词图中
     * @return 合并后的结果
     */
    private List<Vertex> combineByCustomDictionary2(List<Vertex> vertexList, final WordNet wordNetAll, String cid)
    {
        List<Vertex> outputList = combineByCustomDictionary2(vertexList, cid);
        int line = 0;
        for (final Vertex vertex : outputList)
        {
            final int parentLength = vertex.realWord.length();
            final int currentLine = line;
            if (parentLength >= 3)
            {
                CustomDictionary.parseText(vertex.realWord, new AhoCorasickDoubleArrayTrie.IHit<CoreDictionary.Attribute>()
                {
                    @Override
                    public void hit(int begin, int end, CoreDictionary.Attribute value)
                    {
                        if (end - begin == parentLength) return;
                        wordNetAll.add(currentLine + begin, new Vertex(vertex.realWord.substring(begin, end), value));
                    }
                });
            }
            line += parentLength;
        }
        return outputList;
    }
    
    /**
     * 将连续的词语合并为一个
     * @param wordNet 词图
     * @param start 起始下标（包含）
     * @param end 结束下标（不包含）
     * @param value 新的属性
     */
    private void combineWords(Vertex[] wordNet, int start, int end, CoreDictionary.Attribute value)
    {
        if (start + 1 == end)   // 小优化，如果只有一个词，那就不需要合并，直接应用新属性
        {
            wordNet[start].attribute = value;
        }
        else
        {
            StringBuilder sbTerm = new StringBuilder();
            for (int j = start; j < end; ++j)
            {
                if (wordNet[j] == null) continue;
                String realWord = wordNet[j].realWord;
                sbTerm.append(realWord);
                wordNet[j] = null;
            }
            wordNet[start] = new Vertex(sbTerm.toString(), value);
        }
    }
    
    class WorkThread extends Thread
    {
        String[] sentenceArray;
        List<Term>[] termListArray;
        int from;
        int to;

        public WorkThread(String[] sentenceArray, List<Term>[] termListArray, int from, int to)
        {
            this.sentenceArray = sentenceArray;
            this.termListArray = termListArray;
            this.from = from;
            this.to = to;
        }

        @Override
        public void run()
        {
            for (int i = from; i < to; ++i)
            {
                termListArray[i] = segSentence(sentenceArray[i].toCharArray());
            }
        }
    }

}

/**
 * 
 */
package com.yls.app.hanlp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.collection.trie.DoubleArrayTrie;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.hankcs.hanlp.corpus.io.IOUtil;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.CoreDictionary.Attribute;
import com.hankcs.hanlp.dictionary.other.CharTable;
import com.hankcs.hanlp.utility.LexiconUtility;
import com.hankcs.hanlp.utility.Predefine;
import com.hankcs.hanlp.utility.TextUtility;
import com.yls.app.repository.AIKey;
import com.yls.app.repository.impl.RedisApiImpl;
import com.yls.app.util.SerializeUtil;

/**
 * @author huangsy
 * @date 2018年4月11日上午10:58:15
 */
@Component
public class MyCustomDictionary {
	
	private static Logger logger = Logger.getLogger(MyCustomDictionary.class);
	
	@Resource
	private RedisApiImpl redisApiImpl;
	public static BinTrie<CoreDictionary.Attribute> trie;
	private static TreeMap<String, CoreDictionary.Attribute> map = new TreeMap<String, CoreDictionary.Attribute>();
	private DoubleArrayTrie<CoreDictionary.Attribute> dat = new DoubleArrayTrie<CoreDictionary.Attribute>();
	
	public DoubleArrayTrie<CoreDictionary.Attribute> getDat() {
		return dat;
	}
	
	public void setDat(DoubleArrayTrie<CoreDictionary.Attribute> dat) {
		this.dat = dat;
	}
	
	static {
		String[] path = HanLP.Config.CustomDictionaryPath;
		LinkedHashSet<Nature> customNatureCollector = new LinkedHashSet<Nature>();
		for (String p : path) {
			Nature defaultNature = Nature.n;
			int cut = p.indexOf(' ');
			if (cut > 0) {
				// 有默认词性
				String nature = p.substring(cut + 1);
				p = p.substring(0, cut);
				try {
					defaultNature = LexiconUtility.convertStringToNature(nature, customNatureCollector);
				} catch (Exception e) {
					logger.info("配置文件【" + p + "】写错了！" + e);
					continue;
				}
			}
			logger.info("以默认词性[" + defaultNature + "]加载自定义词典" + p + "中……");
			boolean success = load(p, defaultNature, map, customNatureCollector);
			if (!success)
				logger.warn("失败：" + p);
		}
		if (map.size() == 0) {
			logger.warn("没有加载到任何词条");
			map.put(Predefine.TAG_OTHER, null); // 当作空白占位符
		}
	}

	/**
	 * 缓存用户词典到redis
	 * @param mainPath
	 * @param path
	 * @param acid
	 * @return
	 */
	public boolean loadMainDictionary(String mainPath, String[] path, String acid) {
		
		dat = new DoubleArrayTrie<CoreDictionary.Attribute>();
		TreeMap<String, CoreDictionary.Attribute> map = new TreeMap<String, CoreDictionary.Attribute>();
		map.putAll(this.map);
		LinkedHashSet<Nature> customNatureCollector = new LinkedHashSet<Nature>();
		try {
			for (String p : path) {
				Nature defaultNature = Nature.n;
				int cut = p.indexOf(' ');
				if (cut > 0) {
					// 有默认词性
					String nature = p.substring(cut + 1);
					p = p.substring(0, cut);
					try {
						defaultNature = LexiconUtility.convertStringToNature(nature, customNatureCollector);
					} catch (Exception e) {
						logger.info("配置文件【" + p + "】写错了！" + e);
						continue;
					}
				}
				logger.info("以默认词性[" + defaultNature + "]加载自定义词典" + p + "中……");
				boolean success = loadDWord(p, defaultNature, map, customNatureCollector);
				if (!success)
					logger.warn("失败：" + p);
			}
			if (map.size() == 0) {
				logger.warn("没有加载到任何词条");
				map.put(Predefine.TAG_OTHER, null); // 当作空白占位符
			}
			logger.info("正在构建DoubleArrayTrie……");
			dat.build(map);
			//缓存到redis
			logger.info("构建完成，缓存到redis");
			byte[] value = SerializeUtil.serialize(dat);
			String key = AIKey.CUSTOMER_DICTIONARY + ":" + acid;
			redisApiImpl.set(key.getBytes(), value);
			// 缓存成dat文件，下次加载会快很多
//			logger.info("正在缓存词典为dat文件……");
//			// 缓存值文件
//			List<CoreDictionary.Attribute> attributeList = new LinkedList<CoreDictionary.Attribute>();
//			for (Map.Entry<String, CoreDictionary.Attribute> entry : map.entrySet()) {
//				attributeList.add(entry.getValue());
//			}
//			DataOutputStream out = new DataOutputStream(IOUtil.newOutputStream(mainPath + Predefine.BIN_EXT));
//			// 缓存用户词性
//			IOUtil.writeCustomNature(out, customNatureCollector);
//			// 缓存正文
//			out.writeInt(attributeList.size());
//			for (CoreDictionary.Attribute attribute : attributeList) {
//				attribute.save(out);
//			}
//			dat.save(out);
//			out.close();
//		} catch (FileNotFoundException e) {
//			logger.info("自定义词典" + mainPath + "不存在！" + e);
//			return false;
//		} catch (IOException e) {
//			logger.info("自定义词典" + mainPath + "读取错误！" + e);
//			return false;
		} catch (Exception e) {
			logger.warn("自定义词典" + mainPath + "缓存失败！\n" + TextUtility.exceptionToString(e));
		}
		return true;
		
	}

	/**
	 * 加载用户词典（追加）
	 *
	 * @param path
	 *            词典路径
	 * @param defaultNature
	 *            默认词性
	 * @param customNatureCollector
	 *            收集用户词性
	 * @return
	 */
	public static boolean load(String path, Nature defaultNature, TreeMap<String, CoreDictionary.Attribute> map,
			LinkedHashSet<Nature> customNatureCollector) {
		try {
			String splitter = "\\s";
			if (path.endsWith(".csv")) {
				splitter = ",";
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(IOUtil.newInputStream(path), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				String[] param = line.split(splitter);
				if (param[0].length() == 0)
					continue; // 排除空行
				if (HanLP.Config.Normalization)
					param[0] = CharTable.convert(param[0]); // 正规化

				int natureCount = (param.length - 1) / 2;
				CoreDictionary.Attribute attribute;
				if (natureCount == 0) {
					attribute = new CoreDictionary.Attribute(defaultNature);
				} else {
					attribute = new CoreDictionary.Attribute(natureCount);
					for (int i = 0; i < natureCount; ++i) {
						attribute.nature[i] = LexiconUtility.convertStringToNature(param[1 + 2 * i],
								customNatureCollector);
						attribute.frequency[i] = Integer.parseInt(param[2 + 2 * i]);
						attribute.totalFrequency += attribute.frequency[i];
					}
				}
				// if (updateAttributeIfExist(param[0], attribute, map,
				// rewriteTable)) continue;
				map.put(param[0], attribute);
			}
			br.close();
		} catch (Exception e) {
			logger.info("自定义词典" + path + "读取错误！" + e);
			return false;
		}

		return true;
	}
	
	/**
	 * 加载用户词典（含词类型的文件）
	 *
	 * @param path
	 *            词典路径
	 * @param defaultNature
	 *            默认词性
	 * @param customNatureCollector
	 *            收集用户词性
	 * @return
	 */
	public static boolean loadDWord(String path, Nature defaultNature, TreeMap<String, CoreDictionary.Attribute> map,
			LinkedHashSet<Nature> customNatureCollector) {
		int ii = 0;
		try {
			String splitter = "\\s";
			if (path.endsWith(".csv")) {
				splitter = ",";
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(IOUtil.newInputStream(path), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				ii++;
				String[] param = line.split(splitter);
				if (param[0].length() == 0)
					continue; // 排除空行
//				if (HanLP.Config.Normalization)
//					param[0] = CharTable.convert(param[0]); // 正规化

				int natureCount = (param.length - 2) / 2;
				CoreDictionary.Attribute attribute;
				if (natureCount == 0) {
					attribute = new CoreDictionary.Attribute(defaultNature);
				} else {
					attribute = new CoreDictionary.Attribute(natureCount);
					for (int i = 0; i < natureCount; ++i) {
						attribute.nature[i] = LexiconUtility.convertStringToNature(param[2 + 2 * i],
								customNatureCollector);
						attribute.frequency[i] = Integer.parseInt(param[3 + 2 * i]);
						attribute.totalFrequency += attribute.frequency[i];
					}
					List<String> type = new ArrayList<String>();
					type.add(param[1]);
					attribute.type = type;
				}
				// if (updateAttributeIfExist(param[0], attribute, map,
				// rewriteTable)) continue;
				String[] words = param[0].split(",");
				attribute.trueWord = words[0];
				for(String word : words) {
					if(!map.containsKey(word)) {
						map.put(word, attribute);
					} else {
						CoreDictionary.Attribute attributeTemp = map.get(word);
						List<String> type = attributeTemp.type;
						if(type!=null) {
							if(!type.contains(param[1])) {
								type.add(param[1]);
							}
							attributeTemp.type = type;
						} else {
							type = new ArrayList<String>();
							if(!type.contains(param[1])) {
								type.add(param[1]);
							}
							attributeTemp.type = type;
						}
						map.put(word, attributeTemp);
					}
				}
			}
			br.close();
		} catch (Exception e) {
			logger.info("自定义词典" + path + "读取错误！" + e);
			return false;
		}

		return true;
	}
	
	/**
     * 解析一段文本（目前采用了BinTrie+DAT的混合储存形式，此方法可以统一两个数据结构）
     * @param text         文本
     * @param processor    处理器
     */
    public void parseText(char[] text, AhoCorasickDoubleArrayTrie.IHit<CoreDictionary.Attribute> processor, String acid)
    {
        if (trie != null)
        {
            trie.parseText(text, processor);
        }
        String key = AIKey.CUSTOMER_DICTIONARY + ":" + acid;
		byte[] batValue = redisApiImpl.get(key.getBytes());
		DoubleArrayTrie<CoreDictionary.Attribute> dat = (DoubleArrayTrie<Attribute>) SerializeUtil.unserialize(batValue);
        DoubleArrayTrie<CoreDictionary.Attribute>.Searcher searcher = dat.getSearcher(text, 0);
        while (searcher.next())
        {
            processor.hit(searcher.begin, searcher.begin + searcher.length, searcher.value);
        }
    }
    
    /**
     * 词典中是否含有词语
     * @param key 词语
     * @return 是否包含
     */
    public boolean contains(String text, String acid) {
    	String key = AIKey.CUSTOMER_DICTIONARY + ":" + acid;
		byte[] batValue = redisApiImpl.get(key.getBytes());
		DoubleArrayTrie<CoreDictionary.Attribute> dat = (DoubleArrayTrie<Attribute>) SerializeUtil.unserialize(batValue);
        if (dat.exactMatchSearch(text) >= 0) {
        	return true;
        } else {
        	return false;
        }
    }

}

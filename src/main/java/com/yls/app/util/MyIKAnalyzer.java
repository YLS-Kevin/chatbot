/**
 * 
 */
package com.yls.app.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.wltea.analyzer.lucene.IKAnalyzer;

/**
 * @author huangsy
 * 2018年1月26日下午2:14:54
 * 基于Lucene和IKAnalyzer的中文分词器工具
 */
public class MyIKAnalyzer {

	/**
	 * @param str 待处理的一句话
	 * @return 返回关键词List
	 * @throws IOException
	 */
	public static List<String> getTermList(String str) {
		//新建分词器
		Analyzer analyzer = new IKAnalyzer(true);
		//使用分词器处理测试字符串
		StringReader reader = new StringReader(str);
		TokenStream tokenStream;
		List<String> termList = new ArrayList<String>();
		try {
			tokenStream = analyzer.tokenStream("", reader);
			//4.0版本以上要reset
			tokenStream.reset();
			CharTermAttribute term = tokenStream.getAttribute(CharTermAttribute.class);
			while (tokenStream.incrementToken()) {
				termList.add(term.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return termList;
	}
	
	/**
	 * @param str 待处理的一句话
	 * @return 返回n个关键词拼接成字符串
	 * @throws IOException
	 */
	public static String getTermString(String str) {
		//新建分词器
		Analyzer analyzer = new IKAnalyzer(true);
		//使用分词器处理测试字符串
		StringReader reader = new StringReader(str);
		TokenStream tokenStream;
		StringBuffer termString = new StringBuffer();
		try {
			tokenStream = analyzer.tokenStream("", reader);
			//4.0版本以上要reset
			tokenStream.reset();
			CharTermAttribute term = tokenStream.getAttribute(CharTermAttribute.class);
			while (tokenStream.incrementToken()) {
				termString.append(term.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return termString.toString();
	}
	
}

/**
 * 
 */
package com.yls.app.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangsy
 * 20182018年2月1日下午3:46:01
 */
public class Combination {
	
	private List<String> result;
	
	public List<String> getResult() {
		return this.result;
	}
	
	public void combination(List<String> chs) {
		result = new ArrayList<String>();
		if (chs == null || chs.size() == 0) {
			return;
		}
		List<String> list = new ArrayList<String>();
		for (int i = 1; i <= chs.size(); i++) {
			combine(chs, 0, i, list);
		}
	}

	// 从字符数组中第begin个字符开始挑选number个字符加入list中
	public void combine(List<String> cs, int begin, int number, List<String> list) {
		if (number == 0) {
//			System.out.println(list.toString());
			result.add(list.toString());
			return;
		}
		if (begin == cs.size()) {
			return;
		}
		list.add(cs.get(begin));
		combine(cs, begin + 1, number - 1, list);
		list.remove((String) cs.get(begin));
		combine(cs, begin + 1, number, list);
	}

	public static void main(String args[]) {
//		String chs[] = { "深圳", "天气", "怎么样" };
		List<String> chs = new ArrayList<String>();
		chs.add("深圳");
		chs.add("天气");
		chs.add("怎么样");
		Combination cc = new Combination();
		cc.combination(chs);
		System.out.println(cc.getResult());
	}
}

/**
 * 
 */
package com.yls.app.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangsy 20182018年2月1日下午3:49:58
 */
public class Permutation {
	private List<String> result;

	public List<String> getResult() {
		return this.result;
	}

	public void permutation(List<String> ss) {
		result = new ArrayList<String>();
		permutation(ss, 0);
	}

	public void permutation(List<String> ss, int i) {

		if (ss == null || i < 0 || i > ss.size()) {
			return;
		}
		if (i == ss.size()) {
			String all = "";
			for (String s : ss) {
				all = all + s;
			}
			result.add(all);
//			System.out.println(all);
		} else {
			for (int j = i; j < ss.size(); j++) {
				String temp = ss.get(j);// 交换前缀,使之产生下一个前缀
				ss.set(j, ss.get(i));
				ss.set(i, temp);
				permutation(ss, i + 1);
				temp = ss.get(j); // 将前缀换回来,继续做上一个的前缀排列.
				ss.set(j, ss.get(i));
				ss.set(i, temp);
			}
		}
	}

	public static void main(String args[]) {
//		String[] ss = { "深圳", "天气", "怎么样" };
		List<String> ss = new ArrayList<String>();
		ss.add("深圳");
		ss.add("天气");
		ss.add("怎么样");
		Permutation per = new Permutation();
		per.permutation(ss);
		per.getResult();
	}
}

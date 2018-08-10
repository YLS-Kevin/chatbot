/**
 * 
 */
package com.yls.app.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/**
 * @author huangsy
 * @date 2018年5月7日下午3:11:44
 */
public class MyDateUtils {
	
	/**
	 * 关于日期的词
	 */
	public static final Map<String, Integer> DAY = new HashMap<String, Integer>(){
		{
			put("大大前天",-4);
			put("大大前日",-4);
			put("大前天",-3);
			put("大前日",-3);
			put("前天",-2);
			put("前日",-2);
			put("昨天",-1);
			put("昨日",-1);
			put("今天",0);
			put("今日",0);
			put("明天",1);
			put("明日",1);
			put("后天",2);
			put("后日",2);
			put("大后天",3);
			put("大后日",3);
			put("大大后天",4);
			put("大大后日",4);
			put("目前",0);
			put("当前",0);
			put("现在",0);
		}
	};
	
	/**
	 * 关于星期的词
	 */
	public static final Map<String, Integer> WEEK = new HashMap<String, Integer>(){
		{
			put("星期一",1);
			put("星期二",2);
			put("星期三",3);
			put("星期四",4);
			put("星期五",5);
			put("星期六",6);
			put("星期日",7);
			put("星期天",7);
			
			put("星期1",1);
			put("星期2",2);
			put("星期3",3);
			put("星期4",4);
			put("星期5",5);
			put("星期6",6);
			
			put("周一",1);
			put("周二",2);
			put("周三",3);
			put("周四",4);
			put("周五",5);
			put("周六",6);
			put("周日",7);
			put("周天",7);
			
			put("周1",1);
			put("周2",2);
			put("周3",3);
			put("周4",4);
			put("周5",5);
			put("周6",6);
			
		}
	};
	
	/**
	 * 关于月份的词
	 */
	public static final Map<String, Integer> MONTH = new HashMap<String, Integer>(){
		{
			put("一月",1);
			put("二月",2);
			put("三月",3);
			put("四月",4);
			put("五月",5);
			put("六月",6);
			put("七月",7);
			put("八月",8);
			put("九月",9);
			put("十月",10);
			put("十一月",11);
			put("十二月",12);
			
			put("1月",1);
			put("2月",2);
			put("3月",3);
			put("4月",4);
			put("5月",5);
			put("6月",6);
			put("7月",7);
			put("8月",8);
			put("9月",9);
			put("10月",10);
			put("11月",11);
			put("12月",12);
			
		}
	};
	
	/**
	 * 关于年份的词
	 */
	public static final Map<String, Integer> YEAR = new HashMap<String, Integer>(){
		{
			put("大大前年",-4);
			put("大前年",-3);
			put("前年",-2);
			put("去年",-1);
			put("今年",0);
			put("明年",1);
			put("后年",2);
			put("大后年",3);
			put("大大后年",4);
		}
	};

	public Date word2Date(String word) {
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		Date date = new Date();
		
		if("".equals(word) || word==null) {
			date = cal.getTime();
		} else if(DAY.containsKey(word)) {
			
		} else if(WEEK.containsKey(word)) {
			
		}
		
		return date;
		
	}

}

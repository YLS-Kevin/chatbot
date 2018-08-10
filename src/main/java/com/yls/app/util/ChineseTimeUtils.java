package com.yls.app.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文时间描述 转换为 yyyy-mm-dd hh:mm:ss
 *
 * @author Administrator
 * @version 2018-3-22
 */
public class ChineseTimeUtils {
    private final static int SATURDAY = 6;
    private final static int SUNDAY = 7;
    private final static Map<String, Integer> MAP_YEAR = new HashMap() {
        {
            put("大前年", -3);
            put("前年", -2);
            put("去年", -1);
            put("今年", 0);
            put("明年", 1);
            put("后年", 2);
            put("大后年", 3);
        }
    };
    private final static Map<String, Integer> MAP_MONTH = new HashMap() {
        {
            put("上上个月", -2);
            put("上上月", -2);
            put("上个月", -1);
            put("上月", -1);
            put("这个月", 0);
            put("这月", 0);
            put("本月", 0);
            put("下个月", 1);
            put("下月", 1);
            put("下下个月", 2);
            put("下下月", 2);
         }
    };
    private final static Map<String, String> MAP_MONTH_DEC = new HashMap() {
        {
            put("一月", "01");
            put("二月",  "02");
            put("三月",  "03");
            put("四月",  "04");
            put("五月",  "05");
            put("六月",  "06");
            put("七月",  "07");
            put("八月",  "08");
            put("九月",  "09");
            put("十月",  "10");
            put("十一月", "11");
            put("十二月", "12");

            put("1月", "01");
            put("2月", "02");
            put("3月","03");
            put("4月", "04");
            put("5月", "05");
            put("6月", "06");
            put("7月", "07");
            put("8月", "08");
            put("9月", "09");
            put("10月", "10");
            put("11月", "11");
            put("12月", "12");
        }
    };

    private final static Map<String, Integer> MAP_WEEK = new HashMap() {
        {
            put("上上周", -2);
            put("上周", -1);
            put("这周", 0);
            put("本周", 0);
            put("下周", 1);
            put("下下周", 2);
        }
    };
    private final static Map<String, Integer> MAP_WEEKDAY = new HashMap() {
        {
            put("周一", 1);
            put("周二", 2);
            put("周三", 3);
            put("周四", 4);
            put("周五", 5);
            put("周六", 6);
            put("周七", 6);
            put("周日", 7);
            put("周天", 7);
            put("星期一", 1);
            put("星期二", 2);
            put("星期三", 3);
            put("星期四", 4);
            put("星期五", 5);
            put("星期六", 6);
            put("星期七", 6);
            put("星期日", 7);
            put("星期天", 7);
            put("礼拜一", 1);
            put("礼拜二", 2);
            put("礼拜三", 3);
            put("礼拜四", 4);
            put("礼拜五", 5);
            put("礼拜六", 6);
            put("礼拜七", 7);
            put("礼拜天", 7);
            put("礼拜日", 7);
            put("一", 1);
            put("二", 2);
            put("三", 3);
            put("四", 4);
            put("五", 5);
            put("六", 6);
            put("七", 6);
            put("日", 7);
            put("周末", 0);// 包含 星期六 星期日
            put("一", 1);
            put("二", 2);
            put("三", 3);
            put("四", 4);
            put("五", 5);
            put("六", 6);
            put("七", 6);
            put("日", 7);

        }
    };
    private final static Map<String, Integer> MAP_DAY = new HashMap() {
        {
            put("大前日", -3);
            put("大前天", -3);
            put("前日", -2);
            put("前天", -2);
            put("昨日", -1);
            put("昨天", -1);
            put("今日", 0);
            put("最近", 0);
            put("今天", 0);
            put("本日", 0);
            put("明日", 1);
            put("明天", 1);
            put("后日", 2);
            put("后天", 2);
            put("大后天", 3);
            put("大后日", 3);
        }
    };
    private final static Map<String, Integer> MAP_FUTUREDAY = new HashMap() {
        {
            put("零", 0);
            put("一", 1);
            put("二", 2);
            put("三", 3);
            put("四", 4);
            put("五", 5);
            put("六", 6);
            put("七", 7);
            put("八",8);
            put("九", 9);
            put("十", 10);
            put("十一", 11);
            put("十二", 12);
            put("十三", 13);
            put("十四", 14);
            put("十五", 15);
            put("0", 0);
            put("1", 1);
            put("2", 2);
            put("3", 3);
            put("4", 4);
            put("5", 5);
            put("6", 6);
            put("7", 7);
            put("8",8);
            put("9", 9);
            put("10", 10);
            put("11", 11);
            put("12", 12);
            put("13", 13);
            put("14", 14);
            put("15", 15);
        }
    };


    /**
     * 获取时间年月
     *
     * @param param
     * @param
     * @return
     */
    private static Map getYearMonth(String param) {
        Map<String, Object> map = new HashMap();
        List list = new ArrayList();


        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        String resYear="";
        String resMonth="";
        if(param.contains("年")) {
            String  year = param.substring(0, param.indexOf("年") + 1);
            boolean isExistYear = MAP_YEAR.containsKey(year);
            if (isExistYear) {
                Integer yearVal = MAP_YEAR.get(year);
                if (yearVal != null) {
                    cal.add(Calendar.YEAR, yearVal);
                     resYear = String.valueOf(cal.get(Calendar.YEAR));
                }else{
                    map.put("result", "fail");
                    map.put("msg", "没有找到“"+param+"”此类型时间,请重新描述！");
                    return map;
                }
            }else{
                resYear = param.substring(0, param.indexOf("年"));
            }
        }
        if(param.contains("月")){
            String month = param.substring(param.indexOf("年") + 1, param.indexOf("月")+1);
            boolean isExistMonth = MAP_MONTH.containsKey(month);
            if (isExistMonth) {
                Integer monthVal = MAP_MONTH.get(month);
                if (monthVal != null) {
                    cal.add(Calendar.MONTH,monthVal);
                    resMonth=String.valueOf(cal.get(Calendar.MONTH));
                }else{
                    map.put("result", "fail");
                    map.put("msg", "没有找到“"+param+"”此类型时间,请重新描述！");
                    return map;
                }
            }
            boolean isExistMonthDec = MAP_MONTH_DEC.containsKey(month);
            if (isExistMonthDec) {
                String monthVal = MAP_MONTH_DEC.get(month);
                if (monthVal != null) {
                    resMonth=monthVal;
                }
            }else{
                map.put("result", "fail");
                map.put("msg", "没有找到“"+param+"”此类型时间,请重新描述！");
                return map;
            }
        }





        list.add(resYear  + resMonth );
        map.put("result", "success");
        map.put("data", list);
        return map;

    }




    /**
     * 获取时间日期
     *
     * @param param
     * @param date
     * @return
     */
    private static String getDayTime(String param, Date date) {
        boolean
                isExistYear = MAP_DAY.containsKey(param);
        Calendar cal =
                Calendar.getInstance();
        cal.setTime(date);
        if (isExistYear) {
            Integer
                    mapVal = MAP_DAY.get(param);
            if (mapVal == null) {
                mapVal = 0;
            }

            cal.add(Calendar.DAY_OF_MONTH, mapVal);

            return String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        }
        return
                String.valueOf(cal.get(Calendar.DAY_OF_MONTH));

    }

    /**
     * 根据周
     *
     * @param week    周
     * @param weekDay 星期几
     * @return
     */
    private static Map getWeekTime(String weekDay, String week) {

        Map<String, Object> map = new HashMap();
        List list = new ArrayList();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());// 设置时间为当前时间


        boolean isExistWeekDay = MAP_WEEKDAY.containsKey(weekDay);
        if (isExistWeekDay) {
            int[] weekDays = {7, 1, 2, 3, 4, 5, 6};
            // 获得当前日期是一个星期的第几天
            int dayWeek = cal.get(Calendar.DAY_OF_WEEK);
            int day = weekDays[dayWeek - 1];// 转换为 中国习惯的实际天数， 中国的习惯一个星期的第一天是星期一
            int weekDayKey = MAP_WEEKDAY.get(weekDay);

            // 周末
            if (0 == weekDayKey) {
                int SatVal = SATURDAY - day;// 得到与星期六 之差
                cal.add(Calendar.DATE, SatVal);
                list.add(formatter.format(cal.getTime()));

                // 周日 直接向后推一天
                cal.add(Calendar.DATE, 1);
                list.add(formatter.format(cal.getTime()));

                map.put("result", "success");
                map.put("data", list);
                return map;
            }
            int val = weekDayKey - day;// 得到二者之差
            boolean isExistYear = MAP_WEEK.containsKey(week);

            // 存在对于周的描述
            if (isExistYear) {
                Integer mapVal = MAP_WEEK.get(week);
                int sum = mapVal * 7 + val;
                cal.add(Calendar.DATE, sum);
                list.add(formatter.format(cal.getTime()));
                map.put("result", "success");
                map.put("data", list);
                return map;
                // 不存在，但询问星期X比当前星期X大，默认为本周
            } else if (val >= 0) {
                cal.add(Calendar.DATE, val);
                list.add(formatter.format(cal.getTime()));
                map.put("result", "success");
                map.put("data", list);
                return map;
                // 不存在，且询问星期X比当前星期X小
            } else {
                map.put("result", "fail");// 请指定具体是那周，如（本周，下周）
                map.put("msg", "请指定具体是那周，如（本周，下周）");

                return map;
            }
        } else {
            map.put("result", "fail");
            map.put("msg", "不存在有关“周”的描述中不存在" + weekDay + "！");
            return map;
        }

    }

    /**
     * 根据 天 获取时间
     *
     * @param param
     * @return
     */
    private static Map getDayTime(String param) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Object> map = new HashMap<>();
        List list = new ArrayList();
        boolean isExistYear = MAP_DAY.containsKey(param);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());// 默认设置为 当前时间
        if (isExistYear) {
            Integer mapVal = MAP_DAY.get(param);
            if (mapVal == null) {
                mapVal = 0;
            }
            cal.add(Calendar.DAY_OF_MONTH, mapVal);
            list.add(formatter.format(cal.getTime()));
            map.put("result", "success");
            map.put("data", list);
            return map;
        } else {
            map.put("result", "fail");
            map.put("msg", "有关“天”的描述中不存在" + param + "！");
            return map;
        }

    }

    /**
     * 未来X天 接下来X天
     * 中文
     *
     * @param param
     * @return
     */
    private static Map getFutureDay(String param) {
        Map<String, Object> map = new HashMap();
        int day = 0;
        if (MAP_FUTUREDAY.get(param) == null || "".equals(MAP_FUTUREDAY.get(param))) {
            day = 3;
        } else {
            day = (int) MAP_FUTUREDAY.get(param);
        }

        List list = new ArrayList();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());// 设置时间为当前时间

        if (0 < day) {
            for (int i = 1; i <= day; i++) {
                cal.add(Calendar.DATE, i);
                list.add(formatter.format(cal.getTime()));
                cal.setTime(new Date());

            }
            map.put("result", "success");
            map.put("data", list);
            return map;
        } else if (0 == day) {
            list.add(formatter.format(cal.getTime()));
            map.put("result", "success");
            map.put("data", list);
            return map;
        } else {
            map.put("result", "fail");
            map.put("msg", "时间参数错误，参数必须大于等于0");
            return map;
        }
    }

    /**
     * 未来X小时 接下来x小时
     *
     * @param param
     * @return
     */
    private static Map getFutureHour(int param) {
        Map<String, Object> map = new HashMap();
        List list = new ArrayList();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());// 设置时间为当前时间
        if (0 < param) {
            for (int i = 1; i <= param; i++) {
                cal.add(Calendar.HOUR_OF_DAY, i);// 24小时制
                list.add(formatter.format(cal.getTime()));
                cal.setTime(new Date());
            }
            map.put("result", "success");
            map.put("data", list);
            return map;
        } else if (0 == param) {
            list.add(formatter.format(cal.getTime()));
            map.put("result", "success");
            map.put("data", list);
            return map;
        } else {
            map.put("result", "fail");
            map.put("msg", "时间参数错误，参数必须大于等于0");
            return map;
        }
    }

    /**
     * XXX年XX月XX日
     *
     * @param parameter
     * @return
     */
    private static Map getYearMonthDay(String parameter) {
        Map<String, Object> map = new HashMap();
        List list = new ArrayList();
        String year = parameter.substring(0, parameter.indexOf("年"));

        String month = parameter.substring(parameter.indexOf("年") + 1, parameter.indexOf("月"));
        if (1 == month.length()) {
            month = "0" + month;
        }
        String day = parameter.substring(parameter.indexOf("月") + 1, parameter.indexOf("日"));
        if (1 == day.length()) {
            day = "0" + day;
        }
        list.add(year + "-" + month + "-" + day);
        map.put("result", "success");
        map.put("data", list);
        return map;
    }



    /**
     * 未来X天 接下来X天
     * 英文
     *
     * @param day
     * @return
     */
    public static Map<String, Object> getFutureIntDay(int day) {
        Map<String, Object> map = new HashMap();
        List list = new ArrayList();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date());// 设置时间为当前时间

        if (0 < day) {
            for (int i = 1; i <= day; i++) {
                cal.add(Calendar.DATE, i);
                list.add(formatter.format(cal.getTime()));
                cal.setTime(new Date());

            }
            map.put("result", "success");
            map.put("data", list);
            return map;
        } else if (0 == day) {
            list.add(formatter.format(cal.getTime()));
            map.put("result", "success");
            map.put("data", list);
            return map;
        } else {
            map.put("result", "fail");
            map.put("msg", "时间参数错误，参数必须大于等于0");
            return map;
        }
    }
    /**
     * 根据中文时间描述，解析成对应时间
     *
     * @param parameter
     * @return
     */
    public static Map getDateTime(String parameter) {
        Map<String, Object> map = new HashMap();
        //先排除单独只询问 星期的
        if (parameter.contains("周")) {
            if (MAP_WEEKDAY.get(parameter) != null && !"".equals(MAP_WEEKDAY.get(parameter))) {
                map = getWeekTime(parameter, null);
            } else {
                String week = parameter.substring(0, parameter.indexOf("周") + 1);
                String weekDay = parameter.substring(parameter.indexOf("周") + 1, parameter.length());
                map = getWeekTime(weekDay, week);
            }
            return map;
        } else if (parameter.contains("未来") && parameter.contains("天")) {
            String day = parameter.substring(parameter.indexOf("天") - 1, parameter.indexOf("天"));
            if(day.contains("几")||day.contains("多")){
                map.put("result", "fail");
                map.put("msg", "请具体说明是未来多少天。");
                return map;
            }
            return getFutureDay(day);
        } else if (parameter.contains("接下来") && parameter.contains("天")) {
            String day = parameter.substring(parameter.indexOf("天") - 1, parameter.indexOf("天"));
            if(day.contains("几")||day.contains("多")){
                map.put("result", "fail");
                map.put("msg", "请具体说明是接下来多少天。");
                return map;
            }
            return getFutureDay(day);
        } else if (parameter.contains("未来") && parameter.contains("小时")) {
            return map;
        } else if (parameter.contains("接下来") && parameter.contains("小时")) {
            return map;

        }else if(parameter.contains("晚")||parameter.contains("昼")||parameter.contains("夜")||parameter.contains("白天")||parameter.contains("凌晨")||parameter.contains("中午")||parameter.contains("下午")||parameter.contains("黄昏")){
            String day=parameter.substring(0,2);
            if(!day.contains("天")&&!day.contains("日")){
                day="今天";
            }
            map = getDayTime(day);
            return map;
        } else if (parameter.contains("天")||parameter.contains("日")||parameter.contains("最近")) {
            map = getDayTime(parameter);
            return map;
        } else if (parameter.contains("年") && parameter.contains("月") && parameter.contains("日")) {
            map = getYearMonthDay(parameter);
            return map;
        } else if (parameter.contains("年")  ) {
            map = getYearMonth(parameter);
            return map;
        } else if (parameter.contains("月")  ) {
            map = getYearMonth(parameter);
            return map;
        } else {
            map.put("result", "fail");
            map.put("msg", "没有找到“"+parameter+"”此类型时间,请重新描述！");
            return map;
        }
    }


}

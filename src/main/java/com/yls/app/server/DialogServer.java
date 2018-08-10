/**
 * 
 */
package com.yls.app.server;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yls.app.entity.DialogMoreAndOne2;
import com.yls.app.persistence.mapper.DialogCacheMapper;
import com.yls.app.repository.AIKey;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * 对话服务
 * @author huangsy
 * @date 2018年6月6日下午5:07:34
 */
@Service
public class DialogServer {
	
	private final static Logger logger = Logger.getLogger(DialogServer.class);
	
	@Resource
	private JedisPool pool;
	
	@Resource
	private DialogCacheMapper dialogCacheMapper;
	
	/**
	 * 添加对话到redis
	 * @return
	 * @throws Exception 
	 */
	public boolean addDialog2Redis(String id_d) throws Exception {
		boolean flag = true;
		List<DialogMoreAndOne2> dms =  dialogCacheMapper.findDialogById(id_d);
		if(dms.size()==0) {
			logger.info("DialogServer新增，实时缓存对话接口被调用，调用失败，id："+id_d);
			throw new Exception();
		}
		Jedis jedis = pool.getResource();
		//redis事务新增
		Transaction tx = jedis.multi();
		this.save(dms, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DialogServer新增，实时缓存对话接口被调用，调用成功，id："+id_d);
		return flag;
	}
	
	/**
	 * 从redis删除指定的对话
	 * @return
	 * @throws Exception 
	 */
	public boolean delDialogFromRedis(String id_d) throws Exception {
		boolean flag = true;
		List<DialogMoreAndOne2> dms =  dialogCacheMapper.findDialogById4Del(id_d);
		if(dms.size()==0) {
			logger.info("DialogServer删除，实时缓存对话接口被调用，调用失败，id："+id_d);
			throw new Exception();
		}
		Jedis jedis = pool.getResource();
		//redis事务删除
		Transaction tx = jedis.multi();
		this.del(dms, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DialogServer删除，实时缓存对话接口被调用，调用成功，id："+id_d);
		return flag;
	}
	
	/**
	 * 修改redis指定对话
	 * @param dmJson
	 * @return
	 */
	public boolean updateDialogFromRedis(String dmJson, String id_d) {
		boolean flag = true;
		//json转数据实体
		Jedis jedis = pool.getResource();
		Type type = new TypeToken<List<DialogMoreAndOne2>>(){}.getType();
		List<DialogMoreAndOne2> dms2Del = new Gson().fromJson(dmJson, type);
		List<DialogMoreAndOne2> dms2Add =  dialogCacheMapper.findDialogById(id_d);
		//redis事务删除
		Transaction tx = jedis.multi();
		this.del(dms2Del, tx);
		//redis事务新增
		this.save(dms2Add, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DialogServer修改，实时缓存对话接口被调用，调用成功，id："+id_d);
		return flag;
	}
	
	/**
	 * 获取要删除的对话
	 * @param dm
	 */
	public void del(List<DialogMoreAndOne2> dms, Transaction tx) {
		for(DialogMoreAndOne2 dm : dms) {
			String template = "";
			String type = "0";
			String that = "";
	
			// 拼接type字段
			if (dm.getUrl() != null && !"".equals(dm.getUrl())) {
				type = "1";
				if(dm.getIs_front_call() != null && !"".equals(dm.getIs_front_call())) {
					if("1".equals(dm.getIs_front_call())) {//是否是前端调用, 1:是, 0:否.   默认:0
						type = "2";
					}
				}
			}
			// 拼接that字段
			that = "";
			
			// 拼接template字段
			String answer = dm.getAnswer();
			String url = dm.getUrl();
			// 固定类型
			if (answer != null) {
				template = answer;
			}
			// 接口类型
			if (url != null && !"".equals(url)) {
				String pk = "";// url名值对
				String para1 = dm.getAword1para();
				String para2 = dm.getAword2para();
				String para3 = dm.getAword3para();
				String para4 = dm.getAword4para();
				String para5 = dm.getAword5para();
				if (para1 != null && !"".equals(para1)) {
					para1 = para1 + "=" + dm.getAword1dyna();
					pk = para1;
				}
				if (para2 != null && !"".equals(para2)) {
					para2 = para2 + "=" + dm.getAword2dyna();
					pk = pk + "&" + para2;
				}
				if (para3 != null && !"".equals(para3)) {
					para3 = para3 + "=" + dm.getAword3dyna();
					pk = pk + "&" + para3;
				}
				if (para4 != null && !"".equals(para4)) {
					para4 = para4 + "=" + dm.getAword4dyna();
					pk = pk + "&" + para4;
				}
				if (para5 != null && !"".equals(para5)) {
					para5 = para5 + "=" + dm.getAword5dyna();
					pk = pk + "&" + para5;
				}
				if (url.contains("?")) {
					template = url + "&" + pk;
				} else {
					template = url + "?" + pk;
				}
			}
	
			// 添加scripts脚本
			String scripts = "";
			if (dm.getScripts() != null && !"".equals(dm.getScripts())) {
				scripts = dm.getScripts();
			}
			String stype = "";
			if (dm.getStype() != null && !"".equals(dm.getStype())) {
				stype = dm.getStype();
			}
			String respara = "";
			if (dm.getRepara() != null && !"".equals(dm.getRepara())) {
				respara = dm.getRepara();
			}
			String sin = "";
			if (dm.getSin() != null && !"".equals(dm.getSin())) {
				sin = dm.getSin();
	
			}
			String sinword = "";
			if (dm.getSinword() != null && !"".equals(dm.getSinword())) {
				sinword = dm.getSinword();
			}
	
			Map<String, String> script = new HashMap<String, String>();
			script.put("scripts", scripts);
			script.put("stype", stype);
			script.put("respara", respara);
			script.put("sin", sin);
			script.put("sinword", sinword);
	
			//对话id
			String id_d = dm.getId();
			
			//aptype为1是模糊查询，为2是关键词查询
			String aptype = dm.getAptype();
			if("1".equals(aptype)) {
				//拼接pattern字段
				String pattern = dm.getAword();
				//创建key value 然后缓存到redis
				Map<String, Object> kv = this.createKV(pattern, template, type, that, script, id_d, dm, aptype);
				String key = (String)kv.get("key");
				String value = (String)kv.get("value");
	//			List<Answer> toDels = this.toDelFromRedis(key, value);
				this.delFromRedis(key, value, tx);
	//			this.delSentence(pattern, dm, tx);
			} else {
				List<String> patterns = this.combinPatterns(dm);
				for (String pattern : patterns) {
					//创建key value 然后缓存到redis
					Map<String, Object> kv = this.createKV(pattern, template, type, that, script, id_d, dm, aptype);
					String key = (String)kv.get("key");
					String value = (String)kv.get("value");
	//				List<Answer> toDels = this.toDelFromRedis(key, value);
					this.delFromRedis(key, value, tx);
					this.delSentence(pattern, dm, tx);
				}
			}
		}
	}
	
	/**
	 * 保存对话
	 * @param dm
	 */
	public void save(List<DialogMoreAndOne2> dms, Transaction tx) {
		for(DialogMoreAndOne2 dm : dms) {
			String template = "";
			String type = "0";
			String that = "";
	
			// 拼接type字段
			if (dm.getUrl() != null && !"".equals(dm.getUrl())) {
				type = "1";
				if(dm.getIs_front_call() != null && !"".equals(dm.getIs_front_call())) {
					if("1".equals(dm.getIs_front_call())) {//是否是前端调用, 1:是, 0:否.   默认:0
						type = "2";
					}
				}
			}
			// 拼接that字段
			that = "";
			
			// 拼接template字段
			String answer = dm.getAnswer();
			String url = dm.getUrl();
			// 固定类型
			if (answer != null) {
				template = answer;
			}
			// 接口类型
			if (url != null && !"".equals(url)) {
				String pk = "";// url名值对
				String para1 = dm.getAword1para();
				String para2 = dm.getAword2para();
				String para3 = dm.getAword3para();
				String para4 = dm.getAword4para();
				String para5 = dm.getAword5para();
				if (para1 != null && !"".equals(para1)) {
					para1 = para1 + "=" + dm.getAword1dyna();
					pk = para1;
				}
				if (para2 != null && !"".equals(para2)) {
					para2 = para2 + "=" + dm.getAword2dyna();
					pk = pk + "&" + para2;
				}
				if (para3 != null && !"".equals(para3)) {
					para3 = para3 + "=" + dm.getAword3dyna();
					pk = pk + "&" + para3;
				}
				if (para4 != null && !"".equals(para4)) {
					para4 = para4 + "=" + dm.getAword4dyna();
					pk = pk + "&" + para4;
				}
				if (para5 != null && !"".equals(para5)) {
					para5 = para5 + "=" + dm.getAword5dyna();
					pk = pk + "&" + para5;
				}
				if (url.contains("?")) {
					template = url + "&" + pk;
				} else {
					template = url + "?" + pk;
				}
			}
	
			// 添加scripts脚本
			String scripts = "";
			if (dm.getScripts() != null && !"".equals(dm.getScripts())) {
				scripts = dm.getScripts();
			}
			String stype = "";
			if (dm.getStype() != null && !"".equals(dm.getStype())) {
				stype = dm.getStype();
			}
			String respara = "";
			if (dm.getRepara() != null && !"".equals(dm.getRepara())) {
				respara = dm.getRepara();
			}
			String sin = "";
			if (dm.getSin() != null && !"".equals(dm.getSin())) {
				sin = dm.getSin();
	
			}
			String sinword = "";
			if (dm.getSinword() != null && !"".equals(dm.getSinword())) {
				sinword = dm.getSinword();
			}
	
			Map<String, String> script = new HashMap<String, String>();
			script.put("scripts", scripts);
			script.put("stype", stype);
			script.put("respara", respara);
			script.put("sin", sin);
			script.put("sinword", sinword);
			
			String id_d = dm.getId();
			
			//aptype为1是模糊查询，为2是关键词查询
			String aptype = dm.getAptype();
			if("1".equals(aptype)) {
				//拼接pattern字段
				String pattern = dm.getAword();
				//创建key value 然后缓存到redis
				Map<String, Object> kv = this.createKV(pattern, template, type, that, script, id_d, dm, aptype);
				String key = (String)kv.get("key");
				String value = (String)kv.get("value");
				this.push2Redis(key, value, tx);
	//			this.addSentence(pattern, dm, tx);
			} else {
				List<String> patterns = this.combinPatterns(dm);
				for (String pattern : patterns) {
					//创建key value 然后缓存到redis
					Map<String, Object> kv = this.createKV(pattern, template, type, that, script, id_d, dm, aptype);
					String key = (String)kv.get("key");
					String value = (String)kv.get("value");
					this.push2Redis(key, value, tx);
					this.addSentence(pattern, dm, tx);
				}
			}
		}
	}
	
	/**
	 * 删除句式
	 * @param pattern
	 * @param dm
	 * @param tx
	 */
	private void delSentence(String pattern, DialogMoreAndOne2 dm, Transaction tx) {
		//对话id
		String id_d = dm.getId();
		//对话类型id
		String dt = dm.getId_dt();
		//拼接value
		Map<String, String> map = new HashMap<String, String>();
		map.put("sentence", pattern);
		map.put("id_d", id_d);
		String value = new Gson().toJson(map);
		String[] keywords = pattern.split("\\(@\\)");
		for(String keyword : keywords) {
			String key = AIKey.SENTENCE + ":" + dt + ":" + keyword;
			this.delFromRedis(key, value, tx);
		}
	}
	
	/**
	 * 添加句式
	 * @param pattern
	 * @param dm
	 * @param tx
	 */
	private void addSentence(String pattern, DialogMoreAndOne2 dm, Transaction tx) {
		//对话类型id
		String dt = dm.getId_dt();
		//对话id
		String id_d = dm.getId();
		//拼接value
		Map<String, String> map = new HashMap<String, String>();
		map.put("sentence", pattern);
		map.put("id_d", id_d);
		String value = new Gson().toJson(map);
		String[] keywords = pattern.split("\\(@\\)");
		for(String keyword : keywords) {
			String key = AIKey.SENTENCE + ":" + dt + ":" + keyword;
			this.push2Redis(key, value, tx);
		}
	}
	
	/**
	 * 创建key value 然后缓存到redis
	 * @param pattern
	 * @param template
	 * @param type 是否接口
	 * @param that
	 * @param script
	 * @param id_dt 对话类型
	 * @param dm
	 */
	private Map<String, Object> createKV(String pattern, String template, String type, String that, Map<String, String> script, 
			String id_d, DialogMoreAndOne2 dm, String aptype) {
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("pattern", pattern);
		map.put("template", template);
		map.put("type", type);
		map.put("that", that);
		map.put("script", script);
		map.put("id_d", id_d);
		map.put("is_contain_kw", dm.getIs_contain_kw());
		
		//对话类型id
		String id_dt = dm.getId_dt();
		//cid_m_id_dt不空，说明该对话是通用模块自定义对话触发对话
		String cid_m_id_dt = dm.getCid_m_id_dt();
		if(cid_m_id_dt!=null && !"".contentEquals(cid_m_id_dt)) {
			id_dt = cid_m_id_dt;
		}
		String mul_dialog_type = dm.getMul_dialog_type();
		String key = "";
		//aptype为1是模糊查询，为2是关键词查询
		if("1".equals(aptype)) {
			//mul_dialog_type为4说明是多伦入口主题，atype为5说明是多伦对话主题
			if("5".equals(mul_dialog_type)) {
				//多伦对话主题id
				String id_ap = dm.getId_ap();
				map.put("id_ap", id_ap);
				key = AIKey.AWORD_MORENEXT + ":" + id_dt + ":" + id_ap + ":" + pattern; 
			} else if("4".equals(mul_dialog_type)) {
				//多伦入口主题id
				String id_ap = dm.getId_ap();
				map.put("id_ap", id_ap);
				key = AIKey.AWORD_MOREFIRST + ":" + id_dt + ":" + pattern;
			} else {
				map.put("id_ap", "");
				key = AIKey.AWORD_ONE + ":" + id_dt + ":" + pattern;
			}
		} else {
			//如果是多伦对话主题，需要区分主题id
			if("5".equals(mul_dialog_type)) {
				//多伦对话主题id
				String id_ap = dm.getId_ap();
				map.put("id_ap", id_ap);
				key = AIKey.DIALOG_MORENEXT + ":" + id_dt + ":" + id_ap + ":" + pattern; 
			} else if("4".equals(mul_dialog_type)) {
				//多伦入口主题id
				String id_ap = dm.getId_ap();
				map.put("id_ap", id_ap);
				key = AIKey.DIALOG_MOREFIRST + ":" + id_dt + ":" + pattern;
			} else {
				map.put("id_ap", "");
				key = AIKey.DIALOG_ONE + ":" + id_dt + ":" + pattern;
			}
		}
		
		String value = new Gson().toJson(map);
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("key", key);
		result.put("value", value);
		return result;
	}
	
	/**
	 * 组合pattern
	 * 
	 * @param tdm
	 * @return
	 */
	private List<String> combinPatterns(DialogMoreAndOne2 tdm) {

		String k1type = tdm.getAword1type();
		String k2type = tdm.getAword2type();
		String k3type = tdm.getAword3type();
		String k4type = tdm.getAword4type();
		String k5type = tdm.getAword5type();

		String k1 = tdm.getAword1();
		String k2 = tdm.getAword2();
		String k3 = tdm.getAword3();
		String k4 = tdm.getAword4();
		String k5 = tdm.getAword5();

		String aword1near = tdm.getAword1near();
		String aword2near = tdm.getAword2near();
		String aword3near = tdm.getAword3near();
		String aword4near = tdm.getAword4near();
		String aword5near = tdm.getAword5near();

		List<String> near1list = new ArrayList<String>();
		if (k1type != null && !"".equals(k1type)) {
			if ("1".equals(k1type)) {
				if (k1 != null && !"".equals(k1)) {
					if (aword1near != null && !"".equals(aword1near)) {
						String[] near1 = aword1near.split("\\|");
						near1list = new ArrayList<String>(Arrays.asList(near1));
						near1list.add(k1);
					} else {
						near1list.add(k1);
					}
				}
			} else if ("2".equals(k1type)) {
				String dyna1 = tdm.getAword1dyna();
				if (dyna1 != null && !"".equals(dyna1)) {
					near1list.add(dyna1);
				}
			}

		}

		List<String> near2list = new ArrayList<String>();
		if (k2type != null && !"".equals(k2type)) {
			if ("1".equals(k2type)) {
				if (k2 != null && !"".equals(k2)) {
					if (aword2near != null && !"".equals(aword2near)) {
						String[] near2 = aword2near.split("\\|");
						near2list = new ArrayList<String>(Arrays.asList(near2));
						near2list.add(k2);
					} else {
						near2list.add(k2);
					}
				}
			} else if ("2".equals(k2type)) {
				String dyna2 = tdm.getAword2dyna();
				if (dyna2 != null && !"".equals(dyna2)) {
					near2list.add(dyna2);
				}
			}

		}

		List<String> near3list = new ArrayList<String>();
		if (k3type != null && !"".equals(k3type)) {
			if ("1".equals(k3type)) {
				if (k3 != null && !"".equals(k3)) {
					if (aword3near != null && !"".equals(aword3near)) {
						String[] near3 = aword3near.split("\\|");
						near3list = new ArrayList<String>(Arrays.asList(near3));
						near3list.add(k3);
					} else {
						near3list.add(k3);
					}
				}
			} else if ("2".equals(k3type)) {
				String dyna3 = tdm.getAword3dyna();
				if (dyna3 != null && !"".equals(dyna3)) {
					near3list.add(dyna3);
				}
			}

		}

		List<String> near4list = new ArrayList<String>();
		if (k4type != null && !"".equals(k4type)) {
			if ("1".equals(k4type)) {
				if (k4 != null && !"".equals(k4)) {
					if (aword4near != null && !"".equals(aword4near)) {
						String[] near4 = aword4near.split("\\|");
						near4list = new ArrayList<String>(Arrays.asList(near4));
						near4list.add(k4);
					} else {
						near4list.add(k4);
					}
				}
			} else if ("2".equals(k4type)) {
				String dyna4 = tdm.getAword4dyna();
				if (dyna4 != null && !"".equals(dyna4)) {
					near4list.add(dyna4);
				}
			}

		}

		List<String> near5list = new ArrayList<String>();
		if (k5type != null && !"".equals(k5type)) {
			if ("1".equals(k5type)) {
				if (k5 != null && !"".equals(k5)) {
					if (aword5near != null && !"".equals(aword5near)) {
						String[] near5 = aword5near.split("\\|");
						near5list = new ArrayList<String>(Arrays.asList(near5));
						near5list.add(k5);
					} else {
						near5list.add(k5);
					}
				}
			} else if ("2".equals(k5type)) {
				String dyna5 = tdm.getAword5dyna();
				if (dyna5 != null && !"".equals(dyna5)) {
					near5list.add(dyna5);
				}
			}

		}

		List<List<String>> trees = new ArrayList<List<String>>();
		if (near1list != null && near1list.size() > 0) {
			trees.add(near1list);
		}
		if (near2list != null && near2list.size() > 0) {
			trees.add(near2list);
		}
		if (near3list != null && near3list.size() > 0) {
			trees.add(near3list);
		}
		if (near4list != null && near4list.size() > 0) {
			trees.add(near4list);
		}
		if (near5list != null && near5list.size() > 0) {
			trees.add(near5list);
		}
		ArrayList<String> result = new ArrayList<String>();
		manyTree(trees, 0, "", result);
		return result;
	}
	
	/**
	 * 多叉树遍历组合pattern
	 * 
	 * @param trees
	 * @param i
	 * @param temp
	 * @param result
	 */
	private void manyTree(List<List<String>> trees, int i, String temp, List<String> result) {
		if (trees.size() == i) {
			result.add(temp);
			return;
		} else {
			List<String> root = trees.get(i);
			for (String leaf : root) {
				manyTree(trees, i + 1, temp + leaf + AIKey.SEPARATOR, result);
			}
		}

	}
	
	/**
	 * 获取多少秒之后的时间戳
	 * @return
	 */
	private long getStamp(int after) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.SECOND, after);
		return calendar.getTime().getTime();
	}
	
	/**
	 * 把对应键值对推送到redis服务器
	 * @param key
	 * @param value
	 */
	private void push2Redis(String key, String value, Transaction tx) {
		
		long stamp = this.getStamp(0);
		tx.zadd(key, stamp, value);
		
	}
	
	/**
	 * 从redis删除指定键值对
	 * @param key
	 * @param value
	 */
	private void delFromRedis(String key, String value, Transaction tx) {
		
		tx.zrem(key, value);
		
	}

}

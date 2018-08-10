/**
 * 
 */
package com.yls.app.server;

import java.lang.reflect.Type;
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
import com.yls.app.entity.DWordGroup;
import com.yls.app.persistence.mapper.WordCacheMapper;
import com.yls.app.repository.AIKey;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * 动态词服务
 * @author huangsy
 * @date 2018年6月12日下午4:40:51
 */
@Service
public class DynaServer {
	
	private final static Logger logger = Logger.getLogger(DynaServer.class);
	
	@Resource
	private JedisPool pool;
	
	@Resource
	private WordCacheMapper wordCacheMapper;
	
	/**
	 * 添加对话到redis
	 * @return
	 * @throws Exception 
	 */
	public boolean addDyna2Redis(String id) throws Exception {
		boolean flag = true;
		List<DWordGroup> dWordGroups = wordCacheMapper.findWordTypeById(id);
		if(dWordGroups.size() == 0) {
			logger.info("DynaServer新增，实时缓存动态词组接口被调用，调用失败，id："+id);
			throw new Exception();
		}
		Jedis jedis = pool.getResource();
		//redis事务新增
		Transaction tx = jedis.multi();
		this.save(dWordGroups, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DynaServer新增，实时缓存动态词组接口被调用，调用成功，id："+id);
		return flag;
	}
	
	/**
	 * 保存动态词组类型
	 * @param dWordGroup
	 * @param tx
	 */
	private void save(List<DWordGroup> dWordGroups, Transaction tx) {
		for(DWordGroup dWordGroup : dWordGroups) {
			String group_name = dWordGroup.getGroup_name();
			String id_ac = dWordGroup.getId_ac();
			String key = AIKey.DYNA + ":" + id_ac;
			
			Map<String, String> map = new HashMap<String, String>();
			map.put("type", "(&"+group_name+"&)");
			String value = new Gson().toJson(map);
			
			this.push2Redis(key, value, tx);
		}
	}
	
	/**
	 * 从redis删除指定的对话
	 * @return
	 * @throws Exception 
	 */
	public boolean delDynaFromRedis(String id) throws Exception {
		boolean flag = true;
		List<DWordGroup> dWordGroups = wordCacheMapper.findWordTypeById(id);
		if(dWordGroups.size() == 0) {
			logger.info("DynaServer删除，实时缓存动态词组接口被调用，调用失败，id："+id);
			throw new Exception();
		}
		Jedis jedis = pool.getResource();
		//redis事务删除
		Transaction tx = jedis.multi();
		this.del(dWordGroups, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DynaServer删除，实时缓存动态词组接口被调用，调用成功，id："+id);
		return flag;
	}
	
	/**
	 * @param dWordGroups
	 * @param tx
	 */
	private void del(List<DWordGroup> dWordGroups, Transaction tx) {
		for(DWordGroup dWordGroup : dWordGroups) {
			String group_name = dWordGroup.getGroup_name();
			String id_ac = dWordGroup.getId_ac();
			String key = AIKey.DYNA + ":" + id_ac;
			
			Map<String, String> map = new HashMap<String, String>();
			map.put("type", "(&"+group_name+"&)");
			String value = new Gson().toJson(map);
			
			this.delFromRedis(key, value, tx);
		}
	}
	
	/**
	 * 修改redis指定对话
	 * @param dmJson
	 * @return
	 */
	public boolean updateDynaFromRedis(String dynaJson, String id) {
		boolean flag = true;
		//json转数据实体
		Jedis jedis = pool.getResource();
		Type type = new TypeToken<List<DWordGroup>>(){}.getType();
		List<DWordGroup> dms2Del = new Gson().fromJson(dynaJson, type);
		List<DWordGroup> dms2Add = wordCacheMapper.findWordTypeById(id);
		//redis事务删除
		Transaction tx = jedis.multi();
		this.del(dms2Del, tx);
		//redis事务新增
		this.save(dms2Add, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DynaServer修改，实时缓存动态词组接口被调用，调用成功，id："+id);
		return flag;
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

}

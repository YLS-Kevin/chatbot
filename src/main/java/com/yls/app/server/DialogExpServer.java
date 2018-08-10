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
import com.yls.app.entity.DialogExp;
import com.yls.app.persistence.mapper.DialogCacheMapper;
import com.yls.app.repository.AIKey;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * 异常应答服务
 * @author huangsy
 * @date 2018年6月12日下午3:14:03
 */
@Service
public class DialogExpServer {
	
	private final static Logger logger = Logger.getLogger(DialogExpServer.class);
	
	@Resource
	private JedisPool pool;
	
	@Resource
	private DialogCacheMapper dialogCacheMapper;
	
	/**
	 * 添加异常应答到redis
	 * @return
	 * @throws Exception 
	 */
	public boolean addDialogExp2Redis(String id) throws Exception {
		boolean flag = true;
		List<DialogExp> dms =  dialogCacheMapper.findDialogExpById(id);
		if(dms.size() == 0) {
			logger.info("DialogExpServer新增，实时缓存异常应答接口被调用，调用失败，id："+id);
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
		logger.info("DialogExpServer新增，实时缓存异常应答接口被调用，调用成功，id："+id);
		return flag;
	}
	
	/**
	 * 保存异常应答到redis
	 * @param dialogExp
	 * @param tx
	 */
	private void save(List<DialogExp> dms, Transaction tx) {
		for(DialogExp dialogExp : dms) {
			String cid = dialogExp.getCid();
			String answer = dialogExp.getAnswer();
			String stype = dialogExp.getStype();//异常类型，类型：1-无答案时，2-接口异常时，3-系统出错时
			
			Map<String, String> map = new HashMap<String, String>();
			map.put("pattern", AIKey.EXP_ANSWER);
			map.put("template", answer);
			map.put("type", "0");
			map.put("that", "");
	
			String key = AIKey.EXP_ANSWER + ":" + cid + ":" + stype;
			String value = new Gson().toJson(map);
			
			this.push2Redis(key, value, tx);
		}
	}
	
	/**
	 * 从redis删除异常应答
	 * @return
	 * @throws Exception 
	 */
	public boolean delDialogExpFromRedis(String id) throws Exception {
		boolean flag = true;
		List<DialogExp> dms =  dialogCacheMapper.findDialogExpById(id);
		if(dms.size() == 0) {
			logger.info("DialogExpServer删除，实时缓存异常应答接口被调用，调用失败，id："+id);
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
		logger.info("DialogExpServer删除，实时缓存异常应答接口被调用，调用成功，id："+id);
		return flag;
	}
	
	/**
	 * 从redis删除异常应答
	 * @param dialogExp
	 * @param tx
	 */
	private void del(List<DialogExp> dms, Transaction tx) {
		for(DialogExp dialogExp : dms) {
			String cid = dialogExp.getCid();
			String answer = dialogExp.getAnswer();
			String stype = dialogExp.getStype();//异常类型，类型：1-无答案时，2-接口异常时，3-系统出错时
			
			Map<String, String> map = new HashMap<String, String>();
			map.put("pattern", AIKey.EXP_ANSWER);
			map.put("template", answer);
			map.put("type", "0");
			map.put("that", "");
	
			String key = AIKey.EXP_ANSWER + ":" + cid + ":" + stype;
			String value = new Gson().toJson(map);
			
			this.delFromRedis(key, value, tx);
		}
	}
	
	/**
	 * 从redis修改异常应答
	 * @return
	 */
	public boolean updateDialogExpFromRedis(String deJson, String id) {
		boolean flag = true;
		//json转数据实体
		Jedis jedis = pool.getResource();
		Type type = new TypeToken<List<DialogExp>>(){}.getType();
		List<DialogExp> dms2Del = new Gson().fromJson(deJson, type);
		List<DialogExp> dms2Add =  dialogCacheMapper.findDialogExpById(id);
		//redis事务删除
		Transaction tx = jedis.multi();
		this.del(dms2Del, tx);
		//redis事务新增
		this.save(dms2Add, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DialogExpServer修改，实时缓存异常应答接口被调用，调用成功，id："+id);
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

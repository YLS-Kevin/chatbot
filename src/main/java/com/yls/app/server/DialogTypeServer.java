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
import com.yls.app.entity.DialogType2;
import com.yls.app.persistence.mapper.ClientCacheMapper;
import com.yls.app.repository.AIKey;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * 对话库类型服务
 * @author huangsy
 * @date 2018年6月11日下午5:17:22
 */
@Service
public class DialogTypeServer {
	
	private final static Logger logger = Logger.getLogger(DialogTypeServer.class);

	@Resource
	private JedisPool pool;
	
	@Resource
	private ClientCacheMapper clientCacheMapper;
	
	/**
	 * 添加对话库类型到redis
	 * @return
	 * @throws Exception 
	 */
	public boolean addDialogType2Redis(String id) throws Exception {
		boolean flag = true;
		List<DialogType2> clientDialogTypes = clientCacheMapper.findDialogTypeById(id);
		if(clientDialogTypes.size() == 0) {
			logger.info("DialogTypeServer新增，实时缓存对话类型接口被调用，调用失败，id："+id);
			throw new Exception();
		}
		Jedis jedis = pool.getResource();
		//redis事务新增
		Transaction tx = jedis.multi();
		this.save(clientDialogTypes, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DialogTypeServer新增，实时缓存对话类型接口被调用，调用成功，id："+id);
		return flag;
	}
	
	/**
	 * 从redis删除指定的对话库类型
	 * @return
	 * @throws Exception 
	 */
	public boolean delDialogTypeFromRedis(String id) throws Exception {
		boolean flag = true;
		List<DialogType2> clientDialogTypes = clientCacheMapper.findDialogTypeById(id);
		if(clientDialogTypes.size() == 0) {
			logger.info("DialogTypeServer删除，实时缓存对话类型接口被调用，调用失败，id："+id);
			throw new Exception();
		}
		Jedis jedis = pool.getResource();
		//redis事务删除
		Transaction tx = jedis.multi();
		this.del(clientDialogTypes, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DialogTypeServer删除，实时缓存对话类型接口被调用，调用成功，id："+id);
		return flag;
	}
	
	/**
	 * 修改redis指定对话库类型
	 * @param dmJson
	 * @return
	 */
	public boolean updateDialogTypeFromRedis(String dtJson, String id) {
		boolean flag = true;
		//json转数据实体
		Jedis jedis = pool.getResource();
		Type type = new TypeToken<List<DialogType2>>(){}.getType();
		List<DialogType2> dms2Del = new Gson().fromJson(dtJson, type);
		List<DialogType2> dms2Add = clientCacheMapper.findDialogTypeById(id);
		//redis事务删除
		Transaction tx = jedis.multi();
		this.del(dms2Del, tx);
		//redis事务新增
		this.save(dms2Add, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("DialogTypeServer修改，实时缓存对话类型接口被调用，调用成功，id："+id);
		return flag;
	}
	
	/**
	 * 保存对话库类型
	 * @param clientDialogType
	 * @param tx
	 */
	private void save(List<DialogType2> clientDialogTypes, Transaction tx) {
		for(DialogType2 clientDialogType : clientDialogTypes) {
			String cid = clientDialogType.getCid();
			String cid_m = clientDialogType.getCid_m();
			String dialogType = clientDialogType.getId_dt();
			String key = AIKey.CLIENT_DIALOGTYPE + ":" + cid + ":" + cid_m;
			
			String[] dts = dialogType.split(",");
			for(String dt : dts) {
				if(!"".equals(dt)) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("dialogType", dt);
					String value = new Gson().toJson(map);
					this.push2Redis(key, value, tx);
				}
			}
		}
	}
	
	/**
	 * 保存对话库类型
	 * @param clientDialogType
	 * @param tx
	 */
	private void del(List<DialogType2> clientDialogTypes, Transaction tx) {
		for(DialogType2 clientDialogType : clientDialogTypes) {
			String cid = clientDialogType.getCid();
			String cid_m = clientDialogType.getCid_m();
			String dialogType = clientDialogType.getId_dt();
			String key = AIKey.CLIENT_DIALOGTYPE + ":" + cid + ":" + cid_m;
			
			String[] dts = dialogType.split(",");
			for(String dt : dts) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("dialogType", dt);
				String value = new Gson().toJson(map);
				
				this.delFromRedis(key, value, tx);
			}
		}
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

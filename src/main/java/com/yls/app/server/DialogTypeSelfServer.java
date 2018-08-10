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
import com.yls.app.entity.DialogType2;
import com.yls.app.persistence.mapper.ClientCacheMapper;
import com.yls.app.persistence.mapper.DialogCacheMapper;
import com.yls.app.repository.AIKey;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * @author huangsy
 * @date 2018年6月21日下午5:14:01
 */
@Service
public class DialogTypeSelfServer {

	private final static Logger logger = Logger.getLogger(DialogTypeSelfServer.class);

	@Resource
	private JedisPool pool;

	@Resource
	private ClientCacheMapper clientCacheMapper;
	
	@Resource
	private DialogCacheMapper dialogCacheMapper;
	
	@Resource
	private DialogServer dialogServer;

	/**
	 * 添加对话库类型到redis
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean addDialogTypeSelf2Redis(String cid_m) throws Exception {
		boolean flag = true;
		List<DialogType2> clientDialogTypes = clientCacheMapper.findDialogTypeSelfById(cid_m);
		if (clientDialogTypes.size() == 0) {
			logger.info("DialogTypeSelfServer新增，实时缓存自定义-对话类型接口被调用，调用失败，id：" + cid_m);
			throw new Exception();
		}
		Jedis jedis = pool.getResource();
		// redis事务新增
		Transaction tx = jedis.multi();
		this.save(clientDialogTypes, tx);
		tx.exec();
		if (jedis != null) {
			jedis.close();
		}
		logger.info("DialogTypeSelfServer新增，实时缓存自定义-对话类型接口被调用，调用成功，id：" + cid_m);
		return flag;
	}

	/**
	 * 从redis删除指定的对话库类型
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean delDialogTypeSelfFromRedis(String cid_m) throws Exception {
		boolean flag = true;
		//查询要删除的模块
		List<DialogType2> clientDialogTypes = clientCacheMapper.findDialogTypeSelfById(cid_m);
		if (clientDialogTypes.size() == 0) {
			logger.info("DialogTypeSelfServer删除，实时缓存自定义-对话类型接口被调用，调用失败，id：" + cid_m);
			throw new Exception();
		}
		//查询要删除的自定义对话
		List<String> id_dList = clientCacheMapper.findDialogIdByCid_m(cid_m);
		List<DialogMoreAndOne2> toDel = new ArrayList<DialogMoreAndOne2>();
		for(String id_d : id_dList) {
			List<DialogMoreAndOne2> dms =  dialogCacheMapper.findDialogById(id_d);
			toDel.addAll(dms);
		}
		
		Jedis jedis = pool.getResource();
		// redis事务删除
		Transaction tx = jedis.multi();
		this.del(clientDialogTypes, tx);
		dialogServer.del(toDel, tx);
		tx.exec();
		if (jedis != null) {
			jedis.close();
		}
		logger.info("DialogTypeSelfServer删除，实时缓存自定义-对话类型接口被调用，调用成功，id：" + cid_m);
		return flag;
	}
	
	/**
	 * 修改redis指定对话库类型
	 * 
	 * @param dmJson
	 * @return
	 */
	public boolean updateDialogTypeSelfFromRedis(String dtJson, String cid_m) {
		boolean flag = true;
		// json转数据实体
		Jedis jedis = pool.getResource();
		Type type = new TypeToken<List<DialogType2>>() {}.getType();
		List<DialogType2> dms2Del = new Gson().fromJson(dtJson, type);
		List<DialogType2> dms2Add = clientCacheMapper.findDialogTypeSelfById(cid_m);
		// redis事务删除
		Transaction tx = jedis.multi();
		this.del(dms2Del, tx);
		// redis事务新增
		this.save(dms2Add, tx);
		tx.exec();
		if (jedis != null) {
			jedis.close();
		}
		logger.info("DialogTypeSelfServer修改，实时缓存自定义-对话类型接口被调用，调用成功，id：" + cid_m);
		return flag;
	}

	/**
	 * 保存对话库类型
	 * 
	 * @param clientDialogType
	 * @param tx
	 */
	private void save(List<DialogType2> clientDialogTypes, Transaction tx) {
		for (DialogType2 clientDialogType : clientDialogTypes) {
			String cid = clientDialogType.getCid();
			String cid_m = clientDialogType.getCid_m();
			String dialogType = clientDialogType.getId_dt();
			String key = AIKey.CLIENT_DIALOGTYPE + ":" + cid + ":" + cid_m;
	
			String[] dts = dialogType.split(",");
			for (String dt : dts) {
				if (!"".equals(dt)) {
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
	 * 
	 * @param clientDialogType
	 * @param tx
	 */
	private void del(List<DialogType2> clientDialogTypes, Transaction tx) {
		for (DialogType2 clientDialogType : clientDialogTypes) {
			String cid = clientDialogType.getCid();
			String cid_m = clientDialogType.getCid_m();
			String dialogType = clientDialogType.getId_dt();
			String key = AIKey.CLIENT_DIALOGTYPE + ":" + cid + ":" + cid_m;
	
			String[] dts = dialogType.split(",");
			for (String dt : dts) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("dialogType", dt);
				String value = new Gson().toJson(map);
	
				this.delFromRedis(key, value, tx);
			}
		}
	}

	/**
	 * 把对应键值对推送到redis服务器
	 * 
	 * @param key
	 * @param value
	 */
	private void push2Redis(String key, String value, Transaction tx) {

		long stamp = this.getStamp(0);
		tx.zadd(key, stamp, value);

	}

	/**
	 * 从redis删除指定键值对
	 * 
	 * @param key
	 * @param value
	 */
	private void delFromRedis(String key, String value, Transaction tx) {

		tx.zrem(key, value);

	}

	/**
	 * 获取多少秒之后的时间戳
	 * 
	 * @return
	 */
	private long getStamp(int after) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.SECOND, after);
		return calendar.getTime().getTime();
	}

}

/**
 * 
 */
package com.yls.app.server;

import java.lang.reflect.Type;
import java.util.List;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yls.app.entity.DialogMoreAndOne2;
import com.yls.app.persistence.mapper.DialogCacheMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * 数据接口服务
 * @author huangsy
 * @date 2018年6月27日上午9:52:52
 */
@Service
public class InterDataServer {

private final static Logger logger = Logger.getLogger(DialogServer.class);
	
	@Resource
	private JedisPool pool;
	
	@Resource
	private DialogCacheMapper dialogCacheMapper;
	
	@Resource
	private DialogServer dialogServer;
	
	/**
	 * 数据接口修改的时候需要修改对应的对话
	 * @param dmJson
	 * @return
	 */
	public boolean updateDialogFromRedis(String dmJson, String id) {
		boolean flag = true;
		//json转数据实体
		Jedis jedis = pool.getResource();
		Type type = new TypeToken<List<DialogMoreAndOne2>>(){}.getType();
		List<DialogMoreAndOne2> dms2Del = new Gson().fromJson(dmJson, type);
		List<DialogMoreAndOne2> dms2Add =  dialogCacheMapper.findDialogByInterDataId(id);
		//redis事务删除
		Transaction tx = jedis.multi();
		dialogServer.del(dms2Del, tx);
		//redis事务新增
		dialogServer.save(dms2Add, tx);
		tx.exec();
		if(jedis!=null) {
			jedis.close();
		}
		logger.info("InterDataServer修改，实时缓存对话接口被调用，调用成功，id："+id);
		return flag;
	}
	
}

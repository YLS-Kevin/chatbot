package com.yls.app.repository;

import java.util.Map;

public interface RedisApi {
	
	public String get(String key);
	
	public String set(String key, String value);
	
	public Long hset(String key, String field, String value);
	
	public String hget(String key, String field);
	
	/*public void expire(String key, int seconds);*/

	public Map<String,String> hgetall(String key);
	
	public void rename(String oldkey, String newkey);

	public void del(String key);

}

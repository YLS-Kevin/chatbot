/**
 * 
 */
package com.yls.app.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.log4j.Logger;

/**
 * @author huangsy
 * @date 2018年4月11日下午2:13:02
 */
public class SerializeUtil {
	
	private static Logger logger = Logger.getLogger(SerializeUtil.class);
	
	public static byte[] serialize(Object object) {

		ObjectOutputStream oos = null;
		ByteArrayOutputStream baos = null;
		try {
			// 序列化
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			byte[] bytes = baos.toByteArray();
			return bytes;
		} catch (Exception e) {
			logger.info("对象序列化失败");
		}
		return null;

	}

	public static Object unserialize(byte[] bytes) {

		ByteArrayInputStream bais = null;
		try {
			// 反序列化
			bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception e) {
			logger.info("对象反序列化失败");
		}
		return null;
	}

}

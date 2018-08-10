/**
 * 
 */
package com.yls.app.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import com.yls.app.entity.Word;
import com.yls.app.persistence.mapper.KeyWordMapper;

/**
 * @author huangsy
 * @date 20182018年2月11日下午5:30:03
 */
@Component
public class DBUtil {
	
	private final static Logger logger = Logger.getLogger(DBUtil.class);
	
	@Resource
	private KeyWordMapper keyWordMapper;

	public void text2DB() {
		String path = this.getResourcesPath() + File.separator + "data" + 
				File.separator + "CoreNatureDictionary.txt";
		BufferedReader br = null;
		String line = null;
		try {
			int i = 0;
			br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
			while((line = br.readLine())!=null) {
				i++;
				String[] element = line.split(" ");
				Word kw = new Word();
				kw.setId(UUID.randomUUID().toString().replaceAll("-", ""));
				kw.setId_ac("1");
				kw.setWname(element[0]);
				kw.setWx(element[1]);
				kw.setWften(Integer.parseInt(element[2]));
				if(element.length>=5) {
					kw.setWx2(element[3]);
					kw.setWften2(Integer.parseInt(element[4]));
				}
				kw.setAutoin(2);
				kw.setSort(i);
				kw.setRemarks("");
				Date date = new Date();
				kw.setCreate_by("admin");
				kw.setCreate_date(date);
				kw.setUpdate_by("admin");
				kw.setUpdate_date(date);
				keyWordMapper.insert(kw);
			}
		} catch (Exception e) {
			logger.error("关键词入库出错",e);
//			e.printStackTrace();
		}
		
	}
	
	private String getResourcesPath() {
		File currDir = new File(".");
		String path = currDir.getAbsolutePath();
		path = path.substring(0, path.length() - 2);
//		System.out.println(path);
		String resourcesPath = path + File.separator + "src" + File.separator + "main" + File.separator + "resources";
		return resourcesPath;
	}
	
}

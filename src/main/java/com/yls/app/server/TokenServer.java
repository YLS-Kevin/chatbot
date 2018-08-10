/**
 * 
 */
package com.yls.app.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;

/**
 * 权限服务
 * @author huangsy
 * @date 2018年3月9日下午5:32:40
 */
@Service
public class TokenServer {
	
	private final static Logger logger = Logger.getLogger(TokenServer.class);
	
	@Value("#{chatbot.tokenUrl}")
	private String tokenUrl;
	
	/**
	 * 权限验证方法
	 * @param token
	 * @param client_id
	 * @return
	 */
	public int verify(String token, String client_id) {
		
		int flag = 0;
		String result = this.doPost(tokenUrl, token, client_id);
		Map<String, String> json = new Gson().fromJson(result, Map.class);
		String error_des = json.get("error_description");
		if(error_des != null) {
			if("Token has expired".equals(error_des)) {
				flag = 1;
			} else {
				flag = 2;
			}
		}
		return flag;//flag 0为验证通过；1为token过期验证失败；2为其它验证失败
		
	}
	
	/**
	 * 传入url，直接返回调用接口的结果
	 * @param url
	 * @return
	 */
	private String doPost(String url, String token, String client_id) {
		
		String jsonResult = "";
		// 创建一个httpclient对象
		CloseableHttpClient httpClient = HttpClients.createDefault();
		// 创建一个POST对象
		HttpPost post = new HttpPost(url);
		//装填参数  
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();  
        nvps.add(new BasicNameValuePair("token", token));
        nvps.add(new BasicNameValuePair("client_id", client_id));
        //设置参数到请求对象中  
		// 执行请求
		CloseableHttpResponse response = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
			response = httpClient.execute(post);
			// 取响应的结果
			int statusCode = response.getStatusLine().getStatusCode();
//			System.out.println(statusCode);
			HttpEntity entity = response.getEntity();
			jsonResult = EntityUtils.toString(entity, "UTF-8");
//			System.out.println(jsonResult);
		} catch (Exception e) {
			logger.error("执行doGet出错", e);
		} finally {
			// 关闭httpclient
			if(response!=null) {
				try {
					response.close();
				} catch (Exception e) {
					logger.error("关闭httpclient response出错", e);
				}
			}
			if(httpClient!=null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					logger.error("关闭httpclient httpclient出错", e);
				}
			}
		}
		return jsonResult;
	}

}

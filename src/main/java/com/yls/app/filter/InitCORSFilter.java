/**
 * 
 */
package com.yls.app.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CORSFilter 解决跨域问题
 * @author huangsy
 */
@Component
public class InitCORSFilter extends OncePerRequestFilter {

	private final static Logger logger = Logger.getLogger(InitCORSFilter.class);

	public InitCORSFilter() {
		logger.info("==== 初始化系统允许跨域请求 ====");
	}

	/**
	 * 解决跨域：Access-Control-Allow-Origin，值为*表示服务器端允许任意Domain访问请求
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
//		if (request.getHeader("Access-Control-Request-Method") != null && "OPTIONS".equals(request.getMethod())) {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.addHeader("Access-Control-Allow-Methods", "GET, POST");
			response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, x-requested-with, sid, mycustom, smuser");
			response.addHeader("Access-Control-Max-Age", "1800");// 30 min
//		}
		filterChain.doFilter(request, response);
	}

}

/**
 * 
 */
package com.yls.app.aop;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.yls.app.entity.ClientUser;
import com.yls.app.entity.ClientUserUseLog;
import com.yls.app.persistence.mapper.LogMapper;
import com.yls.app.server.ChatServer;

/**
 * @author huangsy
 * @date 2018年3月19日上午10:16:20
 */
@Component
@Aspect
public class LogAspect {
	
	private final static Logger logger = Logger.getLogger(LogAspect.class);
	
	@Resource
	private LogMapper logMapper;
	
	@Resource
	private ChatServer chatServer;
	
	@Pointcut("execution(* com.yls.app.server.ChatServer.respond(..))")
//	@Pointcut("execution(* com.yls.app.controller.ChatBotController.api(..))")
	public void pointCut() {}
	
	@After("pointCut()")
	public  void after(JoinPoint joinPoint) {
		
//		HttpServletRequest request = this.getHttpServletRequest();
//		String info = request.getParameter("info");
//		String cid = request.getParameter("cid");
//		String sbid = request.getParameter("sbid");
		
		String cid = chatServer.getCid();
		String sbid = chatServer.getSbid();
		String cip = chatServer.getCip();
		String isfind = chatServer.getIsfind();
		float lon = 0;
		float lat = 0;
		if(chatServer.getLon()!=null && !"".equals(chatServer.getLon())) {
			lon = Float.parseFloat(chatServer.getLon());
		}
		if(chatServer.getLat()!=null && !"".equals(chatServer.getLat())) {
			lat = Float.parseFloat(chatServer.getLat());
		}
		String scity = chatServer.getScity();
		String saddr = chatServer.getSaddr();
		String mansay = chatServer.getMansay();
		String robotsay = chatServer.getRobotsay();
		String participle = "";
		if(chatServer.getKeywords() != null) {
			participle = chatServer.getKeywords().toString();
		}
		ClientUser find = new ClientUser();
		find.setCid(cid);
		find.setCuserid(sbid);
		List<ClientUser> clientUsers = logMapper.findClientUser(find);
		if(clientUsers.size()>0) {
			//update
			ClientUser clientUser = clientUsers.get(0);
			clientUser.setUsenum(clientUser.getUsenum() + 1);//加一次
			
			ClientUserUseLog clientUserUseLog = new ClientUserUseLog();
			clientUserUseLog.setId(UUID.randomUUID().toString().replaceAll("-", ""));
			clientUserUseLog.setId_cu(clientUser.getId());
			clientUserUseLog.setCip(cip);
			clientUserUseLog.setLon(lon);
			clientUserUseLog.setLat(lat);
			clientUserUseLog.setScity(scity);
			clientUserUseLog.setSaddr(saddr);
			clientUserUseLog.setVdate(new Date());
			clientUserUseLog.setMansay(mansay);
			clientUserUseLog.setRobotsay(robotsay);
			clientUserUseLog.setParticiple(participle);
			clientUserUseLog.setIsfind(isfind);
			this.update(clientUser, clientUserUseLog);
		} else {
			//insert
			ClientUser clientUser = new ClientUser();
			clientUser.setId(UUID.randomUUID().toString().replaceAll("-", ""));
			clientUser.setCid(cid);
			clientUser.setCuserid(sbid);
			clientUser.setCname("");
			clientUser.setUsenum(1);
			clientUser.setCreate_date(new Date());
			clientUser.setUpdate_date(new Date());
			
			ClientUserUseLog clientUserUseLog = new ClientUserUseLog();
			clientUserUseLog.setId(UUID.randomUUID().toString().replaceAll("-", ""));
			clientUserUseLog.setId_cu(clientUser.getId());
			clientUserUseLog.setCip(cip);
			clientUserUseLog.setLon(lon);
			clientUserUseLog.setLat(lat);
			clientUserUseLog.setScity(scity);
			clientUserUseLog.setSaddr(saddr);
			clientUserUseLog.setVdate(new Date());
			clientUserUseLog.setMansay(mansay);
			clientUserUseLog.setRobotsay(robotsay);
			clientUserUseLog.setParticiple(participle);
			clientUserUseLog.setIsfind(isfind);
			this.insert(clientUser, clientUserUseLog);
		}
	}
	
	@Transactional
	private void insert(ClientUser clientUser, ClientUserUseLog clientUserUseLog) {
		logMapper.insertClientUser(clientUser);
		logMapper.insertClientUserUseLog(clientUserUseLog);
	}
	
	@Transactional
	private void update(ClientUser clientUser, ClientUserUseLog clientUserUseLog) {
		logMapper.updateClientUser(clientUser);
		logMapper.insertClientUserUseLog(clientUserUseLog);
	}
	
	private HttpServletRequest getHttpServletRequest(){
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();  
        ServletRequestAttributes sra = (ServletRequestAttributes)ra;  
        HttpServletRequest request = sra.getRequest();
        return request;
    }

}

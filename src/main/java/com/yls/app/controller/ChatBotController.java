/**
 * 
 */
package com.yls.app.controller;

import java.util.HashMap;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.google.gson.Gson;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.yls.app.chatbot.Bot;
import com.yls.app.chatbot.Chat;
import com.yls.app.entity.ChatThat;
import com.yls.app.entity.CutedWord;
import com.yls.app.entity.ResultData;
import com.yls.app.exception.ChatBotException;
import com.yls.app.exception.CutWordException;
import com.yls.app.exception.DialogException;
import com.yls.app.exception.DialogExpException;
import com.yls.app.exception.DialogInterDataException;
import com.yls.app.exception.DialogTypeException;
import com.yls.app.exception.DialogTypeSelfException;
import com.yls.app.exception.DynaException;
import com.yls.app.exception.ReloadCustomDictionaryException;
import com.yls.app.job.PersonalWordExecutorJob;
import com.yls.app.repository.AIKey;
import com.yls.app.repository.impl.RedisApiImpl;
import com.yls.app.server.ChatServer;
import com.yls.app.server.DialogExpServer;
import com.yls.app.server.DialogServer;
import com.yls.app.server.DialogTypeSelfServer;
import com.yls.app.server.DialogTypeServer;
import com.yls.app.server.DynaServer;
import com.yls.app.server.InterDataServer;
import com.yls.app.server.TokenServer;

/**
 * 对话接口
 * @author huangsy
 * @date 20182018年2月9日上午10:23:27
 */
@RequestMapping("/V2")
@Controller
public class ChatBotController {
	
	private final static Logger logger = Logger.getLogger(ChatBotController.class);
	
	@Resource
	private ChatServer chatServer;
	
	@Resource
	private TokenServer tokenServer;
	
	@Resource
	private Bot bot;
	
	@Resource
	private RedisApiImpl redisApiImpl;
	
	@Resource
	private PersonalWordExecutorJob personalWordExecutorJob;
	
	@Resource
	private DialogServer dialogServer;
	
	@Resource
	private DialogTypeServer dialogTypeServer;
	
	@Resource
	private DialogExpServer dialogExpServer;
	
	@Resource
	private DynaServer dynaServer;
	
	@Resource
	private DialogTypeSelfServer dialogTypeSelfServer;
	
	@Resource
	private InterDataServer interDataServer;
	
	/**
	 * 异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({Exception.class})
    @ResponseBody
    public String exceptionHandler(Exception e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo("接口内部异常");
		rd.setRet("0");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**   
     * 对话异常处理
     * ret：1为接口调用成功，2为token过期，3为其它验证失败，4为缺少token参数，5为缺少info参数，6为缺少cid参数，
     * 7为token为空，8为cid为空，9为异常回答，10为图灵api接口回答，0为接口内部异常，11为缺少acid参数，12为acid为空，
     * 13为缺少cid_m参数，14为cid_m为空
     * @return 
     */    
    @ExceptionHandler({ChatBotException.class})
    @ResponseBody
    public String chatBotExceptionHandler(ChatBotException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	@RequestMapping(value = "/api", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String api(HttpServletRequest request) throws ChatBotException {
		String result = "";
		
		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(30);//回话过期时间30秒
		String sessionId = session.getId();
		
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new ChatBotException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new ChatBotException(8, "cid参数不能为空");
			}
		}
		String cid_m = request.getParameter("cid_m");
		if(cid_m==null) {
			throw new ChatBotException(13, "缺少cid_m参数");
		} else {
			if("".equals(cid_m)) {
				throw new ChatBotException(14, "cid_m参数不能为空");
			}
		}
		String sbid = request.getParameter("sbid");
		if(sbid==null) {
			throw new ChatBotException(6, "缺少sbid参数");
		} else {
			if("".equals(sbid)) {
				throw new ChatBotException(8, "sbid参数不能为空");
			}
		}
		String question = request.getParameter("info");
		if(question==null) {
			throw new ChatBotException(5, "缺少info参数");
		}
		String acid = request.getParameter("acid");
		if(acid==null) {
			throw new ChatBotException(11, "缺少acid参数");
		} else {
			if("".equals(acid)) {
				throw new ChatBotException(12, "acid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new ChatBotException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new ChatBotException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new ChatBotException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new ChatBotException(3, "验证失败");
		}
		String lon = request.getParameter("lon");
		if(lon==null || "".equals(lon)) {
			lon = "114.0102867296007";
		}
		String lat = request.getParameter("lat");
		if(lat==null || "".equals(lat)) {
			lat = "22.53913167317708";
		}
		String region = request.getParameter("region");
		if(region==null || "".equals(region)) {
			region = "深圳市";
		}
		String scity = request.getParameter("scity");
		if(scity==null || "".equals(scity)) {
			scity = "深圳市";
		}
		String saddr = request.getParameter("saddr");
		if(saddr==null || "".contentEquals(saddr)) {
			saddr = "福田区";
		}
		String os = request.getParameter("os");
		if(os==null || "".equals(os)) {
			os = "";
		}
		String cip = request.getRemoteAddr();
		
		String key = AIKey.CHAT_THAT + ":" + cid+sbid; 
		String chatThatJson = redisApiImpl.get(key);
		
		if(chatThatJson!=null && !"".equals(chatThatJson)) {
			ChatThat chatThat = new Gson().fromJson(chatThatJson, ChatThat.class);
			Chat chat = new Chat(bot, sessionId);
			chat.setThat(chatThat.getThat());
			chat.setAnswerHistory(chatThat.getAnswerHistory());
			chat.setId_ap(chatThat.getId_ap());
			chat.setKeywordHistory(chatThat.getKeywordHistory());
			
			result = chatServer.respond(chat, question, cid, sbid, lon, lat, scity, saddr, cip, os, acid, region, cid_m);
			String value = chatServer.getChatThatJson();
			redisApiImpl.set(key, value);
			redisApiImpl.expire(key, AIKey.CHATTHAT_TTL);
		} else {
			Chat chat = new Chat(bot, sessionId);
			result = chatServer.respond(chat, question, cid, sbid, lon, lat, scity, saddr, cip, os, acid, region, cid_m);
			String value = chatServer.getChatThatJson();
			redisApiImpl.set(key, value);
			redisApiImpl.expire(key, AIKey.CHATTHAT_TTL);
		}
		return result;
		
	}
	
	/**
	 * 分词异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({CutWordException.class})
    @ResponseBody
    public String cutWordExceptionHandler(CutWordException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**
	 * 分词接口
	 * @param request
	 * @return
	 * @throws CutWordException
	 */
	@RequestMapping(value = "/cutword", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String cutword(HttpServletRequest request) throws CutWordException {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new CutWordException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new CutWordException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new CutWordException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new CutWordException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new CutWordException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new CutWordException(3, "验证失败");
		}
		
		//获取参数
		String question = request.getParameter("info");
		if(question==null) {
			throw new CutWordException(5, "缺少info参数");
		}
		String acid = request.getParameter("acid");
		if(acid==null) {
			throw new CutWordException(11, "缺少acid参数");
		} else {
			if("".equals(acid)) {
				throw new CutWordException(12, "acid参数不能为空");
			}
		}
		
		logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.info("分词接口被调用，账号id：" + acid);
		logger.info("需要分词的话：" + question);
		
		//分词处理
		List<Term> keyTerms = bot.cutWord3(question, acid);
		if(keyTerms.size() == 0) {
			Term t = new Term(question, Nature.n);
			keyTerms.add(t);
		}
		
		//构造返回json
		CutedWord cutedWord = new CutedWord();
		cutedWord.setSize(keyTerms.size());
		cutedWord.setTerms(keyTerms);
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(cutedWord);
		String result = new Gson().toJson(rd);
		
		logger.info("提取关键词返回：" + result);
		
		return result;
		
	}
	
	/**
	 * 重新加载词库异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({ReloadCustomDictionaryException.class})
    @ResponseBody
    public String reloadCustomDictionaryExceptionHandler(ReloadCustomDictionaryException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**
	 * 触发重新加载个性词典
	 * @param request
	 * @return
	 * @throws ReloadCustomDictionaryException
	 */
//	@RequestMapping(value = "/reloadCustomDictionary", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String reloadCustomDictionary(HttpServletRequest request) throws ReloadCustomDictionaryException {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new ReloadCustomDictionaryException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new ReloadCustomDictionaryException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new ReloadCustomDictionaryException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new ReloadCustomDictionaryException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new ReloadCustomDictionaryException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new ReloadCustomDictionaryException(3, "验证失败");
		}
		
		logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.info("重新加载个性化词典");
		//调用job缓存个性化词典
		personalWordExecutorJob.execute();
		
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		String result = new Gson().toJson(rd);
		return result;
		
	}
	
	/**
	 * 实时更新redis对话，异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({DialogException.class})
    @ResponseBody
    public String dialogExceptionHandler(DialogException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**
	 * 实时插入对话到redis
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/addDialog", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String addDialog(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogException(3, "验证失败");
		}
		
		//获取对话id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogServer.addDialog2Redis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 实时从redis删除指定对话
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/delDialog", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String delDialog(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogException(3, "验证失败");
		}
		
		//获取对话id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogServer.delDialogFromRedis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 实时修改redis指定对话
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/updateDialog", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String updateDialog(HttpServletRequest request) throws DialogException {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogException(3, "验证失败");
		}
		
		//获取对话id
		String dmJson = request.getParameter("dm");
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogServer.updateDialogFromRedis(dmJson, id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 实时更新redis对话库类型，异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({DialogTypeException.class})
    @ResponseBody
    public String dialogTypeExceptionHandler(DialogTypeException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**
	 * 实时插入对话类型到redis
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/addDialogType", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String addDialogType(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogTypeException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogTypeException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogTypeException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogTypeException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogTypeException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogTypeException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogTypeServer.addDialogType2Redis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 从redis实时删除对话类型
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/delDialogType", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String delDialogType(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogTypeException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogTypeException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogTypeException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogTypeException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogTypeException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogTypeException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogTypeServer.delDialogTypeFromRedis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 从redis实时删除对话类型
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/updateDialogType", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String updateDialogType(HttpServletRequest request) throws DialogTypeException {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogTypeException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogTypeException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogTypeException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogTypeException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogTypeException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogTypeException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String dtJson = request.getParameter("dt");
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogTypeServer.updateDialogTypeFromRedis(dtJson, id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 实时更新redis异常应答，异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({DynaException.class})
    @ResponseBody
    public String dynaExceptionHandler(DynaException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**
	 * 实时插入对话类型到redis
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/addDyna", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String addDyna(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DynaException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DynaException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DynaException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DynaException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DynaException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DynaException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dynaServer.addDyna2Redis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 从redis实时删除异常应答
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/delDyna", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String delDyna(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DynaException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DynaException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DynaException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DynaException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DynaException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DynaException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dynaServer.delDynaFromRedis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 从redis实时删除对话类型
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/updateDyna", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String updateDyna(HttpServletRequest request) throws DynaException {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DynaException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DynaException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DynaException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DynaException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DynaException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DynaException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String dynaJson = request.getParameter("dyna");
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dynaServer.updateDynaFromRedis(dynaJson, id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 实时更新redis异常应答，异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({DialogExpException.class})
    @ResponseBody
    public String dialogExpExceptionHandler(DialogExpException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**
	 * 实时插入对话类型到redis
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/addDialogExp", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String addDialogExp(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogExpException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogExpException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogExpException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogExpException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogExpException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogExpException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogExpServer.addDialogExp2Redis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 从redis实时删除异常应答
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/delDialogExp", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String delDialogExp(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogExpException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogExpException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogExpException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogExpException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogExpException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogExpException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogExpServer.delDialogExpFromRedis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 从redis实时删除对话类型
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/updateDialogExp", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String updateDialogExp(HttpServletRequest request) throws DialogExpException {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogExpException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogExpException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogExpException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogExpException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogExpException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogExpException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String deJson = request.getParameter("de");
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogExpServer.updateDialogExpFromRedis(deJson, id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 实时更新redis对话库类型，异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({DialogTypeSelfException.class})
    @ResponseBody
    public String dialogTypeSelfExceptionHandler(DialogTypeSelfException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**
	 * 实时插入对话类型到redis
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/addDialogTypeSelf", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String addDialogTypeSelf(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogTypeSelfException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogTypeSelfException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogTypeSelfException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogTypeSelfException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogTypeSelfException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogTypeSelfException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogTypeSelfServer.addDialogTypeSelf2Redis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 从redis实时删除对话类型
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/delDialogTypeSelf", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String delDialogTypeSelf(HttpServletRequest request) throws Exception {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogTypeSelfException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogTypeSelfException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogTypeSelfException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogTypeSelfException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogTypeSelfException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogTypeSelfException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogTypeSelfServer.delDialogTypeSelfFromRedis(id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 从redis实时删除对话类型
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/updateDialogTypeSelf", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String updateDialogTypeSelf(HttpServletRequest request) throws DialogTypeSelfException {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogTypeSelfException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogTypeSelfException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogTypeSelfException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogTypeSelfException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogTypeSelfException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogTypeSelfException(3, "验证失败");
		}
		
		//机器人应用人机对话库id
		String dtJson = request.getParameter("dt");
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		dialogTypeSelfServer.updateDialogTypeSelfFromRedis(dtJson, id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}
	
	/**
	 * 实时更新redis对话，异常处理
	 * @param e
	 * @return
	 */
	@ExceptionHandler({DialogInterDataException.class})
    @ResponseBody
    public String dialogInterDataExceptionHandler(DialogInterDataException e) {     
    	logger.error("接口调用异常", e);
		ResultData rd = new ResultData();
		rd.setInfo(e.getMessage());
		rd.setRet(e.getErrorCode()+"");
		rd.setReturnData(new HashMap<String, Object>());
		String result = new Gson().toJson(rd);
        return result;
    }
	
	/**
	 * 实时修改redis指定对话
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/updateDialogInterData", produces = "text/html;charset=utf-8")
	@ResponseBody
	public String updateDialogInterData(HttpServletRequest request) throws DialogInterDataException {
		//权限验证
		String cid = request.getParameter("cid");
		if(cid==null) {
			throw new DialogInterDataException(6, "缺少cid参数");
		} else {
			if("".equals(cid)) {
				throw new DialogInterDataException(8, "cid参数不能为空");
			}
		}
		String token = request.getParameter("token");
		if(token==null) {
			throw new DialogInterDataException(4, "缺少token参数");
		} else {
			if("".equals(token)) {
				throw new DialogInterDataException(7, "token参数不能为空");
			}
		}
		if(tokenServer.verify(token, cid) == 1) {
			throw new DialogInterDataException(2, "token过期");
		} else if(tokenServer.verify(token, cid) == 2) {
			throw new DialogInterDataException(3, "验证失败");
		}
		
		//获取对话id
		String dmJson = request.getParameter("dm");
		String id = request.getParameter("id");
		//调用服务层缓存指定对话id的数据到redis
		interDataServer.updateDialogFromRedis(dmJson, id);
		//构造返回json
		String result = "";
		ResultData rd = new ResultData();
		rd.setInfo("调用成功");
		rd.setRet("1");
		rd.setReturnData(new HashMap<String, String>());
		result = new Gson().toJson(rd);
		return result;
	}

}

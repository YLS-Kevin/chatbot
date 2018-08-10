/**
 * 
 */
package com.yls.app.job;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.yls.app.entity.Account;
import com.yls.app.entity.Word;
import com.yls.app.hanlp.MyCustomDictionary;
import com.yls.app.persistence.mapper.WordCacheMapper;
import com.yls.app.repository.AIKey;
import com.yls.app.repository.impl.RedisApiImpl;

/**
 * @author huangsy
 * @date 2018年3月21日下午3:24:12
 */
@Service
@DisallowConcurrentExecution
public class PersonalWordExecutorJob implements Executor {

	private static Logger logger = Logger.getLogger(PersonalWordExecutorJob.class);
	
	@Resource
	private WordCacheMapper wordCacheMapper;
	
	@Value("#{hanlp.root}")
	private String rootPath;
	
	@Value("#{chatbot.MyDictionaryPath}")
	private String myDictionaryPath;
	
	@Resource
	private RedisApiImpl redisApiImpl;
	
	@Resource
	private MyCustomDictionary myCustomDictionary;
	
	
	/**
	 * 账号词库，词个数
	 */
	private long count;
	
	@Override
	public void execute() {

		logger.info("执行个性化词库缓存====");
		long startTime = System.currentTimeMillis();

		this.handle();

		logger.info("执行个性化词库缓存====结束" + ((System.currentTimeMillis() - startTime) * 0.001) + "s ");

	}

	/**
	 * 处理方法
	 */
	private void handle() {
		this.cacheWord();
	}

	/**
	 * 缓存词典库到本地txt文件，并生成对应的bin文件
	 */
	private void cacheWord() {
		List<Account> accounts = wordCacheMapper.findAllAccount();
		for(Account account : accounts) {
			String id = account.getId();
			this.reLoad(id);
		}
		
	}
	
	private void reLoad(String id) {
		count = 0;
		String idFile = "data/dictionary/custom/" + id + ".txt";
		//生成词典文件
		String temp = rootPath + idFile + "_temp";
		File tempfile = new File(temp);
		if(!tempfile.exists()) {
			if(!tempfile.getParentFile().exists()) {
				tempfile.getParentFile().mkdirs();
			}
			try {
				tempfile.createNewFile();
			} catch (IOException e) {
				logger.error("缓存词库-创建个性化词库字典文件出错", e);
			}
		}
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(tempfile));
			//写入该账号的词
			this.writeWord(bw, id);
			//写入分享的能力的词
			this.writeShareWord(bw, id);
			bw.flush();
		} catch (IOException e) {
			logger.error("缓存个性化词库出错", e);
		} finally {
			try {
				if(bw!=null) {
					bw.close();
				}
			} catch (IOException e) {
				logger.info("个性化词典，关闭文件流出错");
			}
		}
		//词典文件
		String path = rootPath + idFile;
		File pathfile = new File(path);
		this.reName(tempfile, pathfile);
		
		//重新加载词典
		String myPath = rootPath +"data/dictionary/custom/" + id + ".txt";
		List<String> dp2 = new ArrayList<String>();
		dp2.add(myPath);
		String[] dp3 = new String[dp2.size()];
		myCustomDictionary.loadMainDictionary(myPath, dp2.toArray(dp3), id);
		logger.info("重新加载账号："+ id +"的词库，词的个数：" + count);
	}
	
	/**
	 * 重命名
	 * @param oldfile
	 * @param newfile
	 * @return
	 */
	private boolean reName(File oldfile, File newfile) {
		if(!oldfile.exists()) {
			return false;
		} else {
			if(newfile.exists()) {
				newfile.delete();
				return oldfile.renameTo(newfile);
			} else {
				return oldfile.renameTo(newfile);
			}
		}
	}
	
	/**
	 * @param words
	 * @param bw
	 * @throws IOException
	 */
	private void word2Txt(List<Word> words, BufferedWriter bw) throws IOException {
		for(Word wo : words) {
			if(wo!=null) {
				String wname = wo.getWname();
				String synonym = wo.getSynonym();
				if(synonym!=null && !"".equals(synonym)) {
					wname = wname + "," + synonym;
				}
//				List<String> wnames = this.generateWnames(synonym, wname);
				
				String groupname = wo.getGroup_name();
				String group_name = "";
				if(groupname==null||"".equals(groupname)) {
					group_name = AIKey.DEFAUT_DYNA;
				} else {
					group_name = "(&" + groupname + "&)";
				}
				String wx = wo.getWx();
				int wften = wo.getWften() + AIKey.WORD_FREQUENCY;
				String wx2 = wo.getWx2();
				int wften2 = wo.getWften2() + AIKey.WORD_FREQUENCY;
				
				String line = wname + " " + group_name + " " + wx + " " + wften;
				//如果有词性2则拼接上
				if(wx2!=null && !"".equals(wx2)) {
					line = line + " " + wx2 + " " + wften2;
				}
				line = line + "\n";
				bw.write(line);
				//记录词的个数
				count++;
			}
		}
	}
	
	/**
	 * 生成关键词集合
	 * @param synonym
	 * @param wname
	 * @return
	 */
	private List<String> generateWnames(String synonym, String wname) {
		
		List<String> result = new ArrayList<String>();
		
		if(synonym!=null && !"".equals(synonym)) {
			String[] synonyms = synonym.split(",");
			for(String syn : synonyms) {
				result.add(syn);
			}
		}
		result.add(wname);
		
		return result;
		
	}
	
	/**
	 * @param tempfile 词库文件
	 * @param id 账号id
	 * @throws IOException 
	 */
	private void writeWord(BufferedWriter bw, String id) throws IOException {
		List<Word> words = wordCacheMapper.findByAccountId(id);
		this.word2Txt(words, bw);
	}
	
	/**
	 * @param tempfile
	 * @param id
	 * @throws IOException 
	 */
	private void writeShareWord(BufferedWriter bw, String id) throws IOException {
		List<String> dtList = wordCacheMapper.findAllDialogTypeByAccountId(id);
		Set<String> dtSet = new HashSet<String>();
		for(String dts : dtList) {
			if(dts!=null && "".equals(dts)) {
				String[] dtArray = dts.split(",");
				for(String dt : dtArray) {
					dtSet.add(dt);
				}
			}
		}
		List<Word> result = new ArrayList<Word>();
		for(String dt : dtSet) {
			List<Word> words = wordCacheMapper.findAllWordByDialogType(dt);
			List<Word> words2 = wordCacheMapper.findAllWordByDialogType2(dt);
			result.addAll(words);
		    result.addAll(words2);
		}
		LinkedHashSet<Word> set = new LinkedHashSet<Word>(result.size());
	    set.addAll(result);
	    List<Word> result2 = new ArrayList<Word>();
	    result2.addAll(set);
		this.word2Txt(result2, bw);
	}

}

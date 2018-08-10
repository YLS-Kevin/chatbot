/**
 * 
 */
package com.yls.app.persistence.mapper;

import java.util.List;
import com.yls.app.entity.Account;
import com.yls.app.entity.DWord;
import com.yls.app.entity.DWordGroup;
import com.yls.app.entity.Word;

/**
 * @author huangsy
 * @date 2018年3月5日上午9:16:46
 */
public interface WordCacheMapper {
	
	List<Word> findAllWordByDialogType2(String id);

	List<Word> findAllWordByDialogType(String id);
	
	List<String> findAllDialogTypeByAccountId(String id);
	
	List<Word> findByAccountId(String id);
	List<Word> findAutoin();
	
	List<Word> findAll();
	
	List<Word> findCoreWords();
	List<Word> findUserWords();
	List<Word> findCommonWords();
	
	List<Account> findAllAccount();
	
	void insertDWordGroup(DWordGroup dWordGroup);
	void insertDWord(DWord dWord);
	
	List<DWordGroup> findWordTypeById(String id);

}

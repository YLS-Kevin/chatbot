/**
 * 
 */
package com.yls.app.chatbot;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hankcs.hanlp.seg.common.Term;
import com.yls.app.repository.AIKey;

/**
 * @author huangsy
 * @date 2018年7月12日下午4:48:35
 */
public class KeywordHistory {
	private static final Logger log = LoggerFactory.getLogger(KeywordHistory.class);
    private Term[] history;
    private String name;
    private int currentSize;

    /**
     * Constructor with default history name
     */
    public KeywordHistory() {
        this("unknown");
    }

    /**
     * Constructor with history name
     *
     * @param name    name of history
     */
    public KeywordHistory(String name) {
        this.name = name;
        history = new Term[AIKey.MAX_KEYWORD_HISTORY];
    }

    /**
     * add an item to history
     *
     * @param item    history item to add
     */
    public void add(Term item) {
    	for (int j=0;j<currentSize;j++) {
    		if(item.type!=null) {
//    			if(item.type.equals(history[j].type)) {
    			if(this.isSame(item.type, history[j].type)) {
    				history[j] = item;
    				return;
    			}
    		}
    	}
    	for (int i = AIKey.MAX_KEYWORD_HISTORY-1; i > 0; i--) {
    		history[i] = history[i-1];
    	}
    	history[0] = item;
    	currentSize++;
    }
    
    /**
     * 判断两个集合是否有至少一个元素相同
     * @param types1
     * @param types2
     * @return
     */
    private boolean isSame(List<String> types1, List<String> types2) {
    	if(types1!=null && types2!=null) {
    		for(String type1 : types1 ) {
    			for(String type2 : types2) {
    				if(type1.equals(type2)) {
    					return true;
    				}
    			}
    		}
    	}
    	return false;
    }
    
    /**
     * @param item    history item to set
     * @param index   the index to set
     */
    public void set(Term item, int index) {
    	history[index] = item;
    }

    /**
     * get an item from history
     *
     * @param index       history index
     * @return            history item
     */
    public Term get (int index) {
        if (index < AIKey.MAX_KEYWORD_HISTORY) {
            if (history[index] == null) return null;
            else return (Term)history[index];
        }
        else return null;
    }
    
    public int getCurrentSize() {
    	return this.currentSize;
    }
}

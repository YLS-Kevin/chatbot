package com.yls.app.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yls.app.entity.Answer;
import com.yls.app.repository.AIKey;

/**
 * History object to maintain history of input, that request and response
 *
 * @param <T>    type of history object
 */
public class History {
	private static final Logger log = LoggerFactory.getLogger(History.class);
    private Answer[] history;
    private String name;
    private int currentSize;

    /**
     * Constructor with default history name
     */
    public History () {
        this("unknown");
    }

    /**
     * Constructor with history name
     *
     * @param name    name of history
     */
    public History(String name) {
        this.name = name;
        history = new Answer[AIKey.MAX_HISTORY];
    }

    /**
     * add an item to history
     *
     * @param item    history item to add
     */
    public void add(Answer item) {
        for (int i = AIKey.MAX_HISTORY-1; i > 0; i--) {
              history[i] = history[i-1];
        }
        history[0] = item;
        currentSize++;
    }

    /**
     * get an item from history
     *
     * @param index       history index
     * @return            history item
     */
    public Answer get (int index) {
        if (index < AIKey.MAX_HISTORY) {
            if (history[index] == null) return null;
            else return (Answer)history[index];
        }
        else return null;
    }
    
    public int getCurrentSize() {
    	return this.currentSize;
    }

}

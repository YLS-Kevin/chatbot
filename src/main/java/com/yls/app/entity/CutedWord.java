/**
 * 
 */
package com.yls.app.entity;

import java.util.List;

import com.hankcs.hanlp.seg.common.Term;

/**
 * @author huangsy
 * @date 2018年3月16日上午10:55:25
 */
public class CutedWord {

	private int size;
	private List<Term> terms;

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public List<Term> getTerms() {
		return terms;
	}

	public void setTerms(List<Term> terms) {
		this.terms = terms;
	}

}

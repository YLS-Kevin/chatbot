/**
 * 
 */
package com.yls.app.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.hankcs.hanlp.seg.common.Term;

/**
 * @author huangsy 20182018年2月2日上午9:58:11
 */
public class Answer implements Comparable<Answer>, Serializable {

	private String pattern;
	private String template;
	private String type;
	private String that;
	private List<Term> keywords;
	private Map<String, Object> script;
	private String stamp;
	private String code;
	private String id_ap;
	private String id_d;
	private List<String> dynas;
	private String is_contain_kw;

	public Answer() {

	}

	public Answer(String pattern, String template, String type, String that) {
		this.pattern = pattern;
		this.template = template;
		this.type = type;
		this.that = that;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getThat() {
		return that;
	}

	public void setThat(String that) {
		this.that = that;
	}

	public List<Term> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<Term> keywords) {
		this.keywords = keywords;
	}

	public Map<String, Object> getScript() {
		return script;
	}

	public void setScript(Map<String, Object> script) {
		this.script = script;
	}

	public String getStamp() {
		return stamp;
	}

	public void setStamp(String stamp) {
		this.stamp = stamp;
	}

	@Override
	public int compareTo(Answer o) {// 降序排序
		// 按照规定：如果 this 比 o 小，则返回一个负数，如果 this 比 o 大，则返回正数，否则返回0
		return -(this.pattern.length() - o.pattern.length());
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getId_ap() {
		return id_ap;
	}

	public void setId_ap(String id_ap) {
		this.id_ap = id_ap;
	}

	public String getId_d() {
		return id_d;
	}

	public void setId_d(String id_d) {
		this.id_d = id_d;
	}

	public List<String> getDynas() {
		return dynas;
	}

	public void setDynas(List<String> dynas) {
		this.dynas = dynas;
	}

	public String getIs_contain_kw() {
		return is_contain_kw;
	}

	public void setIs_contain_kw(String is_contain_kw) {
		this.is_contain_kw = is_contain_kw;
	}
	
}

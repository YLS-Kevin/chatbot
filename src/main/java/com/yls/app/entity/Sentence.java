/**
 * 
 */
package com.yls.app.entity;

import java.util.Objects;

/**
 * @author huangsy
 * @date 2018年4月2日上午9:23:50
 */
public class Sentence implements Comparable<Sentence> {

	private String sentence;
	private long stamp;
	private double length;
	private String type;

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public long getStamp() {
		return stamp;
	}

	public void setStamp(long stamp) {
		this.stamp = stamp;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	@Override
	public int compareTo(Sentence o) {// 降序排序
		// 按照规定：如果 this 比 o 小，则返回一个负数，如果 this 比 o 大，则返回正数，否则返回0
		double result = this.getLength() - o.getLength();
		if(result > 0) {
			return -1;
		} else if(result == 0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	@Override
	public String toString() {
		return this.sentence + this.type;
	}

	@Override
	public boolean equals(Object obj) {
		// 如果是sentence类型，则要根据sentence判断是否相等
		if (obj instanceof Sentence) {
			if (((Sentence) obj).getSentence().equals(this.sentence)/* && ((Sentence) obj).getType().equals(this.type)*/) {
				return true;
			} else {
				return false;
			}
		} else {
			return super.equals(obj);
		}
//		return obj.toString().equals(this.toString());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.sentence, this.type);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}

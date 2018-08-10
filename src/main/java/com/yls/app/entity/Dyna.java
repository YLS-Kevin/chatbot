/**
 * 
 */
package com.yls.app.entity;

/**
 * @author huangsy
 * @date 2018年5月16日上午9:30:19
 */
public class Dyna {

	private String stamp;
	private String type;

	public String getStamp() {
		return stamp;
	}

	public void setStamp(String stamp) {
		this.stamp = stamp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		// 如果是Dyna类型，则要根据type判断是否相等
		if (obj instanceof Dyna) {
			if (((Dyna) obj).getType().equals(this.type)) {
				return true;
			} else {
				return false;
			}
		} else {
			return super.equals(obj);
		}
	}

}

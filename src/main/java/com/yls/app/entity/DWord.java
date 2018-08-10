/**
 * 
 */
package com.yls.app.entity;

/**
 * @author huangsy
 * @date 2018年5月23日上午11:57:50
 */
public class DWord {

	private String id;
	private String id_ac;
	private String id_dwg;
	private String dwname;
	private int state;
	private int sort;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId_ac() {
		return id_ac;
	}

	public void setId_ac(String id_ac) {
		this.id_ac = id_ac;
	}

	public String getId_dwg() {
		return id_dwg;
	}

	public void setId_dwg(String id_dwg) {
		this.id_dwg = id_dwg;
	}

	public String getDwname() {
		return dwname;
	}

	public void setDwname(String dwname) {
		this.dwname = dwname;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getSort() {
		return sort;
	}

	public void setSort(int sort) {
		this.sort = sort;
	}

}

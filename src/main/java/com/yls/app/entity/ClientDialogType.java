/**
 * 
 */
package com.yls.app.entity;

import java.util.Date;

/**
 * @author huangsy
 * @date 2018年3月7日上午9:04:03
 */
public class ClientDialogType {

	private String id;
	private String cid;
	private int intype;
	private String id_dt;
	private int sort;
	private String create_by;
	private Date create_date;
	private String update_by;
	private Date update_date;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public int getIntype() {
		return intype;
	}

	public void setIntype(int intype) {
		this.intype = intype;
	}

	public String getId_dt() {
		return id_dt;
	}

	public void setId_dt(String id_dt) {
		this.id_dt = id_dt;
	}

	public int getSort() {
		return sort;
	}

	public void setSort(int sort) {
		this.sort = sort;
	}

	public String getCreate_by() {
		return create_by;
	}

	public void setCreate_by(String create_by) {
		this.create_by = create_by;
	}

	public Date getCreate_date() {
		return create_date;
	}

	public void setCreate_date(Date create_date) {
		this.create_date = create_date;
	}

	public String getUpdate_by() {
		return update_by;
	}

	public void setUpdate_by(String update_by) {
		this.update_by = update_by;
	}

	public Date getUpdate_date() {
		return update_date;
	}

	public void setUpdate_date(Date update_date) {
		this.update_date = update_date;
	}

}

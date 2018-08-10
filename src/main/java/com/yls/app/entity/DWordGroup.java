/**
 * 
 */
package com.yls.app.entity;

import java.util.Date;

/**
 * @author huangsy
 * @date 2018年5月23日上午11:51:21
 */
public class DWordGroup {

	private String id;
	private String id_ac;
	private String group_name;
	private String group_cn_name;
	private String is_share;
	private int state;
	private int sort;
	private String remarks;
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

	public String getId_ac() {
		return id_ac;
	}

	public void setId_ac(String id_ac) {
		this.id_ac = id_ac;
	}

	public String getGroup_name() {
		return group_name;
	}

	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}

	public String getGroup_cn_name() {
		return group_cn_name;
	}

	public void setGroup_cn_name(String group_cn_name) {
		this.group_cn_name = group_cn_name;
	}

	public String getIs_share() {
		return is_share;
	}

	public void setIs_share(String is_share) {
		this.is_share = is_share;
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

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
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

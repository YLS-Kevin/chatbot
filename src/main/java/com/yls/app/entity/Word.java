/**
 * 
 */
package com.yls.app.entity;

import java.util.Date;
import java.util.Objects;

/**
 * @author huangsy
 * @date 20182018年2月11日下午5:40:43
 */
public class Word {

	private String id;
	private String id_ac;
	private String wname;
	private int wften;
	private int wften2;
	private String wx;
	private String wx2;
	private int autoin;
	private int state;
	private int sort;
	private String remarks;
	private String create_by;
	private Date create_date;
	private String update_by;
	private Date update_date;
	private String group_name;
	private String synonym;

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

	public String getWname() {
		return wname;
	}

	public void setWname(String wname) {
		this.wname = wname;
	}

	public int getWften() {
		return wften;
	}

	public void setWften(int wften) {
		this.wften = wften;
	}

	public int getWften2() {
		return wften2;
	}

	public void setWften2(int wften2) {
		this.wften2 = wften2;
	}

	public String getWx() {
		return wx;
	}

	public void setWx(String wx) {
		this.wx = wx;
	}

	public String getWx2() {
		return wx2;
	}

	public void setWx2(String wx2) {
		this.wx2 = wx2;
	}

	public int getAutoin() {
		return autoin;
	}

	public void setAutoin(int autoin) {
		this.autoin = autoin;
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

	public String getGroup_name() {
		return group_name;
	}

	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}
	
	public String getSynonym() {
		return synonym;
	}

	public void setSynonym(String synonym) {
		this.synonym = synonym;
	}

	@Override
	public boolean equals(Object obj) {
		// 如果是word类型，则要根据wname判断是否相等
		if (obj instanceof Word) {
			if (((Word) obj).getWname().equals(this.wname)) {
				return true;
			} else {
				return false;
			}
		} else {
			return super.equals(obj);
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.wname);
	}
	
}

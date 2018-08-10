/**
 * 
 */
package com.yls.app.entity;

import java.util.Date;
import java.util.List;

/**
 * @author huangsy
 * @date 2018年3月19日上午10:29:23
 */
public class ClientUser {

	private String id;
	private String cid;
	private String cuserid;
	private String cname;
	private int usenum;
	private Date create_date;
	private Date update_date;

	private List<ClientUserUseLog> clientUserUseLogs;

	public ClientUser() {

	}

	/**
	 * @param id
	 * @param cid
	 * @param cuserid
	 * @param cname
	 * @param usenum
	 * @param create_date
	 * @param update_date
	 */
	public ClientUser(String id, String cid, String cuserid, String cname, int usenum, Date create_date,
			Date update_date) {
		super();
		this.id = id;
		this.cid = cid;
		this.cuserid = cuserid;
		this.cname = cname;
		this.usenum = usenum;
		this.create_date = create_date;
		this.update_date = update_date;
	}

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

	public String getCuserid() {
		return cuserid;
	}

	public void setCuserid(String cuserid) {
		this.cuserid = cuserid;
	}

	public String getCname() {
		return cname;
	}

	public void setCname(String cname) {
		this.cname = cname;
	}

	public int getUsenum() {
		return usenum;
	}

	public void setUsenum(int usenum) {
		this.usenum = usenum;
	}

	public Date getCreate_date() {
		return create_date;
	}

	public void setCreate_date(Date create_date) {
		this.create_date = create_date;
	}

	public Date getUpdate_date() {
		return update_date;
	}

	public void setUpdate_date(Date update_date) {
		this.update_date = update_date;
	}

}

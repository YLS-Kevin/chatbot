/**
 * 
 */
package com.yls.app.entity;

import java.util.Date;

/**
 * @author huangsy
 * @date 2018年3月19日上午10:31:36
 */
public class ClientUserUseLog {

	private String id;
	private String id_cu;
	private String cip;
	private float lon;
	private float lat;
	private String scity;
	private String saddr;
	private Date vdate;
	private String mansay;
	private String robotsay;
	private String participle;
	private String isfind;

	private ClientUser clientUser;

	public ClientUserUseLog() {

	}

	/**
	 * @param id
	 * @param id_cu
	 * @param cip
	 * @param lon
	 * @param lat
	 * @param scity
	 * @param saddr
	 * @param vdate
	 * @param mansay
	 * @param robotsay
	 * @param participle
	 * @param isfind
	 */
	public ClientUserUseLog(String id, String id_cu, String cip, float lon, float lat, String scity, String saddr,
			Date vdate, String mansay, String robotsay, String participle, String isfind) {
		super();
		this.id = id;
		this.id_cu = id_cu;
		this.cip = cip;
		this.lon = lon;
		this.lat = lat;
		this.scity = scity;
		this.saddr = saddr;
		this.vdate = vdate;
		this.mansay = mansay;
		this.robotsay = robotsay;
		this.participle = participle;
		this.isfind = isfind;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId_cu() {
		return id_cu;
	}

	public void setId_cu(String id_cu) {
		this.id_cu = id_cu;
	}

	public String getCip() {
		return cip;
	}

	public void setCip(String cip) {
		this.cip = cip;
	}

	public float getLon() {
		return lon;
	}

	public void setLon(float lon) {
		this.lon = lon;
	}

	public float getLat() {
		return lat;
	}

	public void setLat(float lat) {
		this.lat = lat;
	}

	public String getScity() {
		return scity;
	}

	public void setScity(String scity) {
		this.scity = scity;
	}

	public String getSaddr() {
		return saddr;
	}

	public void setSaddr(String saddr) {
		this.saddr = saddr;
	}

	public Date getVdate() {
		return vdate;
	}

	public void setVdate(Date vdate) {
		this.vdate = vdate;
	}

	public String getMansay() {
		return mansay;
	}

	public void setMansay(String mansay) {
		this.mansay = mansay;
	}

	public String getRobotsay() {
		return robotsay;
	}

	public void setRobotsay(String robotsay) {
		this.robotsay = robotsay;
	}

	public String getParticiple() {
		return participle;
	}

	public void setParticiple(String participle) {
		this.participle = participle;
	}

	public String getIsfind() {
		return isfind;
	}

	public void setIsfind(String isfind) {
		this.isfind = isfind;
	}

	public ClientUser getClientUser() {
		return clientUser;
	}

	public void setClientUser(ClientUser clientUser) {
		this.clientUser = clientUser;
	}

}

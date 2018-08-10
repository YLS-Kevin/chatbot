/**
 * 
 */
package com.yls.app.persistence.mapper;

import java.util.List;

import com.yls.app.entity.ClientUser;
import com.yls.app.entity.ClientUserUseLog;

/**
 * @author huangsy
 * @date 2018年3月19日上午10:34:12
 */
public interface LogMapper {
	
	List<ClientUser> findClientUser(ClientUser clientUser);
	
	void updateClientUser(ClientUser clientUser);
	
	void insertClientUserUseLog(ClientUserUseLog clientUserUseLog);
	
	void insertClientUser(ClientUser clientUser);

}

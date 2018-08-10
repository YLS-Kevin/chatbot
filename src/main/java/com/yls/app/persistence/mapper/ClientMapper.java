/**
 * 
 */
package com.yls.app.persistence.mapper;

import java.util.List;
import com.yls.app.entity.Client;

/**
 * @author huangsy
 * @date 2018年4月13日下午3:51:52
 */
public interface ClientMapper {
	
	List<Client> findClientById(String cid);

}

/**
 * 
 */
package com.yls.app.persistence.mapper;

import java.util.List;
import com.yls.app.entity.DialogType2;

/**
 * @author huangsy
 * @date 2018年3月7日上午9:10:37
 */
public interface ClientCacheMapper {

	List<DialogType2> findDialogTypeById(String id);
	
	List<DialogType2> findDialogTypeSelfById(String cid_m);
	
	List<String> findDialogIdByCid_m(String cid_m);
	
}

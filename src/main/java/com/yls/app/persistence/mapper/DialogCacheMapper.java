/**
 * 
 */
package com.yls.app.persistence.mapper;

import java.util.List;
import com.yls.app.entity.DialogExp;
import com.yls.app.entity.DialogMoreAndOne2;

/**
 * @author huangsy
 * @date 2018年2月26日上午9:19:43
 */
public interface DialogCacheMapper {

	List<DialogMoreAndOne2> findDialogById4Del(String id);
	
	List<DialogMoreAndOne2> findDialogById(String id);
	
	List<DialogExp> findDialogExpById(String id);
	
	List<DialogMoreAndOne2> findDialogByInterDataId(String id);
	
}

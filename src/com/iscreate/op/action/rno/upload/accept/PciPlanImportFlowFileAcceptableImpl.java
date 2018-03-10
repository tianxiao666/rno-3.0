package com.iscreate.op.action.rno.upload.accept;

import java.util.Map;

import org.apache.struts2.ServletActionContext;
import org.springframework.stereotype.Component;

import com.iscreate.op.action.rno.upload.FileAcceptStatus;
import com.iscreate.op.service.publicinterface.SessionService;

@Component(value = "pciPlanImportFlowFileAcceptableImpl")
public class PciPlanImportFlowFileAcceptableImpl extends BaseFileAcceptableImpl {
	@SuppressWarnings("static-access")
	@Override
	public FileAcceptStatus isAcceptable(long size, String dataType, Map<String, Object> attmsg) {
		FileAcceptStatus status = super.isAcceptable(size, dataType, attmsg);
		if (!status.isFlag()) {
			return status;
		} else {
			// 保存session以便在另一进程使用
			SessionService.getInstance().saveSession("session", ServletActionContext.getRequest().getSession());
			SessionService.getInstance().setValueByKey("pciCellFlow", attmsg.get("pciCell"));
		}
		// 判断是否有相应的日期的小区数据
		// 如果没有，需要拒绝
		return status;
	}
}

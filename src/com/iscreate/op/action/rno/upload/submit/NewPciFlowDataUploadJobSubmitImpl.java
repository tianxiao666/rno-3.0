package com.iscreate.op.action.rno.upload.submit;

import org.springframework.stereotype.Controller;

import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask;
import com.iscreate.op.service.publicinterface.SessionService;

@Controller(value = "newPciFlowDataUploadJobSubmit")
public class NewPciFlowDataUploadJobSubmitImpl extends AddDataUploadRecWithoutJobSubmitImpl {
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.iscreate.op.action.rno.upload.submit.DataUploadJobSubmit#submitDataUploadJob(java.lang.String,
	 * com.iscreate.op.pojo.rno.RnoDataCollectRec)
	 */
	@Override
	protected RnoDataCollectRec more2Do(RnoDataCollectRec dataRec) {
		// 在判断器里保存
		RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) SessionService.getInstance().getValueByKey("MRTASKINFO");
		if (taskobj != null) {
			taskobj.getTaskInfo().setFlowDataCollectId(dataRec.getDataCollectId());
		}
		return dataRec;
	}
}

package com.iscreate.op.action.rno.upload.submit;

import org.springframework.stereotype.Controller;

import com.iscreate.op.pojo.rno.RnoDataCollectRec;
@Controller(value="pciFlowDataUploadJobSubmit")
public class PciFlowDataUploadJobSubmitImpl extends BaseDataUploadJobSubmitImpl {
	/* (non-Javadoc)
	 * @see com.iscreate.op.action.rno.upload.submit.DataUploadJobSubmit#submitDataUploadJob(java.lang.String, com.iscreate.op.pojo.rno.RnoDataCollectRec)
	 */
	@Override
	public RnoDataCollectRec submitDataUploadJob(RnoDataCollectRec dataRec) {
		return rnoCommonService.addFlowDataUploadRec(fileCode, dataRec);
	}
}

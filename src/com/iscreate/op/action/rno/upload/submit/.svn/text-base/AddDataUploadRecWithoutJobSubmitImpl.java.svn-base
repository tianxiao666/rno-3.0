package com.iscreate.op.action.rno.upload.submit;

import org.springframework.stereotype.Controller;

import com.iscreate.op.pojo.rno.RnoDataCollectRec;
@Controller(value="addDataUploadRecWithoutJobSubmit")
public abstract class AddDataUploadRecWithoutJobSubmitImpl extends BaseDataUploadJobSubmitImpl {
	/* (non-Javadoc)
	 * @see com.iscreate.op.action.rno.upload.submit.DataUploadJobSubmit#submitDataUploadJob(java.lang.String, com.iscreate.op.pojo.rno.RnoDataCollectRec)
	 */
	@Override
	public final RnoDataCollectRec submitDataUploadJob(RnoDataCollectRec dataRec) {
		dataRec = rnoCommonService.addDataUploadRecWithoutJob(fileCode, dataRec);
		return more2Do(dataRec);
	}
	protected abstract RnoDataCollectRec more2Do(RnoDataCollectRec dataRec);
}

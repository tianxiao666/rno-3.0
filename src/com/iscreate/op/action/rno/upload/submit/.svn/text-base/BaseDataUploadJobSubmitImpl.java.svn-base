package com.iscreate.op.action.rno.upload.submit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.service.rno.RnoCommonService;
@Controller(value="baseDataUploadJobSubmit")
public class BaseDataUploadJobSubmitImpl implements DataUploadJobSubmit {
	// --------注入--------------------//
	@Autowired
	protected RnoCommonService rnoCommonService;
	protected String fileCode = "default";

	@Override
	public void setFileCode(String fileCode) {
		this.fileCode = fileCode;
	}
	/* (non-Javadoc)
	 * @see com.iscreate.op.action.rno.upload.submit.DataUploadJobSubmit#submitDataUploadJob(java.lang.String, com.iscreate.op.pojo.rno.RnoDataCollectRec)
	 */
	@Override
	public RnoDataCollectRec submitDataUploadJob(RnoDataCollectRec dataRec) {
		if("default".equals(fileCode)){
			return null;
		}
		return rnoCommonService.submitDataUploadJob(fileCode, dataRec);
	}
}

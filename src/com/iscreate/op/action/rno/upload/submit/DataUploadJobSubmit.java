package com.iscreate.op.action.rno.upload.submit;

import com.iscreate.op.pojo.rno.RnoDataCollectRec;

public interface DataUploadJobSubmit {

	public void setFileCode(String fileCode);
	/**
	 * 提交文件导入任务
	 * @param dataRec
	 * @return
	 * @author chen.c10	
	 * @date 2016年3月21日
	 * @version RNO 3.0.1
	 */
	public RnoDataCollectRec submitDataUploadJob(RnoDataCollectRec dataRec);
}

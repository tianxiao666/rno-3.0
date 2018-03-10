package com.iscreate.op.action.rno.upload.submit;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask;
import com.iscreate.op.pojo.rno.RnoThreshold;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.RnoLtePciService;
@Controller(value="pciImportDataUploadJobSubmit")
public class PciImportDataUploadJobSubmitImpl extends BaseDataUploadJobSubmitImpl {
	@Autowired
	private RnoLtePciService rnoLtePciService;
	/* (non-Javadoc)
	 * @see com.iscreate.op.action.rno.upload.submit.DataUploadJobSubmit#submitDataUploadJob(java.lang.String, com.iscreate.op.pojo.rno.RnoDataCollectRec)
	 */
	@Override
	public RnoDataCollectRec submitDataUploadJob(RnoDataCollectRec dataRec) {
		dataRec = rnoCommonService.submitDataUploadForCalcJob("RNO_PCI_PLAN_NEW", dataRec);

		SessionService.getInstance();
		HttpSession session = SessionService.getSession();
		// 在判断器里保存
		RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) session.getAttribute("MRTASKINFO");
		String lteCells = session.getAttribute("pciCell").toString();
		
		if (taskobj != null && lteCells != null) {
			List<RnoThreshold> rnoThresholds = taskobj.getRnoThresholds();
			RnoLteInterferCalcTask.TaskInfo taskInfo = taskobj.getTaskInfo(); // 任务信息
			taskInfo.setMatrixDataCollectId(dataRec.getDataCollectId());
			// 保存需要优化的小区列
			taskInfo.setLteCells(lteCells);
			// 更新任务名，由于是导入任务被默认分配了导入的任务名
			rnoLtePciService.submitPciPlanAnalysisTask(dataRec.getAccount(), rnoThresholds, taskInfo);
			rnoCommonService.updateJobNameById(taskInfo.getTaskName(),dataRec.getJobId());
			// 清空session
			session.removeAttribute("MRTASKINFO");
			session.removeAttribute("pciCell");
		}
		return dataRec;
	}
}

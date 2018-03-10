package com.iscreate.op.action.rno.upload.accept;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.iscreate.op.action.rno.upload.FileAcceptStatus;

@Component(value = "eriNcsFileAcceptableImpl")
public class EriNcsFileAcceptableImpl extends BaseFileAcceptableImpl {
	@Override
	public FileAcceptStatus isAcceptable(long size, String dataType, Map<String, Object> attmsg) {
		FileAcceptStatus status = super.isAcceptable(size, dataType, attmsg);
		if (!status.isFlag()) {
			return status;
		}
		// 判断是否有相应的日期的小区数据
		// 如果没有，需要拒绝
		return status;
	}
}

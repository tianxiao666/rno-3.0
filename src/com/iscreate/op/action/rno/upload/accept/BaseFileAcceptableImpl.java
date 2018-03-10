package com.iscreate.op.action.rno.upload.accept;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.iscreate.op.action.rno.upload.FileAcceptStatus;

@Component(value = "baseFileAcceptableImpl")
public class BaseFileAcceptableImpl implements FileAcceptable {
	@Override
	public FileAcceptStatus isAcceptable(long size, String dataType, Map<String, Object> attmsg) {
		return new FileAcceptStatus(true, "");
	}
}

package com.lemonadesergeant.milestones.managers;

import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.PluginLog;

public class MilestoneProgressHelper {

	public MilestoneProgressHelper() {
		PluginLog.info(LogSource.PROGRESS_HELPER, LogStage.SETUP, "component=MilestoneProgressHelper action=init");
	}

	public void recordEvent(NormalizedGameEvent event) {
		PluginLog.info(
			LogSource.PROGRESS_HELPER,
			LogStage.HANDLE,
			"component=MilestoneProgressHelper action=recordEvent eventType=%s fieldCount=%s",
			event == null ? null : event.getType(),
			event == null ? 0 : event.getFields().size()
		);
	}
}
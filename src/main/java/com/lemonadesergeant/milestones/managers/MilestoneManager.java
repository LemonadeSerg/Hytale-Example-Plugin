package com.lemonadesergeant.milestones.managers;

import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.PluginLog;

public class MilestoneManager {

	private final MilestoneProgressHelper progressHelper = new MilestoneProgressHelper();

	public MilestoneManager() {
		PluginLog.info(LogSource.MILESTONE_MANAGER, LogStage.SETUP, "component=MilestoneManager action=init");
	}

	public void recordEvent(NormalizedGameEvent event) {
		PluginLog.info(
			LogSource.MILESTONE_MANAGER,
			LogStage.HANDLE,
			"component=MilestoneManager action=recordEvent eventType=%s",
			event == null ? null : event.getType()
		);
		progressHelper.recordEvent(event);
	}
}
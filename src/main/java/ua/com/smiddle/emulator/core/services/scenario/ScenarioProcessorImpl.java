package ua.com.smiddle.emulator.core.services.scenario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.CallDescriptor;
import ua.com.smiddle.emulator.core.pool.Pools;
import ua.com.smiddle.emulator.core.services.statistic.Statistic;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

/**
 * @author ksa on 21.12.16.
 * @project emulator
 */
@Service("ScenarioProcessorImpl")
@Description("Implementation of ScenarioProcessor")
public class ScenarioProcessorImpl implements ScenarioProcessor {
    private static final String module = "ScenarioProcessorImpl";
    @Autowired
    @Qualifier("Statistic")
    private Statistic statistic;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;
    @Autowired
    private Environment env;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    private boolean enabledScheduledCall;
    private boolean enabledAgentEventCall;


    @Override
    public void onAgentStateChange(AgentDescriptor agentDescriptor) {
        statistic.logAgentStatistic(agentDescriptor);
        try {
            switch (agentDescriptor.getState()) {
                case AGENT_STATE_AVAILABLE: {
                    if (enabledAgentEventCall)
                        pool.getAgentQueueToCall().put(agentDescriptor);
                }
            }
        } catch (Exception e) {
            if (logger.getDebugLevel() > 0)
                logger.logMore_0(module, "onAgentStateChange: for agent=" + agentDescriptor.getAgentID()
                        + " state=" + agentDescriptor.getState()
                        + " throw Exception=" + e.getMessage());
        }
    }

    @Override
    public void onACDCall(CallDescriptor callDescriptor) {
        statistic.logCallStatistic(callDescriptor);
    }

    @Override
    public void onAgentCallMake() {

    }

    @Override
    public void onAgentCallAnswer(CallDescriptor callDescriptor) {

    }

    @Override
    public void onAgentCallClear() {

    }

    private void updateSettings() {
        enabledScheduledCall = Boolean.valueOf(env.getProperty("connector.generate.scheduled.call"));
        enabledAgentEventCall = Boolean.valueOf(env.getProperty("connector.generate.agentevent.call"));
    }
}

package ua.com.smiddle.emulator.core.services.scenario;

import ua.com.smiddle.emulator.AgentDescriptor;
import ua.com.smiddle.emulator.core.model.CallDescriptor;

/**
 * @author ksa on 21.12.16.
 * @project emulator
 */
public interface ScenarioProcessor {
    void onAgentStateChange(AgentDescriptor agentDescriptor);
    void onACDCall(CallDescriptor callDescriptor);
    void onAgentCallMake();
    void onAgentCallAnswer(CallDescriptor callDescriptor);
    void onAgentCallDrop(CallDescriptor callDescriptor);
    void onClientCallDrop(CallDescriptor callDescriptor);
}

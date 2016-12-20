package ua.com.smiddle.emulator.core.services.processing.calls;

import ua.com.smiddle.cti.messages.model.messages.calls.AnswerCallReq;
import ua.com.smiddle.cti.messages.model.messages.calls.ClearCallReq;
import ua.com.smiddle.emulator.AgentDescriptor;

import java.util.Queue;

/**
 * @author ksa on 01.12.16.
 * @project emulator
 */
public interface CallsProcessor {
    void processIncomingACDCall(int connectionCallId, AgentDescriptor ad);

    void processAnswerCallReq(AnswerCallReq req) throws Exception;

    void processClearCallReq(ClearCallReq req) throws Exception;

    void processACDCallsEndByCustomer(int connectionCallId);

    void generateCallForAgent(AgentDescriptor ad);

    @Deprecated
    void processIncomingACDCallList(Queue<Integer> connectionCallIdQueue);

    @Deprecated
    void processIncomingACDCallList();
}

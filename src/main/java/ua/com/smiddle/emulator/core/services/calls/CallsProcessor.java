package ua.com.smiddle.emulator.core.services.calls;

import ua.com.smiddle.cti.messages.model.messages.calls.AnswerCallReq;
import ua.com.smiddle.cti.messages.model.messages.calls.ClearCallReq;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;

/**
 * @author ksa on 01.12.16.
 * @project emulator
 */
public interface CallsProcessor {
    void processIncomingACDCall(int connectionCallId, ServerDescriptor sd);

    void processAnswerCallReq(AnswerCallReq req, ServerDescriptor sd) throws Exception;

    void processClearCallReq(ClearCallReq req, ServerDescriptor sd) throws Exception;
}

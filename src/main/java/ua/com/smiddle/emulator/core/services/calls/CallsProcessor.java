package ua.com.smiddle.emulator.core.services.calls;

import ua.com.smiddle.cti.messages.model.messages.calls.AnswerCallReq;
import ua.com.smiddle.cti.messages.model.messages.calls.ClearCallReq;

/**
 * @author ksa on 01.12.16.
 * @project emulator
 */
public interface CallsProcessor {
    void processIncomingACDCall(int connectionCallId);

    void processAnswerCallReq(AnswerCallReq req) throws Exception;

    void processClearCallReq(ClearCallReq req) throws Exception;

    void processACDCallsEndByCustomer(int connectionCallId);
}

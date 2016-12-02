package ua.com.smiddle.emulator.core.services.agentstates;

import ua.com.smiddle.emulator.core.model.AgentEvent;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;

/**
 * @author ksa on 02.12.16.
 * @project emulator
 */
public interface AgentStateProcessor {
    /**
     * Выполняется в отдельном потоке. Не требует ручного изменения стостяния агента.
     * Проводит сериализацию сообщений.
     *
     * @param message
     * @param sd
     * @throws Exception
     */
    void processSetAgentStateReq(Object message, ServerDescriptor sd) throws Exception;

    /**
     * Выполняется в текущем потоке. Требует ручное изменения стостяния агента.
     * Проводит сериализацию сообщений.
     *
     * @param event
     * @throws Exception
     */
    void processAgentStateEvent(AgentEvent event) throws Exception;
}

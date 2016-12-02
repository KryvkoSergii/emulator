package ua.com.smiddle.emulator.core.services.agentstates;

import ua.com.smiddle.emulator.AgentDescriptor;

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
     * @throws Exception
     */
    void processSetAgentStateReq(Object message) throws Exception;

    /**
     * Выполняется в текущем потоке. Требует ручное изменения стостяния агента.
     * Проводит сериализацию сообщений.
     *
     * @param agentDescriptor
     * @throws Exception
     */
    void processAgentStateEvent(AgentDescriptor agentDescriptor) throws Exception;

    /**
     * Добавление сообщения всем клиентским подписчикам.
     *
     * @param message
     */
    void sendMessageToAllSubscribers(byte[] message);
}

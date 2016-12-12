package ua.com.smiddle.emulator.core.services.processing;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ksa on 09.12.16.
 * @project emulator
 */
public interface Processor {
    AtomicInteger getMessageReadCounter();
}

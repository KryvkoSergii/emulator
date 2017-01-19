package ua.com.smiddle.emulator.core.services.processing;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ksa on 09.12.16.
 * @project emulator
 */
public interface Processor {
    AtomicLong getMessageReadCounter();
}

package com.recursive.domain;

/**
 * Port for measuring host capabilities. The implementation chooses the
 * best underlying source for each OS (OSHI) but the contract stays a
 * plain value object.
 */
public interface HardwareDetector {

    HardwareSpec detect();
}

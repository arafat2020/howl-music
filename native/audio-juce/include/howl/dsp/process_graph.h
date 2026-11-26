#pragma once

#include <cstdint>

namespace howl {

/**
 * Ordered DSP graph placeholder (Phase 3.1).
 *
 * Control thread: prepare / reset / setBypassed.
 * Audio callback: process() only — no heap alloc, no locks, no I/O.
 *
 * JUCE types stay inside the .cpp (or disabled via HOWL_WITH_JUCE=0).
 */
class ProcessGraph {
public:
    ProcessGraph();
    ~ProcessGraph();

    ProcessGraph(const ProcessGraph&) = delete;
    ProcessGraph& operator=(const ProcessGraph&) = delete;

    /**
     * Allocate/prepare node state for the given stream.
     * Call from the control thread before audio starts (or on rate change).
     */
    void prepare(double sampleRate, int32_t maxBlockSize, int32_t channelCount);

    /** Clear smoothed state; safe on control thread. */
    void reset();

    void setBypassed(bool bypassed);
    bool isBypassed() const;

    bool isPrepared() const;

    double sampleRate() const;
    int32_t maxBlockSize() const;
    int32_t channelCount() const;

    /**
     * In-place process interleaved Float32 (L R L R …).
     * Identity when bypassed or when the active node is unity.
     */
    void process(float* interleaved, int32_t frames);

    /** Non-RT: which backend compiled in. */
    static const char* backendName();

private:
    struct Impl;
    Impl* impl_;  // opaque; allocated in ctor (control), never in process()
};

}  // namespace howl

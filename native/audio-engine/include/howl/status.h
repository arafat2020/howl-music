#pragma once

#include <cstdint>

namespace howl {

/** Result codes for engine control-thread operations. */
enum class Status : int32_t {
    Ok = 0,
    InvalidState = 1,
    AlreadyRunning = 2,
    NotRunning = 3,
    NullHandle = 4,
    DeviceOpenFailed = 5,
};

enum class EngineState : int32_t {
    Idle = 0,
    Prepared = 1,
    Running = 2,
    Stopping = 3,
};

inline const char* statusToString(Status s) {
    switch (s) {
        case Status::Ok:
            return "Ok";
        case Status::InvalidState:
            return "InvalidState";
        case Status::AlreadyRunning:
            return "AlreadyRunning";
        case Status::NotRunning:
            return "NotRunning";
        case Status::NullHandle:
            return "NullHandle";
        case Status::DeviceOpenFailed:
            return "DeviceOpenFailed";
    }
    return "Unknown";
}

inline const char* engineStateToString(EngineState s) {
    switch (s) {
        case EngineState::Idle:
            return "Idle";
        case EngineState::Prepared:
            return "Prepared";
        case EngineState::Running:
            return "Running";
        case EngineState::Stopping:
            return "Stopping";
    }
    return "Unknown";
}

}  // namespace howl

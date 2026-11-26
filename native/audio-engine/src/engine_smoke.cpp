#include "howl/engine_smoke.h"
#include "howl/engine.h"

const char* howl_engine_smoke_version(void) {
    // Keep symbol for older smoke JNI; delegates to Engine version.
    return howl::Engine::version();
}

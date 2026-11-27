#pragma once

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* A single control thread owns each handle and serializes calls through destroy. */

typedef struct HowlTransportSnapshot {
    int64_t position_frames;
    int64_t duration_frames;
    int32_t sample_rate;
    int32_t paused;   // 0/1
    int32_t playing;  // 0/1
} HowlTransportSnapshot;

/** C layout matches howl::EngineDiagnostics field order. */
typedef struct HowlEngineDiagnostics {
    int32_t sample_rate;
    int32_t channel_count;
    int32_t frames_per_burst;
    int32_t frames_per_callback;
    int64_t callback_count;
    int64_t underrun_count;
    int64_t error_count;
} HowlEngineDiagnostics;

typedef struct HowlEngineOpaque HowlEngineHandle;

HowlEngineHandle* howl_engine_create(void);
void howl_engine_destroy(HowlEngineHandle* engine);

int32_t howl_engine_prepare(HowlEngineHandle* engine);
int32_t howl_engine_start(HowlEngineHandle* engine);
int32_t howl_engine_stop(HowlEngineHandle* engine);
int32_t howl_engine_state(HowlEngineHandle* engine);
int32_t howl_engine_set_play_mode(HowlEngineHandle* engine, int32_t mode);
int32_t howl_engine_load(HowlEngineHandle* engine, const char* path);
int32_t howl_engine_play_mode(HowlEngineHandle* engine);

int32_t howl_engine_enqueue(HowlEngineHandle* engine, const char* path);
int32_t howl_engine_clear_queue(HowlEngineHandle* engine);
int32_t howl_engine_play_at(HowlEngineHandle* engine, int32_t index);
int32_t howl_engine_next(HowlEngineHandle* engine);
int32_t howl_engine_previous(HowlEngineHandle* engine);
int32_t howl_engine_set_preamp_gain_db(HowlEngineHandle* engine, float gain_db);
int32_t howl_engine_set_eq_band_gain_db(HowlEngineHandle* engine, int32_t band, float gain_db);
/* bands_db must contain ten floats ordered from 31.25 Hz to 16 kHz. */
int32_t howl_engine_apply_eq_state(
    HowlEngineHandle* engine, float preamp_db, const float* bands_db, int32_t band_count);
int32_t howl_engine_reset_eq_to_flat(HowlEngineHandle* engine);
int32_t howl_engine_set_repeat_mode(HowlEngineHandle* engine, int32_t mode);
int32_t howl_engine_queue_size(HowlEngineHandle* engine);
int32_t howl_engine_current_index(HowlEngineHandle* engine);
int32_t howl_engine_repeat_mode(HowlEngineHandle* engine);

int32_t howl_engine_pause(HowlEngineHandle* engine);
int32_t howl_engine_resume(HowlEngineHandle* engine);
int32_t howl_engine_seek_frames(HowlEngineHandle* engine, int64_t frame);
int32_t howl_engine_seek_ms(HowlEngineHandle* engine, int64_t ms);
int32_t howl_engine_get_transport(HowlEngineHandle* engine, HowlTransportSnapshot* out);

int32_t howl_engine_get_diagnostics(HowlEngineHandle* engine, HowlEngineDiagnostics* out);

const char* howl_engine_version(void);
const char* howl_engine_status_string(int32_t status);
const char* howl_engine_state_string(int32_t state);

#ifdef __cplusplus
}
#endif

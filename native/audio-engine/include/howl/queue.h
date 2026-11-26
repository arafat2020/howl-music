#pragma once

#include "howl/status.h"

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace howl {

enum class RepeatMode : int32_t {
    None = 0,
    One = 1,
    All = 2,
};

struct QueueItem {
    std::string path;
};

/**
 * Engine-side playlist (Phase 2.5). Control-thread only — not realtime-safe.
 * Audio handoff (flush/reopen) lives in Engine; this type owns order + repeat.
 */
class QueueController {
public:
    void clear();
    Status enqueue(std::string path);

    std::size_t size() const { return items_.size(); }
    bool empty() const { return items_.empty(); }

    int currentIndex() const { return currentIndex_; }
    const QueueItem* current() const;

    Status setCurrentIndex(int index);
    void setRepeatMode(RepeatMode mode) { repeatMode_ = mode; }
    RepeatMode repeatMode() const { return repeatMode_; }

    /**
     * Advance after EOF or explicit next.
     * @param honorRepeatOne when true (EOF), Repeat::One stays on the same index.
     *        Explicit next passes false so the user can leave a looping track.
     * @return true if currentIndex_ now points at a playable item.
     *         false → queue finished (Repeat::None at end).
     */
    bool advanceNext(bool honorRepeatOne = true);

    /** Move to previous item (wraps with Repeat::All). */
    bool advancePrevious();

    const std::vector<QueueItem>& items() const { return items_; }

private:
    std::vector<QueueItem> items_;
    int currentIndex_{-1};
    RepeatMode repeatMode_{RepeatMode::None};
};

}  // namespace howl

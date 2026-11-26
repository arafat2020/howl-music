#include "howl/queue.h"

namespace howl {

void QueueController::clear() {
    items_.clear();
    currentIndex_ = -1;
}

Status QueueController::enqueue(std::string path) {
    if (path.empty()) {
        return Status::NullHandle;
    }
    items_.push_back(QueueItem{std::move(path)});
    if (currentIndex_ < 0) {
        currentIndex_ = 0;
    }
    return Status::Ok;
}

const QueueItem* QueueController::current() const {
    if (currentIndex_ < 0 || currentIndex_ >= static_cast<int>(items_.size())) {
        return nullptr;
    }
    return &items_[static_cast<std::size_t>(currentIndex_)];
}

Status QueueController::setCurrentIndex(int index) {
    if (index < 0 || index >= static_cast<int>(items_.size())) {
        return Status::InvalidState;
    }
    currentIndex_ = index;
    return Status::Ok;
}

bool QueueController::advanceNext(bool honorRepeatOne) {
    if (items_.empty()) {
        currentIndex_ = -1;
        return false;
    }

    if (honorRepeatOne && repeatMode_ == RepeatMode::One) {
        // Stay on the same track (EOF path).
        if (currentIndex_ < 0) {
            currentIndex_ = 0;
        }
        return true;
    }

    const int next = currentIndex_ + 1;
    if (next < static_cast<int>(items_.size())) {
        currentIndex_ = next;
        return true;
    }

    // Past the end.
    if (repeatMode_ == RepeatMode::All) {
        currentIndex_ = 0;
        return true;
    }

    currentIndex_ = static_cast<int>(items_.size()) - 1;
    return false;
}

bool QueueController::advancePrevious() {
    if (items_.empty()) {
        currentIndex_ = -1;
        return false;
    }
    if (currentIndex_ <= 0) {
        if (repeatMode_ == RepeatMode::All) {
            currentIndex_ = static_cast<int>(items_.size()) - 1;
            return true;
        }
        currentIndex_ = 0;
        return true;  // clamp to first
    }
    --currentIndex_;
    return true;
}

}  // namespace howl

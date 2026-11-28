#include "howl/gapless_handoff.h"

#include <algorithm>
#include <utility>

namespace howl {

void GaplessHandoff::reset(int64_t durationFrames) {
    *this = GaplessHandoff{};
    current_.durationFrames = durationFrames;
}

Status GaplessHandoff::configure(std::unique_ptr<IDecoder> next, std::vector<float> head,
                                 int64_t token, int64_t acknowledged, const AudioFormat& format) {
    // The ring may already contain the next track. Keep that edit boundary atomic.
    if (current_.token != acknowledged) return Status::InvalidState;
    previousDecoder_.reset();
    next_.reset();
    nextHead_.clear();
    nextToken_ = token;
    if (token == 0) {
        mode_ = current_.token == 0 ? GaplessMode::Idle : GaplessMode::Joined;
    } else if (next && next->frameCount() > 0 && next->format().sampleRate == format.sampleRate &&
               next->format().channelCount == format.channelCount) {
        next_ = std::move(next);
        nextHead_ = std::move(head);
        mode_ = GaplessMode::Primed;
    } else {
        mode_ = GaplessMode::Fallback;
    }
    return Status::Ok;
}

Status GaplessHandoff::read(IDecoder& decoder, float* output, int32_t capacity, int32_t* frames) {
    if (headOffset_ == currentHead_.size()) return decoder.read(output, capacity, frames);
    const auto channels = static_cast<std::size_t>(decoder.format().channelCount);
    const auto count = std::min(static_cast<std::size_t>(capacity) * channels,
                                currentHead_.size() - headOffset_);
    std::copy_n(currentHead_.data() + headOffset_, count, output);
    headOffset_ += count;
    *frames = static_cast<int32_t>(count / channels);
    return Status::Ok;
}

bool GaplessHandoff::stitch(std::unique_ptr<IDecoder>& decoder, int64_t boundary, bool bufferedTail) {
    if (!next_) return false;
    if (!bufferedTail || previousDecoder_ || boundary != current_.startFrame + current_.durationFrames) {
        next_.reset();
        nextHead_.clear();
        mode_ = GaplessMode::Fallback;
        return false;
    }
    previous_ = current_;
    previousDecoder_ = std::move(decoder);
    current_ = {nextToken_, boundary, next_->frameCount()};
    decoder = std::move(next_);
    currentHead_ = std::move(nextHead_);
    headOffset_ = 0;
    mode_ = GaplessMode::Joined;
    return true;
}

Status GaplessHandoff::seek(std::unique_ptr<IDecoder>& decoder, int64_t frame, int64_t consumed) {
    const bool beforeBoundary = previousDecoder_ && consumed < current_.startFrame;
    IDecoder* audible = beforeBoundary ? previousDecoder_.get() : decoder.get();
    if (frame < 0 || frame > audible->frameCount()) return Status::InvalidArgument;
    const auto status = audible->seek(frame);
    if (status != Status::Ok) return status;
    currentHead_.clear();
    headOffset_ = 0;
    if (beforeBoundary) {
        nextToken_ = current_.token;
        if (decoder->seek(0) == Status::Ok) {
            next_ = std::move(decoder);
            mode_ = GaplessMode::Primed;
        } else {
            mode_ = GaplessMode::Fallback;
        }
        decoder = std::move(previousDecoder_);
        current_ = previous_;
    } else {
        previousDecoder_.reset();
    }
    return Status::Ok;
}

GaplessPlayback GaplessHandoff::playback(int64_t consumed) const {
    if (previousDecoder_ && consumed < current_.startFrame) {
        return {previous_, GaplessMode::Primed};
    }
    return {current_, mode_};
}

}  // namespace howl

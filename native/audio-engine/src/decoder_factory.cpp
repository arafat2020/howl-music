#include "howl/decoder.h"
#include "howl/flac_decoder.h"
#include "howl/wav_decoder.h"

#include <algorithm>
#include <cctype>
#include <cstring>
#include <fstream>
#include <string>

namespace howl {
namespace {

enum class ContainerKind {
    Wav,
    Flac,
    Unknown,
};

std::string lowerExt(const char* path) {
    std::string p = path != nullptr ? path : "";
    const auto slash = p.find_last_of("/\\");
    const std::string base = (slash == std::string::npos) ? p : p.substr(slash + 1);
    const auto dot = base.find_last_of('.');
    if (dot == std::string::npos) {
        return {};
    }
    std::string ext = base.substr(dot);
    std::transform(ext.begin(), ext.end(), ext.begin(),
                   [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return ext;
}

ContainerKind sniffFile(const char* path) {
    std::ifstream in(path, std::ios::binary);
    if (!in) {
        return ContainerKind::Unknown;
    }
    uint8_t hdr[12]{};
    in.read(reinterpret_cast<char*>(hdr), 12);
    const auto n = in.gcount();
    if (n >= 4 && std::memcmp(hdr, "fLaC", 4) == 0) {
        return ContainerKind::Flac;
    }
    if (n >= 12 && std::memcmp(hdr, "RIFF", 4) == 0 && std::memcmp(hdr + 8, "WAVE", 4) == 0) {
        return ContainerKind::Wav;
    }

    const std::string ext = lowerExt(path);
    if (ext == ".flac") {
        return ContainerKind::Flac;
    }
    if (ext == ".wav" || ext == ".wave") {
        return ContainerKind::Wav;
    }
    return ContainerKind::Unknown;
}

std::unique_ptr<IDecoder> tryOpen(std::unique_ptr<IDecoder> decoder, const char* path,
                                  Status* statusOut) {
    const Status st = decoder->open(path);
    if (statusOut != nullptr) {
        *statusOut = st;
    }
    if (st != Status::Ok) {
        return nullptr;
    }
    return decoder;
}

}  // namespace

std::unique_ptr<IDecoder> openDecoder(const char* path, Status* statusOut) {
    if (path == nullptr || path[0] == '\0') {
        if (statusOut != nullptr) {
            *statusOut = Status::NullHandle;
        }
        return nullptr;
    }

    const ContainerKind kind = sniffFile(path);
    switch (kind) {
        case ContainerKind::Flac:
            return tryOpen(createFlacDecoder(), path, statusOut);
        case ContainerKind::Wav:
            return tryOpen(createWavDecoder(), path, statusOut);
        case ContainerKind::Unknown:
            break;
    }

    // Last resort: try WAV then FLAC so content-type mismatches still work.
    Status wavSt = Status::UnsupportedFormat;
    auto wav = tryOpen(createWavDecoder(), path, &wavSt);
    if (wav) {
        if (statusOut != nullptr) {
            *statusOut = Status::Ok;
        }
        return wav;
    }
    Status flacSt = Status::UnsupportedFormat;
    auto flac = tryOpen(createFlacDecoder(), path, &flacSt);
    if (flac) {
        if (statusOut != nullptr) {
            *statusOut = Status::Ok;
        }
        return flac;
    }

    if (statusOut != nullptr) {
        // Prefer a concrete open failure over generic unsupported when the file was missing.
        *statusOut = (wavSt == Status::FileOpenFailed || flacSt == Status::FileOpenFailed)
                         ? Status::FileOpenFailed
                         : Status::UnsupportedFormat;
    }
    return nullptr;
}

}  // namespace howl

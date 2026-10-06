"""Synthesizes the meditation bells: additive partials with independent exponential decays."""
# Usage: python3 tools/generate_bells.py OUT_DIR, then encode each WAV to app/src/main/res/raw/*.ogg:
#   ffmpeg -i OUT_DIR/bell1.wav -c:a libvorbis -q:a 4 app/src/main/res/raw/bell1.ogg
import sys, wave
import numpy as np

SR = 44100

def bowl(f0, seconds, partials, beat_hz=0.0, attack_s=0.004):
    t = np.arange(int(SR * seconds)) / SR
    out = np.zeros_like(t)
    for ratio, amp, decay in partials:
        f = f0 * ratio
        tone = np.sin(2 * np.pi * f * t)
        if beat_hz:
            # A second, slightly detuned copy gives the slow "wah" beating of a real bowl.
            tone = 0.5 * tone + 0.5 * np.sin(2 * np.pi * (f + beat_hz * ratio) * t + 0.7)
        out += amp * tone * np.exp(-t / decay)
    # Short strike transient (mallet noise) that dies away in a few milliseconds.
    rng = np.random.default_rng(7)
    out += 0.08 * rng.standard_normal(len(t)) * np.exp(-t / 0.006)
    env = np.minimum(1.0, t / attack_s)
    fade = np.minimum(1.0, (seconds - t) / 0.4)
    out *= env * fade
    return out / np.max(np.abs(out)) * 0.7  # about -3 dBFS

def write(path, samples):
    pcm = (np.clip(samples, -1, 1) * 32767).astype('<i2')
    with wave.open(path, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())

# bell1: bright, short bell for interval and end-of-session strikes.
write(sys.argv[1] + '/bell1.wav', bowl(528.0, 5.0,
      [(1.0, 1.0, 1.6), (2.76, 0.5, 0.9), (5.40, 0.25, 0.45), (8.93, 0.1, 0.25)], beat_hz=1.3))
# bell2: deeper, longer singing bowl that opens a session.
write(sys.argv[1] + '/bell2.wav', bowl(264.0, 8.0,
      [(1.0, 1.0, 3.0), (2.71, 0.6, 1.8), (5.15, 0.3, 0.9), (8.40, 0.12, 0.5)], beat_hz=0.9))

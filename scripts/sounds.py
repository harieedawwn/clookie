"""Synthesize Clookie's seven original alarm sounds into app/src/main/res/raw.

Usage: python -E -P scripts/sounds.py

Everything is generated from oscillators and noise here (no samples, no third-party
audio). Each sound is a seamless loop: the ring-out tail is folded back onto the
start, so MediaPlayer looping has no click. Output: 44.1 kHz mono Ogg Vorbis.
Requires: pip install -r scripts/requirements.txt
"""
from pathlib import Path

import numpy as np
import soundfile as sf

SR = 44100
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"
RNG = np.random.default_rng(20261031)


def t_axis(seconds):
    return np.arange(int(seconds * SR)) / SR


def env(n, attack, decay):
    """Exponential decay envelope with a short linear attack (seconds)."""
    t = np.arange(n) / SR
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    return a * np.exp(-t / decay)


def note(freq):
    """MIDI-ish helper: semitones above A4 -> Hz."""
    return 440.0 * 2 ** (freq / 12)


def place(buf, sound, at):
    i = int(at * SR)
    end = min(len(buf), i + len(sound))
    buf[i:end] += sound[: end - i]


def loop(length, events, tail=3.0):
    """Render events [(time, sound)] into a loop of `length` s, folding the tail."""
    buf = np.zeros(int((length + tail) * SR))
    for at, s in events:
        place(buf, s, at)
    n = int(length * SR)
    out = buf[:n].copy()
    rest = buf[n:]
    while len(rest):
        k = min(n, len(rest))
        out[:k] += rest[:k]
        rest = rest[k:]
    return out


def lowpass(x, cutoff):
    """One-pole low-pass (cheap, stable)."""
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def bandpass_noise(n, lo, hi):
    noise = RNG.standard_normal(n)
    return lowpass(noise, hi) - lowpass(noise, lo)


# --- voices -----------------------------------------------------------------

def marimba(freq, dur=0.9):
    t = t_axis(dur)
    e = env(len(t), 0.002, 0.22)
    tone = np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 4 * t) * np.exp(-t / 0.05)
    return tone * e


def crunch(dur=0.12):
    n = int(dur * SR)
    return bandpass_noise(n, 1800, 7000) * env(n, 0.001, 0.025) * 2.2


def mallet(freq, dur=1.4, bright=1.0):
    """Wooden bar struck by a soft mallet: tuned-bar partials (1 : 3.93 : 10.7), each decaying
    faster than the last, a body resonance an octave down, and a short felt thump."""
    t = t_axis(dur)
    out = np.sin(2 * np.pi * freq * t) * np.exp(-t / 0.55)
    out += 0.28 * bright * np.sin(2 * np.pi * freq * 3.93 * t) * np.exp(-t / 0.09)
    out += 0.07 * bright * np.sin(2 * np.pi * freq * 10.7 * t) * np.exp(-t / 0.025)
    out += 0.22 * np.sin(2 * np.pi * freq * 0.5 * t) * np.exp(-t / 0.35)
    thump = lowpass(RNG.standard_normal(len(t)), 900) * env(len(t), 0.0005, 0.008) * 0.6
    return (out + thump) * np.clip(t / 0.003, 0, 1)


def crumble(dur=0.22, grains=7):
    """A cookie bite: a few tiny dry cracks close together, soft and mid-high, not hiss."""
    n = int(dur * SR)
    out = np.zeros(n)
    times = np.sort(RNG.uniform(0, dur * 0.6, grains))
    for k, at in enumerate(times):
        g = int(0.012 * SR)
        crack = bandpass_noise(g, 1200, 4200) * env(g, 0.0003, 0.0025 + 0.001 * (k % 3))
        place(out, crack * RNG.uniform(0.5, 1.0), at)
    return out


def soft_limit(x, drive):
    """Loudness by saturation: tanh pushes the waveform toward a square (more RMS, same peak)."""
    return np.tanh(drive * x / np.max(np.abs(x)))


def soft_pad(freq, dur=2.4):
    t = t_axis(dur)
    a = np.clip(t / 0.25, 0, 1) * np.exp(-t / 0.9)
    tone = np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(2 * np.pi * 2 * freq * t) + 0.12 * np.sin(2 * np.pi * 3 * freq * t)
    return tone * a


def bell(freq, dur=2.5):
    t = t_axis(dur)
    out = np.zeros_like(t)
    for ratio, amp, dec in ((1.0, 1.0, 0.9), (2.76, 0.5, 0.45), (5.4, 0.3, 0.25), (8.93, 0.15, 0.12)):
        out += amp * np.sin(2 * np.pi * freq * ratio * t) * np.exp(-t / dec)
    return out * np.clip(t / 0.002, 0, 1)


def woodblock(freq, dur=0.35):
    t = t_axis(dur)
    body = np.sin(2 * np.pi * freq * t) * np.exp(-t / 0.035)
    body += 0.5 * np.sin(2 * np.pi * freq * 1.48 * t) * np.exp(-t / 0.02)
    click = bandpass_noise(len(t), 1500, 6000) * env(len(t), 0.0005, 0.006)
    return body + 0.6 * click


def chirp(f0, f1, dur):
    t = t_axis(dur)
    f = np.geomspace(f0, f1, len(t))
    phase = 2 * np.pi * np.cumsum(f) / SR
    wobble = 1 + 0.25 * np.sin(2 * np.pi * 38 * t)
    shape = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 1.5
    return np.sin(phase) * shape * wobble


def beep(freq, dur=0.11):
    t = t_axis(dur)
    tone = sum(np.sin(2 * np.pi * freq * k * t) / k for k in (1, 3, 5, 7))
    edge = np.minimum(np.clip(t / 0.004, 0, 1), np.clip((dur - t) / 0.004, 0, 1))
    return tone * edge


# --- the six sounds -----------------------------------------------------------

def make_crunch():
    """Giòn tan: a bouncy C-major pentatonic hook on warm wooden bars over a soft bass, with a
    quiet cookie-bite crumble on two off-beats. Melody leads; the crumble is texture (-18 dB)."""
    eighth = 60 / 112 / 2
    # semitones from A4: C5=3 D5=5 E5=7 G5=10 A5=12 C6=15; bars: C, Am, F, G
    hook = [3, 7, 10, 7, 12, 10, 7, 5,
            3, 7, 10, 15, 12, 10, None, None,
            5, 7, 10, 12, 10, 7, 5, 3,
            7, 5, 3, 5, 3, None, None, None]
    bass = [-21, -24, -28, -26]  # C3 A2 F2 G2
    ev = []
    for i, n in enumerate(hook):
        if n is not None:
            accent = 1.0 if i % 2 == 0 else 0.72
            ev.append((i * eighth, mallet(note(n), bright=accent) * accent))
    for b, n in enumerate(bass):
        ev.append((b * 8 * eighth, mallet(note(n), dur=1.8, bright=0.4) * 0.55))
        ev.append((b * 8 * eighth + 4 * eighth, mallet(note(n + 7), dur=1.2, bright=0.4) * 0.35))
    for at in (14, 29):
        ev.append((at * eighth, crumble() * 0.12))
    return loop(32 * eighth, ev, tail=1.8)


def make_dawn():
    """Bình minh: rising warm arpeggio, soft attack, gets busier in the second half."""
    chord = [-9, -5, -2, 3, 7, 10, 14, 15]
    ev = [(i * 0.45, soft_pad(note(n)) * 0.7) for i, n in enumerate(chord)]
    ev += [(3.6 + i * 0.225, soft_pad(note(n + 12), 1.2) * 0.45) for i, n in enumerate(chord)]
    return loop(7.2, ev)


def make_chime():
    """Leng keng: inharmonic bells in a little descending-rising figure."""
    seq = [(0.0, 19), (0.3, 15), (0.6, 12), (0.9, 15), (1.5, 22), (1.8, 19), (2.4, 12), (2.7, 15), (3.0, 19)]
    ev = [(at, bell(note(n) / 2)) for at, n in seq]
    return loop(4.2, ev)


def make_knock():
    """Gõ cửa: knock-knock, knock-knock-knock on two woodblocks."""
    ev = []
    for bar in range(2):
        b = bar * 2.0
        ev += [(b, woodblock(820)), (b + 0.18, woodblock(820))]
        ev += [(b + 0.8, woodblock(1040)), (b + 0.98, woodblock(1040)), (b + 1.16, woodblock(820))]
    return loop(4.0, ev)


def make_chirp():
    """Chim sẻ: quick sparrow-like chirp calls and answers."""
    ev = []
    for k, start in enumerate((0.0, 1.6, 3.2)):
        for i in range(4):
            ev.append((start + i * 0.12, chirp(3800 + 300 * i, 5200 + 200 * k, 0.07) * 0.9))
        ev.append((start + 0.65, chirp(5600, 3000, 0.18) * 0.8))
        ev.append((start + 0.95, chirp(2900, 4700, 0.12) * 0.7))
    return loop(4.8, ev, tail=0.5)


def make_beeper():
    """Bíp bíp: four fast beeps, a pause. Plain and loud for heavy sleepers."""
    ev = []
    for g in range(2):
        for i in range(4):
            ev.append((g * 1.0 + i * 0.14, beep(2050)))
    return loop(2.0, ev, tail=0.2)


def make_siren():
    """Còi báo động (crazy sleeper): deliberately unbearable. A fast yelp siren swept 650-1700 Hz
    four times a second, two square-ish voices a quarter-tone apart beating against each other,
    cut by harsh 3.2 kHz bursts where the ear is most sensitive, saturated to near-square for the
    highest loudness a full-scale file can carry. No pleasant interval anywhere."""
    dur = 2.0
    t = t_axis(dur)
    sweep = (t * 4) % 1.0
    f = 650 + 1050 * sweep ** 0.6
    phase = 2 * np.pi * np.cumsum(f) / SR
    yelp = np.sign(np.sin(phase)) * 0.6 + np.sign(np.sin(phase * 1.0293)) * 0.6
    burst = np.zeros_like(t)
    for k in range(8):
        at = k * 0.25 + 0.17
        mask = (t >= at) & (t < at + 0.06)
        burst[mask] = np.sign(np.sin(2 * np.pi * 3200 * t[mask])) + 0.7 * np.sign(np.sin(2 * np.pi * 4530 * t[mask]))
    x = yelp + 0.9 * burst
    x = lowpass(x, 9000)  # tame aliasing, keep the bite
    return soft_limit(x, 3.5)


SOUNDS = {
    "clookie_crunch": make_crunch,
    "clookie_dawn": make_dawn,
    "clookie_chime": make_chime,
    "clookie_knock": make_knock,
    "clookie_chirp": make_chirp,
    "clookie_beeper": make_beeper,
    "clookie_siren": make_siren,
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, make in SOUNDS.items():
        x = make()
        x = x - np.mean(x)
        x = x / np.max(np.abs(x)) * 10 ** (-1 / 20)  # -1 dBFS peak
        if name == "clookie_crunch":
            x = soft_limit(x, 1.6) * 10 ** (-1 / 20)  # gentle: a few loud peaks no longer make the rest quiet
        path = OUT / f"{name}.ogg"
        sf.write(path, x.astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print(f"{path.name}: {len(x) / SR:.1f} s, {path.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    main()

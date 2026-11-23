"""Synthesises the 28 s soundtrack: 120 BPM in C major, sfx tuned to the same key, one shared reverb."""
import numpy as np, wave, sys

SR = 44100
DUR = 30.0
N = int(SR * DUR)
rng = np.random.default_rng(3)
music = np.zeros((N, 2))
sfx = np.zeros((N, 2))
send = np.zeros((N, 2))  # reverb send

def hz(m): return 440 * 2 ** ((m - 69) / 12)
def env(n, a=0.005, d=0.3):
    t = np.arange(n) / SR
    return np.minimum(1, t / a) * np.exp(-t / d)

def add(buf, start, sig, gain=1.0, pan=0.0, rev=0.0):
    i = int(start * SR)
    if i >= N: return
    sig = sig[: N - i].copy()
    f = min(len(sig), int(.008 * SR)); sig[-f:] *= np.linspace(1, 0, f)  # no clicks at cut-off tails
    l, r = np.sqrt(0.5 * (1 - pan)), np.sqrt(0.5 * (1 + pan))
    buf[i:i + len(sig), 0] += sig * gain * l
    buf[i:i + len(sig), 1] += sig * gain * r
    if rev:
        send[i:i + len(sig), 0] += sig * gain * rev * l
        send[i:i + len(sig), 1] += sig * gain * rev * r

def tone(f, length, harm=6, roll=1.6, d=0.35, a=0.004, detune=0.0):
    n = int(length * SR); t = np.arange(n) / SR
    s = sum(np.sin(2 * np.pi * f * k * t * (1 + detune * (k % 2 - .5))) / k ** roll for k in range(1, harm + 1))
    return s * env(n, a, d)

def noise(length, d, a=0.002):
    n = int(length * SR); return rng.standard_normal(n) * env(n, a, d)

def lowpass(x, k):  # crude moving-average lowpass, k samples
    return np.convolve(x, np.ones(k) / k, mode='same')

beat = 0.5
CHORDS = [[48, 60, 64, 67], [45, 57, 60, 64], [41, 57, 60, 65], [43, 55, 59, 62]]  # C Am F G

# ---------- groove 3.5 -> 22 ----------
groove_start, groove_end = 3.5, 24.0
t = groove_start
while t < groove_end - 1e-6:
    b = round((t - groove_start) / beat)
    bar = b // 4
    ch = CHORDS[bar % 4]
    # kick
    n = int(.35 * SR); tt = np.arange(n) / SR
    kick = np.sin(2 * np.pi * (45 * tt + 90 * (1 - np.exp(-tt * 30)) / 30)) * np.exp(-tt / .18)
    add(music, t, kick, .9)
    # clap on 2 and 4
    if b % 2 == 1:
        add(music, t, lowpass(noise(.25, .07), 3), .22, 0, rev=.5)
    # offbeat hat
    add(music, t + beat / 2, np.diff(noise(.06, .015), prepend=0), .1, .3)
    # bass: eighths on root
    for k in range(2):
        add(music, t + k * beat / 2, tone(hz(ch[0] - 12), .24, harm=4, roll=1.2, d=.12), .32)
    # chord stabs on the "and" of 2 and 4
    if b % 2 == 1:
        for j, m in enumerate(ch[1:]):
            add(music, t + beat / 2, tone(hz(m + 12), .4, harm=5, roll=1.8, d=.16, detune=.002), .085, (j - 1) * .5, rev=.35)
    t += beat

# lead motif over the groove (pentatonic, on beats), quiet
motif = [76, 79, 81, 79, 76, 74, 72, 74]
for i, tm in enumerate(np.arange(8.5, 23.5, beat * 2)):
    add(music, tm, tone(hz(motif[i % 8]), .5, harm=3, roll=2, d=.25), .07, .2 * ((i % 2) * 2 - 1), rev=.5)

# ---------- intro 0 -> 3.5: soft pluck + typing + bubble pops ----------
for i, tm in enumerate(np.arange(0, 3.0, beat)):
    add(music, tm, tone(hz([72, 76, 79, 76, 72, 74][i % 6]), .6, harm=3, roll=2.2, d=.3), .12, 0, rev=.6)
    add(music, tm, tone(hz(48 if i < 4 else 43), .5, harm=2, d=.3), .14)
for k in range(12):  # typing ticks
    add(sfx, .25 + k * .072, np.diff(noise(.03, .006), prepend=0), .1, .1 * (k % 3 - 1))
PENTA = [72, 74, 76, 79, 81, 84]
for i in range(12):  # bubble pops
    add(sfx, 1.35 + i * .11, tone(hz(PENTA[i % 6] + 12), .15, harm=1, d=.05), .09, (i % 4 - 1.5) * .35, rev=.4)

def whoosh(start, length=.5, gain=.14, up=True):
    n = int(length * SR); x = rng.standard_normal(n)
    k = np.linspace(40, 4, n) if up else np.linspace(4, 40, n)
    out = np.zeros(n); seg = 512
    for s in range(0, n, seg):  # sweep the lowpass width segment by segment
        out[s:s + seg] = lowpass(x[max(0, s - 64):s + seg + 64], int(k[s]))[min(s, 64):min(s, 64) + len(out[s:s + seg])]
    shape = np.sin(np.pi * np.linspace(0, 1, n)) ** 2
    add(sfx, start, out * shape, gain, 0, rev=.3)

whoosh(2.5, .9, .16)               # Etha runs through the chat
for k in range(12):                 # each knocked bubble: a pop in key, left to right as Etha passes
    add(sfx, 2.62 + k * .065, tone(hz(PENTA[(11 - k) % 6] + 12), .14, harm=1, d=.05), .08, .6 - k * .1, rev=.4)
add(sfx, 3.5, noise(1.2, .5) * 0, 0)
# riser into the drop
n = int(.5 * SR); tt = np.arange(n) / SR
add(music, 3.0, np.sin(2 * np.pi * (300 * tt + 900 * tt ** 2)) * (tt / .5) ** 2 * .5, .08, rev=.4)
# the drop: logo chord
for j, m in enumerate([60, 64, 67, 72]):
    add(music, 3.5, tone(hz(m), 1.6, harm=5, roll=1.7, d=.7, detune=.003), .1, (j - 1.5) * .4, rev=.6)

for i, m in enumerate([84, 88, 91, 96, 100]):
    add(sfx, 4.1 + i * .05, tone(hz(m), .35, harm=1, d=.12), .05, (i - 2) * .3, rev=.7)
# phone moves
# Etha hops in (4.35 landing), says hi (6.85)
n2 = int(.25 * SR); t2 = np.arange(n2) / SR
add(sfx, 4.1, np.sin(2 * np.pi * (300 * t2 + 700 * t2 ** 2)) * env(n2, .005, .1), .08, .5, rev=.4)
add(sfx, 4.85, tone(hz(55), .2, harm=2, d=.06), .2, .4)
for i, m in enumerate([79, 84]):
    add(sfx, 6.85 + i * .11, tone(hz(m), .4, harm=2, roll=2, d=.18), .09, -.3, rev=.6)
for tm in (8.45, 12.5, 16.55, 21.0, 24.0):
    whoosh(tm - .05, .45, .11)
# swipes in the deck clip: NOPE drag and the right flick
whoosh(14.05, .4, .1, up=False)
whoosh(15.8, .35, .15)


# Etha's journey between scenes: landings, runs, the leap into the match
def steps(start, dur, pan):
    for k in range(int(dur / .1)):
        add(sfx, start + k * .1, lowpass(noise(.05, .012), 4), .05, pan, rev=.2)
add(sfx, 8.1, np.sin(2 * np.pi * (260 * t2 + 600 * t2 ** 2)) * env(n2, .005, .1), .07, -.2, rev=.4)   # hop off
add(sfx, 8.75, tone(hz(55), .2, harm=2, d=.06), .18, .5)                                                  # land on the phone
steps(12.35, .65, .3)                                                                                     # run left
add(sfx, 16.3, np.sin(2 * np.pi * (300 * t2 + 900 * t2 ** 2)) * env(n2, .005, .12), .08, 0, rev=.5)     # leap
add(sfx, 16.85, tone(hz(55), .2, harm=2, d=.06), .18, -.4)                                                # land, confetti
steps(20.75, .7, .2)                                                                                      # run right
steps(23.7, .7, .7)                                                                                       # run off
# ---------- match ----------
add(sfx, 16.5, lowpass(noise(1.6, .6), 2), .1, 0, rev=.6)  # soft crash
for i, m in enumerate([84, 88, 91, 96, 91, 96]):
    add(sfx, 16.55 + i * .075, tone(hz(m), .6, harm=2, roll=2, d=.25), .09, (i % 2 - .5) * .6, rev=.7)
# ---------- level up 19.7 ----------
for i, m in enumerate([72, 76, 79, 84, 88]):
    add(sfx, 21.75 + i * .09, tone(hz(m), .4, harm=3, roll=1.8, d=.18), .08, 0, rev=.6)

# ---------- outro 22 -> 28 ----------
for j, m in enumerate([48, 55, 60, 64, 67, 71, 74]):  # Cmaj9 pad
    add(music, 24.0, tone(hz(m), 6.0, harm=4, roll=2, d=2.6, a=.05, detune=.004), .07, (j / 3 - 1) * .6, rev=.7)
for i, tm in enumerate(np.arange(24.5, 28.5, beat)):
    add(music, tm, tone(hz([79, 76, 72, 74, 76, 72, 67, 72][i % 8]), .7, harm=3, roll=2, d=.35), .09, 0, rev=.6)
add(sfx, 25.0, tone(hz(84), .5, harm=1, d=.15), .07, .5, rev=.6)  # QR pop
add(sfx, 25.5, tone(hz(79), .25, harm=1, d=.08), .07, .5, rev=.5)     # Etha peeks over the card
add(sfx, 26.25, np.sin(2 * np.pi * (300 * t2 + 700 * t2 ** 2)) * env(n2, .005, .1), .07, .5, rev=.4)  # jump
add(sfx, 26.75, tone(hz(60), .2, harm=2, d=.06), .12, .5)            # lands on the QR card
for i, m in enumerate([76, 79, 84]):                                  # scan me!
    add(sfx, 27.05 + i * .08, tone(hz(m), .35, harm=2, roll=2, d=.15), .07, .4, rev=.6)

# ---------- mix ----------
# shared room: one decaying-noise impulse for everything
ir_n = int(1.6 * SR); ir_t = np.arange(ir_n) / SR
ir = np.stack([rng.standard_normal(ir_n) * np.exp(-ir_t / .45) for _ in range(2)], 1)
ir = np.stack([lowpass(ir[:, c], 6) for c in range(2)], 1) * .02
L = 1 << int(np.ceil(np.log2(N + ir_n)))
wet = np.stack([np.fft.irfft(np.fft.rfft(send[:, c], L) * np.fft.rfft(ir[:, c], L), L)[:N] for c in range(2)], 1)

mix = music + sfx * .8 + wet
fade = np.ones(N); fi = int(.03 * SR); fo = int(1.2 * SR)
fade[:fi] = np.linspace(0, 1, fi); fade[-fo:] = np.linspace(1, 0, fo)
mix *= fade[:, None]
mix = np.tanh(mix * 1.4) / np.tanh(1.4)          # gentle glue
mix *= 0.89 / np.max(np.abs(mix))               # about -1 dBFS peak
out = (mix * 32767).astype('<i2')
with wave.open(sys.argv[1], 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(out.tobytes())
print('ok', np.sqrt(np.mean(mix ** 2)))

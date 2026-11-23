"""24 s seamless booth loop, 120 BPM, C major. Everything wraps: notes and reverb tails past the end land at the start."""
import numpy as np, wave, sys

SR = 44100
LOOP = 24.0
N = int(SR * LOOP)
rng = np.random.default_rng(5)
bus = np.zeros((N, 2))
send = np.zeros((N, 2))

def hz(m): return 440 * 2 ** ((m - 69) / 12)
def env(n, a=.005, d=.3):
    t = np.arange(n) / SR
    return np.minimum(1, t / a) * np.exp(-t / d)

def add(start, sig, gain=1., pan=0., rev=0.):
    sig = sig.copy(); f = min(len(sig), int(.008 * SR)); sig[-f:] *= np.linspace(1, 0, f)
    idx = (int(start * SR) + np.arange(len(sig))) % N          # wrap round the loop
    l, r = np.sqrt(.5 * (1 - pan)), np.sqrt(.5 * (1 + pan))
    np.add.at(bus[:, 0], idx, sig * gain * l); np.add.at(bus[:, 1], idx, sig * gain * r)
    if rev:
        np.add.at(send[:, 0], idx, sig * gain * rev * l); np.add.at(send[:, 1], idx, sig * gain * rev * r)

def tone(f, length, harm=5, roll=1.7, d=.3, a=.004, detune=0.):
    n = int(length * SR); t = np.arange(n) / SR
    return sum(np.sin(2 * np.pi * f * k * t * (1 + detune * (k % 2 - .5))) / k ** roll for k in range(1, harm + 1)) * env(n, a, d)

def noise(length, d, a=.002):
    n = int(length * SR); return rng.standard_normal(n) * env(n, a, d)

def lowpass(x, k): return np.convolve(x, np.ones(k) / k, mode='same')

beat = .5
CHORDS = [[48, 60, 64, 67], [45, 57, 60, 64], [41, 57, 60, 65], [43, 55, 59, 62]]   # C Am F G, one bar each
for b in range(int(LOOP / beat)):
    t = b * beat; ch = CHORDS[(b // 4) % 4]
    n = int(.3 * SR); tt = np.arange(n) / SR
    add(t, np.sin(2 * np.pi * (45 * tt + 80 * (1 - np.exp(-tt * 30)) / 30)) * np.exp(-tt / .16), .6)       # soft kick
    add(t + beat / 2, np.diff(noise(.05, .012), prepend=0), .07, .3)                                         # offbeat hat
    if b % 2 == 1: add(t, lowpass(noise(.2, .06), 3), .14, 0, rev=.5)                                        # clap
    for k in range(2): add(t + k * beat / 2, tone(hz(ch[0] - 12), .24, harm=4, roll=1.2, d=.12), .26)       # bass
    if b % 2 == 1:
        for j, m in enumerate(ch[1:]):
            add(t + beat / 2, tone(hz(m + 12), .4, harm=5, roll=1.8, d=.16, detune=.002), .07, (j - 1) * .5, rev=.4)
# a soft pad under each bar so the loop never feels empty
for bar in range(int(LOOP / 2)):
    for j, m in enumerate(CHORDS[bar % 4][1:]):
        add(bar * 2, tone(hz(m), 2.2, harm=3, roll=2.2, d=1.4, a=.15, detune=.004), .035, (j - 1) * .6, rev=.6)
# melody: one phrase per step, same shape each time
PHRASE = [76, 79, 81, 79, 76, 74, 72, 74, 76, 79, 84, 81]
for s in range(4):
    for i, m in enumerate(PHRASE):
        add(s * 6 + i * .5, tone(hz(m), .45, harm=3, roll=2, d=.22), .05, .25 * ((i % 2) * 2 - 1), rev=.5)
# step changes: a little hop sound (Etha) and a whoosh (the phone flip)
n2 = int(.25 * SR); t2 = np.arange(n2) / SR
for s in range(4):
    b = s * 6
    add(b - .35, np.sin(2 * np.pi * (300 * t2 + 700 * t2 ** 2)) * env(n2, .005, .1), .06, -.3, rev=.4)
    add(b + .2, tone(hz(55), .2, harm=2, d=.06), .14, -.3)
    w = lowpass(rng.standard_normal(int(.5 * SR)), 12) * np.sin(np.pi * np.linspace(0, 1, int(.5 * SR))) ** 2
    add(b - .25, w, .1, .3, rev=.3)
# match sparkle and level-up arpeggio
for i, m in enumerate([84, 88, 91, 96, 91, 96]): add(12.5 + i * .075, tone(hz(m), .6, harm=2, roll=2, d=.25), .07, (i % 2 - .5) * .6, rev=.7)
for i, m in enumerate([72, 76, 79, 84, 88]): add(18.5 + i * .09, tone(hz(m), .4, harm=3, roll=1.8, d=.18), .06, 0, rev=.6)

# shared room, convolved circularly so the tail wraps into the start
irn = int(1.6 * SR); irt = np.arange(irn) / SR
ir = np.zeros((N, 2))
for c in range(2): ir[:irn, c] = lowpass(rng.standard_normal(irn) * np.exp(-irt / .45), 6) * .02
wet = np.stack([np.fft.irfft(np.fft.rfft(send[:, c]) * np.fft.rfft(ir[:, c]), N) for c in range(2)], 1)
mix = bus + wet
mix = np.tanh(mix * 1.4) / np.tanh(1.4)
mix *= .89 / np.max(np.abs(mix))
with wave.open(sys.argv[1], 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes((mix * 32767).astype('<i2').tobytes())
print('ok', 'seam jump', np.abs(mix[0] - mix[-1]).max())

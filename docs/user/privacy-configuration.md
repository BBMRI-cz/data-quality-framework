# Privacy Configuration

The agent adds random noise to every number it reports. [Privacy & Security](./privacy.md) explains how this protects
patients and where its limits are. You set the strength of the protection on the **Differential Privacy** page of the
agent UI, or with environment variables (see [Configuration](./configuration.md)).

## Settings

| Setting          | What it does                                                                  | Default   | Allowed            |
|:-----------------|:------------------------------------------------------------------------------|:----------|:-------------------|
| `epsilon`        | Privacy budget (ε) of one report. Lower means more noise and stronger protection. | `3.0`     | `0.1` – `10`       |
| `minThreshold`   | Noisy counts below this value are reported as 0.                              | `50`      | `≥ 10`             |
| `noiseMechanism` | Type of noise, `LAPLACE` or `GAUSSIAN`. Keep `LAPLACE` unless you have a specific reason. | `LAPLACE` |                    |
| `delta`          | Only used by `GAUSSIAN`: the small probability that the guarantee does not hold. | `1e-8`    | `> 0` and `≤ 1e-5` |

The agent rejects out-of-range values. If an environment override is invalid, the agent logs an error and applies
none of the overrides, so the stored settings stay in effect.

## Choosing epsilon

ε is shared by everything in one report. 80 % goes to the quality checks, split equally between them, and 20 % to the
patient and sample totals. The more checks you run, the more noise each one gets.

Average difference between the published and the real count, with the default 17 checks:

| ε             | Each check    | Patient total | Sample total   |
|:--------------|:--------------|:--------------|:---------------|
| 1             | ± 21 patients | ± 10          | ± 200 samples  |
| 3 (default)   | ± 7 patients  | ± 3           | ± 67 samples   |
| 10            | ± 2 patients  | ± 1           | ± 20 samples   |

How much this matters depends on the size of the site. At 10 000 patients, ± 7 disappears when the result is rounded
to whole percent. At 300 patients it is about two percentage points.

The default of 3 is a moderate guarantee rather than a strong one. It was chosen so that results stay useful for sites
with a few thousand patients. Lower values protect better but make small sites' results mostly noise. Values above
about 5 give little formal protection, although single patients are still hard to spot in practice.

## Per-check budgets

You can give an individual check its own share of ε (the *epsilon budget* of the check), for example to make an
important check more accurate. The other checks share what is left. If the per-check budgets take up the whole check
share, the agent ignores them and splits the share equally.

## Noise mechanism

Laplace noise suits single counts and gives the plain ε guarantee. Gaussian noise is common in other systems, but here
it adds about six times more noise for the same ε, requires ε ≤ 1, and weakens the guarantee by δ. The patient and
sample totals always use Laplace noise.

## Repeated reports

The agent resends its previous noisy value until the real count moves by at least 5 (100 for the sample total), so
stable data costs no further privacy and cannot be averaged out. Data that changes often still gets new noise
regularly. See [Limits you should know about](./privacy.md#limits-you-should-know-about).
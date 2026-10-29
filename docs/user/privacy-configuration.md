# Privacy Configuration

The agent applies **differential privacy** to the quality metrics it reports, so only privacy-protected aggregates ever leave your site. The privacy parameters are configured on the **Differential Privacy** page of the agent UI and can also be set through environment variables. For a general introduction to how the framework protects data, see the [Privacy & Security](./privacy.md) page.

## Settings

| Setting          | Description                                                                                                                          | Default     | Validation                            |
|:-----------------|:-------------------------------------------------------------------------------------------------------------------------------------|:------------|:--------------------------------------|
| `epsilon`        | Privacy budget (ε). Smaller values mean stronger privacy but add more noise to results.                                              | `3.0`       | `0.1` – `10`                          |
| `delta`          | Delta parameter (δ) — probability of privacy failure. Typically a very small value such as `1e-8`.                                   | `1e-8`      | `> 0` and `<= 1e-5`                   |
| `minThreshold`   | Minimum count threshold for low count suppression. Reported values below this are hidden.                                            | `50`        | `>= 10`                               |
| `noiseMechanism` | Noise distribution used. One of `LAPLACE` or `GAUSSIAN`.                                                                             | `LAPLACE`   | `LAPLACE` or `GAUSSIAN`               |

### Noise mechanisms

- **Laplace**: adds noise from a Laplace distribution. Best for unbounded queries and provides (ε, 0)-differential privacy.
- **Gaussian**: adds noise from a Gaussian (normal) distribution. Used for (ε, δ)-differential privacy where δ > 0. Provides better utility for bounded queries with the same privacy guarantee.

::: warning Gaussian constraint
When `noiseMechanism` is `GAUSSIAN`, `epsilon` must be `<= 1.0`. If you need a larger privacy budget, use `LAPLACE`.
:::

### How the privacy budget is spent

The configured `epsilon` is the total privacy budget of a **single report**. Each generated report spends it as follows:

- **80%** is split among the quality check results (individually configured per-check budgets are honored within this share; if the preferences already consume the entire share, report generation fails loudly instead of giving the remaining checks an epsilon of 0).
- **20%** is reserved for the noisy entity totals (patient count and sample count), 10% each.

The minThreshold is applied to all noisy counts, including the totals: a site whose noisy patient total falls below the threshold reports it as 0, and its check ratios are reported as 0 as well. See [Privacy & Security](./privacy.md#protection-of-entity-totals) for details.
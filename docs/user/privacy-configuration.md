# Privacy Configuration

The agent applies **differential privacy** to the quality metrics it reports, so only privacy-protected aggregates ever leave your site. The privacy parameters are configured on the **Differential Privacy** page of the agent UI and can also be set through environment variables. For a general introduction to how the framework protects data, see the [Privacy & Security](./privacy.md) page.

## Settings

| Setting          | Description                                                                                                                          | Default     | Validation                            |
|:-----------------|:-------------------------------------------------------------------------------------------------------------------------------------|:------------|:--------------------------------------|
| `epsilon`        | Privacy budget (ε). Smaller values mean stronger privacy but add more noise to results.                                              | `3.0`       | `> 0`                                 |
| `delta`          | Delta parameter (δ) — probability of privacy failure. Typically a very small value such as `1e-8`.                                   | `1e-8`      | `> 0`                                 |
| `minThreshold`   | Minimum count threshold for low count suppression. Reported values below this are hidden.                                            | `50`        | `>= 0`                                |
| `noiseMechanism` | Noise distribution used. One of `LAPLACE` or `GAUSSIAN`.                                                                             | `LAPLACE`   | `LAPLACE` or `GAUSSIAN`               |

### Noise mechanisms

- **Laplace**: adds noise from a Laplace distribution. Best for unbounded queries and provides (ε, 0)-differential privacy.
- **Gaussian**: adds noise from a Gaussian (normal) distribution. Used for (ε, δ)-differential privacy where δ > 0. Provides better utility for bounded queries with the same privacy guarantee.

::: warning Gaussian constraint
When `noiseMechanism` is `GAUSSIAN`, `epsilon` must be `<= 1.0`. If you need a larger privacy budget, use `LAPLACE`.
:::
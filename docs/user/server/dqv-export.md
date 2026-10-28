# DQV Export

The server can render any stored report as an RDF/Turtle document modeled with the W3C
[Data Quality Vocabulary (DQV)](https://www.w3.org/TR/vocab-dqv/). The export is read-only: it is
computed from the stored report at request time and never changes how reports are ingested or kept.

## Endpoint

```
GET /api/v1/reports/{id}/export
```

- Authorization: same rule as other report reads (`HUMAN_USER` or `ADMIN` role).
- Response: `text/turtle;charset=UTF-8` with a `Content-Disposition: attachment` header.
- Query texts (CQL/SQL/…) are **never** exported — only the query hash and the query language.

The frontend report detail page offers the same document via the *Export DQV (Turtle)* button.

## Model

| FDQF entity              | DQV mapping                                                                                                             |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------- |
| Report                   | `dqv:QualityMeasurementDataset` + `dqv:QualityMetadata`, `prov:generatedAtTime`, `prov:wasAttributedTo` the site agent  |
| Computed result          | `dqv:QualityMeasurement` (`dqv:value` as `xsd:double`, unit `om:one`)                                                   |
| Report totals            | Integer measurements of `fdqf:patientCount` / `fdqf:sampleCount` in dimension `fdqf:volume`                             |
| Quality check version    | `dqv:Metric` with version-stable facts only: query hash (`dcterms:identifier`), `fdqf:queryLanguage`, `owl:versionInfo` |
| Quality check            | Mutable editorial data (`skos:prefLabel`, `skos:definition`, `dcat:keyword`)                                            |
| Category                 | `dqv:Dimension` in the `<categories>` concept scheme; checks without one use `fdqf:uncategorised`                       |
| Agent                    | `dcat:Dataset` (the assessed data) + `prov:SoftwareAgent` (the agent software)                                          |

Every result is additionally classified by a `dqv:QualityAnnotation`:

- The status body (`fdqf:passed`, `fdqf:warning`, `fdqf:failed`) is computed with the same strict
  greater-than rule used for the PDF summary: a result is a warning or failure only when its
  affected percentage is **strictly greater** than the respective threshold.
- The annotation carries the **thresholds applied at export time** and a `prov:generatedAtTime`
  timestamp, and is attributed to the server (`<#server>`), because thresholds live on the editable
  quality check and may change between exports.
- A `null` or non-finite result produces **no measurement** (DQV requires a value); instead the
  annotation carries `fdqf:notComputed` and is derived from the metric only.

::: warning Differentially private zero
A result of `0` does not necessarily mean "no affected records". The agent suppresses noisy counts
below its suppression threshold and reports them as `0`, which is then classified as passed. This is
documented on each exported metric via `rdfs:comment`. See
[Privacy Configuration](../privacy-configuration.md) for the suppression threshold.
:::

## Vocabulary terms

Custom terms in the `fdqf` namespace
(`https://github.com/bbmri-cz/data-quality-framework/ns/fdqf#`) are declared inline in every export:
status bodies as SKOS concepts in the `fdqf:statuses` scheme, and `fdqf:warningThreshold` /
`fdqf:errorThreshold` / `fdqf:queryLanguage` as datatype properties with `rdfs:range` and a comment
describing the comparison direction.

Minted IRIs (`reports/{id}`, `quality-checks/{id}/versions/{v}`, `agents/{id}#dataset`, `<#server>`,
…) are identifiers. Some of them happen to resolve to JSON endpoints, but dereferenceability is not
guaranteed.

## Base IRI

All IRIs are emitted relative to a `BASE` IRI. By default it is derived from the incoming request
(`{scheme}://{host}/api/v1/`). To pin it to a stable canonical IRI in deployments with multiple
hostnames or reverse proxies rewriting `Host`, configure:

```properties
app.dqv.base-iri=${CENTRAL_SERVER_DQV_BASE_IRI:}
```

for example `CENTRAL_SERVER_DQV_BASE_IRI=https://quality.example.org/api/v1`.

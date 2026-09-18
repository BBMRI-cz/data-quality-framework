---
layout: home

hero:
  name: "Data Quality Framework"
  text: "For Federated Environments"
  tagline: "Open-source, privacy-preserving tooling for assessing health data quality across federated networks, without sharing raw data."
  image:
    src: /logo.svg
    alt: Data Quality Framework
  actions:
    - theme: brand
      text: Get Started
      link: /user/deployment
    - theme: alt
      text: Learn more
      link: /user

features:
  - icon: <i class="bi bi-database"></i>
    title: FHIR & SQL
    details: Runs quality checks against HL7 FHIR R4 servers with CQL, or against SQL databases and CSV files with plain SQL.
    link: /user/data-sources
    linkText: Learn More

  - icon: <i class="bi bi-shield-lock"></i>
    title: Privacy-Preserving
    details: Results are aggregated locally and anonymized with differential privacy before leaving the site, so they cannot be traced back to individuals.
    link: /user/privacy
    linkText: Learn More

  - icon: <i class="bi bi-rocket-takeoff"></i>
    title: Easy to Deploy
    details: Deploy the agent and the server as Docker containers with Docker Compose in any environment.
    link: /user/deployment
    linkText: Learn More

  - icon: <i class="bi bi-puzzle"></i>
    title: Extensible
    details: Modular architecture with REST APIs for integrating quality reports into external systems.
    link: /developer/
    linkText: Learn More
---

<div class="institution-logos-home">
  <p class="logos-heading">FDQF is developed thanks to our partners:</p>
  <div class="logos-container">
    <img src="/bbmri_eric.svg" alt="BBMRI-ERIC" class="institution-logo logo-light" />
    <img src="/bbmri_eric-white.svg" alt="BBMRI-ERIC" class="institution-logo logo-dark" />
    <img src="/mmci.svg" alt="Masaryk Memorial Cancer Institute" class="institution-logo logo-light" />
    <img src="/mmci-white.svg" alt="Masaryk Memorial Cancer Institute" class="institution-logo logo-dark" />
    <img src="/BBMRI-CZ.svg" alt="BBMRI.cz" class="institution-logo logo-light" />
    <img src="/BBMRI-CZ-white.svg" alt="BBMRI.cz" class="institution-logo logo-dark" />
    <img src="/muni.png" alt="Masaryk University" class="institution-logo" />
  </div>
</div>

<style>
.institution-logos-home {
  padding: 3rem 1.5rem 0.1rem;
  margin-top: 2rem;
  margin-bottom: -6.1rem;
}

.logos-heading {
  margin: 0 auto 1.5rem;
  padding: 0 1.5rem;
  font-size: clamp(1.05rem, 2vw, 1.3rem);
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: 0.01em;
  display: block;
  width: 100%;
  text-align: center;
  color: var(--vp-c-text-2);
}

.logos-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2rem;
  flex-wrap: nowrap;
  max-width: 1400px;
  margin: 0 auto;
}

.institution-logo {
  max-height: 100px;
  max-width: 180px;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
}

/* Make BBMRI-ERIC logo bigger to match other logos */
.institution-logo[alt="BBMRI-ERIC"] {
  max-height: 130px;
  max-width: 230px;
}

/* Show/hide logos based on theme */
.logo-dark {
  display: none;
}

.dark .logo-light {
  display: none;
}

.dark .logo-dark {
  display: block;
}

@media (max-width: 768px) {
  .institution-logos-home {
    padding: 2.5rem 1rem 0.4rem;
    margin-bottom: -0.7rem;
  }

  .logos-heading {
    margin-bottom: 1.2rem;
    font-size: 1.15rem;
  }

  .logos-container {
    flex-wrap: wrap;
    gap: 1.5rem;
  }

  .institution-logo {
    max-height: 70px;
    max-width: 120px;
  }

  .institution-logo[alt="BBMRI-ERIC"] {
    max-height: 90px;
    max-width: 155px;
  }
}

@media (max-width: 480px) {
  .institution-logos-home {
    padding-bottom: 0.25rem;
    margin-bottom: -0.45rem;
  }

  .logos-heading {
    margin-bottom: 1rem;
    font-size: 1.05rem;
  }

  .logos-container {
    gap: 1rem;
  }

  .institution-logo {
    max-height: 60px;
    max-width: 100px;
  }

  .institution-logo[alt="BBMRI-ERIC"] {
    max-height: 78px;
    max-width: 130px;
  }
}
</style>

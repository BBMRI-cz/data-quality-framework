---
layout: home

hero:
  name: "Data Quality Framework"
  text: "For Federated Environments"
  tagline: "Comprehensive Health Data Quality Framework for Federated Environments with Privacy Preserving techniques for Sensitive Data"
  image:
    src: /logo.svg
    alt: Data Quality Framework
  actions:
    - theme: brand
      text: Overview
      link: /user/
    - theme: alt
      text: Get Started
      link: /user/getting-started
    - theme: alt
      text: Installation
      link: /user/deployment
    - theme: alt
      text: Developer Documentation
      link: /developer/

features:
  - icon: <i class="bi bi-code-slash"></i>
    title: Open-Source
    details: Fully open-source framework released under the GNU GPL v3.0 License, enabling transparency, collaboration, and community-driven development.
    link: /developer/contributing
    linkText: Learn More

  - icon: <i class="bi bi-shield-lock"></i>
    title: Secure
    details: Implements advanced privacy-preserving techniques to ensure sensitive health data remains secure while maintaining data quality monitoring capabilities.
    link: /user/privacy
    linkText: Learn More

  - icon: <i class="bi bi-rocket-takeoff"></i>
    title: Easy to Deploy
    details: Simple deployment with Docker containers and Docker Compose, making it easy to get started quickly in any environment.
    link: /user/deployment
    linkText: Learn More

  - icon: <i class="bi bi-puzzle"></i>
    title: Extensible
    details: Modular architecture with RESTful APIs and plugin support, making it easy to integrate with existing systems and extend functionality.
    link: /developer/
    linkText: Learn More
---

<div class="institution-logos-home">
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
  padding: 3rem 1.5rem 2.5rem;
  margin-top: 2rem;
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
    padding: 2.5rem 1rem 2rem;
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

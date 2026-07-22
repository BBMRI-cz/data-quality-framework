# Contributing Guide

Welcome! This guide covers everything you need to know to contribute to the Data Quality Framework.

## Getting Started

### Prerequisites

- **Java 25+** (OpenJDK or Oracle JDK)
- **Maven 3.9+**
- **Node.js 20+**
- **Docker** (for testing and local deployment)
- **Git**
- **IDE**: IntelliJ IDEA (recommended) or Eclipse

### Development Environment Setup

1. **Clone and Setup**
   ```bash
   git clone https://github.com/BBMRI-cz/data-quality-framework.git
   cd data-quality-framework
   ```

2. **Start Agent Development Environment**
   ```bash
   # Terminal 1: Backend with hot reload
   cd agent/backend
   mvn spring-boot:run -Dspring-boot.run.profiles=dev

   # Terminal 2: Frontend with hot reload
   cd agent/frontend
   npm install
   npm run dev
   ```

3. **Access Development Environment**
   - **Frontend Dev Server**: http://localhost:5173 (hot reload enabled)
   - **Backend API**: http://localhost:8081
   - **API Documentation**: http://localhost:8081/api/swagger-ui/index.html

4. **Start Server Development Environment** (if needed)
   ```bash
   # Terminal 3: Server backend
   cd server/backend
   mvn spring-boot:run -Dspring-boot.run.profiles=dev

   # Terminal 4: Server frontend
   cd server/frontend
   npm install
   npm run dev
   ```

   - **Server Frontend**: http://localhost:5174
   - **Server Backend**: http://localhost:8082

   ::: warning
   Starting the server backend will launch a testcontainer on port 5432. Make sure you have Docker Engine running and that port 5432 is available.
   :::

## Development Workflow

We use **GitHub Flow** - a simple, branch-based workflow with only the `master` branch as the main branch.

### End-to-End Contribution Flow

1. **Create Feature Branch**
   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/your-feature-name
   ```

2. **Make Changes**
   - Write code following our [coding principles](#coding-principles)
   - Write tests for new functionality
   - Update documentation if needed

3. **Test Locally**
   ```bash
   # Run all tests
   mvn test

   # Format code
   mvn com.spotify.fmt:fmt-maven-plugin:format

   # Build and test with Docker
   docker build -t data-quality-agent:test ./agent
   ```

4. **Commit Changes**
   ```bash
   git add .
   git commit -m "feat: add new quality check for patient data"
   ```

5. **Push and Create Pull Request**
   ```bash
   git push origin feature/your-feature-name
   ```
   - Open pull request on GitHub
   - Fill out the PR template
   - Wait for CI checks to pass
   - Address review feedback

6. **Merge and Cleanup**
   - After approval, maintainer will merge to `master`
   - Delete your feature branch
   ```bash
   git checkout master
   git pull origin master
   git branch -d feature/your-feature-name
   ```

## Coding Principles

### KISS (Keep It Simple, Stupid)

- **Prefer simple solutions** over complex ones
- **Avoid over-engineering** - solve the problem at hand
- **Use standard patterns** rather than inventing new ones
- **Write self-documenting code** that's easy to understand

### Comment Why, Not What

```java
// Good - explains WHY
// Use epsilon=1.0 to balance privacy with utility for small datasets
private static final double DEFAULT_EPSILON = 1.0;

// Bad - explains WHAT (obvious from code)
// Set epsilon to 1.0
private static final double DEFAULT_EPSILON = 1.0;
```

**Comment Guidelines:**
- Explain **business logic** and **decisions**
- Document **non-obvious algorithms**
- Clarify **privacy parameters** and their implications
- Avoid comments that just repeat the code

### Additional Principles

- **Fail fast** - validate inputs early and throw meaningful exceptions
- **Single responsibility** - each class/method should do one thing well
- **Testable code** - write code that's easy to test
- **Security first** - consider security implications in every change

## Code Conventions

### Java Backend

- **Code Style**: Use Spotify's fmt-maven-plugin (automatically enforced)
- **Naming**: 
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase.with.dots`

- **Structure**:
  ```java
  @Service
  public class QualityCheckService {
      
      private static final Logger logger = LoggerFactory.getLogger(QualityCheckService.class);
      
      private final QualityCheckRepository repository;
      
      public QualityCheckService(QualityCheckRepository repository) {
          this.repository = repository;
      }
      
      public void executeCheck(String checkId) {
          // Implementation
      }
  }
  ```

### Vue.js Frontend

- **Component Names**: `PascalCase` (e.g., `QualityCheckCard.vue`)
- **Props/Variables**: `camelCase`
- **File Structure**:
  ```vue
  <template>
    <!-- Template content -->
  </template>
  
  <script>
  export default {
    name: 'ComponentName',
    // Component logic
  }
  </script>
  
  <style scoped>
  /* Component styles */
  </style>
  ```

### Testing Conventions

- **Test Classes**: End with `Test` (e.g., `QualityCheckServiceTest`)
- **Test Methods**: Use descriptive names
  ```java
  @Test
  void shouldApplyDifferentialPrivacyWhenCountBelowThreshold() {
      // Test implementation
  }
  ```

- **Test Structure**: Arrange-Act-Assert pattern
  ```java
  @Test
  void shouldCalculateCorrectQualityScore() {
      // Arrange
      var input = createTestData();
      
      // Act
      var result = service.calculateScore(input);
      
      // Assert
      assertThat(result).isEqualTo(expectedScore);
  }
  ```

## Documentation

The project documentation is built using [VitePress](https://vitepress.dev/) and automatically published to GitHub Pages when changes are merged to `master`.

### Development Mode

To work on documentation locally:

```bash
cd docs
npm install
npm run dev
```

Access at: http://localhost:5173 with hot reload enabled for immediate preview of changes.

### Auto-Publishing

Documentation is automatically built and deployed to GitHub Pages via CI/CD when pull requests are merged to the `master` branch.

## Release Process

### Versioning Strategy

We use **Semantic Versioning** (SemVer): `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### Automated Release via GitHub

Releases are handled automatically through GitHub:

1. **Merge to Master**: When PRs are merged to `master`, GitHub Actions automatically:
   - Builds and tests the code
   - Creates Docker images
   - Signs images with Cosign
   - Publishes to GitHub Container Registry

2. **Create GitHub Release**: Maintainers create releases through GitHub UI:
   - Go to GitHub Releases page
   - Click "Create a new release"
   - Choose or create a tag (e.g., `v1.2.3`)
   - Add release notes with changelog
   - Publish the release

3. **Automatic Tagging**: GitHub automatically creates and manages version tags when releases are published

### Pre-release Checklist

- [ ] All tests passing
- [ ] Code coverage maintained
- [ ] Documentation updated
- [ ] Breaking changes documented
- [ ] Security review completed (if applicable)
- [ ] Manual testing of critical features

## Code of Conduct

Please follow our [Code of Conduct](https://github.com/BBMRI-cz/data-quality-framework/blob/master/CODE_OF_CONDUCT.md) in all interactions.

---

**Questions?** Open a GitHub Discussion or reach out to the maintainers.

**Last updated:** November 6, 2025

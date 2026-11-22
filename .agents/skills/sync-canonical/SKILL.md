---
name: sync-canonical
description: >-
  Synchronize the spring-petclinic-ai repository with the original canonical
  spring-projects/spring-petclinic repository. Make sure to use this skill whenever the
  user asks to sync or merge changes from the original/canonical/initial Spring Petclinic
  repository, align with upstream Petclinic commits, upgrade Spring Boot by pulling from
  canonical/main, or resolve merge conflicts between canonical Petclinic and this
  Spring AI fork.
---

# Synchronize with Canonical Spring Petclinic (`sync-canonical`)

This skill guides you through synchronizing `spring-petclinic-ai` with the official [spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic) repository (`canonical`) while preserving all Spring AI chatbot features, RAG vector store setup, build configurations, and tests.

## Context & Goal

`spring-petclinic-ai` is a fork of the canonical `spring-petclinic` sample application that integrates a Generative AI chatbot powered by **Spring AI** (supporting OpenAI, Azure OpenAI, and Ollama). Periodically, we merge `canonical/main` into this repository to inherit Spring Boot upgrades, dependency updates, bug fixes, and UI/domain improvements from the official project without losing any Spring AI customizations.

---

## Step-by-Step Workflow

### 1. Prepare Remotes and Branch

1. Verify the git remotes and ensure `canonical` points to `spring-projects/spring-petclinic` and `upstream` points to `spring-petclinic/spring-petclinic-ai`:
   ```bash
   git remote -v
   ```
   If `canonical` is missing, add it:
   ```bash
   git remote add canonical https://github.com/spring-projects/spring-petclinic.git
   ```
2. Fetch the latest changes from `canonical`, `upstream`, and `origin`:
   ```bash
   git fetch --multiple canonical upstream origin
   ```
3. Ensure local `main` is up to date with `upstream/main`:
   ```bash
   git checkout main
   git merge --ff-only upstream/main
   ```
4. Inspect what changed in `canonical/main` since the last synchronization:
   ```bash
   git log HEAD..canonical/main --oneline
   git diff HEAD...canonical/main --stat
   ```
   Look out for:
   - Spring Boot version upgrades (e.g., `3.5.x`, `4.0.x`, `4.1.x`) and whether `<spring-ai.version>` / `ext.springAiVersion` needs a corresponding upgrade for compatibility
   - Domain or repository refactoring (e.g., changes to `OwnerRepository`, `VetRepository`, or entity classes `Owner`, `Pet`, `Visit`, `Vet`)
   - Build plugin updates in `pom.xml` and `build.gradle`
   - Changes to `src/main/resources/messages/messages*.properties` or **newly added translation files** (e.g., `messages_hi.properties`, `messages_ja.properties`), which will not trigger merge conflicts on their own but must be completed with all `spring-petclinic-ai`-specific i18n keys (such as `sendMessage`) translated into the new target languages so `I18nPropertiesSyncTest` passes
5. Create and switch to a dedicated synchronization branch from `main`:
   ```bash
   git checkout -b feature/sync-spring-boot-<version> main
   ```
   *(Or `feature/sync-canonical` if not tied to a specific Spring Boot upgrade; check `git branch -a` first in case `feature/sync-canonical` already exists).*

---

### 2. Merge `canonical/main` Without Auto-Committing

Run the merge with `--no-commit` so you can inspect both conflicted and auto-merged files before finalizing:
```bash
git merge canonical/main --no-commit --no-ff
```

---

### 3. Resolve Conflicts & Preserve Spring AI Invariants

When resolving conflicts and reviewing merged files, adopt all upstream upgrades from `canonical/main` while preserving the following `spring-petclinic-ai` invariants. Understanding *why* each customization exists ensures you adapt cleanly even if `canonical` refactors surrounding code:

#### A. Maven Build (`pom.xml`)
- **Project Coordinates**: Keep `<artifactId>spring-petclinic-ai</artifactId>` (do not revert to `spring-petclinic`) and omit redundant `<version>` if inherited or keep it aligned with the parent version.
- **Parent & Plugin Versions**: Adopt the new `<parent>` (`spring-boot-starter-parent`) version and tool/plugin versions (`checkstyle`, `jacoco`, `lifecycle-mapping`, `maven-checkstyle`, `nohttp-checkstyle`, `spring-format`, webjars) from `canonical`.
- **Spring AI & Webjar Properties**: Preserve `<webjars-marked.version>` (used by the chat UI to render Markdown responses) and `<spring-ai.version>`. If a Spring Boot upgrade requires a newer Spring AI version (e.g., a milestone release), update `<spring-ai.version>` and `ext.springAiVersion` accordingly.
- **Dependency Management**: Preserve the `<dependencyManagement>` section importing `org.springframework.ai:spring-ai-bom:${spring-ai.version}`.
- **Dependencies**:
  - Preserve `org.springframework.ai:spring-ai-vector-store`.
  - Preserve `org.webjars.npm:marked:${webjars-marked.version}`.
  - **`spring-boot-starter-restclient` Scope**: Ensure `org.springframework.boot:spring-boot-starter-restclient` is in `compile` scope (do **not** keep `<scope>test</scope>` from `canonical`). In `canonical/main`, `spring-boot-starter-restclient` is declared with `<scope>test</scope>` because canonical Petclinic only uses `RestTemplateBuilder` in tests. However, in Maven, a direct `<scope>test</scope>` dependency overrides the transitive `compile` dependency that `spring-ai-starter-model-openai` has on `spring-boot-starter-restclient`, stripping `RestClientAutoConfiguration` from the runtime classpath and causing `TypeNotPresentException: Type org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration not present` warnings on application startup.
- **LLM Maven Profiles**: Preserve the three profiles at the bottom of `pom.xml`:
  - `openai` (active by default, with `org.springframework.ai:spring-ai-starter-model-openai`)
  - `azure-openai` (with `org.springframework.ai:spring-ai-starter-model-azure-openai`)
  - `ollama` (with `org.springframework.ai:spring-ai-starter-model-ollama`)

#### B. Gradle Build (`build.gradle`)
- **Versions**: Update the `org.springframework.boot` plugin version, other plugin versions (`org.graalvm.buildtools.native`, `org.cyclonedx.bom`, `io.spring.javaformat`, `io.spring.nohttp`), project `version`, and `ext.*` dependency versions to match `canonical`.
- **Spring AI & Extra Dependencies**:
  - Preserve `ext.webjarsMarkedVersion` and `ext.springAiVersion`.
  - Preserve the `dependencyManagement` block importing `org.springframework.ai:spring-ai-bom:${springAiVersion}`.
  - Preserve `implementation 'org.springframework.ai:spring-ai-starter-model-openai'` (along with the commented-out `spring-ai-starter-model-azure-openai` and `spring-ai-starter-model-ollama` lines), `implementation 'org.springframework.ai:spring-ai-vector-store'`, and `runtimeOnly "org.webjars.npm:marked:${webjarsMarkedVersion}"`.
  - Preserve the `checkstyleAot*`, `checkFormatAot*`, and `formatAot*` task configurations at the bottom of `build.gradle`.

#### C. Domain Entities, Repositories & `genai/` Package
- **Serialization (`serialVersionUID`)**: Preserve `private static final long serialVersionUID` across `BaseEntity`, `NamedEntity`, `Person`, `Owner`, `Pet`, `PetType`, `Visit`, `Specialty`, `Vet`, and anonymous `PetType` subclasses in `PetTypeFormatterTests`. When `canonical` adds validation annotations (such as `@Column(length = ...)` or `@Size(max = ...)` on fields in `Person`, `Owner`, `NamedEntity`, or `Visit`), keep `serialVersionUID` at the top of the class alongside the new field annotations.
- **RAG Serialization on `Vet.java`**: `VectorStoreController` and `AIDataProvider` serialize `Vet` entities to JSON using Jackson (`ObjectMapper`) to load and query veterinarian data in the `SimpleVectorStore`. Therefore, in `Vet.java`, preserve:
  - `@JsonProperty("specialties")` and `@JsonSerialize(as = ArrayList.class)` on `getSpecialties()` (replacing `@XmlElement` if canonical touches it)
  - `@JsonIgnore` on `getNrOfSpecialties()` and `addSpecialty(Specialty)`
- **`VetRepository.java` & `VetController.java`**:
  - Preserve `Integer countVets()` in `VetRepository` (`@Query("SELECT COUNT(v) FROM Vet v")`).
  - Preserve `int pageSize = 20;` in `VetController.findPaginated` (increased from 5 so the enlarged vet dataset displays comfortably) while keeping any pagination safety logic added by `canonical` (such as `int safePage = Math.max(page, 1);`).
- **Adapting `genai/` to Domain or Framework Changes**: If `canonical` changes method signatures in `OwnerRepository`, `VetRepository`, or domain entities (`Owner`, `Pet`, `Visit`, `Vet`), or upgrades Spring Boot / Jackson APIs, update the classes in `src/main/java/org/springframework/samples/petclinic/genai/` (`AIBeanConfiguration`, `AIDataProvider`, `ChatConfiguration`, `PetclinicChatClient`, `PetclinicTools`, `VectorStoreController`) to match the new APIs.

#### D. Configuration, UI, Internationalization & Dataset
- **`src/main/resources/application.properties`**:
  - Preserve `logging.level.org.springframework.ai.chat.client.advisor=DEBUG`, `logging.level.com.azure=DEBUG`, and all `# Azure OpenAI` (`spring.ai.azure.openai.*`), `# OpenAI` (`spring.ai.openai.*`), and `## OLLAMA` (`spring.ai.ollama.*`) configuration blocks.
  - **Crucial for `RestTemplate` / `PetClinicConcurrencyTests`**: Preserve `spring.http.clients.imperative.factory=jdk` in `src/main/resources/application.properties`. Because `spring-ai-starter-model-openai` transitively pulls in `spring-boot-starter-webclient` (`reactor-netty-http`), Spring Boot 4's HTTP client auto-detection would otherwise select `ReactorClientHttpRequestFactory` over `JdkClientHttpRequestFactory`. Reactor Netty's `HttpClient` follows `302 Found` redirects on `POST /owners/{id}/pets/new` by re-sending `POST /owners/{id}` instead of switching to `GET`, which causes `OwnerController` to return `405 Method Not Allowed` and breaks `PetClinicConcurrencyTests`. Pinning `spring.http.clients.imperative.factory=jdk` restores the standard JDK `HttpClient` behavior expected by canonical Petclinic without modifying any test classes.
- **Prompts & Pre-embedded Vector Store (`system.st` & `vectorstore.json`)**:
  - Preserve `src/main/resources/prompts/system.st`.
  - Preserve `src/main/resources/vectorstore.json`. `VectorStoreController` loads this pre-computed embedding file from the classpath on startup so local runs and `@SpringBootTest` tests do not make live embedding API calls. Because `spring.ai.openai.api-key=${OPENAI_API_KEY}` resolves to a literal placeholder when unset and `vectorstore.json` is loaded locally, `@SpringBootTest` classes do **not** need a custom `@ActiveProfiles("test")` annotation and can remain identical to `canonical/main`.
- **Chatbox UI (`layout.html`, `chatbot.scss`, `petclinic.scss`, `chat.css`, `petclinic.css`, `chat.js`, `.gitattributes`, `.gitignore`)**:
  - In `src/main/resources/templates/fragments/layout.html`, preserve `<link rel="stylesheet" th:href="@{/resources/css/chat.css}" />`, the `<div class="chatbox" id="chatbox">...</div>` markup (including `th:text="#{sendMessage}"` on the Send button), `<script th:src="@{/webjars/marked/.../marked.min.js}"></script>`, and `<script th:src="@{/resources/js/chat.js}"></script>`. If `webjars-marked.version` changed, update the script path in `layout.html` accordingly.
  - Preserve `src/main/scss/chatbot.scss`, the custom SCSS rules in `src/main/scss/petclinic.scss`, and the CSS rules in `src/main/resources/static/resources/css/chat.css` and `petclinic.css`.
  - Preserve `.gitattributes` (`src/main/resources/static/resources/css/petclinic.css linguist-generated=true`) and `.gitignore` (keeping CSS files tracked).
- **Internationalization (`src/main/resources/messages/messages*.properties` & `I18nPropertiesSyncTest`)**:
  - Preserve all `spring-petclinic-ai`-specific translation keys (currently `sendMessage`, plus any future chatbot UI keys added to `messages.properties`) at the end of every `src/main/resources/messages/messages*.properties` file (except `messages_en.properties`, which is intentionally empty for English fallback).
  - **Detecting and Translating Newly Added Languages from `canonical`**:
    1. Compare `messages.properties` against `canonical/main` to list all keys specific to `spring-petclinic-ai`:
       ```bash
       git diff canonical/main -- src/main/resources/messages/messages.properties
       ```
    2. Check if `canonical/main` introduced any new `messages_<lang>.properties` files (which Git adds cleanly without merge conflicts):
       ```bash
       git status -s src/main/resources/messages/
       ```
    3. For every newly added `messages_<lang>.properties` file (e.g., `messages_hi.properties`, `messages_ja.properties`), translate each `spring-petclinic-ai`-specific key into the target language and append it to the file, matching the file's encoding convention (UTF-8 characters or `\uXXXX` Unicode escapes). Otherwise, `I18nPropertiesSyncTest.checkI18nPropertyFilesAreInSync()` will fail at test time.
- **Enriched H2 Dataset (`data.sql`, `generate_records.sql` & `ClinicServiceTests.java`)**:
  - `src/main/resources/db/h2/data.sql` (and `generate_records.sql`) contains an expanded set of vets, owners, pets, and visits tailored for rich chatbot & RAG interactions. Preserve this dataset.
  - Because `data.sql` differs from canonical, keep the adjusted vet assertions in `ClinicServiceTests.shouldFindVets()` (checking vet ID 2 "Leary" with "anesthesia" and "dentistry").
- **`README.md` & `docs/`**: Keep the Spring AI introduction, blog post links, LLM setup instructions (Maven profiles & Gradle dependencies for OpenAI, Azure OpenAI, and Ollama), `docs/` screenshots, and `spring-petclinic/spring-petclinic-ai` URLs, while integrating any relevant updates from `canonical` (such as updated MySQL/PostgreSQL container image tags or Java version notes).

---

### 4. Format, Verify, and Validate Both Builds

`spring-petclinic` enforces strict formatting (`spring-javaformat`) and Checkstyle rules (including `nohttp` and import ordering). Always run formatting before running tests:

1. **Apply Spring Java Format**:
   ```bash
   ./mvnw spring-javaformat:apply
   ```
   *(Or `./gradlew format` if working with Gradle — do not combine `format` and `check` in the same `./gradlew format check` invocation on Gradle 9+, as Gradle task validation rejects `:checkFormatMain` reading `:formatMain`'s output without an explicit task dependency).*
2. **Run Maven verification (Checkstyle + Tests)**:
   ```bash
   ./mvnw test
   ```
3. **Run Gradle verification (CheckFormat + Checkstyle + Tests)**:
   ```bash
   ./gradlew check
   ```
4. **Final Diff Sanity Check**:
   Compare the working tree against `canonical/main` to ensure only expected Spring AI additions and customizations differ:
   ```bash
   git diff canonical/main --stat
   ```

---

### 5. Commit, Push, and Create the Pull Request

1. **Stage and Commit the Merge**:
   Once both Maven and Gradle builds succeed cleanly:
   ```bash
   git add -u
   git commit --no-edit
   ```
2. **Push the Synchronization Branch to `origin`**:
   Push the branch to your fork (`origin`):
   ```bash
   git push -u origin <branch-name>
   ```
3. **Create the Pull Request Against `spring-petclinic/spring-petclinic-ai`**:
   Use the GitHub CLI (`gh`) or REST API to open the PR against `spring-petclinic/spring-petclinic-ai` (`main` branch), summarizing the Spring Boot upgrade and main changes brought in from `canonical/main`:
   ```bash
   gh pr create \
     --repo spring-petclinic/spring-petclinic-ai \
     --base main \
     --head arey:<branch-name> \
     --title "Sync with the original Spring Boot <version> version" \
     --body "<summary of canonical changes and preserved Spring AI invariants>"
   ```

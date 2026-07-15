<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->

# Apache Jackrabbit Security Threat Model (draft)

**Why a separate Jackrabbit model (not folded into Jackrabbit Oak).**
The Jackrabbit PMC owns three functionally distinct codebases. The
original `jackrabbit` (commonly "Jackrabbit 2.x" / jackrabbit-core) and
`jackrabbit-oak` share a JCR API contract but have different storage
architectures, different security stacks (Oak introduced a redesigned
`SecurityProvider`-based model; Jackrabbit-2 uses the older JR2 ACL +
JAAS plumbing), different threading models, and — most importantly —
different maintenance postures. Jackrabbit is in *maintenance mode* with
JCR-API and reference-implementation work still happening but no major
feature development; Oak is the active product. Triage rules and
"what's in scope" differ enough that one document would have to caveat
every claim. The Jackrabbit PMC has confirmed (`.asf.yaml` ties commits
to `oak-dev@jackrabbit.apache.org` for Oak and `jackrabbit-dev@…` for
this repo; they are run as one project but the codebases are
independent) that the two are independent codebases. Modelling them
separately is the more truthful representation. `jackrabbit-filevault`
also gets its own model because its trust boundary (untrusted zip
package → repository) is qualitatively different.

## §1 Header

- **Project:** Apache Jackrabbit (`apache/jackrabbit`) — the original /
  legacy JCR implementation, distinct from Apache Jackrabbit Oak.
  Includes jackrabbit-core (repository), jackrabbit-spi /
  jackrabbit-spi2dav (SPI layer), jackrabbit-jcr-*, jackrabbit-webdav,
  jackrabbit-jcr2dav, and the JCR API artefacts *(documented:
  `README.txt`, repo top-level)*.
- **Commit / version binding:** drafted against `trunk`. A
  vulnerability report against Jackrabbit version *N* should be
  triaged against the model as it stood at *N* (release tag).
- **Date:** 2026-05-30.
- **Authors:** ASF Security team draft, awaiting Jackrabbit PMC review.
- **Status:** draft — under maintainer review.
- **Reporting cross-reference:** findings that may violate a §8
  property should be reported per the ASF Security Team disclosure
  channel (`security@apache.org`) and the project's security mailing
  list, before public disclosure. Findings under §3, §9, §11a
  will be closed by Jackrabbit triagers citing this document. (The
  Jackrabbit website hosts a CVE list at
  `jackrabbit.apache.org/jcr/jackrabbit-security-reports.html` —
  draft-time review of this URL did not produce a full enumeration
  for this draft; §14 Q1 asks the PMC to confirm the CVE
  history.)
- **Provenance legend:** *(documented)* — drawn from in-repo docs or
  website docs with citation; *(maintainer)* — confirmed by a
  Jackrabbit maintainer in response to this draft; *(inferred)* —
  synthesised from code structure or domain knowledge, awaiting PMC
  ratification (every *(inferred)* tag has a matching §14 question).
- **Draft confidence:** 14 documented / 0 maintainer / 32 inferred.
  *(This model leans more heavily on inference than the Oak model
  because jackrabbit-core's docs are older and less prescriptive
  about modern security claims; many §8 properties need PMC
  ratification.)*

**About the project.** Apache Jackrabbit is "a fully conforming
implementation of the Content Repository for Java Technology API (JCR,
specified in JSR 170 and 283)" *(documented: `README.txt`)*. It
predates Apache Jackrabbit Oak and was the original JCR reference
implementation. The repository hosts:

- **jackrabbit-core** — the legacy repository implementation (JR2 /
  jackrabbit-2 storage architecture);
- **jackrabbit-spi** + **jackrabbit-spi-commons** + **jackrabbit-spi2dav** +
  **jackrabbit-spi2jcr** — the SPI layer used as a bridge between
  client and server when WebDAV is involved;
- **jackrabbit-jcr-commons**, **jackrabbit-jcr-server**,
  **jackrabbit-jcr-tests**, **jackrabbit-jcr-client** — the JCR
  client / server bindings and the JCR TCK harness;
- **jackrabbit-webdav** + **jackrabbit-jcr2dav** — WebDAV-over-JCR;
- **examples/jackrabbit-firsthops** — sample code.

The maintenance posture is "in active maintenance, not active feature
development". Some users still run jackrabbit-2 production
repositories (especially the WebDAV-over-JCR stack) but the PMC's
forward-looking effort is on Oak *(inferred — §14 Q2)*.

## §2 Scope and intended use

### Intended use

- **In-process JCR 2.0 (JSR 283) repository implementation** embedded
  by a host application, *or* exposed through one of jackrabbit-
  webdav / jackrabbit-jcr-server as a network
  service *(documented: `README.txt`; module presence)*. Unlike Oak,
  Jackrabbit *does* ship network bindings.
- **The JCR API artefacts and SPI layer** are reusable on their own
  (callers depend on `javax.jcr.*` interfaces published from this
  repo); they are also dependencies of Oak and of jackrabbit-
  filevault.

### Deployment shape

Jackrabbit covers two distinct deployment shapes:

1. **In-process JCR library** — same shape as Oak: a host application
   instantiates a `Repository`, logs in, gets a `Session`, reads/writes.
2. **Network-exposed via jackrabbit-webdav + jackrabbit-jcr-server +
   jackrabbit-jcr2dav** — the repository is fronted by a WebDAV
   servlet; HTTP/WebDAV clients authenticate (usually via HTTP Basic
   or Digest) and the servlet translates WebDAV verbs into JCR
   operations. This brings authentication, HTTP parsing, and (in
   2.x) XML parsing for WebDAV bodies onto the in-model surface.

This split is the most consequential modelling decision: the
in-process and the network-exposed deployments have different
adversary models (§7), different reachable surfaces (§4), and
different downstream responsibilities (§10).

### Caller roles

| Role | Trust level | Notes |
| --- | --- | --- |
| **Host application code (embedded mode)** | trusted | Holds the JCR `Repository` handle; chooses the security configuration. |
| **JCR session principal** | untrusted but authenticated | Identified via `Repository.login(Credentials)` against the configured `LoginModule`. |
| **System principal / admin user** | trusted | Default `admin`/`admin` ships with jackrabbit-core; widely cited as the "first thing to change in production" *(inferred — §14 Q3)*. |
| **WebDAV / HTTP client (network-exposed mode)** | untrusted | Reaches Jackrabbit through jackrabbit-webdav / jackrabbit-jcr-server. Carries HTTP Basic or Digest credentials. |
| **WebDAV operator / servlet container** | trusted | Configures Tomcat/Jetty, TLS, authentication realm, and chooses which JCR repository the servlet exposes. |
| **External resource referenced by WebDAV body (XML entities, etc.)** | untrusted control plane | jackrabbit-webdav historically parses XML bodies for `PROPFIND`, `LOCK`, `REPORT`; XXE-class concerns are in-model here *(inferred — §14 Q4)*. |
| **Persistence manager backend (filesystem / JDBC database)** | trusted | jackrabbit-core's persistence managers write to disk or to a relational DB the operator configures *(inferred — §14 Q5)*. |

### Component-family table

| Family | Representative entry | Touches outside the process? | In-model? |
| --- | --- | --- | --- |
| **JCR API artefacts** (`javax.jcr.*` published from `jackrabbit-jcr-commons`) | API surface | no | **yes** (consumer contract; almost no implementation though) |
| **jackrabbit-core** — repository implementation, JR2 persistence | `RepositoryImpl.login` | filesystem (PM, search index, data store) | **yes** |
| **jackrabbit-spi**, **jackrabbit-spi-commons**, **jackrabbit-spi2jcr**, **jackrabbit-spi2dav** | SPI bridge | depends on the SPI impl | **yes** |
| **jackrabbit-jcr-commons** | utilities | no | **yes** |
| **jackrabbit-jcr-server** | server-side glue | **yes — HTTP** | **yes** (network surface) |
| **jackrabbit-jcr-client** | client-side glue | **yes — HTTP** | **yes** for the client posture |
| **jackrabbit-webdav** | WebDAV protocol + XML handling | **yes — HTTP + XML parse** | **yes** (high security weight, XML / XXE-class) |
| **jackrabbit-jcr2dav** | WebDAV ↔ JCR adaptation | **yes — HTTP** | **yes** |
| **jackrabbit-jcr-tests** | JCR TCK | none | **out of model** — test harness *(§3)* |
| **examples/jackrabbit-firsthops** | sample app | varies | **out of model** *(§3)* |
| **jackrabbit-standalone** / **jackrabbit-standalone-components** *(inferred — §14 Q6)* | standalone server launcher | filesystem + HTTP | in-model only as a runner, not a security boundary |
| **jackrabbit-aws-ext** *(inferred — §14 Q6)* | S3 data-store extension | **yes — S3** | in-model for in-Jackrabbit code; cloud APIs trusted (§3) |

A finding is in-model only if it lands in a row marked **yes**; see
§4 for per-component reachability tests.

## §3 Out of scope (explicit non-goals)

Reports requiring any of these will be closed with the cited
disposition:

1. **Host application correctness (embedded mode) / servlet
   container configuration (network mode).** Tomcat / Jetty
   misconfiguration, missing TLS, missing HTTP-Basic realm — those are
   operator / host issues *(inferred — §14 Q7)*. → `OUT-OF-MODEL:
   adversary-not-in-scope`.
2. **Underlying persistence-manager correctness.** Jackrabbit-core's
   PMs delegate to filesystem or JDBC; a hostile filesystem / hostile
   database is not Jackrabbit's threat *(inferred — §14 Q5)*. →
   `OUT-OF-MODEL: trusted-input`.
3. **Failing to rotate the default `admin`/`admin` user.** Documented
   convention; operator responsibility *(inferred — §14 Q3)*. →
   `OUT-OF-MODEL: non-default-build`.
4. **Code that ships but is not part of the supported product:**
   `jackrabbit-jcr-tests/` (TCK harness),
   `examples/jackrabbit-firsthops/`. → `OUT-OF-MODEL:
   unsupported-component`.
5. **Issues that exist in `jackrabbit-oak`.** Oak has its own
   threat model; cross-references not duplications.
6. **Issues in `jackrabbit-filevault`.** Filevault has its own
   threat model.
7. **Build / release / SDLC hygiene.** Out of model per the SKILL.
8. **Java EE / J2EE container vulnerabilities** when jackrabbit-jcr-
   server / jackrabbit-webdav is hosted under one. The container is
   the operator's choice *(inferred — §14 Q8)*. → `OUT-OF-MODEL:
   adversary-not-in-scope`.
9. **Maintenance-mode reality.** This repository is in active
   maintenance for **bug** and **security** fixes but is not under
   active feature development *(inferred — §14 Q2)*. A report that
   "feature X would be more secure if implemented as Y" is, in
   maintenance mode, more of a roadmap item than a triagable
   security claim, *unless* it concerns an upgrade path to a fix.
10. **Side channels** (cache timing, JVM JIT, co-resident JVM
    code). → `OUT-OF-MODEL: adversary-not-in-scope`.
11. **WebDAV semantics that the protocol requires.** WebDAV exposes
    directory listing, file PUT, MOVE, COPY, etc. A complaint that
    "this user can `PUT` and overwrite my file" is the JCR
    authorisation model's call, not a WebDAV-layer bug.

## §4 Trust boundaries and data flow

Jackrabbit has up to six trust transitions a finding must land
inside to be in-model:

| # | Transition | Authentication | Authorisation |
| --- | --- | --- | --- |
| B1 | Host code → `Repository.login(Credentials)` (embedded mode) | configured `LoginModule` chain via JAAS | `AccessManager` returned by the security configuration *(inferred — §14 Q9)* |
| B2 | HTTP client → jackrabbit-webdav (network mode) | HTTP Basic / Digest at the servlet layer | per-JCR-session, post-translation |
| B3 | JCR session principal → tree read | `Subject` established at B1/B2 | jackrabbit-core's `AccessManager` |
| B4 | JCR session principal → tree write | same | `AccessManager` evaluated on `Session.save()` |
| B5 | Jackrabbit → persistence manager (filesystem / JDBC) | backend-configured | backend ACLs |
| B6 | jackrabbit-webdav body parse | none (request body bytes) | n/a; the XML parser must be configured safely (XXE-class) *(inferred — §14 Q4)* |

### Reachability preconditions per family

A finding is in-model only if it meets the family's reachability test:

- **jackrabbit-core**: reachable from a JCR session at B1 / B2
  with the principal carrying *less than* admin authority.
- **jackrabbit-webdav**: reachable from an HTTP request body or
  header. XML-parse vulnerabilities (XXE, XML entity expansion,
  DTDs) are in-model here; the WebDAV bodies are the highest-
  blast-radius input on the network surface *(inferred — §14 Q4)*.
- **jackrabbit-jcr-server**: in-model for the request-routing and
  authentication glue.
- **jackrabbit-spi**: in-model for the SPI bridge's own logic; the
  underlying SPI implementation is per-deployment trusted.
- **jackrabbit-aws-ext** (S3 data store): in-model for in-Jackrabbit
  code paths; the S3 backend itself is trusted.
- **examples / TCK**: out (§3 item 4).

## §5 Assumptions about the environment

- **Java 11+** at HEAD; Maven 3 build *(documented: `README.txt`)*.
  JVM-conformant runtime; no SecurityManager required.
- **Servlet container** (Tomcat / Jetty) when running in network
  mode. The container handles connection management, TLS, and
  often HTTP Basic / Digest realms.
- **Persistence**. jackrabbit-core uses one of several PMs (bundle
  PM, ObjectPersistenceManager, database PMs); each writes to a
  filesystem directory or a JDBC database the operator configures
  *(inferred — §14 Q5)*.
- **Data store**. jackrabbit-core can use the FileDataStore (local
  files) or extensions (jackrabbit-aws-ext for S3); operator-
  configured trusted *(inferred — §14 Q5)*.
- **Search**. Lucene-based search index on the filesystem under the
  repository home.
- **Time** — `System.currentTimeMillis` for lock timeouts and
  observation event timestamps; clock skew internal-only.
- **What Jackrabbit does NOT do to its host** *(predominantly
  negative — §14 Q10)*:
  - opens listening sockets *only* through jackrabbit-jcr-server
    when it is deployed (in pure-library mode, none);
  - installs **no** process-wide signal handlers;
  - spawns **no** child processes from the core;
  - reads system properties (`jackrabbit.*`, etc.) but not
    arbitrary `LD_*` for security-relevant decisions;
  - writes log entries through `slf4j`.

## §5a Build-time and configuration variants

| Knob / configuration | Default | Maintainer stance | Effect |
| --- | --- | --- | --- |
| `repository.xml` `Security` element — `LoginModule`, `AccessManager`, `WorkspaceAccessManager` | jackrabbit defaults (e.g. `SimpleSecurityManager` + `DefaultLoginModule` + `AccessManagerImpl`) *(inferred — §14 Q9)* | required for any meaningful security; misconfiguration voids §8 P1–P3 | what auth + authz Jackrabbit applies |
| Default `admin`/`admin` user | exists, full repository access *(inferred — §14 Q3)* | operator must rotate before production | `BY-DESIGN`; see §3 item 3 |
| jackrabbit-webdav exposure | not exposed unless the operator deploys it | deploying it brings network attack surface in-scope (B2/B6) | network mode trades library posture for service posture |
| XML parser configuration in jackrabbit-webdav | depends on JAXP defaults at the JVM level | **maintainer ruling required**: does jackrabbit-webdav harden its `DocumentBuilderFactory` against XXE / DTD / external-entity expansion, or does it rely on JVM defaults? *(inferred — §14 Q4)* | XXE-class behaviour |
| HTTP Basic vs Digest in jackrabbit-jcr-server | configurable | Digest preferred; Basic-without-TLS is a §11 misuse | which auth mechanism is on the WebDAV port |
| FileDataStore directory permissions | operator-controlled | operator responsibility | binary content readability |
| Persistence manager (bundle / object / database) | bundle PM is the common default *(inferred — §14 Q5)* | operator choice; all are trusted as backends | the choice changes nothing in the security envelope |
| `jackrabbit-aws-ext` S3 data store | optional | when enabled, S3 backend trust applies (§3 item 2) | cloud blob storage |
| Anonymous read (some `JCRRepositoryFactory` configs allow guest login) | configurable | maintainer ruling: is anonymous read a supported posture or a dev-time default? *(inferred — §14 Q11)* | whether `Repository.login(null, ws)` succeeds |

**The insecure-default case.** The single most load-bearing knob is the
default `admin`/`admin` user. The model assumes the operator's posture
is "rotate or remove before production" — i.e. failing to rotate is
`OUT-OF-MODEL: non-default-build`. PMC must ratify this in §14 Q3.

## §6 Assumptions about inputs

### Per-entry-point trust table (in-process API + network)

| Entry point | Parameter | Attacker-controllable in the model? | Caller must enforce |
| --- | --- | --- | --- |
| `Repository.login(Credentials)` | `Credentials` | **yes** (untrusted) | configured `LoginModule` does the work |
| `Session.getNode/Property(path)` | path | **yes** via the authenticated user, filtered through `AccessManager` | nothing |
| `Session.save()` | accumulated transient changes | **yes** via the authenticated user | nothing |
| `QueryManager.createQuery(stmt, lang)` | SQL2 / XPath / SQL1 | **yes** | Jackrabbit parses + plans + filters |
| `AccessControlManager.setPolicy` | policy bytes | only by callers with the right privileges | Jackrabbit gates |
| WebDAV `PROPFIND` request body | XML | **yes** (HTTP-network input) | per §5a, the XML parser must be hardened (XXE) *(inferred — §14 Q4)* |
| WebDAV `LOCK`, `REPORT` request bodies | XML | **yes** | same |
| WebDAV `PUT` body | bytes | **yes** | binary into a JCR property/binary value |
| WebDAV `MOVE`, `COPY` Destination header | URL | **yes** | parsed; URL-decoded path may not contain `..` after decode |
| WebDAV `If` header | conditional state | **yes** | parsed |
| HTTP Basic / Digest credentials | bytes | **yes** | configured by `jackrabbit-jcr-server` |
| Persistence-manager bytes | DB rows / filesystem bytes | **no** — backend is trusted | none |

### Size / shape / rate

- WebDAV / JCR-server accept arbitrary-length request bodies subject to
  the servlet container's limits *(inferred — §14 Q12)*.
- Jackrabbit core imposes no enforced cap on tree depth, child
  count, or query complexity *(inferred — §14 Q12)*.
- Lucene full-text query parsing is unbounded by default; a
  pathological query can burn CPU *(inferred — §14 Q12)*. **No DoS
  protection by default.**

## §7 Adversary model

### Actors

| Actor | In scope? | Capabilities granted |
| --- | --- | --- |
| Authenticated end-user session (limited privileges) | **yes** | read/write the tree under JCR privileges; submit SQL2/XPath queries |
| Authenticated end-user session (broad privileges, non-admin) | partial | only privilege-envelope escapes are in scope |
| Unauthenticated HTTP / WebDAV peer reaching the network port | **yes** (when deployed) | TCP connect; HTTP requests; XML body parse; HTTP Basic/Digest auth attempts |
| Authenticated WebDAV client (HTTP Basic/Digest) | **yes** | same as authenticated JCR session via WebDAV translation |
| Cross-session adversary (one JCR session influencing another) | **yes** | through `AccessManager` cache or commit-hook side effects |
| Hostile persistence-manager backend (DB, FS) | **out of scope** — §3 item 2 |
| Hostile servlet container / operator | **out of scope** — §3 item 1 |
| Hostile XXE-supplying WebDAV body | **yes** (when WebDAV exposed) — *the* main XML-parse adversary |
| Same-JVM attacker code | **partial** — host's container is the boundary *(inferred — §14 Q13)* |
| Side-channel observer | **out of scope** *(§14 Q14)* |
| Quantum adversary | **out of scope** |

The model does **not** include "authenticated-but-Byzantine peer"
(there is no consensus / replication in jackrabbit-core).

## §8 Security properties the project provides

### P1 — Authentication of JCR sessions through the configured `LoginModule` chain (embedded mode)

- **Condition.** `repository.xml` `Security` element defines a
  `LoginModule`; `Repository.login` invokes the configured JAAS chain.
- **Violation symptom.** A `Repository.login` call returns a `Session`
  for credentials the configured chain should have rejected.
- **Severity.** Security-critical (`VALID`).
- *(inferred — §14 Q9)*

### P2 — Authorisation of reads through `AccessManager`

- **Condition.** Non-admin principal session; default `AccessManager`
  configured.
- **Violation symptom.** `Session.getNode(path)` returns an item the
  configured ACLs do not license.
- **Severity.** Security-critical (`VALID`).
- *(inferred — §14 Q9)*

### P3 — Authorisation of writes through `AccessManager` on commit

- **Condition.** Non-admin principal session; default `AccessManager`.
- **Violation symptom.** A `Session.save()` commits a change the
  ACLs do not license.
- **Severity.** Security-critical (`VALID`).
- *(inferred — §14 Q9)*

### P4 — HTTP Basic / Digest authentication at the WebDAV servlet boundary (network mode)

- **Condition.** `jackrabbit-jcr-server` deployed; HTTP Basic or
  Digest realm configured; the servlet container honours the realm.
- **Violation symptom.** A WebDAV request without valid credentials
  reaches B2 trust transitions.
- **Severity.** Security-critical (`VALID`).

### P5 — TLS confidentiality / integrity at the network surface (when configured)

- **Condition.** Servlet container (Tomcat/Jetty) configured for
  HTTPS; jackrabbit-webdav does not strip TLS.
- **Violation symptom.** Cleartext on the wire after TLS is
  configured.
- **Severity.** Security-critical (`VALID`).
- *(inferred — §14 Q15)*

### P6 — XML safety of jackrabbit-webdav body parses

- **Condition.** `DocumentBuilderFactory` configured to disable
  external entities, disallow DOCTYPEs (or restrict them safely), and
  cap entity expansion.
- **Violation symptom.** A WebDAV body triggers XXE (file read,
  SSRF), Billion-Laughs, or DTD-based remote-fetch.
- **Severity.** Security-critical (`VALID`) when reachable from an
  unauthenticated or low-privilege HTTP peer.
- *(inferred — §14 Q4; this is the single highest-confidence-required
  property for the network-exposed deployment)*

### P7 — `Session` per-thread isolation and MVCC-like read consistency

- **Condition.** Default jackrabbit-core MVCC layer; one `Session` per
  thread.
- **Violation symptom.** Cross-session data leakage; a session reading
  data committed after its login without an intentional refresh.
- **Severity.** Correctness by default; security-critical iff
  cross-session leakage crosses an ACL boundary.
- *(inferred — §14 Q16)*

### P8 — Memory safety of safe-Java core

- **Condition.** JVM semantics; no `Unsafe` use in the core security
  paths.
- **Violation symptom.** OOB / UAF / data race observable.
- **Severity.** Security-critical when reachable from §6 input.
- *(inferred — §14 Q17)*

## §9 Security properties the project does NOT provide

### 9.1 No defence against the host application / operator

`SimpleCredentials("admin","admin")` always works against default
config. If the host hands those out, the host has lost. *(inferred —
§14 Q3)*

### 9.2 No defence against a malicious persistence-manager backend

JDBC / FileDataStore / FS / S3 are all trusted. *(inferred —
§14 Q5)*

### 9.3 No defence against a malicious servlet-container operator

Tomcat / Jetty operator owns the realm, the TLS posture, and the
deployment descriptor. *(inferred — §14 Q1)*

### 9.4 No DoS protection against authenticated callers

No enforced cap on query complexity, tree depth, child count,
transient state per session, or WebDAV body size. *(inferred —
§14 Q12)*

### 9.5 No XXE / DTD / external-entity protection in WebDAV bodies (subject to confirmation)

§8 P6 is the inverse — this is the disclaimer if §14 Q4 reveals
that jackrabbit-webdav does *not* harden its parser.

### 9.6 No data-at-rest encryption at the Jackrabbit layer

Delegated to PM / data-store backend.

### 9.7 No constant-time comparison of authentication secrets

Beyond what `LoginModule` / JCA helpers provide. *(inferred —
§14 Q18)*

### 9.8 No defender stance against `loginAdministrative`-equivalent host paths

If jackrabbit's `SimpleSecurityManager` was configured to grant
admin to any subject, that's the configuration speaking, not a bug.

### False-friend properties (call out separately)

- **Default `admin`/`admin` is *not* a deployed-by-default backdoor
  — it is documented; the operator is expected to rotate.** Whether
  failing to do so is `VALID` Jackrabbit or `OUT-OF-MODEL`
  operator-config is §14 Q3.
- **HTTP Basic without TLS is *not* secure authentication.**
  Credentials are base64 in cleartext.
- **HTTP Digest is *not* TLS substitute.** It protects the password
  in transit, not the *content*.
- **WebDAV `LOCK` is an advisory locking primitive, not access
  control.** A holder of `LOCK` does not become an authoriser.

### Well-known attack classes the project does not defend against

- **XXE / Billion-Laughs / DTD-fetch via WebDAV bodies** — *(inferred
  — §14 Q4)*.
- **Authenticated-DoS** (pathological queries / tree shapes) —
  §9.4.
- **HTTP Basic over plaintext** when `jackrabbit-jcr-server` is
  exposed without TLS — `OUT-OF-MODEL: non-default-build` or
  operator misconfiguration.
- **TOCTOU between ACL evaluation and tree write** — *(inferred —
  §14 Q16)*.

## §10 Downstream responsibilities

The host application (and, in network mode, the operator) MUST:

1. **Rotate or remove the default `admin`/`admin` user** before
   exposing the repository to any non-trusted caller *(inferred —
   §14 Q3)*.
2. **Configure a `LoginModule` and an `AccessManager`** that match
   the production trust posture. The shipped defaults are intended
   for demos.
3. **Wrap `jackrabbit-jcr-server` / jackrabbit-webdav with TLS**
   (servlet container HTTPS). HTTP Basic over plaintext is not
   acceptable production posture *(inferred — §14 Q15)*.
4. **Harden the XML parser configuration** if `jackrabbit-webdav` is
   exposed (XXE, DTD, external-entity, billion-laughs)
   *(inferred — §14 Q4)*.
5. **Apply admission control / rate limits** at the servlet
   container / reverse-proxy layer; Jackrabbit does not.
6. **Restrict OS filesystem permissions** on `repository.home`,
   persistence-manager directories, data-store directories, search
   index, and any keystores.
7. **Track CVE advisories on this repo.** Maintenance-mode posture
   means the security mailing list is the right channel for
   reports; binary patches are not produced per Apache convention.
8. **Plan migration to Oak** if the deployment is greenfield or if
   bug-fixes diverge — Oak is the active codebase *(inferred —
   §14 Q2)*.

## §11 Known misuse patterns

- **Failing to rotate default `admin`/`admin`** before production.
- **Deploying jackrabbit-jcr-server / jackrabbit-webdav without
  TLS.** HTTP Basic over plaintext is the default if the operator
  does not flip a switch.
- **Exposing the WebDAV port directly without an authentication
  realm.** WebDAV has no built-in auth; the servlet container does.
- **Treating Jackrabbit as the active codebase and not Oak.** For
  greenfield deployments, Oak is the active product.
- **Using the SQL1 query language without auditing what callers can
  submit.** SQL1 is more permissive than SQL2 / XPath.
- **WebDAV `PROPFIND` `Depth: infinity` against the root** — a
  documented amplification vector for any WebDAV-fronted store
  *(inferred — §14 Q12)*.
- **WebDAV `MOVE` / `COPY` with a `Destination` header pointing
  outside the configured workspace.** ACL-bounded; the configured
  ACL is the gate.

## §11a Known non-findings (recurring false positives)

- **"Default `admin`/`admin` user exists in the shipped
  configuration."** Documented; operator must rotate per §10. →
  `OUT-OF-MODEL: non-default-build` (subject to §14 Q3).
- **"HTTP Basic credentials sent in cleartext when
  `jackrabbit-jcr-server` is exposed over HTTP."** Operator must
  enable TLS *(inferred — §14 Q15)*. → `OUT-OF-MODEL:
  non-default-build`.
- **"DoS via pathological SQL2 / XPath query."** No engine-level
  cap *(inferred — §14 Q12)*. → `BY-DESIGN: property-disclaimed`.
- **"Hardcoded test password / keystore in
  `examples/jackrabbit-firsthops/`, `jackrabbit-jcr-tests/`."**
  Unsupported components. → `OUT-OF-MODEL: unsupported-component`.
- **"Tomcat / Jetty configuration weakness."** Container is the
  operator's. → `OUT-OF-MODEL: adversary-not-in-scope`.
- **"Vulnerability X exists only in jackrabbit-oak."** Oak's
  threat model and triage queue. → `OUT-OF-MODEL:
  unsupported-component` (cross-reference Oak model).
- **"Vulnerability X exists only in jackrabbit-filevault."**
  Filevault's threat model. → `OUT-OF-MODEL:
  unsupported-component` (cross-reference filevault model).
- **"`PROPFIND Depth: infinity` is amplification."** WebDAV
  protocol semantics; ACL-bounded.
- **"Authenticated user with `MODIFY_ACCESS_CONTROL` rewrote ACL to
  grant self more privileges."** Documented privilege behaviour. →
  `BY-DESIGN: property-disclaimed`.

## §12 Conditions that would change this model

- A new network listener in jackrabbit (currently only via
  jackrabbit-jcr-server / jackrabbit-webdav when deployed).
- A change in the shipped default `LoginModule` / `AccessManager`.
- A change in the shipped default `admin`/`admin` posture
  (e.g. randomised on first start).
- A change in jackrabbit-webdav's XML parser default hardening.
- A change in the project's maintenance posture (return to active
  feature development).
- A vulnerability report that cannot be cleanly routed to one of the
  §13 dispositions — that is evidence the model is incomplete.

## §13 Triage dispositions

A report against Jackrabbit receives exactly one of:

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a §8 property via an in-scope §7 adversary using an in-scope §6 input. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 violation, but a §11 misuse pattern can be made harder. Typically no CVE. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of a §6 parameter the model marks trusted. | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires a §7 actor the model excludes. | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in `jackrabbit-jcr-tests/`, `examples/`, or in Oak / filevault. | §3 |
| `OUT-OF-MODEL: non-default-build` | Manifests only under a discouraged or non-default §5a value (un-rotated `admin`, HTTP Basic without TLS). | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a §9 property the project explicitly does not provide. | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a recurring false positive. | §11a |
| `MODEL-GAP` | Cannot be cleanly routed to any of the above — triggers §12 model revision. | §12 |

## §14 Open questions for the maintainers

### Wave 1 — scope, posture, defaults

**Q1.** Confirm or correct the CVE inventory and the project's
disclosure history page at
`https://jackrabbit.apache.org/jcr/jackrabbit-security-reports.html`.
Specifically: what CVEs have been issued against jackrabbit-core /
jackrabbit-webdav in the last 5 years, and which patterns recur in
inbound reports that you close as not-a-bug?
*(maps to §1 reporting, §11a)*

**Q2.** Maintenance posture. Proposed: "active for bug + security
fixes; not active for feature development; Oak is the recommended
codebase for new deployments". Confirm or correct. *(maps to
§3 item 9, §10 item 8)*

**Q3.** The default `admin`/`admin` user. Proposed disposition for
"deployment X has not rotated the default `admin`":
`OUT-OF-MODEL: non-default-build`. Confirm — or has the project
moved to randomised first-start? *(maps to §5a, §10 item 1,
§11a)*

**Q4.** **The XXE question.** Does `jackrabbit-webdav` ship
hardened XML parser configuration (disable external entities,
disallow DOCTYPE, cap entity expansion) for `PROPFIND`, `LOCK`,
`REPORT` body parsing? This is the highest-leverage single question
for the network-exposed deployment. If not, §8 P6 collapses into
§9.5 and §10 item 4 becomes a hard production requirement.
*(maps to §4 B6, §6 WebDAV rows, §8 P6, §9.5)*

### Wave 2 — backends, environment

**Q5.** Are persistence-manager backends (FileDataStore, bundle PM,
database PMs, S3 via jackrabbit-aws-ext) all modelled as trusted
backends — i.e. Jackrabbit does not defend against a malicious
backend? Proposed: yes. *(maps to §3 item 2, §9.2)*

**Q6.** Module inventory: confirm or correct the list of supported
modules vs unsupported / sample / TCK / extension modules. In
particular: is `jackrabbit-standalone` / `jackrabbit-standalone-components`
still part of the repo (the README didn't reach it); is
`jackrabbit-aws-ext` an in-tree module or a separate artefact? *(maps
to §2 component family)*

**Q7.** Servlet-container responsibility: confirmed that Tomcat /
Jetty configuration (TLS, HTTP Basic / Digest realm, request body
size caps, request timeouts) is out-of-model and the operator's
responsibility? *(maps to §3 item 1)*

**Q8.** Java EE container vulnerabilities: confirmed out-of-model?
*(maps to §3 item 8)*

### Wave 3 — `AccessManager`, login

**Q9.** What is the *default* `LoginModule` / `AccessManager` in
the shipped `repository.xml` of jackrabbit-core? The §8 P1–P3
properties hinge on this configuration; this draft uses generic
"configured `LoginModule` / `AccessManager`" placeholders. Please
state the concrete default class names and the JAAS app-name they
register under. *(maps to §5a, §8 P1–P3)*

### Wave 4 — environment, MVCC, memory safety

**Q10.** "What Jackrabbit does NOT do to its host" inventory: no
process-wide signal handlers, no child process spawn (outside
network-mode servlet container), no arbitrary `LD_*` consumption,
no on-disk persistence outside `repository.home`. Confirm any
inaccuracies. *(maps to §5)*

**Q11.** Anonymous read posture. Is `Repository.login(null, ws)`
(guest login without credentials) supported in any default
configuration, or is it always disabled by default? *(maps to
§5a, §6, §7 unauthenticated row)*

**Q12.** WebDAV / JCR / Lucene resource bounds. Confirmed that
Jackrabbit imposes **no** enforced cap on:
- WebDAV request body size beyond servlet container limits;
- JCR query complexity (Lucene-based);
- tree depth / child count;
- transient session state;
- `PROPFIND Depth: infinity` cost?

If correct, all of these belong in §9.4 / §11a as `BY-DESIGN:
property-disclaimed`. *(maps to §6, §9.4)*

**Q13.** Same-JVM attacker code (co-installed servlet, OSGi bundle,
servlet container's other webapps): confirmed out-of-model? The
host container is the security boundary. *(maps to §7, §9)*

**Q14.** Side-channel adversaries: confirmed out-of-model? *(maps
to §3 item 10, §7, §9)*

**Q15.** TLS: confirmed that TLS is **always** the servlet
container's job in network mode, never Jackrabbit's? *(maps to
§8 P5)*

**Q16.** MVCC visibility: how does jackrabbit-core handle
cross-session refresh? Proposed: session reads the revision
captured at login, refreshable via `Session.refresh`. Confirm.
*(maps to §8 P7)*

**Q17.** Memory safety of safe-Java: confirmed that the core
security paths use no `Unsafe` / JNI? *(maps to §8 P8)*

**Q18.** Constant-time comparison of authentication secrets:
confirmed all delegated to JAAS / `LoginModule` / JCA, with no
Jackrabbit-internal `equals` on secrets? *(maps to §9.7)*

### Wave 5 — meta

**Q19.** Should this document live as `docs/threat-model.md` or
under `jackrabbit-jcr-commons/src/site/markdown/`? *(meta)*

**Q20.** Is there an existing Jackrabbit threat-model document
(Confluence, JIRA, mailing-list summary, prior PMC discussion) that
this should reconcile with rather than supersede? *(meta — §3.1a)*

**Q21.** §11a known-non-findings is currently 9 entries, mostly
operator-misconfiguration-shaped. Could the PMC contribute 3–5
patterns the Jackrabbit triage queue sees recur in inbound reports
that you close as not-a-bug? Patterns like "WebDAV exposed without
TLS — operator misconfig, not Jackrabbit" and "DoS via huge
PROPFIND Depth-infinity — WebDAV semantics" would harden §11a
substantially. *(meta — §11a)*

**Q22.** Should this Jackrabbit model cross-reference the (still-to-
be-drafted) `jackrabbit-oak` and `jackrabbit-filevault` models for
items §3 items 5–6? *(meta)*

---

## Appendix: Existing security artefact → §x back-map

Jackrabbit does not currently ship an in-repo `SECURITY.md`. The
de-facto security-policy artefacts are:

- the website page
  `https://jackrabbit.apache.org/jcr/jackrabbit-security-reports.html`
  (CVE history; not fully readable at draft time — §14 Q1);
- the `README.txt` at the top level (project description,
  Java / Maven requirements);
- the `.asf.yaml` (which routes notifications to
  `jackrabbit-dev@…`).

The back-map is sparse because the existing artefacts are mostly
process-only:

| Source | Claim | Lands in |
| --- | --- | --- |
| `README.txt` | "fully conforming implementation of JCR (JSR 170 and 283)" | §2 intended use |
| `README.txt` | Java 11+, Maven 3 build | §5 |
| website security-reports page | CVE history (to be enumerated — §14 Q1) | §1 reporting cross-ref, §11a |
| `.asf.yaml` | notifications routed to `jackrabbit-dev@…` | §1 |

# cedar-artifact-server

[![CI](https://github.com/metadatacenter/cedar-artifact-server/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/metadatacenter/cedar-artifact-server/actions/workflows/ci.yml)

The internal persistence service for CEDAR templates, template elements, template fields, and metadata
instances. It stores artifact JSON in MongoDB; authorization and workspace graph operations belong to
the resource server at the public edge.

The deployable Dropwizard service is in `cedar-artifact-server-application`. The reactor also retains
the legacy `cedar-artifact-server-core` module, which currently contains no Java sources.

## Development

CEDAR backend development uses Java 17. Infrastructure versions and startup order are managed for the
whole local CEDAR stack, so do not install a repository-specific MongoDB version from this README.

From a configured CEDAR workspace:

```bash
export CEDAR_HOME="$HOME/CEDAR"
source "$CEDAR_HOME/cedar-profile-native-develop.sh"
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw test
```

Use `cedar-development/ops/cedar-services.sh` to start, stop, and inspect the service as part of the
native stack. The canonical setup, build, test, and runtime instructions are in the
[CEDAR backend runbook](https://github.com/metadatacenter/cedar-development/blob/develop/ops/BACKEND-RUNBOOK.md).

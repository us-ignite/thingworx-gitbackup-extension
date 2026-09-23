# Connection Server image inputs

The `MED-61352-...DockerFiles...tar.gz` packages in `vendor/` contain the
Dockerfiles, build scripts, and template processor. They do **not** contain
the Connection Server application itself. Download these additional application
archives from PTC Support Downloads alongside the DockerFiles releases:

| Required local file | Supported ThingWorx versions |
| --- | --- |
| `vendor/connection-server-9.2.2.2.zip` | 9.6 |
| `vendor/connection-server-9.3.0.4.zip` | 9.7, 10.0, 10.1 |

Include both application archives in the encrypted vendor archive used by CI.
The Gradle build copies the appropriate ZIP into each build's `staging/`
directory alongside its JDK and template processor. It reports a missing ZIP
before starting a Docker build.

Runtime licences must not be included in the vendor bundle or image layers.
Supply the platform licence through its existing read-only volume mount.

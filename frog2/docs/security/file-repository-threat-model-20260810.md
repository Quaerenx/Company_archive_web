# Archive file-repository threat model

Verified on 2026-08-10. Production/customer files were not read, moved, or
deleted; executable tests used temporary directories only.

## Trust boundaries and lifecycle

```text
authenticated browser
  -> multipart Servlet limits and CSRF
  -> filename/extension/MIME/size validation
  -> bounded stream copy and content-prefix checks
  -> hidden temporary data file
  -> durable close/force
  -> opaque hidden data name (atomic publish)
  -> hidden temporary metadata
  -> durable close/force
  -> opaque metadata name (final publish marker)
  -> list/download only when the pair validates
```

The repository root is outside the webroot. Browser filenames are metadata;
the server storage identifier is a random opaque value.

## Enforced controls

- Maximum 5 files per request, 10 MiB per file, and a bounded multipart
  request size.
- Extension/MIME allowlist; JSP/JSPX/HTML/SVG/script and common executable
  extensions are denied.
- Prefix checks reject MZ, ELF, shebang, JSP/PHP, HTML, SVG, and script
  signatures even when the extension/MIME claims otherwise.
- Canonical/normalized containment checks reject traversal and unsafe names.
- Symlink files and directories are not followed.
- Metadata is bounded to 8 KiB and must match ID, size, and data-file state.
- Downloads use `application/octet-stream`, attachment-only disposition,
  `nosniff`, and `private, no-store`.
- The listing exposes only validated data/metadata pairs and has bounded cursor
  and snapshot-cache limits.

## Crash and orphan recovery

The data file is durable before it is published, and metadata is the final
visibility marker. A normal handled failure rolls back files created by that
request. A process crash may still leave a hidden temp file or one half of a
pair; this is intentionally invisible to listing/download.

On the next upload to the same directory, files older than one hour that match
only Archive-managed temp/pair naming are rechecked and atomically moved into a
hidden `.frog2-quarantine` directory. Fresh files are left alone to avoid
racing another request. Recovery fails closed if the quarantine path is a
symlink or is not a real direct child directory.

Quarantine is recoverable and is not auto-deleted. Operational review and a
separately approved retention job are still required for permanent cleanup.
Recovery currently runs on upload in the affected directory, not as a global
background sweep.

## Residual threats

- No antivirus/content-disarm service is connected. A permitted Office/PDF or
  archive can still contain malware for the person who downloads and opens it.
- Archives are stored, not expanded, so zip bombs do not execute server-side;
  recipients can still be exposed after download.
- MIME is supplied by the client and paired with extension/prefix checks, not
  full document parsing.
- Filesystem durability after a host/power failure ultimately depends on the
  mounted filesystem; directory-entry `fsync` is not portable in this Java
  implementation.
- A full authenticated HTTP upload-list-download lifecycle still requires an
  isolated test environment and must not use customer files.

Antivirus, DLP, content disarm, global cleanup scheduling, and retention policy
are external operational/product decisions and were not added without approval.

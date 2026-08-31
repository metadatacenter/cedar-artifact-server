package org.metadatacenter.cedar.artifact.resources;

/**
 * Descriptions shared by the artifact server's OpenAPI annotations.
 *
 * <p>The four artifact resources answer the same way as each other, so the text describing an ETag,
 * an {@code If-Match} precondition, or a paging parameter is written once here and referenced from
 * every operation that needs it. Annotation values must be compile-time constants, which is why
 * these are strings rather than a shared annotation.
 *
 * <p>The resource server keeps the equivalent wording in an {@code openapi-base.yaml} and reaches it
 * with {@code $ref}. The artifact server has no such document yet, so a {@code $ref} here would
 * dangle and the descriptions are declared inline instead.
 */
final class ArtifactApiDocs {

  static final String ETAG = "Strong validator for the returned representation and its stored revision. "
      + "A representation suffix such as \"7-yaml\" identifies different bytes of the same stored "
      + "revision and is a valid write precondition.";

  static final String IF_MATCH_REQUIRED = "Strong ETag returned by the corresponding GET. `*` accepts "
      + "whichever revision currently exists and therefore deliberately gives up lost-update "
      + "protection. Weak, stale, and malformed validators fail with 412.";

  static final String IF_MATCH_FOR_CREATE_OR_REPLACE = "Required when the identifier already exists and "
      + "the PUT replaces it; omit it when creating an artifact with a new client-supplied "
      + "identifier. Supplying it for an absent artifact fails with 412 rather than creating one.";

  static final String PRECONDITION_FAILED = "The supplied If-Match value is stale, weak, or malformed, "
      + "or the artifact has been written since it was read. Where a revision still exists, the error "
      + "body's `parameters.currentETag` identifies it.";

  static final String PRECONDITION_REQUIRED = "Writing over an artifact that already exists requires its "
      + "current ETag in If-Match.";

  static final String NOT_ACCEPTABLE = "The Accept header names no representation this artifact can be "
      + "served in.";

  static final String VALIDATION_STATUS = "Whether the artifact passed validation against the CEDAR "
      + "model. Every write is validated; an invalid artifact is refused rather than stored.";

  static final String COMPACT_ON_READ = "Return the compact YAML form. It is a lossy read-time "
      + "convenience, so it is accepted only on reads.";

  static final String COMPACT_ON_WRITE = "Not supported on writes, and rejected with 400. A write "
      + "response always renders the full form, and the compact form can not be stored.";

  static final String VERBATIM = "Store the body exactly as supplied, without preserving creation "
      + "provenance or minting missing child identifiers. Requires the verbatim-write permission.";

  static final String LIMIT = "Maximum number of artifacts to return.";

  static final String OFFSET = "Number of artifacts to skip before the first one returned.";

  static final String SUMMARY = "Return only the summary fields configured for this artifact type.";

  static final String FIELD_NAMES = "Comma-separated list of fields to return, in place of the default "
      + "selection. Ignored when `summary` is set.";

  static final String TOTAL_COUNT = "Total number of artifacts of this type, which is the whole "
      + "collection rather than the returned page.";

  static final String LINK = "Paging links for the first, previous, next, and last pages, where those "
      + "exist.";

  static final String BODY_FORMAT = "The body may be JSON or YAML, selected by the Content-Type header. "
      + "A YAML body must be the full or minimal form.";

  static final String READ_FORMAT = "Served as JSON or YAML, selected by the Accept header.";

  private ArtifactApiDocs() {
  }
}

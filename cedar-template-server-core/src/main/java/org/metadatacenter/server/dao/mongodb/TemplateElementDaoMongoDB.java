package org.metadatacenter.server.dao.mongodb;

import checkers.nullness.quals.NonNull;

public class TemplateElementDaoMongoDB extends GenericDaoMongoDB {

  public TemplateElementDaoMongoDB(@NonNull String dbName, @NonNull String collectionName, String
      linkedDataIdBasePath) {
    super(dbName, collectionName, linkedDataIdBasePath);
  }
}

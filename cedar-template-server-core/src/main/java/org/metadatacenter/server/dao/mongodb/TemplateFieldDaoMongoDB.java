package org.metadatacenter.server.dao.mongodb;

import checkers.nullness.quals.NonNull;

public class TemplateFieldDaoMongoDB extends GenericDaoMongoDB {

  public TemplateFieldDaoMongoDB(@NonNull String dbName, @NonNull String collectionName, String
      linkedDataIdBasePath) {
    super(dbName, collectionName, linkedDataIdBasePath);
  }
}

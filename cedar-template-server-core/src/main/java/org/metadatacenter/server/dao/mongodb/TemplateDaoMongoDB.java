package org.metadatacenter.server.dao.mongodb;

import checkers.nullness.quals.NonNull;

public class TemplateDaoMongoDB extends GenericDaoMongoDB {

  public TemplateDaoMongoDB(@NonNull String dbName, @NonNull String collectionName, String linkedDataIdBasePath) {
    super(dbName, collectionName, linkedDataIdBasePath);
  }

}
package org.metadatacenter.templates.mongodb;

import checkers.nullness.quals.NonNull;

public class TemplateDaoMongoDB extends MongoDBDao
{
  public TemplateDaoMongoDB(@NonNull String dbName, @NonNull String collectionName)
  {
    super(dbName, collectionName);
  }

  // Additional operations ...
}

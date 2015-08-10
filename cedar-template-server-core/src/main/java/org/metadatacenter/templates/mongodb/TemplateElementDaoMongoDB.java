package org.metadatacenter.templates.mongodb;

import checkers.nullness.quals.NonNull;

public class TemplateElementDaoMongoDB extends MongoDBDao
{
  public TemplateElementDaoMongoDB(@NonNull String dbName, @NonNull String collectionName)
  {
    super(dbName, collectionName);
  }

  // Additional operations ...
}

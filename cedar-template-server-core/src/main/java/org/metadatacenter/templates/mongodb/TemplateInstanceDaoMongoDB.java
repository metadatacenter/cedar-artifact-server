package org.metadatacenter.templates.mongodb;

import checkers.nullness.quals.NonNull;

public class TemplateInstanceDaoMongoDB extends GenericDaoMongoDB
{
  public TemplateInstanceDaoMongoDB(@NonNull String dbName, @NonNull String collectionName)
  {
    super(dbName, collectionName);
  }

  // Additional operations ...
}

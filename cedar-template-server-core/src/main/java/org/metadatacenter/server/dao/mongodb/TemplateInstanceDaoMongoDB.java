package org.metadatacenter.server.dao.mongodb;

import checkers.nullness.quals.NonNull;
import org.metadatacenter.server.dao.mongodb.GenericDaoMongoDB;

public class TemplateInstanceDaoMongoDB extends GenericDaoMongoDB {

  public TemplateInstanceDaoMongoDB(@NonNull String dbName, @NonNull String collectionName) {
    super(dbName, collectionName);
  }
}

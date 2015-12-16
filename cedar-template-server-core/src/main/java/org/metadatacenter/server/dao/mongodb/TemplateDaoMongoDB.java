package org.metadatacenter.server.dao.mongodb;

import checkers.nullness.quals.NonNull;
import org.metadatacenter.server.dao.mongodb.GenericDaoMongoDB;

import java.util.List;

public class TemplateDaoMongoDB extends GenericDaoMongoDB {

  public TemplateDaoMongoDB(@NonNull String dbName, @NonNull String collectionName, String linkedDataIdBasePath) {
    super(dbName, collectionName, linkedDataIdBasePath);
  }

}
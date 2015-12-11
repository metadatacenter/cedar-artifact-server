package org.metadatacenter.templates.mongodb;

import checkers.nullness.quals.NonNull;
import com.mongodb.MongoClient;

public class MongoFactory
{
  @NonNull private static final MongoClient mongoClient = new MongoClient();

  @NonNull public static MongoClient getClient()
  {
    return mongoClient;
  }
}

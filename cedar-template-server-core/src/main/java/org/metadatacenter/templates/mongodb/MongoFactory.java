package org.metadatacenter.templates.mongodb;

import com.mongodb.MongoClient;

public class MongoFactory
{
  private static final MongoClient mongoClient = new MongoClient();

  public static MongoClient getClient()
  {
    return mongoClient;
  }
}

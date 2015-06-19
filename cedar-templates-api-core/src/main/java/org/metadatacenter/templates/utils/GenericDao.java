package org.metadatacenter.templates.utils;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.List;

public interface GenericDao<K, T>
{

  T create(T element) throws IOException;

  List<T> findAll() throws IOException;

  T find(K id) throws InstanceNotFoundException, IOException;

  T findByLinkedDataId(K id) throws InstanceNotFoundException, IOException;

  T update(K id, T modifications) throws InstanceNotFoundException, IOException;

  void delete(K id) throws InstanceNotFoundException, IOException;

  boolean exists(K id) throws IOException;

  void deleteAll();

}

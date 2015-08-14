package org.metadatacenter.templates.dao;

import checkers.nullness.quals.NonNull;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.List;

public interface GenericDao<K, T>
{
  @NonNull T create(@NonNull T element) throws IOException;

  @NonNull List<T> findAll() throws IOException;

  T find(@NonNull K id) throws IOException;

  T findByLinkedDataId(@NonNull K id) throws IOException;

  @NonNull T update(@NonNull K id, @NonNull T modifications) throws InstanceNotFoundException, IOException;

  @NonNull T updateByLinkedDataId(@NonNull K id, @NonNull T modifications) throws InstanceNotFoundException, IOException;

  void delete(@NonNull K id) throws InstanceNotFoundException, IOException;

  void deleteByLinkedDataId(@NonNull K id) throws InstanceNotFoundException, IOException;

  boolean exists(@NonNull K id) throws IOException;

  boolean existsByLinkedDataId(@NonNull K id) throws IOException;

  void deleteAll();
}

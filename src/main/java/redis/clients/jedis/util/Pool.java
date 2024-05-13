package redis.clients.jedis.util;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisException;

public class Pool<T> extends GenericObjectPool<T> {

  // Legacy
  public Pool(GenericObjectPoolConfig<T> poolConfig, PooledObjectFactory<T> factory) {
    this(factory, poolConfig);
  }

  public Pool(final PooledObjectFactory<T> factory, final GenericObjectPoolConfig<T> poolConfig) {
    super(factory, poolConfig);
  }

  public Pool(final PooledObjectFactory<T> factory) {
    super(factory);
  }

  @Override
  /**
  * Closes the pool. Calls destroy().
  */
  public void close() {
    destroy();
  }

  /**
  * Destroys the pool.
  */
  public void destroy() {
    try {
      super.close();
    } catch (RuntimeException e) {
      throw new JedisException("Could not destroy the pool", e);
    }
  }

  /**
  * Gets a resource from the pool.
  * @return The borrowed resource.
  */
  public T getResource() {
    try {
      return super.borrowObject();
    } catch (JedisException je) {
      throw je;
    } catch (Exception e) {
      throw new JedisException("Could not get a resource from the pool", e);
    }
  }

  /**
  * Returns a resource to the pool.
  * @param resource The resource to be returned.
  */
  public void returnResource(final T resource) {
    if (resource == null) {
      return;
    }
    try {
      super.returnObject(resource);
    } catch (RuntimeException e) {
      throw new JedisException("Could not return the resource to the pool", e);
    }
  }

  /**
  * Returns a broken resource to the pool.
  * @param resource The broken resource to be returned.
  */
  public void returnBrokenResource(final T resource) {
    if (resource == null) {
      return;
    }
    try {
      super.invalidateObject(resource);
    } catch (Exception e) {
      throw new JedisException("Could not return the broken resource to the pool", e);
    }
  }
  
  /**
  * Adds a singular object to the pool.
  */
  public void addObject() {
    try {
      // to add a singular object to the pool
      super.addObject();
    } catch (Exception e) {
      throw new JedisException("Error trying to add idle object", e);
    }
  }

  @Override
  /**
  * Adds multiple objects to the pool.
  * @param count The number of objects to add.
  */
  public void addObjects(int count) {
    try {
      for (int i = 0; i < count; i++) {
        addObject();
      }
    } catch (Exception e) {
      throw new JedisException("Error trying to add idle objects", e);
    }
  }
}

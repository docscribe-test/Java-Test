package redis.clients.jedis.providers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.util.Pool;


@Experimental
public class MultiClusterPooledConnectionProvider implements ConnectionProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Integer, Cluster> multiClusterMap = new ConcurrentHashMap<>();

    private volatile Integer activeMultiClusterIndex = 1;

    private volatile boolean lastClusterCircuitBreakerForcedOpen = false;

    private Consumer<String> clusterFailoverPostProcessor;

    private List<Class<? extends Throwable>> fallbackExceptionList;

    public MultiClusterPooledConnectionProvider(MultiClusterClientConfig multiClusterClientConfig) {

        if (multiClusterClientConfig == null)
            throw new JedisValidationException("MultiClusterClientConfig must not be NULL for MultiClusterPooledConnectionProvider");

        ////////////// Configure Retry ////////////////////

        RetryConfig.Builder retryConfigBuilder = RetryConfig.custom();
        retryConfigBuilder.maxAttempts(multiClusterClientConfig.getRetryMaxAttempts());
        retryConfigBuilder.intervalFunction(IntervalFunction.ofExponentialBackoff(multiClusterClientConfig.getRetryWaitDuration(),
                multiClusterClientConfig.getRetryWaitDurationExponentialBackoffMultiplier()));
        retryConfigBuilder.failAfterMaxAttempts(false); // JedisConnectionException will be thrown
        retryConfigBuilder.retryExceptions(multiClusterClientConfig.getRetryIncludedExceptionList().stream().toArray(Class[]::new));

        List<Class> retryIgnoreExceptionList = multiClusterClientConfig.getRetryIgnoreExceptionList();
        if (retryIgnoreExceptionList != null)
            retryConfigBuilder.ignoreExceptions(retryIgnoreExceptionList.stream().toArray(Class[]::new));

        RetryConfig retryConfig = retryConfigBuilder.build();

        ////////////// Configure Circuit Breaker ////////////////////

        CircuitBreakerConfig.Builder circuitBreakerConfigBuilder = CircuitBreakerConfig.custom();
        circuitBreakerConfigBuilder.failureRateThreshold(multiClusterClientConfig.getCircuitBreakerFailureRateThreshold());
        circuitBreakerConfigBuilder.slowCallRateThreshold(multiClusterClientConfig.getCircuitBreakerSlowCallRateThreshold());
        circuitBreakerConfigBuilder.slowCallDurationThreshold(multiClusterClientConfig.getCircuitBreakerSlowCallDurationThreshold());
        circuitBreakerConfigBuilder.minimumNumberOfCalls(multiClusterClientConfig.getCircuitBreakerSlidingWindowMinCalls());
        circuitBreakerConfigBuilder.slidingWindowType(multiClusterClientConfig.getCircuitBreakerSlidingWindowType());
        circuitBreakerConfigBuilder.slidingWindowSize(multiClusterClientConfig.getCircuitBreakerSlidingWindowSize());
        circuitBreakerConfigBuilder.recordExceptions(multiClusterClientConfig.getCircuitBreakerIncludedExceptionList().stream().toArray(Class[]::new));
        circuitBreakerConfigBuilder.automaticTransitionFromOpenToHalfOpenEnabled(false); // State transitions are forced. No half open states are used

        List<Class> circuitBreakerIgnoreExceptionList = multiClusterClientConfig.getCircuitBreakerIgnoreExceptionList();
        if (circuitBreakerIgnoreExceptionList != null)
            circuitBreakerConfigBuilder.ignoreExceptions(circuitBreakerIgnoreExceptionList.stream().toArray(Class[]::new));

        CircuitBreakerConfig circuitBreakerConfig = circuitBreakerConfigBuilder.build();

        ////////////// Configure Cluster Map ////////////////////

        ClusterConfig[] clusterConfigs = multiClusterClientConfig.getClusterConfigs();
        for (ClusterConfig config : clusterConfigs) {
            GenericObjectPoolConfig<Connection> poolConfig = config.getConnectionPoolConfig();

            String clusterId = "cluster:" + config.getPriority() + ":" + config.getHostAndPort();

            Retry retry = RetryRegistry.of(retryConfig).retry(clusterId);

            Retry.EventPublisher retryPublisher = retry.getEventPublisher();
            retryPublisher.onRetry(event -> log.warn(String.valueOf(event)));
            retryPublisher.onError(event -> log.error(String.valueOf(event)));

            CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(circuitBreakerConfig).circuitBreaker(clusterId);

            CircuitBreaker.EventPublisher circuitBreakerEventPublisher = circuitBreaker.getEventPublisher();
            circuitBreakerEventPublisher.onCallNotPermitted(event -> log.error(String.valueOf(event)));
            circuitBreakerEventPublisher.onError(event -> log.error(String.valueOf(event)));
            circuitBreakerEventPublisher.onFailureRateExceeded(event -> log.error(String.valueOf(event)));
            circuitBreakerEventPublisher.onSlowCallRateExceeded(event -> log.error(String.valueOf(event)));
            circuitBreakerEventPublisher.onStateTransition(event -> log.warn(String.valueOf(event)));

            if (poolConfig != null) {
                multiClusterMap.put(config.getPriority(),
                        new Cluster(new ConnectionPool(config.getHostAndPort(),
                                config.getJedisClientConfig(), poolConfig), retry, circuitBreaker));
            } else {
                multiClusterMap.put(config.getPriority(),
                        new Cluster(new ConnectionPool(config.getHostAndPort(),
                                config.getJedisClientConfig()), retry, circuitBreaker));
            }
        }

        /// --- ///

        this.fallbackExceptionList = multiClusterClientConfig.getFallbackExceptionList();
    }

    /**
    * Increments the active multi-cluster index.
    * Synchronizes field-level to avoid edge cases.
    * @return The updated active multi-cluster index.
    */
    public int incrementActiveMultiClusterIndex() {

        // Field-level synchronization is used to avoid the edge case in which
        // setActiveMultiClusterIndex(int multiClusterIndex) is called at the same time
        synchronized (activeMultiClusterIndex) {

            String originalClusterName = getClusterCircuitBreaker().getName();

            // Only increment if it can pass this validation otherwise we will need to check for NULL in the data path
            if (activeMultiClusterIndex + 1 > multiClusterMap.size()) {

                lastClusterCircuitBreakerForcedOpen = true;

                throw new JedisConnectionException("Cluster/database endpoint could not failover since the MultiClusterClientConfig was not " +
                                                   "provided with an additional cluster/database endpoint according to its prioritized sequence. " +
                                                   "If applicable, consider failing back OR restarting with an available cluster/database endpoint");
            }
            else activeMultiClusterIndex++;

            CircuitBreaker circuitBreaker = getClusterCircuitBreaker();

            // Handles edge-case in which the user resets the activeMultiClusterIndex to a higher priority prematurely
            // which forces a failover to the next prioritized cluster that has potentially not yet recovered
            if (CircuitBreaker.State.FORCED_OPEN.equals(circuitBreaker.getState()))
                incrementActiveMultiClusterIndex();

            else log.warn("Cluster/database endpoint successfully updated from '{}' to '{}'", originalClusterName, circuitBreaker.getName());
        }

        return activeMultiClusterIndex;
    }

    /**
    * Validates the target connection for the specified multi-cluster index.
    * @param multiClusterIndex The index of the multi-cluster.
    */
    public void validateTargetConnection(int multiClusterIndex) {

        CircuitBreaker circuitBreaker = getClusterCircuitBreaker(multiClusterIndex);

        State originalState = circuitBreaker.getState();
        try {
            // Transitions the state machine to a CLOSED state, allowing state transition, metrics and event publishing
            // Safe since the activeMultiClusterIndex has not yet been changed and therefore no traffic will be routed yet
            circuitBreaker.transitionToClosedState();

            try (Connection targetConnection = getConnection(multiClusterIndex)) {
                targetConnection.ping();
            }
        }
        catch (Exception e) {

            // If the original state was FORCED_OPEN, then transition it back which stops state transition, metrics and event publishing
            if (CircuitBreaker.State.FORCED_OPEN.equals(originalState))
                circuitBreaker.transitionToForcedOpenState();

            throw new JedisValidationException(circuitBreaker.getName() + " failed to connect. Please check configuration and try again.", e);
        }
    }

    /**
    * Sets the active multi-cluster index.
    * Field-level synchronization is used to avoid edge cases.
    * @param multiClusterIndex The index of the multi-cluster to set as active.
    */
    public synchronized void setActiveMultiClusterIndex(int multiClusterIndex) {

        // Field-level synchronization is used to avoid the edge case in which
        // incrementActiveMultiClusterIndex() is called at the same time
        synchronized (activeMultiClusterIndex) {

            // Allows an attempt to reset the current cluster from a FORCED_OPEN to CLOSED state in the event that no failover is possible
            if (activeMultiClusterIndex == multiClusterIndex &&
                !CircuitBreaker.State.FORCED_OPEN.equals(getClusterCircuitBreaker(multiClusterIndex).getState()))
                    return;

            if (multiClusterIndex < 1 || multiClusterIndex > multiClusterMap.size())
                throw new JedisValidationException("MultiClusterIndex: " + multiClusterIndex + " is not within " +
                          "the configured range. Please choose an index between 1 and " + multiClusterMap.size());

            validateTargetConnection(multiClusterIndex);

            String originalClusterName = getClusterCircuitBreaker().getName();

            if (activeMultiClusterIndex == multiClusterIndex)
                log.warn("Cluster/database endpoint '{}' successfully closed its circuit breaker", originalClusterName);
            else
                log.warn("Cluster/database endpoint successfully updated from '{}' to '{}'",
                         originalClusterName, getClusterCircuitBreaker(multiClusterIndex).getName());

            activeMultiClusterIndex = multiClusterIndex;
            lastClusterCircuitBreakerForcedOpen = false;
        }
    }

    @Override
    public void close() {
        multiClusterMap.get(activeMultiClusterIndex).getConnectionPool().close();
    }

    @Override
    public Connection getConnection() {
        return multiClusterMap.get(activeMultiClusterIndex).getConnection();
    }

    public Connection getConnection(int multiClusterIndex) {
        return multiClusterMap.get(multiClusterIndex).getConnection();
    }

    @Override
    public Connection getConnection(CommandArguments args) {
        return multiClusterMap.get(activeMultiClusterIndex).getConnection();
    }

    @Override
    public Map<?, Pool<Connection>> getConnectionMap() {
        ConnectionPool connectionPool = multiClusterMap.get(activeMultiClusterIndex).getConnectionPool();
        return Collections.singletonMap(connectionPool.getFactory(), connectionPool);
    }

    public Cluster getCluster() {
        return multiClusterMap.get(activeMultiClusterIndex);
    }

    public CircuitBreaker getClusterCircuitBreaker() {
        return multiClusterMap.get(activeMultiClusterIndex).getCircuitBreaker();
    }

    public CircuitBreaker getClusterCircuitBreaker(int multiClusterIndex) {
        return multiClusterMap.get(multiClusterIndex).getCircuitBreaker();
    }

    public boolean isLastClusterCircuitBreakerForcedOpen() {
        return lastClusterCircuitBreakerForcedOpen;
    }

    public void runClusterFailoverPostProcessor(Integer multiClusterIndex) {
        if (clusterFailoverPostProcessor != null)
            clusterFailoverPostProcessor.accept(getClusterCircuitBreaker(multiClusterIndex).getName());
    }

    public void setClusterFailoverPostProcessor(Consumer<String> clusterFailoverPostProcessor) {
        this.clusterFailoverPostProcessor = clusterFailoverPostProcessor;
    }

    public List<Class<? extends Throwable>> getFallbackExceptionList() {
        return fallbackExceptionList;
    }

    public static class Cluster {

        private final ConnectionPool connectionPool;
        private final Retry retry;
        private final CircuitBreaker circuitBreaker;

        public Cluster(ConnectionPool connectionPool, Retry retry, CircuitBreaker circuitBreaker) {
            this.connectionPool = connectionPool;
            this.retry = retry;
            this.circuitBreaker = circuitBreaker;
        }

        public Connection getConnection() {
            return connectionPool.getResource();
        }

        public ConnectionPool getConnectionPool() {
            return connectionPool;
        }

        public Retry getRetry() {
            return retry;
        }

        public CircuitBreaker getCircuitBreaker() {
            return circuitBreaker;
        }
    }

}
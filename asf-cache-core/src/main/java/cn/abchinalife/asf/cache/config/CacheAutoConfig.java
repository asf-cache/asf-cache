package cn.abchinalife.asf.cache.config;


import cn.abchinalife.asf.cache.serializer.ASFCacheKeyNoMethodSerializer;
import cn.abchinalife.asf.cache.serializer.ASFCacheKeySerializer;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @author xzq
 * @version 1.0.0
 * @date 2018年4月14日 下午3:11:22
 */

@Configuration
@EnableCaching
@ConditionalOnProperty(value = "asf.cache.cache-names")
public class CacheAutoConfig extends CachingConfigurerSupport {

    private static final Logger logger = LoggerFactory.getLogger(CacheAutoConfig.class);

    @Autowired
    private Environment environment;

    @Value("${asf.cache.expireAfterWrite:86444s}")
    private String DEFAULT_TTL;

    @Value("${asf.cache.maximumSize:10000}")
    private Long DEFAULT_MAXSIZE;

    @Value("${asf.cache.refreshAfterWrite:86000s}")
    private String DEFAULT_RTTL;

    @Value("${asf.cache.initialCapacity:100}")
    private int DEFAULT_INITC;

    @Value("${asf.cache.cache-names:asf-cache}")
    private String[] cachenames;

    @Value("${spring.application.name:default}")
    private String appName;


//    public enum Caches {
//        menu(30, 2);
//
//        Caches() {
//        }
//
//        Caches(int ttl) {
//            this.ttl = ttl;
//        }
//
//        Caches(int ttl, int maxSize) {
//            this.ttl = ttl;
//            this.maxSize = maxSize;
//        }
//
//        private int maxSize = DEFAULT_MAXSIZE;    //最大數量
//        private int ttl = DEFAULT_TTL;        //过期时间（秒）
//
//        public int getMaxSize() {
//            return maxSize;
//        }
//
//        public void setMaxSize(int maxSize) {
//            this.maxSize = maxSize;
//        }
//
//        public int getTtl() {
//            return ttl;
//        }
//
//        public void setTtl(int ttl) {
//            this.ttl = ttl;
//        }
//    }


    @Bean
    public CacheLoader<Object, Object> cacheLoader() {

        CacheLoader<Object, Object> cacheLoader = new CacheLoader<Object, Object>() {

            @Override
            public Object load(Object key) throws Exception {
                return null;
            }

            // 重写这个方法将oldValue值返回回去，进而刷新缓存
            @Override
            public Object reload(Object key, Object oldValue) throws Exception {
                return oldValue;
            }
        };

        return cacheLoader;
    }


    @Bean(name = "asfLocalCacheManager")
    @Primary
    public CacheManager caffeineCacheManager() {
//        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
//        cacheManager.setCacheLoader(cacheLoader());
//        return cacheManager;

        SimpleCacheManager manager = new SimpleCacheManager();

        ArrayList<CaffeineCache> caches = new ArrayList<>();

        logger.info(">>>Local CacheManager Init : asfLocalCacheManager");

        for (String cachename : cachenames) {
            caches.add(new CaffeineCache(cachename,
                    Caffeine.newBuilder().recordStats()
                            .expireAfterWrite(parseDuration("expireAfterWrite", environment.getProperty("asf.cache.cachename." + cachename + ".expireAfterWrite", DEFAULT_TTL)),
                                    parseTimeUnit("expireAfterWrite", environment.getProperty("asf.cache.cachename." + cachename + ".expireAfterWrite", DEFAULT_TTL)))
                            .maximumSize(Long.parseLong(environment.getProperty("asf.cache.cachename." + cachename + ".maximumSize", String.valueOf(DEFAULT_MAXSIZE))))
                            .initialCapacity(Integer.parseInt(environment.getProperty("asf.cache.cachename." + cachename + ".initialCapacity", String.valueOf(DEFAULT_INITC))))
                            .refreshAfterWrite(parseDuration("refreshAfterWrite", environment.getProperty("asf.cache.cachename." + cachename + ".refreshAfterWrite", DEFAULT_RTTL)),
                                    parseTimeUnit("refreshAfterWrite", environment.getProperty("asf.cache.cachename." + cachename + ".refreshAfterWrite", DEFAULT_RTTL)))
                            .build(this.cacheLoader()))
            );
            logger.info("<<<asfLocalCacheManager : Local CacheName Init : {}", cachename);
        }

        if (cachenames.length == 0) {
            caches.add(new CaffeineCache(environment.getProperty("spring.application.name", "asf-cache"),
                    Caffeine.newBuilder().recordStats()
                            .expireAfterWrite(parseDuration("expireAfterWrite", DEFAULT_TTL),
                                    parseTimeUnit("expireAfterWrite", DEFAULT_TTL))
                            .maximumSize(DEFAULT_MAXSIZE)
                            .initialCapacity(DEFAULT_INITC)
                            .refreshAfterWrite(parseDuration("refreshAfterWrite", DEFAULT_RTTL),
                                    parseTimeUnit("refreshAfterWrite", DEFAULT_RTTL))
                            .build(this.cacheLoader())));

            logger.info("<<<asfLocalCacheManager : Local CacheName Init default : {}", environment.getProperty("spring.application.name", "asf-cache"));
        }


        manager.setCaches(caches);
        return manager;

    }


    @Bean(name = "asfGlobalCacheManager")
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.lockingRedisCacheWriter(connectionFactory);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
//        FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer(loader.getClass());
        JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer(loader);
//        RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(fastJsonRedisSerializer);
        RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jdkSerializer);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
//        cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(3600));//设置所有的超时时间
        cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(parseSeconds("defaultExpireAfterWrite", DEFAULT_TTL)));//设置所有的超时时间

        //设置单个缓存的超时时间

        logger.info(">>>Global CacheManager Init : asfGlobalCacheManager");

        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();

//        initialCacheConfigurations.put("menu", cacheConfig.entryTtl(Duration.ofSeconds(600)));

        for (String cachename : cachenames) {

            initialCacheConfigurations.put(cachename, cacheConfig.entryTtl(Duration.ofSeconds(parseSeconds("expireAfterWrite", environment.getProperty("asf.cache.cachename." + cachename + ".expireAfterWrite", DEFAULT_TTL)))));

            logger.info("<<<asfGlobalCacheManager : CacheName Init : {}", cachename);

        }

        if (cachenames.length == 0) {

            initialCacheConfigurations.put(environment.getProperty("spring.application.name", "asf-cache"), cacheConfig.entryTtl(Duration.ofSeconds(parseSeconds("expireAfterWrite", environment.getProperty("asf.cache.cachename.expireAfterWrite", DEFAULT_TTL)))));

            logger.info("<<<asfGlobalCacheManager: CacheName Init default : {}", environment.getProperty("spring.application.name", "asf-cache"));
        }


        RedisCacheManager cacheManager = new RedisCacheManager(cacheWriter, cacheConfig, initialCacheConfigurations);

//        cacheManager.afterPropertiesSet();

        return cacheManager;
    }


    @Bean(name = "asfGlobalCacheManager30s")
    public RedisCacheManager redisCacheManager60s(RedisConnectionFactory connectionFactory) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.lockingRedisCacheWriter(connectionFactory);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer(loader);
        RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jdkSerializer);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
        cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(30));//设置所有的超时时间

        logger.info(">>>Global CacheManager Init : asfGlobalCacheManager30s");

        RedisCacheManager cacheManager = new RedisCacheManager(cacheWriter, cacheConfig);

        return cacheManager;
    }

    @Bean(name = "asfGlobalCacheManager1m")
    public RedisCacheManager redisCacheManager1m(RedisConnectionFactory connectionFactory) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.lockingRedisCacheWriter(connectionFactory);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer(loader);
        RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jdkSerializer);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
        cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(60));//设置所有的超时时间

        logger.info(">>>Global CacheManager Init : asfGlobalCacheManager1m");

        RedisCacheManager cacheManager = new RedisCacheManager(cacheWriter, cacheConfig);

        return cacheManager;
    }


    @Bean(name = "asfGlobalCacheManager1h")
    public RedisCacheManager redisCacheManager1h(RedisConnectionFactory connectionFactory) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.lockingRedisCacheWriter(connectionFactory);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer(loader);
        RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jdkSerializer);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
        cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(3600));//设置所有的超时时间

        logger.info(">>>Global CacheManager Init : asfGlobalCacheManager1h");

        RedisCacheManager cacheManager = new RedisCacheManager(cacheWriter, cacheConfig);

        return cacheManager;
    }

    @Bean(name = "asfGlobalCacheManager1d")
    public RedisCacheManager redisCacheManager1d(RedisConnectionFactory connectionFactory) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.lockingRedisCacheWriter(connectionFactory);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer(loader);
        RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jdkSerializer);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
        cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(86400));//设置所有的超时时间

        logger.info(">>>Global CacheManager Init : asfGlobalCacheManager1d");

        RedisCacheManager cacheManager = new RedisCacheManager(cacheWriter, cacheConfig);

        return cacheManager;
    }


////    @Primary
//    public CacheManager redisCacheManager(ObjectMapper objectMapper, RedisConnectionFactory redisConnectionFactory) {
//        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofSeconds(6000))
//                .disableCachingNullValues()
//                .computePrefixWith(cacheName -> "${spring.application.name}".concat(":").concat(cacheName).concat(":"))
//                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJackson2JsonRedisSerializer(objectMapper)));
//
//        return RedisCacheManager.builder(redisConnectionFactory)
//                .cacheDefaults(cacheConfiguration)
//                .build();
//    }

    /**
     * 显示声明缓存key生成器
     * 不要问我为什么要返回一个字符串key.toString()，完全可以返回key。哥就是传说...
     * @return
     */
    @Bean(name = "ASFKeyGenerator")
    @Primary
    public KeyGenerator keyGenerator() {

//        return new ASFKeyGenerator();

        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                Object key = new ASFCacheKeySerializer(target, method, params);
                logger.info("ASFKeyGenerator : {}",appName +":"+ key.toString());
                return appName+":"+ key.toString();
            }
        };
    }

    @Bean(name = "ASFKeyGeneratorNoMethod")
    public KeyGenerator keyGeneratorNoMethod() {

        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                Object key = new ASFCacheKeyNoMethodSerializer(target, params);
                logger.info("ASFKeyGeneratorNoMethod : {}",appName +":"+ key.toString());
                return appName +":"+ key.toString();
            }
        };
    }


    static TimeUnit parseTimeUnit(String key, String value) {
//        Caffeine.requireArgument(value != null && !value.isEmpty(), "value of key %s omitted", new Object[]{key});
        char lastChar = Character.toLowerCase(value.charAt(value.length() - 1));
        switch (lastChar) {
            case 'd':
                return TimeUnit.DAYS;
            case 'h':
                return TimeUnit.HOURS;
            case 'm':
                return TimeUnit.MINUTES;
            case 's':
                return TimeUnit.SECONDS;
            default:
                throw new IllegalArgumentException(String.format("key %s invalid format; was %s, must end with one of [dDhHmMsS]", key, value));
        }

    }

    static long parseDuration(String key, String value) {
//        Caffeine.requireArgument(value != null && !value.isEmpty(), "value of key %s omitted", new Object[]{key});
        String duration = value.substring(0, value.length() - 1);
        return parseLong(key, duration);
    }

    static long parseLong(String key, String value) {
//        Caffeine.requireArgument(value != null && !value.isEmpty(), "value of key %s was omitted", new Object[]{key});

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException var3) {
            throw new IllegalArgumentException(String.format("key %s value was set to %s, must be a long", key, value), var3);
        }
    }

    static long parseSeconds(String key, String value) {

        Long number = parseDuration(key, value);

        TimeUnit timeUnit = parseTimeUnit(key, value);

        Long senconds = timeUnit.toSeconds(number);

        return senconds;
    }

}

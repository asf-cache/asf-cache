package cn.abchinalife.asf.cache.support;

import cn.abchinalife.asf.cache.serializer.ASFCacheKeySerializer;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * @Description: 不要问我为什么要返回一个字符串key.toString()，完全可以返回key。哥就是传说...
 * @Author: xzq
 * @CreateDate: 2018-04-20 10:40
 * @Version: 1.0
 * @Copyright: Copyright (c) 2018
 */

//public class ASFKeyGenerator implements KeyGenerator {
public class ASFKeyGenerator {


//    @Override
//    public Object generate(Object target, Method method, Object... params) {
//        Object key = new ASFCacheKeySerializer(target,method,params);
//        return key.toString();
//    }
}

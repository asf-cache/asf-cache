package cn.abchinalife.asf.cache.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ASFAnalysisActuator {

    String note() default "";

}

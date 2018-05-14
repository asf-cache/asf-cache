package cn.abchinalife.asf.cache.support.aspect;



import cn.abchinalife.asf.cache.annotation.ASFAnalysisActuator;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class ASFAnalysisActuatorAspect {


    final static Logger logger = LoggerFactory.getLogger(ASFAnalysisActuatorAspect.class);

    ThreadLocal<Long> beginTime = new ThreadLocal<>();

    @Pointcut("@annotation(asfAnalysisActuator)")
    public void serviceStatistics(ASFAnalysisActuator asfAnalysisActuator) {
    }

    @Before("serviceStatistics(asfAnalysisActuator)")
    public void doBefore(JoinPoint joinPoint, ASFAnalysisActuator asfAnalysisActuator) {
        // 记录请求到达时间
        beginTime.set(System.currentTimeMillis());
        logger.info("asfAnalysisActuator start note:{}", asfAnalysisActuator.note());
    }

    @After("serviceStatistics(asfAnalysisActuator)")
    public void doAfter(ASFAnalysisActuator asfAnalysisActuator) {
        logger.info("asfAnalysisActuator end time:{}, note:{}", System.currentTimeMillis() - beginTime.get(), asfAnalysisActuator.note());
    }


}

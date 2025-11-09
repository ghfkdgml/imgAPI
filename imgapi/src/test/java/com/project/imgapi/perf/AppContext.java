package com.project.imgapi.perf;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AppContext implements ApplicationContextAware {
    private static ApplicationContext ctx;
    @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException { ctx = applicationContext; }
    public static <T> T get(Class<T> type){ return ctx.getBean(type); }
}

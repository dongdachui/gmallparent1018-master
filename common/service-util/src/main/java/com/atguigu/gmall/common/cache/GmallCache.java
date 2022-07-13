package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD}) // 使用的位置！
@Retention(RetentionPolicy.RUNTIME) // 注解的声明周期！
public @interface GmallCache {

    //  定义一个 prefix 属性 --- 用它来做前缀   组成缓存的key   cache:skuId:info
    String prefix() default "cache:";
    //  后缀
    String suffix() default ":info";
}

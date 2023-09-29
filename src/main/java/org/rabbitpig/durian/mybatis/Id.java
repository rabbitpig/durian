package com.cmcc.coc.ummp.common.common.baseclass;

import java.lang.annotation.*;

/**
 * @author jiajia
 * @date 2019/9/16 11:49 下午
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Id {
    Type type();

    enum Type {
        MYSQL, ID_WORKER;
    }
}
package com.cmcc.coc.ummp.common.common.baseclass;

import java.lang.annotation.*;

/**
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
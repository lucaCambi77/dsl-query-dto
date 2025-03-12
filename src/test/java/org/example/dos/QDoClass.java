package org.example.dos;

import com.querydsl.core.types.dsl.EntityPathBase;

public class QDoClass extends EntityPathBase<DoClass> {

    public static final QDoClass doClass = new QDoClass("doClass");

    public QDoClass(String variable) {
        super(DoClass.class, variable);
    }
}

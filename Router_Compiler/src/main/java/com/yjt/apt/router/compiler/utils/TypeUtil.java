package com.yjt.apt.router.compiler.utils;

import com.yjt.apt.router.compiler.constant.Constant;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class TypeUtil {

    public static int typeExchange(TypeMirror rawType) {
        if (rawType.getKind().isPrimitive()) {  // is java base type
            return rawType.getKind().ordinal();
        }else {
            switch (rawType.toString()) {
                case Constant.BYTE:
                    return TypeKind.BYTE.ordinal();
                case Constant.SHORT:
                    return TypeKind.SHORT.ordinal();
                case Constant.INTEGER:
                    return TypeKind.INT.ordinal();
                case Constant.LONG:
                    return TypeKind.LONG.ordinal();
                case Constant.FLOAT:
                    return TypeKind.FLOAT.ordinal();
                case Constant.DOUBEL:
                    return TypeKind.DOUBLE.ordinal();
                case Constant.BOOLEAN:
                    return TypeKind.BOOLEAN.ordinal();
                case Constant.STRING:
                default:
                    return TypeKind.OTHER.ordinal();
            }
        }
    }
}

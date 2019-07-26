package com.tekartik.sqflite;

import io.flutter.plugin.common.MethodCall;

import static com.tekartik.sqflite.Constant.PARAM_LOG_LEVEL;

public class LogLevel {

    public static final int none = 0;
    public static final int sql = 1;
    public static final int verbose = 2;

    static Integer getLogLevel(MethodCall methodCall) {
        return methodCall.argument(PARAM_LOG_LEVEL);
    }

    static boolean hasSqlLevel(int level) {
        return level >= sql;
    }

    static boolean hasVerboseLevel(int level) {
        return level >= verbose;
    }
}

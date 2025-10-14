package edu.ncu.vvaicoding.utils;

import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;

public class ThrowUtils {
    public static void throwIf(boolean error, ErrorCode errorCode, String message) {
        if (error) {
            throw new BusinessException(errorCode, message);
        }
    }
    public static void throwIf(boolean error, ErrorCode errorCode ) {
        if (error) {
            throw new BusinessException(errorCode);
        }
    }
}

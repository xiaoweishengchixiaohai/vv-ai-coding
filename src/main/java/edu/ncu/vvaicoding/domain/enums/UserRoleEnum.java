package edu.ncu.vvaicoding.domain.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举类
 * 用于定义系统中不同的用户角色及其对应的中文名称和值
 */
@Getter
public enum UserRoleEnum {


    // 用户角色枚举实例
    USER("用户", "user"),    // 普通用户角色
    ADMIN("管理员", "admin"), // 管理员角色
    BAN("永久封禁", "ban"); // 封禁角色

    // 角色中文名称
    private final String text;

    // 角色对应的值
    private final String value;

    /**
     * 枚举构造函数
     *
     * @param text  角色中文名称
     * @param value 角色对应的值
     */
    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

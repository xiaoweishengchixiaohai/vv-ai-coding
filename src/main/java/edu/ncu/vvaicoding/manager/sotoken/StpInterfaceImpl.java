package edu.ncu.vvaicoding.manager.sotoken;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.enums.UserRoleEnum;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static edu.ncu.vvaicoding.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserVO user = (UserVO) StpUtil.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);

        if(user == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }

        if (user.getUserRole().equals(UserRoleEnum.BAN.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该账号已经永久封禁");
        }

        ArrayList<String> list = new ArrayList<>();
        list.add(UserRoleEnum.USER.getValue());
        if (user.getUserRole().equals(UserRoleEnum.USER.getValue())) {
            return list;
        }

        list.add(UserRoleEnum.ADMIN.getValue());
        return list;
    }

}

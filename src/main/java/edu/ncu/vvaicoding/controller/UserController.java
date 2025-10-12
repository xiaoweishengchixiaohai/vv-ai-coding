package edu.ncu.vvaicoding.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.ncu.vvaicoding.common.BaseResponse;
import edu.ncu.vvaicoding.common.DeleteRequest;
import edu.ncu.vvaicoding.common.ResultUtils;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.dto.user.*;
import edu.ncu.vvaicoding.domain.enetity.User;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import edu.ncu.vvaicoding.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static edu.ncu.vvaicoding.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户模块
 */

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @return 用户基本信息
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        Long userId = userService.userRegister(userRegisterRequest);

        return ResultUtils.success(userId);
    }

    /**
     * 用户登录
     *
     * @return 用户基本信息
     */
    @PostMapping("/login")
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        UserVO userVO = userService.userLogin(userLoginRequest);

        return ResultUtils.success(userVO);
    }

    /**
     * 用户登出
     *
     * @return 用户基本信息
     */
    @GetMapping("/logout")
    public BaseResponse<Boolean> userLogout() {
        StpUtil.logout();
        return ResultUtils.success(true);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("currentMe")
    @SaCheckLogin
    public BaseResponse<UserVO> getCurrentMe() {
        UserVO user = (UserVO) StpUtil.getSession().get(USER_LOGIN_STATE);
        return ResultUtils.success(user);
    }

    /**
     * 更新用户信息
     */
    @PostMapping("updateInfo")  // HTTP POST请求映射到/updateInfo路径
    @SaCheckLogin  // 登录检查注解，确保只有登录用户可以访问此接口
    public BaseResponse<UserVO> updateInfo(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 检查请求参数是否为空
        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        UserVO userVO = userService.userUpdateInfo(userUpdateRequest);

        return ResultUtils.success(userVO);
    }

    /**
     * 更新用
     */
    @PostMapping("/updateAvatar")
    @SaCheckLogin
    public BaseResponse<UserVO> updateAvatar(@RequestPart("file") MultipartFile file, UserUpdateAvatarRequest userUpdateAvatarRequest) {
        if (file == null || userUpdateAvatarRequest == null || userUpdateAvatarRequest.getId() == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");

        UserVO userVO = userService.userUpdateAvatar(userUpdateAvatarRequest.getId(), file);

        return ResultUtils.success(userVO);
    }

    /**
     * 通过id获取用户信息(用户)
     */
    @GetMapping("/getUserInfoById")
    public BaseResponse<UserVO> getUserInfoById(Long id) {
        if (id == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");

        UserVO userVO = userService.toUserVO(userService.lambdaQuery().eq(User::getId, id).one());
        if (ObjUtil.isEmpty(userVO)) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
        return ResultUtils.success(userVO);
    }

    /**
     * 通过id获取用户信息(管理员)
     */
    @GetMapping("/getUserInfoByIdAdmin")
    @SaCheckRole("admin")
    public BaseResponse<User> getUserInfoByIdAdmin(Long id) {
        if (id == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");

        User user = userService.lambdaQuery().eq(User::getId, id).one();
        if (ObjUtil.isEmpty(user)) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
        return ResultUtils.success(user);
    }

    /**
     * 搜索用户(分页)
     */
    @PostMapping("adminSearchUserPage")
    @SaCheckRole("admin")
    public BaseResponse<Page<User>> adminSearchUserPage(@RequestBody AdminQueryRequest adminQueryRequest) {
        if (adminQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        Page<User> userPage = userService.adminSearchUserPage(adminQueryRequest);

        return ResultUtils.success(userPage);
    }

    /**
     * 删除用户(管理员)
     */
    @PostMapping("adminDeleteUser")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> adminDeleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        boolean login = StpUtil.isLogin(deleteRequest.getId());
        if (login) {
            StpUtil.logout(deleteRequest.getId());
        }

        boolean result = userService.removeById(deleteRequest.getId());

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }

        return ResultUtils.success(true);

    }

    /**
     * 更新用户信息(管理员)
     */
    @PostMapping("adminUpdateInfo")
    @SaCheckRole("admin")
    public BaseResponse<User> adminUpdateInfo(@RequestBody AdminUpdateRequest adminUpdateRequest) {
        if (adminUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        User user = userService.adminUpdateInfo(adminUpdateRequest);

        return ResultUtils.success(user);
    }

    /**
     * 新增用户（管理员）
     */
    @PostMapping("adminAddUser")
    @SaCheckRole("admin")
    public BaseResponse<Long> adminAddUser(@RequestBody AdminAddRequest adminAddRequest) {
        if (adminAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        Long id = userService.adminAddUser(adminAddRequest);

        return ResultUtils.success(id);
    }

    @GetMapping("adminBan")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> adminBan(Long id) {
        if (id == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");

        boolean login = StpUtil.isLogin(id);
        if (login) StpUtil.logout(id);

        StpUtil.disable(id, 60 * 60 * 24 * 7);

        return ResultUtils.success(true);
    }
}

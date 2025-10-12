package edu.ncu.vvaicoding.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.model.PutObjectResult;
import edu.ncu.vvaicoding.config.COSClientConfig;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.dto.user.*;
import edu.ncu.vvaicoding.domain.enetity.User;
import edu.ncu.vvaicoding.domain.enums.UserRoleEnum;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import edu.ncu.vvaicoding.manager.cos.COSUpload;
import edu.ncu.vvaicoding.service.UserService;
import edu.ncu.vvaicoding.mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static edu.ncu.vvaicoding.constant.UserConstant.*;

/**
 * @author winter
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-10-10 11:50:29
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private COSUpload cosUpload;

    @Resource
    private COSClientConfig cosClientConfig;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 需要保存的用户对象，包含用户的完整信息
     * @return 返回用户的id
     */
    @Override
    public Long userRegister(UserRegisterRequest userRegisterRequest) {
        //参数校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册参数为空");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (!StrUtil.isAllNotEmpty(userAccount, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册参数为空");
        }

        if (userAccount.length() < 8 || userAccount.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于8位或大于20位");
        }

        boolean exists = this.lambdaQuery().eq(User::getUserAccount, userAccount).exists();

        if (exists) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }

        if (password.length() < 8 || password.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位或大于20位");
        }

        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        MD5 md5 = MD5.create();
        password = md5.digestHex(SALT + password);

        // 封装数据库存储对象
        User user = new User();

        user.setUserAccount(userAccount);
        user.setUserPassword(password);
        user.setUserName("vv-" + RandomUtil.randomNumbers(6));
        user.setUserAvatar(DEFAULT_AVATAR);
        user.setUserRole(UserRoleEnum.USER.getValue());

        this.save(user);

        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 需要登录的用户对象，包含用户的完整信息
     * @return 返回用户的基本信息
     */
    @Override
    public UserVO userLogin(UserLoginRequest userLoginRequest) {
        //参数校验
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录参数为空");
        }
        String userAccount = userLoginRequest.getUserAccount();
        String password = userLoginRequest.getUserPassword();
        if (!StrUtil.isAllNotEmpty(userAccount, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录参数为空");
        }

        if (userAccount.length() < 8 || userAccount.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于8位或大于20位");
        }
        if (password.length() < 8 || password.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位或大于20位");
        }
        MD5 md5 = MD5.create();
        password = md5.digestHex(SALT + password);
        //查询用户
        User user = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        }
        if (user.getUserRole().equals(UserRoleEnum.BAN.getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已被永久封禁");
        }


        if (!user.getUserPassword().equals(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        }

        boolean disable = StpUtil.isDisable(user.getId());
        if (disable) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已被封禁,封禁时间还剩" + StpUtil.getDisableTime(user.getId()) + "秒");
        }
        //登录
        StpUtil.login(user.getId(), userLoginRequest.getRememberMe());
        UserVO userVO = toUserVO(user);
        StpUtil.getSession().set(USER_LOGIN_STATE, userVO);

        return userVO;
    }

    /**
     * 用户更新信息
     *
     * @param userUpdateRequest 需要更新的用户对象，包含用户的部分信息
     * @return 返回用户的基本信息
     */
    @Override
    public UserVO userUpdateInfo(UserUpdateRequest userUpdateRequest) {
        //参数校验
        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新参数为空");
        }
        Long id = userUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新参数为空");
        }

        User user = this.lambdaQuery().eq(User::getId, id).one();

        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        String userAccount = userUpdateRequest.getUserAccount();

        String userPassword = userUpdateRequest.getUserPassword();

        String checkPassword = userUpdateRequest.getCheckPassword();

        String userName = userUpdateRequest.getUserName();

        String userProfile = userUpdateRequest.getUserProfile();

        if(userProfile.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人简介不能超过30个字");
        }
        if(userName.length() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不能超过10个字");
        }

        if (userAccount != null && !userAccount.equals(user.getUserAccount())) {
            if (userAccount.length() < 8) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于8位");
            }

            if (this.lambdaQuery().eq(User::getUserAccount, userAccount).exists()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
            }
        }

        if (userPassword != null) {
            if (userPassword.length() < 8) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
            }

            if (!userPassword.equals(checkPassword)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
            }

            userPassword = MD5.create().digestHex(SALT + userPassword);
        }

        // 封装数据库存储对象
        BeanUtil.copyProperties(userUpdateRequest, user);
        if (userPassword != null) user.setUserPassword(userPassword);

        this.updateById(user);

        UserVO userVO = toUserVO(user);

        StpUtil.getSession().set(USER_LOGIN_STATE, userVO);

        return userVO;
    }

    /**
     * 用户更新头像
     *
     * @param id            用户id
     * @param multipartFile 用户头像
     * @return 返回用户的基本信息
     */
    @Override
    public UserVO userUpdateAvatar(Long id, MultipartFile multipartFile) {
        ////参数校验
        if (multipartFile == null || id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新参数为空");
        }

        User user = this.lambdaQuery().eq(User::getId, id).one();
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        String oldUrl = user.getUserAvatar();

        File file = null;
        try {
            //上传图片
            file = FileUtil.createTempFile();
            multipartFile.transferTo(file);
            validAvatar(multipartFile, file);

            PutObjectResult upload = cosUpload.upload(file);

            if (upload == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
            }

            //操作数据库,封装返回对象
            String url = cosClientConfig.getHost() + "/" + upload.getCiUploadResult().getOriginalInfo().getKey();

            user.setId(id);
            user.setUserAvatar(url);
            boolean result = this.updateById(user);

            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据上传失败");
            }

            //删除原头像
            if (!DEFAULT_AVATAR.equals(oldUrl)) {
                boolean delete = cosUpload.delete(oldUrl);
                if (!delete) {
                    log.info("删除原头像失败{}", oldUrl);
                }
            }

            UserVO userVO = toUserVO(user);
            StpUtil.getSession().set(USER_LOGIN_STATE, userVO);
            return userVO;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.info("删除临时文件失败{}", file.getAbsolutePath());
                }
            }

        }
    }

    private static void validAvatar(MultipartFile multipartFile, File file) {
        long size = multipartFile.getSize();
        long MAX_SIZE = 1024 * 1024 * 5;
        if (size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过5MB");
        }

        String type = FileUtil.getType(file);
        String[] types = {"jpg", "png", "webp", "jpeg"};
        if (!ArrayUtil.contains(types, type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型暂不支持");
        }
    }

    @Override
    public UserVO toUserVO(User user) {
        UserVO userVO = new UserVO();

        BeanUtil.copyProperties(user, userVO);

        return userVO;
    }

    @Override
    public Page<User> adminSearchUserPage(AdminQueryRequest adminSearchRequest) {
        if (adminSearchRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数为空");
        }

        return this.page(new Page<>(adminSearchRequest.getPageNum(),
                adminSearchRequest.getPageSize()), getQueryWrapper(adminSearchRequest));
    }

    @Override
    public User adminUpdateInfo(AdminUpdateRequest adminUpdateRequest) {
        //参数校验
        if (adminUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新参数为空");
        }

        String userAccount = adminUpdateRequest.getUserAccount();

        String userPassword = adminUpdateRequest.getUserPassword();
        String role = adminUpdateRequest.getUserRole();

        Long id = adminUpdateRequest.getId();

        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新参数为空");
        }

        User user = this.lambdaQuery().eq(User::getId, id).one();

        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        if (userAccount != null) {
            if (userAccount.length() < 8) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于8位");
            }

            if (this.lambdaQuery().eq(User::getUserAccount, userAccount).exists()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
            }
        }

        if (userPassword != null) {
            if (userPassword.length() < 8) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
            }

            userPassword = MD5.create().digestHex(SALT + userPassword);
        }

        if (role != null) {
            UserRoleEnum enumByValue = UserRoleEnum.getEnumByValue(role);
            if (enumByValue == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色错误");
            }
        }


        // 封装数据库存储对象
        BeanUtil.copyProperties(adminUpdateRequest, user);
        if (userPassword != null) user.setUserPassword(userPassword);

        this.updateById(user);

        UserVO userVO = toUserVO(user);

        StpUtil.getSessionByLoginId(user.getId()).set(USER_LOGIN_STATE, userVO);
        if (role != null) {
            StpUtil.logout(userVO.getId());
        }

        return user;
    }

    @Override
    public Long adminAddUser(AdminAddRequest adminAddRequest) {
        if (adminAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加参数为空");
        }

        String userAccount = adminAddRequest.getUserAccount();
        String userPassword = adminAddRequest.getUserPassword();

        if (!StrUtil.isAllNotEmpty(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册参数为空");
        }

        if (userAccount.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于8位");
        }

        boolean exists = this.lambdaQuery().eq(User::getUserAccount, userAccount).exists();

        if (exists) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }

        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
        }

        MD5 md5 = MD5.create();
        userPassword = md5.digestHex(SALT + userPassword);

        // 封装数据库存储对象
        User user = new User();

        user.setUserAccount(userAccount);
        user.setUserPassword(userPassword);
        user.setUserName("vv-" + UUID.randomUUID());
        user.setUserAvatar(DEFAULT_AVATAR);
        user.setUserRole(UserRoleEnum.USER.getValue());

        this.save(user);

        return user.getId();

    }


    private QueryWrapper<User> getQueryWrapper(AdminQueryRequest adminQueryRequest) {
        String userAccount = adminQueryRequest.getUserAccount();
        String userName = adminQueryRequest.getUserName();
        String userProfile = adminQueryRequest.getUserProfile();
        String userRole = adminQueryRequest.getUserRole();
        Date createStartTime = adminQueryRequest.getCreateStartTime();
        Date createEndTime = adminQueryRequest.getCreateEndTime();
        String sortField = adminQueryRequest.getSortField();
        String sortOrder = adminQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.ge(ObjectUtil.isNotNull(createStartTime), "createTime", createStartTime);
        queryWrapper.le(ObjectUtil.isNotNull(createEndTime), "createTime", createEndTime);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("asc"), sortField);

        return queryWrapper;
    }
}





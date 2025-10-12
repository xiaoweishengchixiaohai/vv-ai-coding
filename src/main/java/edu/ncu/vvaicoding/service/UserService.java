package edu.ncu.vvaicoding.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.dto.user.*;
import edu.ncu.vvaicoding.domain.enetity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
* @author winter
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-10-10 11:50:29
*/
public interface UserService extends IService<User> {

    Long userRegister(UserRegisterRequest userRegisterRequest);

    UserVO userLogin(UserLoginRequest userLoginRequest);

    UserVO userUpdateInfo(UserUpdateRequest userUpdateRequest);

    UserVO userUpdateAvatar(Long id, MultipartFile multipartFile);

    UserVO toUserVO(User user);

    Page<User> adminSearchUserPage(AdminQueryRequest adminSearchRequest);

    User adminUpdateInfo(AdminUpdateRequest adminUpdateRequest);

    Long adminAddUser(AdminAddRequest adminAddRequest);
}

package edu.ncu.vvaicoding.domain.dto.user;

import lombok.Data;

@Data
public class AdminAddRequest {
    //账号
    private String userAccount;
    //密码
    private String userPassword;
}

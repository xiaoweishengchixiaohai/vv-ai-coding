package edu.ncu.vvaicoding.domain.dto.user;

import lombok.Data;

@Data
public class UserLoginRequest {

    private String userAccount;

    private String userPassword;

    private Boolean rememberMe  = false;
}

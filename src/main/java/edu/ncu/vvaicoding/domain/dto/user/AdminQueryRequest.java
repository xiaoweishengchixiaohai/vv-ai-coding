package edu.ncu.vvaicoding.domain.dto.user;

import edu.ncu.vvaicoding.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminQueryRequest extends PageRequest {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;
    /**
     * 创建时间 start
     */
    private Date createStartTime;

    /**
     * 创建时间 end
     */
    private Date createEndTime;

}

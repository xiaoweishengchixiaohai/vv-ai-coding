package edu.ncu.vvaicoding.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.ncu.vvaicoding.common.BaseResponse;
import edu.ncu.vvaicoding.common.ResultUtils;
import edu.ncu.vvaicoding.constant.UserConstant;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.dto.chatHistory.ChatHistoryQueryRequest;
import edu.ncu.vvaicoding.domain.enetity.ChatHistory;
import edu.ncu.vvaicoding.exception.ErrorCode;
import edu.ncu.vvaicoding.service.ChatHistoryService;
import edu.ncu.vvaicoding.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {


    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    @SaCheckLogin
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime
    ) {
        UserVO user = (UserVO) StpUtil.getSession().get(UserConstant.USER_LOGIN_STATE);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, user);
        return ResultUtils.success(result);
    }


    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @SaCheckRole("admin")
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper<ChatHistory> queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }


}

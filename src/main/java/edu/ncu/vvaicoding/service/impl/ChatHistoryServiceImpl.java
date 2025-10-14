package edu.ncu.vvaicoding.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.dto.chatHistory.ChatHistoryQueryRequest;
import edu.ncu.vvaicoding.domain.enetity.App;
import edu.ncu.vvaicoding.domain.enetity.ChatHistory;
import edu.ncu.vvaicoding.domain.enums.ChatHistoryMessageTypeEnum;
import edu.ncu.vvaicoding.domain.enums.UserRoleEnum;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import edu.ncu.vvaicoding.service.AppService;
import edu.ncu.vvaicoding.service.ChatHistoryService;
import edu.ncu.vvaicoding.mapper.ChatHistoryMapper;
import edu.ncu.vvaicoding.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author winter
 * @description 针对表【chat_history(对话历史)】的数据库操作Service实现
 * @createDate 2025-10-13 11:09:06
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);
        ChatHistory chatHistory = ChatHistory.builder().appId(appId).message(message).messageType(messageType).userId(userId).build();
        return this.save(chatHistory);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<ChatHistory>().eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 获取查询包装类
     */
    @Override
    public QueryWrapper<ChatHistory> getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(id != null, "id", id).like(message != null, "message", message).eq(messageType != null, "messageType", messageType).eq(appId != null, "appId", appId).eq(userId != null, "userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, StrUtil.equals(sortOrder, "asc"), sortField);
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy(true, StrUtil.equals(sortOrder, "asc"), "createTime");
        }
        return queryWrapper;
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, UserVO user) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
        boolean isCreator = app.getUserId().equals(user.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper<ChatHistory> queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public int initChatMessage(Long appId, MessageWindowChatMemory chatMessage, int maxCount) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "appId不能为空");
        }

        if (chatMessage == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
        }

        if (maxCount <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
        }

        List<ChatHistory> list = this.lambdaQuery().eq(ChatHistory::getAppId, appId).orderByDesc(ChatHistory::getCreateTime).last("limit 1," + maxCount).list();

        if (list == null || list.isEmpty()) {
            return 0;
        }

        chatMessage.clear();
        list = list.reversed();

        for (ChatHistory chatHistory : list) {
            if (ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())) {
                chatMessage.add(UserMessage.from(chatHistory.getMessage()));
            }
            if (ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())) {
                chatMessage.add(AiMessage.from(chatHistory.getMessage()));
            }
        }

        return list.size();
    }
}





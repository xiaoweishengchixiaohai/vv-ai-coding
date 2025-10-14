package edu.ncu.vvaicoding.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.dto.chatHistory.ChatHistoryQueryRequest;
import edu.ncu.vvaicoding.domain.enetity.ChatHistory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
* @author winter
* @description 针对表【chat_history(对话历史)】的数据库操作Service
* @createDate 2025-10-13 11:09:06
*/
public interface ChatHistoryService extends IService<ChatHistory> {

    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    boolean deleteByAppId(Long appId);

    QueryWrapper<ChatHistory> getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               UserVO user);

    int initChatMessage(Long appId, MessageWindowChatMemory chatMessage, int maxCount);
}

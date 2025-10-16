package edu.ncu.vvaicoding.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;
import edu.ncu.vvaicoding.constant.AppConstant;
import edu.ncu.vvaicoding.cores.AICodeGenerateFacade;
import edu.ncu.vvaicoding.cores.builder.VueProjectBuilder;
import edu.ncu.vvaicoding.cores.processor.StreamHandlerExecutor;
import edu.ncu.vvaicoding.domain.VO.AppVO;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.dto.app.AppQueryRequest;
import edu.ncu.vvaicoding.domain.enetity.App;
import edu.ncu.vvaicoding.domain.enetity.User;
import edu.ncu.vvaicoding.domain.enums.ChatHistoryMessageTypeEnum;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import edu.ncu.vvaicoding.mapper.AppMapper;
import edu.ncu.vvaicoding.service.AppService;
import edu.ncu.vvaicoding.service.ChatHistoryService;
import edu.ncu.vvaicoding.service.UserService;
import edu.ncu.vvaicoding.utils.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.ncu.vvaicoding.constant.AppConstant.*;

/**
 * @author winter
 * @description 针对表【app(应用)】的数据库操作Service实现
 * @createDate 2025-10-12 19:32:15
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>
        implements AppService {

    @Resource
    private UserService userService;


    @Resource
    private AICodeGenerateFacade aiCodeGenerateFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private WebScreenshotServiceImpl webScreenshotService;




    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        // 创建AppVO对象，用于存储转换后的应用信息
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.toUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return new QueryWrapper<App>().eq(id != null, "id", id)
                .like(appName != null, "appName", appName)
                .like(cover != null, "cover", cover)
                .like(initPrompt != null, "initPrompt", initPrompt)
                .eq(codeGenType != null, "codeGenType", codeGenType)
                .eq(deployKey != null, "deployKey", deployKey)
                .eq(priority != null, "priority", priority)
                .eq(userId != null, "userId", userId)
                .orderBy(sortField != null, "ascend".equals(sortOrder), sortField);
    }

    @Override
    public List<AppVO> toAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::toUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, UserVO user) {
        if (appId == null || appId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        }

        if (message == null || message.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        App app = this.getById(appId);

        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        if (!app.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权限访问");
        }
        CodeGenTypeEnum typeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());

        if (typeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用类型错误");
        }
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 通过校验后，添加用户消息到对话历史
        boolean result = chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), user.getId());
        if (!result) {
            log.info("添加用户消息到对话历史失败");
        }
        // 6. 调用 AI 生成代码（流式）
        Flux<String> flux = aiCodeGenerateFacade.generateCode(message, codeGenTypeEnum, appId);

        return streamHandlerExecutor.process(codeGenTypeEnum, flux, appId, user);

    }

    @Override
    public String deployApp(Long appId, UserVO user) {
        //参数校验
        if (appId == null || appId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        }

        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户信息不能为空");
        }

        App app = this.getById(appId);

        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        if (!app.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权限访问");
        }

        String deployKey = app.getDeployKey();

        //构建部署的唯一标识
        if (deployKey == null || deployKey.isEmpty()) {
            deployKey = RandomUtil.randomString(6);
        }

        //部署路径
        String deployPath = CODE_DEPLOY_ROOT_DIR + "/" + app.getCodeGenType() + "_" + deployKey;

        String sourcePath = CODE_OUTPUT_ROOT_DIR + "/" + app.getCodeGenType() + "_" + appId;

        CodeGenTypeEnum typeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        if (typeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }

        if (typeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // 异步构造 Vue 项目
            String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
            boolean result = vueProjectBuilder.buildProject(projectPath);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败");
            }

            sourcePath = projectPath + "/dist";
        }

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists() || !sourceFile.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码不存在");
        }
        //复制文件到部署路径
        try {
            FileUtil.copyContent(sourceFile, new File(deployPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败");
        }

        App newApp = new App();

        newApp.setId(appId);
        newApp.setDeployKey(deployKey);
        this.updateById(newApp);
        String deployUrl = CODE_DEPLOY_HOST + "/" + app.getCodeGenType() + "_" + deployKey;
        generateAppScreenshotAsync(appId, deployUrl);

        return deployUrl;
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = webScreenshotService.takeScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }


    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            boolean result = chatHistoryService.deleteByAppId(appId);
            if (!result) {
                log.error("删除应用关联对话历史失败");
            }
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

}





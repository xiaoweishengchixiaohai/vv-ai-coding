package edu.ncu.vvaicoding.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ncu.vvaicoding.domain.VO.AppVO;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.domain.dto.app.AppQueryRequest;
import edu.ncu.vvaicoding.domain.enetity.App;
import com.baomidou.mybatisplus.extension.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
* @author winter
* @description 针对表【app(应用)】的数据库操作Service
* @createDate 2025-10-12 19:32:15
*/
public interface AppService extends IService<App> {

    AppVO getAppVO(App app);

    QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVO> toAppVOList(List<App> appList);

    Flux<String> chatToGenCode(Long appId, String message, UserVO user);

    String deployApp(Long appId, UserVO user);


    void generateAppScreenshotAsync(Long appId, String appUrl);
}

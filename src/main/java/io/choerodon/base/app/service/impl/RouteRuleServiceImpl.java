package io.choerodon.base.app.service.impl;import io.choerodon.base.api.vo.RouteRuleVO;import io.choerodon.base.app.service.RouteRuleService;import io.choerodon.base.infra.dto.RouteMemberRuleDTO;import io.choerodon.base.infra.dto.RouteRuleDTO;import io.choerodon.base.infra.dto.UserDTO;import io.choerodon.base.infra.mapper.RouteMemberRuleMapper;import io.choerodon.base.infra.mapper.RouteRuleMapper;import io.choerodon.base.infra.mapper.UserMapper;import io.choerodon.core.exception.CommonException;import io.choerodon.core.exception.ext.UpdateException;import org.springframework.beans.BeanUtils;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.util.ObjectUtils;import java.util.List;/** * RouteRuleServiceImpl * * @author pengyuhua * @date 2019/10/25 */@Servicepublic class RouteRuleServiceImpl implements RouteRuleService {    private static final String ERROR_ROUTE_RULE_INSERT_EXCEPTION = "error.route.rule.insert";    private static final String ERROR_ROUTE_RULE_UPDATE_EXCEPTION = "error.route.rule.update";    private static final String ERROR_ROUTE_MEMBER_RULE_DELETE_EXCEPTION = "error.route.member.rule.delete";    private static final String ERROR_ROUTE_MEMBER_RULE_INSERT_EXCEPTION = "error.route.member.rule.insert";    private static final String ERROR_USER_NOT_EXIST = "error.user.not.exist";    private RouteRuleMapper routeRuleMapper;    private RouteMemberRuleMapper routeMemberRuleMapper;    private UserMapper userMapper;    public RouteRuleServiceImpl(RouteRuleMapper routeRuleMapper, RouteMemberRuleMapper routeMemberRuleMapper, UserMapper userMapper) {        this.routeRuleMapper = routeRuleMapper;        this.routeMemberRuleMapper = routeMemberRuleMapper;        this.userMapper = userMapper;    }    @Override    public List<RouteRuleVO> listRouteRules(String code) {        return routeRuleMapper.listRouteRules(code);    }    @Override    public RouteRuleVO queryRouteRuleDetailById(Long id) {        // 查询路由详情        RouteRuleDTO routeRuleDTO = checkRouteRuleExist(new RouteRuleDTO().setId(id));        RouteRuleVO routeRuleVO = new RouteRuleVO();        BeanUtils.copyProperties(routeRuleDTO, routeRuleVO);        // 查询用户信息        routeRuleVO.setRouteMemberRuleDTOS(routeMemberRuleMapper.select(new RouteMemberRuleDTO().setRouteRuleCode(routeRuleVO.getCode())));        // 查询用户名信息        routeRuleVO.getRouteMemberRuleDTOS().forEach(v -> {            UserDTO userDTO = userMapper.selectByPrimaryKey(v.getUserId());            if (!ObjectUtils.isEmpty(userDTO)) {                v.setUserName(userDTO.getLoginName() + userDTO.getRealName());            }        });        // todo 查询路由配置的主机信息        return routeRuleVO;    }    @Override    @Transactional    public RouteRuleVO insertRouteRule(RouteRuleVO routeRuleVO) {        RouteRuleDTO routeRuleDTO = new RouteRuleDTO();        // 校验名称是否已经存在        checkCode(new RouteRuleDTO().setCode(routeRuleVO.getCode()));        // 拷贝插入信息        routeRuleDTO.setCode(routeRuleVO.getCode()).setDescription(routeRuleVO.getDescription());        // 插入路由信息        try {            if (routeRuleMapper.insert(routeRuleDTO) != 1) {                throw new CommonException(ERROR_ROUTE_RULE_INSERT_EXCEPTION);            }            // 获取添加的信息            routeRuleVO.setId(routeRuleDTO.getId()).setCode(routeRuleDTO.getCode()).setObjectVersionNumber(routeRuleDTO.getObjectVersionNumber());        } catch (Exception e) {            throw new CommonException(ERROR_ROUTE_RULE_INSERT_EXCEPTION, e);        }        // 添加路由配置用户信息        routeRuleVO.getRouteMemberRuleDTOS().forEach(v -> {            if (ObjectUtils.isEmpty(userMapper.selectByPrimaryKey(v.getUserId()))) {                throw new CommonException(ERROR_USER_NOT_EXIST);            }            v.setRouteRuleCode(routeRuleVO.getCode());            if (routeMemberRuleMapper.insert(v) != 1) {                throw new CommonException(ERROR_ROUTE_MEMBER_RULE_INSERT_EXCEPTION);            }        });        // TODO 添加路由配置主机信息        return routeRuleVO;    }    @Override    @Transactional    public Boolean deleteRouteRuleById(Long id) {        // 查询路由规则code信息        RouteRuleDTO routeRuleDTO = checkRouteRuleExist(new RouteRuleDTO().setId(id));        // 根据code删除引用的相关记录        // todo 删除主机引用信息        // 删除用户引用信息        List<RouteMemberRuleDTO> selectMemberRules = routeMemberRuleMapper.select(new RouteMemberRuleDTO().setRouteRuleCode(routeRuleDTO.getCode()));        selectMemberRules.forEach(v -> {            if (routeMemberRuleMapper.deleteByPrimaryKey(v.getId()) != 1) {                throw new CommonException(ERROR_ROUTE_MEMBER_RULE_DELETE_EXCEPTION);            }        });        // 删除路由信息        if (routeRuleMapper.deleteByPrimaryKey(id) != 1) {            throw new CommonException("error.route.rule.delete");        }        return true;    }    @Override    @Transactional    public RouteRuleVO updateRouteRule(RouteRuleVO routeRuleVO, Long objectVersionNumber) {        // 检查待更新路由是否存在        RouteRuleDTO routeRuleDTOSelect = checkRouteRuleExist(new RouteRuleDTO().setId(routeRuleVO.getId()));        // 获取更新信息 不允许更新code        routeRuleDTOSelect.setDescription(routeRuleVO.getDescription()).setObjectVersionNumber(objectVersionNumber);        // 更新信息 更新失败触发事务        try {            if (routeRuleMapper.updateByPrimaryKey(routeRuleDTOSelect) != 1) {                throw new UpdateException(ERROR_ROUTE_RULE_UPDATE_EXCEPTION);            }            routeRuleVO.setCode(routeRuleDTOSelect.getCode()).setObjectVersionNumber(routeRuleDTOSelect.getObjectVersionNumber());        } catch (Exception e) {            throw new UpdateException(ERROR_ROUTE_RULE_UPDATE_EXCEPTION, e);        }        // 更新配置用户信息 1. 删除已配置的用户信息        routeMemberRuleMapper.select(new RouteMemberRuleDTO().setRouteRuleCode(routeRuleVO.getCode())).forEach(v -> {            if (routeMemberRuleMapper.deleteByPrimaryKey(v.getId()) != 1) {                throw new CommonException(ERROR_ROUTE_MEMBER_RULE_DELETE_EXCEPTION);            }        });        // 2.重新添加配置用户信息        routeRuleVO.getRouteMemberRuleDTOS().forEach(v -> {            if (ObjectUtils.isEmpty(userMapper.selectByPrimaryKey(v.getUserId()))) {                throw new CommonException(ERROR_USER_NOT_EXIST);            }            v.setRouteRuleCode(routeRuleVO.getCode());            if (routeMemberRuleMapper.insert(v) != 1) {                throw new CommonException(ERROR_ROUTE_MEMBER_RULE_INSERT_EXCEPTION);            }        });        // todo 更新路由配置主机信息        return routeRuleVO;    }    @Override    public Boolean checkCode(RouteRuleDTO routeRuleDTO) {        if (!ObjectUtils.isEmpty(routeRuleMapper.selectOne(routeRuleDTO))) {            throw new CommonException("error.route.rule.code.already.exist");        }        return true;    }    @Override    public RouteRuleDTO checkRouteRuleExist(RouteRuleDTO routeRuleDTO) {        RouteRuleDTO routeRuleDTOSelect = routeRuleMapper.selectOne(routeRuleDTO);        if (ObjectUtils.isEmpty(routeRuleDTOSelect)) {            throw new CommonException("error.route.rule.not.exist");        }        return routeRuleDTOSelect;    }}
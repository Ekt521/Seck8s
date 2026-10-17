package com.seck8s.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seck8s.common.exception.BizException;
import com.seck8s.common.result.ResultCode;
import com.seck8s.common.util.JwtUtil;
import com.seck8s.user.entity.User;
import com.seck8s.user.mapper.UserMapper;
import com.seck8s.user.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public User register(String username, String password, String phone, String realName, String idCard) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名和密码不能为空");
        }
        if (this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
            throw new BizException(ResultCode.BIZ_ERROR, "用户名已存在");
        }
        if (StringUtils.hasText(phone)
                && this.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) != null) {
            throw new BizException(ResultCode.BIZ_ERROR, "手机号已被注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setRealName(realName);
        user.setIdCard(idCard);
        this.save(user);
        return user;
    }

    @Override
    public String login(String username, String password) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public User getProfile(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(Long userId, User updatedInfo) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 用户名/密码不允许通过资料接口修改，仅更新资料字段
        updatedInfo.setId(userId);
        updatedInfo.setUsername(null);
        updatedInfo.setPassword(null);
        this.updateById(updatedInfo);
    }
}

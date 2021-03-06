package com.platform.admin.service.controller;

import com.platform.admin.service.iface.UserService;
import com.platform.common.modal.ResultInfo;
import com.platform.common.modal.user.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Huangyonghao
 * @date 2019/8/29 15:03
 */

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/getUserByAccout")
    public ResultInfo<UserInfo> getUserByAccout(@RequestBody String userName) {
        return userService.getUserByAccout(userName);
    }
}

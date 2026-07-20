package com.synapxnet.mlopslogin.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import com.synapxnet.mlopslogin.entity.User;
import org.apache.ibatis.annotations.Update;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.util.List;

public interface UserMapper {

    //web传入phone查询数据库对应数据
    @Select("SELECT * FROM xnet_mlops_user_infos WHERE phone = #{phone}")
    User findByPhone(String phone);
}

package com.supermall.takeout.mapper;

import com.supermall.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    void insert(User user);

    User getByUsername(String username);

    User getById(Long id);

    void update(User user);
}

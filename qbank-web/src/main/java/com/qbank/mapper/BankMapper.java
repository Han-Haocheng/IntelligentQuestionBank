package com.qbank.mapper;

import com.qbank.entity.Bank;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BankMapper {

    int insert(Bank bank);

    int update(Bank bank);

    int deleteById(@Param("id") Long id);

    Bank findById(@Param("id") Long id);

    /** userId 为 null 时返回全部(管理员) */
    List<Bank> selectByUser(@Param("userId") Long userId);

    /** 订阅中的共享题库(指定用户共享 + 公开共享, 排除已退订) */
    List<Bank> selectShared(@Param("userId") Long userId);

    Bank findByName(@Param("userId") Long userId, @Param("name") String name);
}

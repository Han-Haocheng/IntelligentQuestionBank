package com.qbank.mapper;

import com.qbank.entity.Bank;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BankMapper {

    int insert(Bank bank);

    int update(Bank bank);

    int deleteById(@Param("id") Long id);

    Bank findById(@Param("id") Long id);

    List<Bank> selectByUser(@Param("userId") Long userId);

    Bank findByName(@Param("userId") Long userId, @Param("name") String name);
}

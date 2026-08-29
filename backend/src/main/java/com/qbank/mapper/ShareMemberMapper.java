package com.qbank.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 共享订阅状态(收件人侧)
 */
public interface ShareMemberMapper {

    /** 订阅/退订(存在则更新) */
    int upsert(@Param("shareId") Long shareId, @Param("userId") Long userId, @Param("subscribed") int subscribed);
}

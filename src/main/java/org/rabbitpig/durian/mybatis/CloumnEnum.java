package org.rabbitpig.durian.mybatis;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum CloumnEnum {
    // ID
    ID("id", "id"),
    // 创建时间
    CREATE_AT("createTime", "create_time"),
    // 创建人ID
    CREATE_BY("creatorId", "creator_id"),
    // 创建姓名
    CREATE_BY_NAME("creatorName", "creator_name"),
    // 修改时间
    UPDATE_AT("updateTime", "update_time"),
    // 修改人ID
    UPDATE_BY("updaterId", "updater_id"),
    // 修改人姓名
    UPDATE_BY_NAME("updaterName", "updater_name"),
    // 逻辑删除标识
    IS_DELETED("deleted", "deleted");

    @Getter
    private String clazz;

    @Getter
    private String sql;
}

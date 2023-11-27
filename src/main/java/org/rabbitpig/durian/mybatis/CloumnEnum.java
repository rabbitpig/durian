/*
 * Copyright 2013 rabbitpig <admin@rabbitpig.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

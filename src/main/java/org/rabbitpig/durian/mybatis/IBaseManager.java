package org.rabbitpig.durian.mybatis;

import java.util.Collection;
import java.util.List;

public interface IBaseManager<M> {

    Integer deleteByIds(M record, Long[] ids);

    Integer deleteByIds(M record, List<Long> ids);

    Integer deleteById(M record, long id);

    M findById(Long id);

    void save(M model);

    List<M> findByIds(Long[] ids);

    List<M> findByIds(List<Long> ids);

    int batchInsert(Collection<M> records);

    int batchUpdate(Collection<M> records);

    int batchUpdateSelective(Collection<M> records, Class<M> clazz);
}

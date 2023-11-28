package org.rabbitpig.durian.mybatis;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * manager 抽象类
 *
 * @author jj
 */
@Slf4j
public abstract class BaseManager<M> implements IBaseManager<M> {

    protected abstract Object getBaseMapper();

    protected abstract Object newExample();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteByIds(M record, Long[] ids) {
        return deleteByIds(record, Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteByIds(M record, List<Long> ids) {
        Integer result = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Object example = newExample();
            Object criteria = invoke(example, "createCriteria");
            invoke(criteria, "andIdIn", ids, List.class);
            result = (Integer) invoke(getBaseMapper(), "deleteByExample", new Object[]{record, example});
        }
        return result;
    }

    @Override
    public Integer deleteById(M record, long id) {
        return (Integer) invoke(getBaseMapper(), "deleteByPrimaryKey", new Object[]{record, id});
    }

    @SuppressWarnings("unchecked")
    @Override
    public M findById(Long id) {
        return (M) invoke(getBaseMapper(), "selectByPrimaryKey", id);
    }

    @Override
    public void save(M model) {

        Object object;
        try {
            object = PropertyUtils.getSimpleProperty(model, "id");
        } catch (Exception ex10) {
            throw new RuntimeException(ex10.getMessage(), ex10);
        }

        if (object != null) {
            invoke(getBaseMapper(), "updateByPrimaryKeySelective", model);
        } else {
            invoke(getBaseMapper(), "insertSelective", model);
        }
    }

    @Override
    public List<M> findByIds(Long[] ids) {
        List<M> result = null;
        if (ids != null && ids.length > 0) {
            findByIds(Arrays.asList(ids));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<M> findByIds(List<Long> ids) {
        List<M> result = null;
        if (ids != null && ids.size() > 0) {
            Object example = newExample();
            invoke(invoke(example, "createCriteria"), "andIdIn", ids, List.class);
            result = (List<M>) invoke(getBaseMapper(), "selectByExample", example);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchInsert(Collection<M> records) {
        int count = 0;
        if (records != null && records.size() > 0) {
            if (MethodUtils.getAccessibleMethod(getBaseMapper().getClass(), "insertBatch", List.class) != null) {
                int maxBatchSize = 300;
                List<M> recordList;
                if (records instanceof List) {
                    recordList = (List) records;
                } else {
                    recordList = new ArrayList<M>(records);
                }
                for (List<M> temp : Lists.partition(recordList, maxBatchSize)) {
                    log.debug(String.format("开始批量插入%d条数据", temp.size()));
                    count += (Integer) invoke(getBaseMapper(), "insertBatch", temp, List.class);
                }
            } else {
                for (M record : records) {
                    count += (Integer) invoke(getBaseMapper(), "insertSelective", record);
                }
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdate(Collection<M> records) {
        int count = 0;
        if (records != null && records.size() > 0) {
            if (MethodUtils.getAccessibleMethod(getBaseMapper().getClass(), "updateBatch", List.class) != null) {
                int maxBatchSize = 300;
                List<M> recordList;
                if (records instanceof List) {
                    recordList = (List) records;
                } else {
                    recordList = new ArrayList<M>(records);
                }
                for (List<M> temp : Lists.partition(recordList, maxBatchSize)) {
                    log.debug(String.format("开始批量更新%d条数据", records.size()));
                    count = (Integer) invoke(getBaseMapper(), "updateBatch", Lists.newArrayList(records), List.class);
                }
            } else {
                throw new UnsupportedOperationException("找不到updateBatch方法");
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateSelective(Collection<M> records, Class<M> clazz) {
        int count = 0;
        if (records != null && records.size() > 0) {
            if (MethodUtils.getAccessibleMethod(getBaseMapper().getClass(), "updateByPrimaryKeySelective", clazz) != null) {
                log.debug(String.format("开始批量更新%d条数据", records.size()));
                for (M record : records) {
                    count += (Integer) invoke(getBaseMapper(), "updateByPrimaryKeySelective", record, clazz);
                }
            } else {
                throw new UnsupportedOperationException("找不到updateByPrimaryKeySelective方法");
            }
        }
        return count;
    }

    private Object invoke(Object object, String methodName, Object arg) {
        return invoke(object, methodName, new Object[]{arg});
    }

    private Object invoke(Object object, String methodName, Object[] arg) {
        try {
            return MethodUtils.invokeExactMethod(object, methodName, arg);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new UnsupportedOperationException("invoke " + methodName + " error", e);
        }
    }

    /**
     * 对于方法中定义的参数为父类或者接口的，需要指定参数类型，否则会找不到方法
     *
     * @param object
     * @param methodName
     * @param arg
     * @param clazz
     * @return
     */
    private Object invoke(Object object, String methodName, Object arg, @SuppressWarnings("rawtypes") Class clazz) {
        try {
            return MethodUtils.invokeExactMethod(object, methodName, new Object[]{arg}, new Class[]{clazz});
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new UnsupportedOperationException("invoke " + methodName + " error", e);
        }
    }

    private Object invoke(Object object, String methodName) {
        try {
            return MethodUtils.invokeExactMethod(object, methodName, new Object[0], new Class[0]);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new UnsupportedOperationException("invoke " + methodName + " error", e);
        }
    }
}

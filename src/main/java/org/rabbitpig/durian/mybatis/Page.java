package org.rabbitpig.durian.mybatis;

import java.io.Serializable;

public class Page implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 分页查询开始记录位置.
     */
    private long offset;
    /**
     * 每页显示记录数.
     */
    private long limit = 20;

    public Page() {
    }

    public Page(long offset, long limit) {
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * @return the offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * @return the limit
     */
    public long getLimit() {
        return limit;
    }

    /**
     * @param limit the limit to set
     */
    public void setLimit(long limit) {
        this.limit = limit;
    }
}

package com.bbangpatrol.common.dto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * offset 을 그대로 들고 있는 Pageable.
 * PageRequest 는 offset 을 page * size 로만 표현할 수 있어, 페이징 도중 size 가 바뀌면
 * 같은 cursor 가 전혀 다른 위치를 가리킨다. cursor 를 "건너뛴 개수" 로 두면 그 문제가 없다.
 */
public final class OffsetPageable implements Pageable {

    private final long offset;
    private final int limit;

    public OffsetPageable(long offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset 은 0 이상이어야 한다");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit 은 1 이상이어야 한다");
        }
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        // 정렬은 @Query 의 ORDER BY 가 담당한다
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetPageable(offset + limit, limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageable(Math.max(offset - limit, 0), limit) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageable(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageable((long) pageNumber * limit, limit);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}

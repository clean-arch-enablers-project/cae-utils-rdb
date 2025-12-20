package com.cae.rdb.operations;

import com.cae.context.ExecutionContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public interface RetrievePaginatedOperation <E> {

    Page<E> retrievePaginated(Integer pageNumber, Integer pageSize);
    Page<E> retrievePaginated(Integer pageNumber, Integer pageSize, ExecutionContext executionContext);
    Page<E> retrievePaginated(Integer pageNumber, Integer pageSize, ExecutionContext executionContext, boolean transactional);

    @Getter
    @Setter
    class Page<E> {

        private int pageNumber;
        private int pageSize;
        private long totalItems;
        private List<E> items;
        private Boolean lastPage;

        public Page(int pageNumber, int pageSize, long totalItems, List<E> items) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalItems = totalItems;
            this.items = items;
            this.lastPage = Page.isLast(pageNumber, pageSize, totalItems);
        }

        public static Boolean isLast(int pageNumber, int pageSize, long totalItems){
            return pageNumber >= ((double) totalItems / (double) pageSize);
        }

        @Override
        public String toString() {
            return "Page{" +
                    "pageNumber=" + pageNumber +
                    ", pageSize=" + pageSize +
                    ", totalItems=" + totalItems +
                    ", items=" + items +
                    '}';
        }

    }
}

package com.equiron.acc.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class CouchDbIterator<E, K, T extends CouchDbAbstractMapQuery<K, ?, ?, ?, ?>> implements CouchDbIterable<E> {
    private Function<T, List<E>> f;

    private List<E> list = new ArrayList<>();

    private int index;

    private int batchSize;

    private int totalFetch;

    private T query;

    private int originalLimit;

    public CouchDbIterator(Function<T, List<E>> f, int batchSize, T query) {
        this.f = f;
        this.batchSize = batchSize + 1;//Hiddenly prefetch n+1 element for safe deletion elements during iteration.
        this.query = query;

        originalLimit = query.queryObject.getLimit();

        if (this.batchSize < originalLimit || originalLimit == 0) {
            query.limit(this.batchSize);
        }
    }

    @Override
    public boolean hasNext() {
        if (list.size() > index) return true;

        int left = originalLimit - totalFetch;

        if (query.getLastKeyDocId() != null) {//if it's not first request
            if (left == 0) return false;

            query.limit(Math.min(left, batchSize));
        }

        list = f.apply(query);

        if (!list.isEmpty()) {
            if (originalLimit == 0) originalLimit = query.getTotalRows();

            index = 0;

            if ((list.size() > 1) && (list.size() != originalLimit)) {//If it's not last element and we don't have all rows
                list.remove(list.size() - 1);//remove the last element for prevent it's deletion. Safe deletion elements during iteration.

                totalFetch += list.size();

                query.startKeyDocId(query.getLastKeyDocId()).startKey(query.getLastKey());
            } else {
                totalFetch += list.size();

                originalLimit = totalFetch; //hack for stop when we reach the last part
            }

            return true;
        }

        return false;
    }

    @Override
    public E next() {
        return hasNext() ? list.get(index++) : null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        return this;
    }
}

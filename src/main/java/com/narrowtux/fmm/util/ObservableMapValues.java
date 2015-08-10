package com.narrowtux.fmm.util;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ObservableMapValues<T> implements ObservableList<T> {
    private ObservableList<T> internalStore;
    private ObservableMap<?, T> referencedMap;

    public ObservableMapValues(ObservableMap<?, T> referencedMap) {
        this(FXCollections.observableArrayList(), referencedMap);
    }

    public ObservableMapValues(ObservableList<T> internalStore, ObservableMap<?, T> referencedMap) {
        this.internalStore = internalStore;
        this.referencedMap = referencedMap;

        referencedMap.addListener((MapChangeListener<Object, T>) change -> {
            if (change.wasAdded()) {
                internalStore.add(change.getValueAdded());
            }
            if (change.wasRemoved()) {
                internalStore.remove(change.getValueRemoved());
            }
        });
    }

    @Override
    public void addListener(ListChangeListener<? super T> listChangeListener) {
        internalStore.addListener(listChangeListener);
    }

    @Override
    public void removeListener(ListChangeListener<? super T> listChangeListener) {
        internalStore.removeListener(listChangeListener);
    }

    @Override
    public boolean addAll(T... ts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setAll(T... ts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(T... ts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(T... ts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(int i, int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilteredList<T> filtered(Predicate<T> predicate) {
        return internalStore.filtered(predicate);
    }

    @Override
    public SortedList<T> sorted(Comparator<T> comparator) {
        return internalStore.sorted(comparator);
    }

    @Override
    public SortedList<T> sorted() {
        return internalStore.sorted();
    }

    @Override
    public int size() {
        return internalStore.size();
    }

    @Override
    public boolean isEmpty() {
        return internalStore.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return internalStore.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return internalStore.iterator();
    }

    @Override
    public Object[] toArray() {
        return internalStore.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return internalStore.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return internalStore.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sort(Comparator<? super T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return internalStore.equals(o);
    }

    @Override
    public int hashCode() {
        return internalStore.hashCode();
    }

    @Override
    public T get(int index) {
        return internalStore.get(index);
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        return internalStore.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return internalStore.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return internalStore.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return internalStore.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return internalStore.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<T> spliterator() {
        return internalStore.spliterator();
    }

    @Override
    public void addListener(InvalidationListener invalidationListener) {
        internalStore.addListener(invalidationListener);
    }

    @Override
    public void removeListener(InvalidationListener invalidationListener) {
        internalStore.removeListener(invalidationListener);
    }
}

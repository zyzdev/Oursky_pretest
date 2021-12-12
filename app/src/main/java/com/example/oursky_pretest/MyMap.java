package com.example.oursky_pretest;

import android.os.SystemClock;
import android.util.Log;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Objects;

public class MyMap<K, V> {

    private static final boolean DEBUG = true;

    private final String dTag = getClass().getSimpleName();

    public MyMap(){}

    public MyMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        this.threshold = tableSizeFor(initialCapacity);
    }

    static class Node<K,V> implements Map.Entry<K,V> {
        private final String dTag = getClass().getSimpleName();

        final int hash;
        final K key;
        V value;
        final double weight;
        long last_access_time;
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next, double weight) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
            this.weight = weight;
            updateAccessTime();
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final void updateAccessTime() {
            this.last_access_time = System.currentTimeMillis();
        }

        public double getScore() {
            long current_time = System.currentTimeMillis();
            double score;
            Log.d(dTag, "current_time:" + current_time + ", last_access_time:" + last_access_time);
            if(current_time != last_access_time) {
                score = weight / Math.log(current_time - last_access_time + 1);
            } else score = weight / -100;

            return score;
        }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            this.last_access_time = SystemClock.currentThreadTimeMillis();
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    transient Node<K,V>[] table;

    transient int modCount;
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final float DEFAULT_LOAD_FACTOR = 1f;
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
    final float loadFactor = DEFAULT_LOAD_FACTOR;
    int threshold;
    transient int size;

    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (first = tab[(n - 1) & hash]) != null) {
            if (first.hash == hash && // always check first node
                    ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            if ((e = first.next) != null) {
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    public V get(K key) {
        log("get, key:" + key.toString());
        Node<K,V> e = getNode(hash(key), key);
        if(e != null) afterNodeAccess(e);
        return e == null ? null : e.value;
    }

    public V put(K key, V value, double weight) {
        log("put, key:" + key.toString() + " ,value:" + value.toString() + " ,weight:" + weight);
        return putVal(hash(key), key, value, false, true, weight);
    }

    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict, double weight) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;

        ++modCount;
        if (++size > threshold) {
            removeLowestScoreNode();
        }

        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null, weight);
        else {
            Node<K,V> e; K k;
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            else {
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null, weight);
                        break;
                    }
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }

        afterNodeInsertion(evict);
        return null;
    }

    public V remove(K key) {
        log("remove, key:" + key.toString());
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
                null : e.value;
    }

    final Node<K,V> removeNode(int hash, K key, V value,
                               boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (p = tab[index = (n - 1) & hash]) != null) {
            Node<K,V> node = null, e; K k; V v;
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            else if ((e = p.next) != null) {
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key ||
                                    (key != null && key.equals(k)))) {
                        node = e;
                        break;
                    }
                    p = e;
                } while ((e = e.next) != null);
            }
            if (node != null && (!matchValue || (v = node.value) == value ||
                    (value != null && value.equals(v)))) {
                if (node == p)
                    tab[index] = node.next;
                else
                    p.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }

    private void removeLowestScoreNode() {
        //find the node with lowest score
        double smallestScore = 0;
        K keyShouldRemove = null;
        Node<K,V>[] tab;
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            // Android-changed: Detect changes to modCount early.
            for (int i = 0; (i < tab.length && mc == modCount); ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    double score = e.getScore();
                    if(keyShouldRemove == null) {
                        smallestScore = score;
                        keyShouldRemove = e.key;
                    } else if(score < smallestScore){
                        smallestScore = score;
                        keyShouldRemove = e.key;
                    }
                    log("removeLowestScoreNode, smallestKey:" + keyShouldRemove + " smallestScore," + smallestScore + " key:" + e.key + ", score:" + score);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
        log("removeLowestScoreNode, keyShouldRemove:" + keyShouldRemove + " smallestScore," + smallestScore);
        if(keyShouldRemove != null) remove(keyShouldRemove);
    }

    Node<K,V> newNode(int hash, K key, V value, Node<K,V> next, double weight) {
        return new Node<>(hash, key, value, next, weight);
    }
    void afterNodeAccess(Node<K,V> p) {
        p.updateAccessTime();
    }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }

    final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                    (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else { // preserve order
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

    private void log(String log) {
        if(DEBUG && log != null) Log.d(dTag, log);
    }
}

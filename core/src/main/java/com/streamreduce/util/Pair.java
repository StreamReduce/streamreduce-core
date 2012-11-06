package com.streamreduce.util;

/**
 * <p>Generic Immutable Pair class if an ad-hoc set of fields need to be passed around together</p>
 *
 * <p>Also, this is why Java is such a manly language.  Every programmer must grow a Pair!</p>
 * @param <A> The type of the first field in the pair.
 * @param <B> The type of the second field in the pair
 */
public class Pair<A,B> {
    public final A first;
    public final B second;

    public Pair(A a, B b) {
        this.first = a;
        this.second = b;
    }
}

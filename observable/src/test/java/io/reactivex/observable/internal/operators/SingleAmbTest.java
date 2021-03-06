/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.observable.internal.operators;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.common.*;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.*;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.*;

public class SingleAmbTest {
    @Test
    public void ambWithFirstFires() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> ts = pp1.single(-99).ambWith(pp2.single(-99)).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        ts.assertResult(1);

    }

    @Test
    public void ambWithSecondFires() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> ts = pp1.single(-99).ambWith(pp2.single(-99)).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        ts.assertResult(2);
    }

    @SuppressWarnings("unchecked")
    @Test(timeout = 1000)
    public void ambIterableWithFirstFires() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        List<Single<Integer>> singles = Arrays.asList(pp1.single(-99), pp2.single(-99));
        TestObserver<Integer> ts = Single.amb(singles).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        ts.assertResult(1);

    }

    @SuppressWarnings("unchecked")
    @Test(timeout = 1000)
    public void ambIterableWithSecondFires() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        List<Single<Integer>> singles = Arrays.asList(pp1.single(-99), pp2.single(-99));
        TestObserver<Integer> ts = Single.amb(singles).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        ts.assertResult(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayEmpty() {
        Single.ambArray()
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambSingleSource() {
        assertSame(Single.never(), Single.ambArray(Single.never()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void error() {
        Single.ambArray(Single.error(new TestException()), Single.just(1))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void nullSourceSuccessRace() {
        for (int i = 0; i < 1000; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();

            try {

                final Subject<Integer> ps = ReplaySubject.create();
                ps.onNext(1);

                @SuppressWarnings("unchecked")
                final Single<Integer> source = Single.ambArray(ps.singleOrError(), Single.<Integer>never(), Single.<Integer>never(), null);

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        source.test();
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps.onComplete();
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertError(errors, 0, NullPointerException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void multipleErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();

            try {

                final Subject<Integer> ps1 = PublishSubject.create();
                final Subject<Integer> ps2 = PublishSubject.create();

                Single.ambArray(ps1.singleOrError(), ps2.singleOrError()).test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void successErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();

            try {

                final Subject<Integer> ps1 = PublishSubject.create();
                final Subject<Integer> ps2 = PublishSubject.create();

                Single.ambArray(ps1.singleOrError(), ps2.singleOrError()).test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onNext(1);
                        ps1.onComplete();
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @Test
    public void manySources() {
        Single<?>[] sources = new Single[32];
        Arrays.fill(sources, Single.never());
        sources[31] = Single.just(31);

        Single.amb(Arrays.asList(sources))
        .test()
        .assertResult(31);
    }

    @Test
    public void ambWithOrder() {
        Single<Integer> error = Single.error(new RuntimeException());
        Single.just(1).ambWith(error).test().assertValue(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterableOrder() {
        Single<Integer> error = Single.error(new RuntimeException());
        Single.amb(Arrays.asList(Single.just(1), error)).test().assertValue(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayOrder() {
        Single<Integer> error = Single.error(new RuntimeException());
        Single.ambArray(Single.just(1), error).test().assertValue(1);
    }
}
